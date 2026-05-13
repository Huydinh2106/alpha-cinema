const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function checkKids() {
    const snap = await db.collection('movies').where('isKidsFriendly', '==', true).get();
    console.log("Total kids movies:", snap.docs.length);
    if(snap.docs.length > 0) {
        console.log("Sample:", snap.docs[0].id, snap.docs[0].data().title);
    }
    process.exit(0);
}

checkKids();
