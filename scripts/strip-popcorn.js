/**
 * Xoá icon 🍿 (popcorn) khỏi title các thông báo đã có trong Firestore.
 * Đồng thời chuẩn hoá khoảng trắng dư thừa nếu có.
 *
 * Cách chạy:
 *   cd scripts && node strip-popcorn.js
 */
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

const POPCORN = "🍿";

(async () => {
    const usersSnapshot = await admin.firestore().collection("users").get();
    let updated = 0;

    for (const userDoc of usersSnapshot.docs) {
        const snap = await userDoc.ref.collection("notifications").get();
        for (const doc of snap.docs) {
            const data = doc.data();
            const title = data.title || "";
            const body = data.body || "";

            const hasPopcornInTitle = title.includes(POPCORN);
            const hasPopcornInBody = body.includes(POPCORN);
            if (!hasPopcornInTitle && !hasPopcornInBody) continue;

            const newTitle = title.split(POPCORN).join("").replace(/\s+/g, " ").trim();
            const newBody = body.split(POPCORN).join("").replace(/\s+/g, " ").trim();

            const patch = {};
            if (newTitle !== title) patch.title = newTitle;
            if (newBody !== body) patch.body = newBody;
            if (Object.keys(patch).length === 0) continue;

            await doc.ref.update(patch);
            updated++;
            console.log(`  ✓ users/${userDoc.id}/notifications/${doc.id}  →  "${newTitle}"`);
        }
    }

    console.log(`\nDone. Updated ${updated} notifications.`);
    process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
