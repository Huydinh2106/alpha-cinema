const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function test() {
    const cat = await db.collection('home_categories').doc('phim-bo-moi').get();
    console.log("Cat exists:", cat.exists);
    const slugs = cat.data().movieSlugs;
    console.log("Slugs:", slugs.length);
    
    const chunk = slugs.slice(0, 10);
    console.log("Chunk:", chunk);
    
    const snap = await db.collection('movies').where(admin.firestore.FieldPath.documentId(), 'in', chunk).get();
    console.log("Found movies:", snap.docs.length);
    process.exit(0);
}
test();
