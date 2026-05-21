const { db } = require('./firebaseAdmin');

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
