const { admin, db } = require('./firebaseAdmin');
const https = require('https');

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

async function addKoreanKidsMovies() {
    const keywords = ['pororo', 'tayo', 'poli', 'pinkfong', 'larva', 'tobot', 'carbot', 'hello jadoo', 'miniforce'];
    const newSlugs = [];

    for (const kw of keywords) {
        console.log(`Searching for keyword: ${kw}...`);
        const listUrl = `https://phimapi.com/v1/api/tim-kiem?keyword=${encodeURIComponent(kw)}&limit=10`;
        
        let listData;
        try {
            listData = await fetchJson(listUrl);
        } catch(e) {
            console.error(`Failed to fetch list for ${kw}`, e);
            continue;
        }

        const items = (listData.data && listData.data.items) ? listData.data.items : (listData.items ? listData.items : []);
        console.log(`Found ${items.length} items for ${kw}.`);

        for (const item of items) {
            const slug = item.slug;
            const docSnap = await db.collection('movies').doc(slug).get();
            if (docSnap.exists) {
                console.log(`Skipping ${slug}, already in DB.`);
                // We might still want to add it to the home_category if it's not there
                if (!newSlugs.includes(slug)) newSlugs.push(slug);
                continue;
            }

            console.log(`Fetching details for ${slug}...`);
            let detailData;
            try {
                detailData = await fetchJson(`https://phimapi.com/phim/${slug}`);
            } catch(e) {
                console.error(`Failed to fetch detail for ${slug}`);
                continue;
            }

            if (!detailData.movie) continue;
            const m = detailData.movie;

            const categories = m.category ? m.category.map(c => c.name) : [];
            const countries = m.country ? m.country.map(c => c.name) : [];
            
            // Check if it's actually animation or kids
            const isKids = categories.some(c => c.toLowerCase().includes('hoạt hình') || c.toLowerCase().includes('gia đình'));
            // If it's not kids friendly, skip
            if (!isKids) {
                 console.log(`Skipping ${slug}, not kids friendly.`);
                 continue;
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
                categories: categories,
                countries: countries,
                actors: m.actor || [],
                directors: m.director || [],
                searchKeywords: Array.from(searchKeywords),
                ageRating: "P",
                isKidsFriendly: true,
                modifiedTime: admin.firestore.FieldValue.serverTimestamp()
            };

            await db.collection('movies').doc(slug).set(firestoreMovie);
            console.log(`Saved ${slug} to Firestore.`);
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
            
            // Add to 'phim-han-quoc', 'phim-hoat-hinh', 'anime-moi'
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

addKoreanKidsMovies();
