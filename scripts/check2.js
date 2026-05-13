const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function check() {
    const snap = await db.collection('movies').where('title', '==', 'Thời Vàng Son').get();
    snap.docs.forEach(doc => {
        const data = doc.data();
        console.log(data.title, "- type:", data.type, "- categories:", data.categories, "- isKidsFriendly:", data.isKidsFriendly);
    });
    
    console.log("-----");
    
    const snap2 = await db.collection('home_categories').doc('phim-hoat-hinh').get();
    if(snap2.exists) {
        console.log("phim-hoat-hinh movieSlugs:");
        console.log(snap2.data().movieSlugs);
    }

    process.exit(0);
}
check();
