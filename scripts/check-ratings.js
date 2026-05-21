const { db } = require('./firebaseAdmin');

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
