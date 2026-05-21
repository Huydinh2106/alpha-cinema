const { db } = require('./firebaseAdmin');

async function run() {
    console.log("Fetching all movies...");
    const moviesSnap = await db.collection('movies').get();
    
    let movies = [];
    moviesSnap.docs.forEach(doc => {
        movies.push({ slug: doc.id, data: doc.data(), ref: doc.ref });
    });
    
    // Sort to get the latest first (if year is available)
    movies.sort((a, b) => {
        const yearA = a.data.year || 0;
        const yearB = b.data.year || 0;
        return yearB - yearA;
    });

    console.log("Updating isKidsFriendly flag...");
    let updateCount = 0;
    
    for (const item of movies) {
        const data = item.data;
        const cats = data.categories || [];
        const age = data.ageRating || '';
        
        let shouldBeKidsFriendly = false;
        
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');
        const isGiaDinh = cats.includes('Gia Đình') || cats.includes('Trẻ Em') || cats.includes('Thiếu Nhi');
        
        const isMatureAge = ['16+', '18+', 'C18', 'C16'].includes(age);
        const hasSensitiveCats = cats.includes('Kinh Dị') || cats.includes('Tình Cảm') || cats.includes('Tội Phạm');
        
        if (!isMatureAge && !hasSensitiveCats) {
            if (isHoatHinh || isGiaDinh) {
                shouldBeKidsFriendly = true;
            }
        }
        
        // Keep existing kids friendly flag if it's already set manually
        if (shouldBeKidsFriendly && !data.isKidsFriendly) {
            await item.ref.update({ isKidsFriendly: true });
            updateCount++;
            data.isKidsFriendly = true; // update local copy for categorization
        }
    }
    console.log(`Updated isKidsFriendly flag for ${updateCount} movies.`);

    // Now categorize them into 6 lists
    const kidsHoatHinh = [];
    const kidsAnime = [];
    const kidsGiaDinh = [];
    const kidsPhieuLuu = [];
    const kidsHaiHuoc = [];
    const kidsKhoaHoc = [];

    movies.forEach(item => {
        const data = item.data;
        if (!data.isKidsFriendly) return;
        
        const slug = item.slug;
        const cats = data.categories || [];
        const countries = data.countries || [];
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');

        if (isHoatHinh) {
            kidsHoatHinh.push(slug);
            if (countries.includes('Nhật Bản') || cats.includes('Anime')) {
                kidsAnime.push(slug);
            }
        }
        
        if (cats.includes('Gia Đình') || cats.includes('Trẻ Em') || cats.includes('Thiếu Nhi')) {
            kidsGiaDinh.push(slug);
        }
        
        if (cats.includes('Phiêu Lưu') || cats.includes('Hành Động') || cats.includes('Viễn Tưởng')) {
            kidsPhieuLuu.push(slug);
        }
        
        if (cats.includes('Hài Hước')) {
            kidsHaiHuoc.push(slug);
        }
        
        if (cats.includes('Khoa Học') || cats.includes('Bí Ẩn') || cats.includes('Tài Liệu')) {
            kidsKhoaHoc.push(slug);
        }
    });

    const categoriesToSeed = [
        { id: "kids-hoat-hinh", title: "Thế giới Hoạt Hình", movieSlugs: kidsHoatHinh.slice(0, 200) },
        { id: "kids-anime", title: "Anime dễ thương", movieSlugs: kidsAnime.slice(0, 200) },
        { id: "kids-gia-dinh", title: "Phim Gia Đình ấm áp", movieSlugs: kidsGiaDinh.slice(0, 200) },
        { id: "kids-phieu-luu", title: "Khám phá & Phiêu lưu", movieSlugs: kidsPhieuLuu.slice(0, 200) },
        { id: "kids-hai-huoc", title: "Phim Hài Hước vui nhộn", movieSlugs: kidsHaiHuoc.slice(0, 200) },
        { id: "kids-khoa-hoc", title: "Khoa học & Bí ẩn", movieSlugs: kidsKhoaHoc.slice(0, 200) }
    ];

    console.log("Seeding Kids categories into Firestore...");
    for (const cat of categoriesToSeed) {
        await db.collection('home_categories').doc(cat.id).set(cat);
        console.log(`Created category: ${cat.title} (${cat.movieSlugs.length} movies)`);
    }

    console.log("Done!");
    process.exit(0);
}

run();
