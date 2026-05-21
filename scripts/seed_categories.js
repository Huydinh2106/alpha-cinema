const { db } = require('./firebaseAdmin');

async function seedCategories() {
    console.log("Fetching some movies to use as samples...");
    const moviesSnap = await db.collection('movies').limit(200).get();
    
    // Categorize some movies
    const series = [];
    const singles = [];
    const hoathinh = [];
    const hanquoc = [];
    const trungquoc = [];
    const aumy = [];

    moviesSnap.docs.forEach(doc => {
        const data = doc.data();
        const slug = doc.id;
        
        if (data.type === 'series' && series.length < 15) series.push(slug);
        if (data.type === 'single' && singles.length < 15) singles.push(slug);
        if (data.type === 'hoathinh' && hoathinh.length < 15) hoathinh.push(slug);
        
        const countries = data.countries || [];
        if (countries.includes('Hàn Quốc') && hanquoc.length < 15) hanquoc.push(slug);
        if (countries.includes('Trung Quốc') && trungquoc.length < 15) trungquoc.push(slug);
        if (countries.includes('Mỹ') || countries.includes('Âu Mỹ') || countries.includes('Anh')) {
            if (aumy.length < 15) aumy.push(slug);
        }
    });

    const categoriesToSeed = [
        { id: "phim-bo-moi", title: "Phim bộ mới tải lên", movieSlugs: series },
        { id: "phim-le-hot", title: "Phim lẻ nổi bật", movieSlugs: singles },
        { id: "phim-hoat-hinh", title: "Phim hoạt hình", movieSlugs: hoathinh },
        { id: "phim-han-quoc", title: "Phim Hàn Quốc", movieSlugs: hanquoc },
        { id: "phim-trung-quoc", title: "Phim Trung Quốc", movieSlugs: trungquoc },
        { id: "phim-au-my", title: "Phim Âu Mỹ", movieSlugs: aumy },
        { id: "phim-chieu-rap", title: "Phim chiếu rạp", movieSlugs: singles.slice(0, 5) },
        { id: "anime-moi", title: "Anime Mới", movieSlugs: hoathinh.slice(0, 8) }
    ];

    console.log("Seeding categories into Firestore...");
    
    for (const cat of categoriesToSeed) {
        await db.collection('home_categories').doc(cat.id).set(cat);
        console.log(`Created category: ${cat.title} (${cat.movieSlugs.length} movies)`);
    }

    console.log("Done!");
    process.exit(0);
}

seedCategories();
