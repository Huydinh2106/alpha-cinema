const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function run() {
    const moviesSnap = await db.collection('movies').get();
    
    let categories = {};
    let countries = {};
    
    moviesSnap.docs.forEach(doc => {
        const data = doc.data();
        const cats = data.categories || [];
        const countryList = data.countries || [];
        
        cats.forEach(c => {
            categories[c] = (categories[c] || 0) + 1;
        });
        
        countryList.forEach(c => {
            countries[c] = (countries[c] || 0) + 1;
        });
    });

    const sortedCats = Object.entries(categories).sort((a, b) => b[1] - a[1]);
    const sortedCountries = Object.entries(countries).sort((a, b) => b[1] - a[1]);

    console.log("Top Categories:");
    sortedCats.slice(0, 20).forEach(([c, n]) => console.log(`${c}: ${n}`));
    console.log("\nTop Countries:");
    sortedCountries.slice(0, 10).forEach(([c, n]) => console.log(`${c}: ${n}`));
    
    process.exit(0);
}

run();
