const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function run() {
    const moviesSnap = await db.collection('movies').get();
    
    let candidates = [];
    moviesSnap.docs.forEach(doc => {
        const data = doc.data();
        const cats = data.categories || [];
        const age = data.ageRating || '';
        
        const isMatureAge = ['16+', '18+', 'C18', 'C16'].includes(age);
        const hasSensitiveCats = cats.includes('Kinh Dị') || cats.includes('Tình Cảm') || cats.includes('Tội Phạm');
        
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');
        const isGiaDinh = cats.includes('Gia Đình') || cats.includes('Trẻ Em') || cats.includes('Thiếu Nhi');
        
        if (!isMatureAge && !hasSensitiveCats && (isHoatHinh || isGiaDinh)) {
            candidates.push({
                slug: doc.id,
                title: data.name || data.title,
                origin_name: data.origin_name,
                categories: cats.join(', '),
                ageRating: age,
                content: (data.content || '').substring(0, 150).replace(/\n/g, ' ') // Short desc
            });
        }
    });

    fs.writeFileSync('candidates.json', JSON.stringify(candidates, null, 2));
    console.log(`Dumped ${candidates.length} candidates to candidates.json`);
    process.exit(0);
}

run();
