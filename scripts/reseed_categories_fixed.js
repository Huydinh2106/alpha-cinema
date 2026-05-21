const { db } = require('./firebaseAdmin');

async function seedCategories() {
    console.log("Fetching all movies for categorization...");
    // Need to fetch all to sort them properly
    const moviesSnap = await db.collection('movies').get();
    
    // Sort by latest added / updated if possible, but for now just shuffle or take first ones.
    // Assuming docs are ordered arbitrarily, we'll collect all matches and then slice the ones we want.
    let movies = [];
    moviesSnap.docs.forEach(doc => {
        movies.push({ slug: doc.id, data: doc.data() });
    });
    
    // Sort by year descending to get newest first if year exists, otherwise by slug
    movies.sort((a, b) => {
        const yearA = a.data.year || 0;
        const yearB = b.data.year || 0;
        return yearB - yearA;
    });

    const series = [];
    const singles = [];
    const hoathinh3d = [];
    const hoathinhAll = [];
    const hanquoc = [];
    const trungquoc = [];
    const aumy = [];
    const anime = [];
    const chieurap = [];

    movies.forEach(item => {
        const data = item.data;
        const slug = item.slug;
        const cats = data.categories || [];
        const countries = data.countries || [];
        
        const isHoatHinh = data.type === 'hoathinh' || cats.includes('Hoạt Hình') || cats.includes('Anime');

        if (data.type === 'series') series.push(slug);
        if (data.type === 'single') singles.push(slug);
        
        if (isHoatHinh) {
            hoathinhAll.push(slug);
            if (countries.includes('Nhật Bản')) {
                anime.push(slug);
            }
            if (countries.includes('Trung Quốc') || cats.includes('3D')) {
                hoathinh3d.push(slug);
            }
        }
        
        if (!isHoatHinh) {
            if (countries.includes('Hàn Quốc')) hanquoc.push(slug);
            if (countries.includes('Trung Quốc')) trungquoc.push(slug);
        }
        
        if (countries.includes('Mỹ') || countries.includes('Âu Mỹ') || countries.includes('Anh') || countries.includes('Mỹ - Châu Âu')) {
            aumy.push(slug);
        }
        
        if (cats.includes('Chiếu Rạp') || cats.includes('Phim Chiếu Rạp')) {
            chieurap.push(slug);
        }
    });

    const chieurapFinal = chieurap.length > 0 ? chieurap : singles;

    const categoriesToSeed = [
        { id: "phim-bo-moi", title: "Phim bộ mới tải lên", movieSlugs: series.slice(0, 200) },
        { id: "phim-le-hot", title: "Phim lẻ nổi bật", movieSlugs: singles.slice(0, 200) },
        { id: "hoat-hinh-3d", title: "Hoạt hình 3D", movieSlugs: hoathinh3d.slice(0, 200) },
        { id: "phim-han-quoc", title: "Phim Hàn Quốc", movieSlugs: hanquoc.slice(0, 200) },
        { id: "phim-trung-quoc", title: "Phim Trung Quốc", movieSlugs: trungquoc.slice(0, 200) },
        { id: "phim-au-my", title: "Phim Âu Mỹ", movieSlugs: aumy.slice(0, 200) },
        { id: "phim-chieu-rap", title: "Phim chiếu rạp", movieSlugs: chieurapFinal.slice(0, 200) },
        { id: "anime-moi", title: "Anime Mới", movieSlugs: anime.slice(0, 200) },
        { id: "phim-hoat-hinh", title: "Phim Hoạt Hình", movieSlugs: hoathinhAll.slice(0, 200) } // Keep this for fallback
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
