const { db } = require('./firebaseAdmin');

async function checkCount() {
    const res = await db.collection('movies').where('title', '>=', 'Phòng Thí Nghiệm').where('title', '<=', 'Phòng Thí Nghiệm\uf8ff').get();
    res.docs.forEach(d => console.log('ID:', d.id, 'TITLE:', d.data().title));
    process.exit(0);
}
checkCount();
