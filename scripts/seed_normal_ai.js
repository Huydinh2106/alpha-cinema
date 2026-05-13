const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

async function run() {
    console.log("Fetching all movies...");
    const moviesSnap = await db.collection('movies').get();
    
    let movies = [];
    moviesSnap.docs.forEach(doc => {
        movies.push({ slug: doc.id, data: doc.data() });
    });
    
    // Sort to get the latest first (if year is available)
    movies.sort((a, b) => {
        const yearA = a.data.year || 0;
        const yearB = b.data.year || 0;
        return yearB - yearA;
    });

    // 8 new categories
    const normalHanhDong = []; // Hành Động, Phiêu Lưu
    const normalTinhCam = []; // Tình Cảm
    const normalAuMy = []; // Âu Mỹ
    const normalHinhSu = []; // Hình Sự, Bí Ẩn, Trinh Thám
    const normalVienTuong = []; // Viễn Tưởng, Khoa Học
    const normalHaiHuoc = []; // Hài Hước
    const normalKinhDi = []; // Kinh Dị
    const normalCoTrang = []; // Cổ Trang, Thần Thoại

    movies.forEach(item => {
        const data = item.data;
        const slug = item.slug;
        const cats = data.categories || [];
        const countries = data.countries || [];
        
        // 1. Hành Động Khai Mở Nhãn Quan
        if (cats.includes('Hành Động') || cats.includes('Phiêu Lưu')) {
            normalHanhDong.push(slug);
        }
        
        // 2. Tình Cảm Ngọt Ngào & Lãng Mạn
        if (cats.includes('Tình Cảm')) {
            normalTinhCam.push(slug);
        }
        
        // 3. Siêu Phẩm Điện Ảnh Âu Mỹ
        if (countries.includes('Âu Mỹ') || countries.includes('Mỹ') || countries.includes('Anh')) {
            normalAuMy.push(slug);
        }
        
        // 4. Kỳ Án & Phá Án Đỉnh Cao
        if (cats.includes('Hình Sự') || cats.includes('Bí Ẩn') || cats.includes('Trinh Thám')) {
            normalHinhSu.push(slug);
        }
        
        // 5. Khoa Học & Viễn Tưởng Đột Phá
        if (cats.includes('Viễn Tưởng') || cats.includes('Khoa Học')) {
            normalVienTuong.push(slug);
        }
        
        // 6. Hài Hước Cười Ra Nước Mắt
        if (cats.includes('Hài Hước')) {
            normalHaiHuoc.push(slug);
        }
        
        // 7. Kinh Dị Lạnh Sống Lưng
        if (cats.includes('Kinh Dị') || cats.includes('Giật Gân')) {
            normalKinhDi.push(slug);
        }
        
        // 8. Cổ Trang & Tiên Hiệp Đặc Sắc
        if (cats.includes('Cổ Trang') || cats.includes('Thần Thoại')) {
            normalCoTrang.push(slug);
        }
    });

    const categoriesToSeed = [
        { id: "normal-hanh-dong", title: "Hành Động Khai Mở Nhãn Quan", movieSlugs: normalHanhDong.slice(0, 200) },
        { id: "normal-tinh-cam", title: "Tình Cảm Ngọt Ngào & Lãng Mạn", movieSlugs: normalTinhCam.slice(0, 200) },
        { id: "normal-au-my", title: "Siêu Phẩm Điện Ảnh Âu Mỹ", movieSlugs: normalAuMy.slice(0, 200) },
        { id: "normal-hinh-su", title: "Kỳ Án & Phá Án Đỉnh Cao", movieSlugs: normalHinhSu.slice(0, 200) },
        { id: "normal-vien-tuong", title: "Khoa Học & Viễn Tưởng Đột Phá", movieSlugs: normalVienTuong.slice(0, 200) },
        { id: "normal-hai-huoc", title: "Hài Hước Cười Ra Nước Mắt", movieSlugs: normalHaiHuoc.slice(0, 200) },
        { id: "normal-kinh-di", title: "Kinh Dị Lạnh Sống Lưng", movieSlugs: normalKinhDi.slice(0, 200) },
        { id: "normal-co-trang", title: "Cổ Trang & Tiên Hiệp Đặc Sắc", movieSlugs: normalCoTrang.slice(0, 200) }
    ];

    console.log("Seeding Normal categories into Firestore...");
    let batch = db.batch();
    for (const cat of categoriesToSeed) {
        const ref = db.collection('home_categories').doc(cat.id);
        batch.set(ref, cat);
        console.log(`Prepared category: ${cat.title} (${cat.movieSlugs.length} movies)`);
    }
    
    await batch.commit();
    console.log("Done!");
    process.exit(0);
}

run().catch(console.error);
