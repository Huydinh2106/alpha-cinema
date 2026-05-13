const admin = require('firebase-admin');
const path = require('path');
const https = require('https');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

function fetchJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch(e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

async function fetchKoreanKids() {
    const newSlugs = [];
    
    // We will just fetch the first 10 pages of Korean movies and pick out the animated/kids ones
    for (let page = 1; page <= 5; page++) {
        console.log(`Fetching Korean movies page ${page}...`);
        const listUrl = `https://phimapi.com/v1/api/quoc-gia/han-quoc?page=${page}&limit=30`;
        
        let listData;
        try {
            listData = await fetchJson(listUrl);
        } catch(e) {
            console.error(`Failed to fetch list on page ${page}`, e);
            continue;
        }

        const items = (listData.data && listData.data.items) ? listData.data.items : (listData.items ? listData.items : []);
        console.log(`Page ${page}: found ${items.length} items`);
        
        for (const item of items) {
            const slug = item.slug;
            
            // Check if it looks like animation from the list item type
            // (Often, type is not set to hoathinh in the list, but let's check)
            
            console.log(`Fetching details for ${slug}...`);
            let detailData;
            try {
                detailData = await fetchJson(`https://phimapi.com/phim/${slug}`);
            } catch(e) {
                continue;
            }

            if (!detailData.movie) continue;
            const m = detailData.movie;

            const categories = m.category ? m.category.map(c => c.name.toLowerCase()) : [];
            const countries = m.country ? m.country.map(c => c.name.toLowerCase()) : [];
            
            const isKorean = countries.some(c => c.includes('hàn'));
            const isKids = categories.some(c => c.includes('hoạt hình') || c.includes('gia đình'));
            
            if (!isKorean || !isKids) {
                 continue; // skip
            }
            
            // Generate keywords
            const searchKeywords = new Set();
            const addWords = (str) => {
                if (!str) return;
                const norm = str.toLowerCase().trim();
                if (norm) {
                    searchKeywords.add(norm);
                    norm.split(/\s+/).forEach(w => { if (w.length > 1) searchKeywords.add(w) });
                }
            };
            addWords(m.name);
            addWords(m.origin_name);

            let posterUrl = m.poster_url || "";
            let thumbUrl = m.thumb_url || "";
            const domain = (listData.data && listData.data.APP_DOMAIN_CDN_IMAGE) ? listData.data.APP_DOMAIN_CDN_IMAGE : "https://img.phimapi.com";
            
            if (posterUrl && !posterUrl.startsWith('http')) {
                 posterUrl = `${domain}/uploads/movies/${posterUrl}`;
            }
            if (thumbUrl && !thumbUrl.startsWith('http')) {
                 thumbUrl = `${domain}/uploads/movies/${thumbUrl}`;
            }

            const firestoreMovie = {
                slug: m.slug,
                title: m.name,
                originName: m.origin_name || "",
                type: m.type || "hoathinh",
                status: m.status || "completed",
                posterUrl: posterUrl,
                thumbUrl: thumbUrl,
                year: parseInt(m.year) || 0,
                content: m.content || "",
                categories: m.category ? m.category.map(c => c.name) : [],
                countries: m.country ? m.country.map(c => c.name) : [],
                actors: m.actor || [],
                directors: m.director || [],
                searchKeywords: Array.from(searchKeywords),
                ageRating: "P",
                isKidsFriendly: true,
                modifiedTime: admin.firestore.FieldValue.serverTimestamp()
            };

            await db.collection('movies').doc(slug).set(firestoreMovie);
            console.log(`Saved KOREAN KIDS MOVIE ${slug} to Firestore.`);
            if (!newSlugs.includes(slug)) newSlugs.push(slug);
        }
    }

    if (newSlugs.length > 0) {
        console.log("Updating home_categories...");
        const catsSnap = await db.collection('home_categories').get();
        for (const doc of catsSnap.docs) {
            const data = doc.data();
            const id = doc.id;
            let updated = false;
            
            // Add to 'phim-han-quoc', 'phim-hoat-hinh', 'phim-bo-moi'
            if (id === 'phim-han-quoc' || id === 'phim-hoat-hinh' || id === 'phim-bo-moi') {
                for (const slug of newSlugs) {
                    if (!data.movieSlugs.includes(slug)) {
                        data.movieSlugs.unshift(slug); // add to front
                        updated = true;
                    }
                }
            }
            
            if (updated) {
                await db.collection('home_categories').doc(id).update({ movieSlugs: data.movieSlugs });
                console.log(`Updated category ${id} with new Korean kids movies.`);
            }
        }
    }

    console.log("Done!");
    process.exit(0);
}

fetchKoreanKids();
