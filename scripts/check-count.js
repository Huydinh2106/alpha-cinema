const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function checkCount() {
    const res = await db.collection('movies').where('title', '>=', 'Phòng Thí Nghiệm').where('title', '<=', 'Phòng Thí Nghiệm\uf8ff').get();
    res.docs.forEach(d => console.log('ID:', d.id, 'TITLE:', d.data().title));
    process.exit(0);
}
checkCount();
