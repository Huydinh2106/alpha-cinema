const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function checkRatings() {
    const res = await db.collection('movies').get();
    const counts = {};
    res.docs.forEach(d => {
        let age = d.data().ageRating || 'undefined';
        counts[age] = (counts[age] || 0) + 1;
    });
    console.log('Age rating counts:', counts);
    process.exit(0);
}
checkRatings();
