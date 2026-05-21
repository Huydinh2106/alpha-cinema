const { db } = require('./firebaseAdmin');

// Danh sách các phim đã được "AI" duyệt và phân loại thủ công cho chuẩn
const doraemonSlugs = [
    'doraemon-nobita-du-hanh-bien-phuong-nam',
    'doraemon-nobita-o-vuong-quoc-cho-meo',
    'doraemon-nobita-o-xu-so-nghin-le-mot-dem',
    'doraemon-nobita-tay-du-ki',
    'doraemon-nobita-tham-hiem-vung-dat-moi-peko-va-5-nha-tham-hiem',
    'doraemon-nobita-va-ba-chang-hiep-si-mong-mo',
    'doraemon-nobita-va-chuyen-tau-toc-hanh-ngan-ha',
    'doraemon-nobita-va-cuoc-phieu-luu-o-thanh-pho-day-cot',
    'doraemon-nobita-va-cuoc-phieu-luu-vao-the-gioi-trong-tranh',
    'doraemon-nobita-va-hanh-tinh-muong-thu',
    'doraemon-nobita-va-hon-dao-dieu-ki-cuoc-phieu-luu-cua-loai-thu',
    'doraemon-nobita-va-me-cung-thiec',
    'doraemon-nobita-va-nhung-dung-si-co-canh',
    'doraemon-nobita-va-nhung-phap-su-gio-bi-an',
    'doraemon-nobita-va-nuoc-nhat-thoi-nguyen-thuy-1989',
    'doraemon-nobita-va-truyen-thuyet-vua-mat-troi',
    'doraemon-nobita-va-vuong-quoc-robot',
    'doraemon-nobita-va-vuong-quoc-tren-may',
    'doraemon-nobita-vu-tru-phieu-luu-ki',
    'doraemon-tuyen-tap-moi-nhat'
];

const conanSlugs = [
    'tham-tu-lung-danh-conan',
    'tham-tu-lung-danh-conan-17-con-mat-bi-an-ngoai-bien-xa',
    'tham-tu-lung-danh-conan-18-sat-thu-ban-tia-khong-tuong',
    'tham-tu-lung-danh-conan-19-hoa-huong-duong-ruc-lua',
    'tham-tu-lung-danh-conan-20-con-ac-mong-den-toi',
    'tham-tu-lung-danh-conan-21-ban-tinh-ca-mau-do-tham',
    'tham-tu-lung-danh-conan-22-ke-hanh-phap-zero',
    'tham-tu-lung-danh-conan-23-cu-dam-sapphire-xanh',
    'tham-tu-lung-danh-conan-25-nang-dau-halloween',
    'tham-tu-lung-danh-conan-26-tau-ngam-sat-mau-den',
    'tham-tu-lung-danh-conan-28-du-anh-cua-doc-nhan',
    'tham-tu-lung-danh-conan-ngoi-sao-5-canh-1-trieu-do'
];

const generalKids = [
    'arco',
    'au-trung-tinh-nghich-mat-day-chuyen',
    'bo-phim-dao-au-trung',
    'conan-cau-be-tuong-lai',
    'cuc-vang-cua-ngoai',
    'dee-va-cac-ban-o-xu-oz-phan-1',
    'dee-va-cac-ban-o-xu-oz-phan-2',
    'gia-dinh-phep-thuat',
    'hoc-vien-ky-lan-bi-mat-he-lo',
    'hoppers-cu-nhay-ky-dieu',
    'mac-bay-lu-ti-quay',
    'momo',
    'nha-cua-chung-minh',
    'nhung-ke-xau-xa-phan-1',
    'nhung-ke-xau-xa-phan-2',
    'phong-thi-nghiem-crunch-phan-1',
    'phong-thi-nghiem-crunch-phan-2',
    'phong-thi-nghiem-crunch-phan-3',
    'pororo-cuoc-phieu-luu-den-dinh-thu-rong',
    'pororo-duong-dua-mao-hiem',
    'trong-rong-phan-1',
    'trong-rong-phan-2',
    'tuyen-thu-de-mui-vi-chien-thang',
    'vua-cua-nhung-vi-vua-vua-cua-cac-vua',
    'vua-david'
];

const allSafeSlugs = [...doraemonSlugs, ...conanSlugs, ...generalKids];

// Categories definition
const categoriesToSeed = [
    {
        id: "kids-hoat-hinh",
        title: "Thế giới Hoạt Hình",
        movieSlugs: [
            ...doraemonSlugs,
            'au-trung-tinh-nghich-mat-day-chuyen', 'bo-phim-dao-au-trung',
            'nhung-ke-xau-xa-phan-1', 'nhung-ke-xau-xa-phan-2',
            'pororo-cuoc-phieu-luu-den-dinh-thu-rong', 'pororo-duong-dua-mao-hiem',
            'dee-va-cac-ban-o-xu-oz-phan-1', 'dee-va-cac-ban-o-xu-oz-phan-2',
            'tuyen-thu-de-mui-vi-chien-thang'
        ]
    },
    {
        id: "kids-anime",
        title: "Anime dễ thương",
        movieSlugs: [
            ...doraemonSlugs,
            ...conanSlugs,
            'conan-cau-be-tuong-lai'
        ]
    },
    {
        id: "kids-gia-dinh",
        title: "Phim Gia Đình ấm áp",
        movieSlugs: [
            'cuc-vang-cua-ngoai', 'gia-dinh-phep-thuat', 'mac-bay-lu-ti-quay',
            'nha-cua-chung-minh', 'vua-cua-nhung-vi-vua-vua-cua-cac-vua', 'vua-david',
            'momo', 'arco'
        ]
    },
    {
        id: "kids-phieu-luu",
        title: "Khám phá & Phiêu lưu",
        movieSlugs: [
            ...conanSlugs,
            ...doraemonSlugs.slice(0, 5), // Lấy vài phim doraemon tiêu biểu
            'hoc-vien-ky-lan-bi-mat-he-lo',
            'trong-rong-phan-1', 'trong-rong-phan-2',
            'momo', 'arco'
        ]
    },
    {
        id: "kids-hai-huoc",
        title: "Phim Hài Hước vui nhộn",
        movieSlugs: [
            'au-trung-tinh-nghich-mat-day-chuyen', 'bo-phim-dao-au-trung',
            'nhung-ke-xau-xa-phan-1', 'nhung-ke-xau-xa-phan-2',
            'pororo-cuoc-phieu-luu-den-dinh-thu-rong', 'pororo-duong-dua-mao-hiem',
            'gia-dinh-phep-thuat', 'mac-bay-lu-ti-quay'
        ]
    },
    {
        id: "kids-khoa-hoc",
        title: "Khoa học & Bí ẩn",
        movieSlugs: [
            'phong-thi-nghiem-crunch-phan-1', 'phong-thi-nghiem-crunch-phan-2', 'phong-thi-nghiem-crunch-phan-3',
            'conan-cau-be-tuong-lai', 'hoppers-cu-nhay-ky-dieu',
            'tham-tu-lung-danh-conan', 'tham-tu-lung-danh-conan-20-con-ac-mong-den-toi', 'tham-tu-lung-danh-conan-23-cu-dam-sapphire-xanh'
        ]
    }
];

async function run() {
    console.log("Resetting all movies' isKidsFriendly flag to false (if previously true)...");
    const moviesSnap = await db.collection('movies').where('isKidsFriendly', '==', true).get();
    
    let batch = db.batch();
    let count = 0;
    
    for (const doc of moviesSnap.docs) {
        if (!allSafeSlugs.includes(doc.id)) {
            batch.update(doc.ref, { isKidsFriendly: false });
            count++;
            if (count % 500 === 0) {
                await batch.commit();
                batch = db.batch();
            }
        }
    }
    await batch.commit();
    console.log(`Reset ${count} movies to false.`);

    console.log("Setting safe movies' isKidsFriendly flag to true...");
    batch = db.batch();
    for (const slug of allSafeSlugs) {
        const ref = db.collection('movies').doc(slug);
        batch.update(ref, { isKidsFriendly: true });
    }
    await batch.commit();
    console.log(`Set ${allSafeSlugs.length} movies to true.`);

    console.log("Seeding Kids categories into Firestore...");
    for (const cat of categoriesToSeed) {
        // shuffle the arrays just so they look dynamic (optional)
        cat.movieSlugs = cat.movieSlugs.sort(() => Math.random() - 0.5);
        
        await db.collection('home_categories').doc(cat.id).set(cat);
        console.log(`Created category: ${cat.title} (${cat.movieSlugs.length} movies)`);
    }

    console.log("Done AI processing and categorizing!");
    process.exit(0);
}

run().catch(console.error);
