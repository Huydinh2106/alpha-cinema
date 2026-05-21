const { db } = require('./firebaseAdmin');

async function checkKids() {
    const snap = await db.collection('movies').where('isKidsFriendly', '==', true).get();
    console.log("Total kids movies:", snap.docs.length);
    if(snap.docs.length > 0) {
        console.log("Sample:", snap.docs[0].id, snap.docs[0].data().title);
    }
    process.exit(0);
}

checkKids();
