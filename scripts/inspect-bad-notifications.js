const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

function readJson(p) { return JSON.parse(fs.readFileSync(p, "utf8")); }

const firebaserc = readJson(path.resolve(__dirname, "..", ".firebaserc"));
const projectId = firebaserc.projects.default;

if (!admin.apps.length) {
    const saPath = path.resolve(__dirname, "..", "service-account.json");
    if (fs.existsSync(saPath)) {
        admin.initializeApp({ credential: admin.credential.cert(readJson(saPath)), projectId });
    } else {
        admin.initializeApp({ credential: admin.credential.applicationDefault(), projectId });
    }
}

(async () => {
    const usersSnapshot = await admin.firestore().collection("users").get();
    for (const userDoc of usersSnapshot.docs) {
        const snap = await userDoc.ref.collection("notifications").get();
        const suspicious = snap.docs.filter((d) => {
            const data = d.data();
            const title = (data.title || "");
            const body = (data.body || "");
            const hasImage = !!(data.imageUrl);
            const hasMovieId = !!(data.movieId);
            // Nghi ngờ: type=system, hoặc title chứa "Alpha Cinema" nhưng không phải định dạng "<phim> vừa cập bến!"
            const looksFallback =
                title === "Thông báo từ Alpha Cinema" ||
                title.includes("Alpha Cinema") && !title.includes("vừa cập bến");
            const suspicious =
                looksFallback ||
                (data.type === "new_movie" && !hasImage) ||
                (data.type === "system" && !body && !hasMovieId);
            return suspicious;
        });

        if (suspicious.length === 0) continue;
        console.log(`\n--- user ${userDoc.id} (${suspicious.length} suspicious) ---`);
        for (const d of suspicious) {
            const data = d.data();
            console.log(`  id=${d.id}  type=${data.type}  title="${data.title}"  body="${(data.body || '').slice(0, 60)}"  movieId=${data.movieId || ''}  imageUrl=${data.imageUrl ? 'YES' : 'NO'}  ts=${data.timestamp?.toDate?.().toISOString?.() || ''}`);
        }
    }
    process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
