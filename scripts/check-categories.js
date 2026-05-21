const { db } = require('./firebaseAdmin');

async function checkCategories() {
    const snap = await db.collection('movies').limit(50).get();
    const allCats = new Set();
    const typeSet = new Set();
    snap.docs.forEach(doc => {
        const d = doc.data();
        typeSet.add(d.type);
        if (d.categories) d.categories.forEach(c => allCats.add(c));
    });
    console.log('Types found:', [...typeSet]);
    console.log('Categories found:', [...allCats].sort());
    process.exit(0);
}
checkCategories();
