const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function check() {
    const snap = await db.collection('movies').get();
    let ageRatings = {};
    
    snap.docs.forEach(doc => {
        const data = doc.data();
        const cats = data.categories || [];
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');
        
        if (isHoatHinh) {
            const age = data.ageRating || 'Unknown';
            ageRatings[age] = (ageRatings[age] || 0) + 1;
        }
    });
    
    console.log("Animation Age Ratings:");
    Object.entries(ageRatings).forEach(([age, count]) => console.log(`${age}: ${count}`));

    process.exit(0);
}
check();
