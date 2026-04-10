const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Path to your service account key
const serviceAccountPath = path.resolve(__dirname, '../service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
    console.error(`ERROR: cannot find service account at ${serviceAccountPath}`);
    console.error('Please download your service-account.json from Firebase Console -> Project Settings -> Service Accounts');
    process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// API Endpoints
const API_BASE = "https://phimapi.com";
const LIST_API = `${API_BASE}/danh-sach/phim-moi-cap-nhat`;
const DETAIL_API = `${API_BASE}/phim/`;

/**
 * Generate an array of lowercase keywords from a title.
 * Used for basic searching in Firestore using array-contains
 */
function generateKeywords(title, originName) {
    const keywords = new Set();
    const addWords = (str) => {
        if (!str) return;
        const normalized = str.toLowerCase().trim();
        keywords.add(normalized);
        const tokens = normalized.split(/\s+/);
        tokens.forEach(t => {
            if (t.length > 1) keywords.add(t);
        });
        
        // Also add substrings for prefix like search
        if (tokens.length > 1) {
            for (let i = 0; i < tokens.length; i++) {
                let phrase = tokens.slice(0, i+1).join(' ');
                keywords.add(phrase);
            }
        }
    };
    
    addWords(title);
    addWords(originName);
    return Array.from(keywords);
}

/**
 * Determine age rating and kids friendly flag based on categories
 * @returns {ageRating: string, isKidsFriendly: boolean}
 */
function evaluateAgeRating(categoriesArr) {
    let ageRating = "13+"; // Default
    let isKidsFriendly = false;
    
    if (!categoriesArr || categoriesArr.length === 0) {
        return { ageRating, isKidsFriendly };
    }
    
    const catNames = categoriesArr.map(c => c.name ? c.name.toLowerCase() : "");
    
    // Check 18+ strict
    const isAdult = catNames.some(c => 
        c.includes('18+') || c.includes('kinh dị') || c.includes('người lớn') || c.includes('tình cảm')
    );
    
    // Check 16+
    const isMature = catNames.some(c => 
        c.includes('chiến tranh') || c.includes('bạo lực') || c.includes('hành động') || c.includes('giật gân') || c.includes('tội phạm')
    );
    
    // Check Kids
    const isKids = catNames.some(c => 
        c.includes('hoạt hình') || c.includes('gia đình') || c.includes('học đường') || c.includes('âm nhạc')
    );
    
    if (isAdult) {
        ageRating = "18+";
    } else if (isMature) {
        ageRating = "16+";
    } else if (isKids) {
        ageRating = "G";
        isKidsFriendly = true;
    }
    
    return { ageRating, isKidsFriendly };
}

// Ensure full URL
function getFullUrl(url) {
    if (!url) return "";
    if (url.startsWith("http")) return url;
    return `https://phimimg.com/${url}`;
}

async function startMigration() {
    console.log("Starting script to fetch 500 movies...");
    const moviesToProcess = [];
    
    // Fetch pages until we get 500
    let target = 500;
    let page = 1;
    while(moviesToProcess.length < target) {
        console.log(`Fetching page ${page} of list...`);
        try {
            const res = await fetch(`${LIST_API}?page=${page}`);
            const data = await res.json();
            
            if (!data.status || !data.items || data.items.length === 0) {
                console.log("No more items found in page", page);
                break;
            }
            
            for (const item of data.items) {
                moviesToProcess.push(item);
                if (moviesToProcess.length >= target) break;
            }
            
            page++;
        } catch (e) {
            console.error("Error fetching list:", e);
            break;
        }
    }
    
    console.log(`Found ${moviesToProcess.length} movies. Storing to Firestore...`);
    
    let count = 0;
    for (const baseItem of moviesToProcess) {
        // Fetch detail to get full info (categories, actors, description, content)
        console.log(`Fetching detail ${count+1}/${moviesToProcess.length} - ${baseItem.slug}...`);
        try {
            const detailRes = await fetch(`${DETAIL_API}${baseItem.slug}`);
            const detailData = await detailRes.json();
            
            if (!detailData.status || !detailData.movie) {
                console.warn(`Movie detail not found for slug: ${baseItem.slug}. Skipping...`);
                continue;
            }
            
            const m = detailData.movie;
            const categories = m.category || [];
            
            const ratingInfo = evaluateAgeRating(categories);
            const keywords = generateKeywords(m.name, m.origin_name);
            
            const movieDoc = {
                title: m.name || "",
                originName: m.origin_name || "",
                slug: m.slug || "",
                type: m.type || "single",
                status: m.status || "completed",
                posterUrl: getFullUrl(m.poster_url),
                thumbUrl: getFullUrl(m.thumb_url),
                year: parseInt(m.year) || 0,
                content: m.content || "",
                categories: categories.map(c => c.name), // Save as array of strings
                countries: (m.country || []).map(c => c.name),
                actors: m.actor || [],
                directors: m.director || [],
                searchKeywords: keywords,
                ageRating: ratingInfo.ageRating,
                isKidsFriendly: ratingInfo.isKidsFriendly,
                modifiedTime: admin.firestore.FieldValue.serverTimestamp()
            };
            
            // Push to Firebase
            await db.collection("movies").doc(movieDoc.slug).set(movieDoc);
            console.log(`Saved: ${movieDoc.title} [${movieDoc.ageRating}]`);
            count++;
            
            // Tiny sleep to avoid abusing API rate limits
            await new Promise(r => setTimeout(r, 200));
        } catch(e) {
            console.error(`Error processing ${baseItem.slug}:`, e);
        }
    }
    
    console.log(`Successfully imported ${count} movies to Firestore.`);
    console.log(`Done!`);
    process.exit(0);
}

startMigration();
