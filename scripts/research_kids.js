const { db } = require('./firebaseAdmin');

async function check() {
    const snap = await db.collection('movies').where('isKidsFriendly', '==', true).get();
    let categoryCounts = {};
    let countryCounts = {};
    
    snap.docs.forEach(doc => {
        const data = doc.data();
        const cats = data.categories || [];
        const countries = data.countries || [];
        
        cats.forEach(c => {
            categoryCounts[c] = (categoryCounts[c] || 0) + 1;
        });
        
        countries.forEach(c => {
            countryCounts[c] = (countryCounts[c] || 0) + 1;
        });
    });
    
    // Sort categories by count
    const sortedCats = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1]);
    const sortedCountries = Object.entries(countryCounts).sort((a, b) => b[1] - a[1]);
    
    console.log("Top Kids Categories:");
    sortedCats.slice(0, 20).forEach(([cat, count]) => console.log(`${cat}: ${count}`));
    
    console.log("\nTop Kids Countries:");
    sortedCountries.slice(0, 10).forEach(([c, count]) => console.log(`${c}: ${count}`));

    process.exit(0);
}
check();
