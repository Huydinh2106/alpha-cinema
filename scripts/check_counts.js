const { db } = require('./firebaseAdmin');

async function check() {
    const snap = await db.collection('movies').get();
    let hoathinhCount = 0;
    let animeCount = 0;
    let hoathinh3dCount = 0;
    let chieurapCount = 0;
    
    snap.docs.forEach(doc => {
        const data = doc.data();
        const cats = data.categories || [];
        const countries = data.countries || [];
        
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');
        if (isHoatHinh) {
            hoathinhCount++;
            if (countries.includes('Nhật Bản')) {
                animeCount++;
            }
            if (countries.includes('Trung Quốc') || cats.includes('3D')) {
                hoathinh3dCount++;
            }
        }
        if (cats.includes('Chiếu Rạp') || cats.includes('Phim Chiếu Rạp')) {
            chieurapCount++;
        }
    });
    
    console.log("Total Hoat Hinh:", hoathinhCount);
    console.log("Total Anime:", animeCount);
    console.log("Total Hoat Hinh 3D (Trung Quoc/3D):", hoathinh3dCount);
    console.log("Total Chieu Rap:", chieurapCount);

    process.exit(0);
}
check();
