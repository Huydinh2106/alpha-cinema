/**
 * Xoá triệt để các thông báo "lỗi" còn sót lại trong Firestore.
 *
 * Các thông báo bị xoá:
 *  1) Title bằng đúng "Thông báo từ Alpha Cinema"
 *     (do MainActivity.handleFCMIntent fallback tạo ra trước khi sửa).
 *  2) Bản trùng cùng movieId: nếu có nhiều document cho cùng 1 movieId, giữ lại
 *     document có imageUrl + mới nhất, xoá phần còn lại.
 *  3) Document type=new_movie nhưng KHÔNG có imageUrl trong khi cùng user còn
 *     document khác cùng movieId có imageUrl → xoá document thiếu poster.
 *
 * Cách chạy (Windows cmd):
 *   set GOOGLE_APPLICATION_CREDENTIALS=path\to\service-account.json
 *   node scripts/cleanup-bad-notifications.js
 *
 * Hoặc nếu đã firebase login + có quyền owner trên project, có thể dùng
 * firebase emulators hoặc Application Default Credentials.
 */

const admin = require("firebase-admin");
const path = require("path");
const fs = require("fs");

// Đọc JSON thủ công vì Node >=22 yêu cầu import attribute cho require(JSON).
function readJson(p) {
    return JSON.parse(fs.readFileSync(p, "utf8"));
}

// Ưu tiên dùng service-account.json ở root (đã có sẵn trong repo này).
// Nếu không có, fallback sang ApplicationDefault credential.
const firebaserc = readJson(path.resolve(__dirname, "..", ".firebaserc"));
const projectId = process.env.GCLOUD_PROJECT
    || process.env.GOOGLE_CLOUD_PROJECT
    || firebaserc.projects.default;

if (!admin.apps.length) {
    const saPath = path.resolve(__dirname, "..", "service-account.json");
    if (fs.existsSync(saPath)) {
        const serviceAccount = readJson(saPath);
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            projectId,
        });
    } else {
        admin.initializeApp({
            credential: admin.credential.applicationDefault(),
            projectId,
        });
    }
}

const db = admin.firestore();

const BAD_FALLBACK_TITLE = "Thông báo từ Alpha Cinema";
// Format title cũ trước khi sửa code, không có poster, cần xoá hẳn.
const LEGACY_TITLE_PREFIX = "Phim mới: ";

function isBadNotification(data) {
    const title = (data.title || "").trim();
    const type = data.type;
    const hasImage = typeof data.imageUrl === "string" && data.imageUrl.length > 0;

    // 1. Title fallback do MainActivity.handleFCMIntent cũ tạo ra.
    if (title === BAD_FALLBACK_TITLE) return true;
    // 2. Format cũ "Phim mới: ..." đều là rác (chưa có poster, không có movieId).
    if (title.startsWith(LEGACY_TITLE_PREFIX)) return true;
    // 3. type new_movie nhưng thiếu poster → là document do client cũ tạo (FCM service / MainActivity).
    //    Bản chuẩn do Cloud Function tạo luôn có imageUrl + ID là slug phim.
    if (type === "new_movie" && !hasImage) return true;
    return false;
}

async function cleanupForUser(userDoc) {
    const userId = userDoc.id;
    const notificationsRef = userDoc.ref.collection("notifications");
    const snapshot = await notificationsRef.get();

    if (snapshot.empty) return { deleted: 0, kept: 0 };

    let deleted = 0;
    let kept = 0;

    // Bước 1: xoá thẳng các document "rác" theo các tiêu chí trên.
    for (const doc of snapshot.docs) {
        const data = doc.data();
        if (isBadNotification(data)) {
            await doc.ref.delete();
            deleted++;
            const reason =
                (data.title || "").trim() === BAD_FALLBACK_TITLE ? "fallback-title"
                    : (data.title || "").startsWith(LEGACY_TITLE_PREFIX) ? "legacy-title"
                        : "no-imageUrl";
            console.log(
                `  ✗ [${reason}] users/${userId}/notifications/${doc.id}` +
                `  title="${(data.title || "").slice(0, 60)}"`
            );
        }
    }

    // Bước 2: với phần còn lại, gom theo movieId và xoá bản trùng (giữ bản có imageUrl & mới nhất).
    const remaining = await notificationsRef.get();
    const byMovie = new Map();
    for (const doc of remaining.docs) {
        const movieId = doc.data().movieId;
        if (!movieId) continue;
        if (!byMovie.has(movieId)) byMovie.set(movieId, []);
        byMovie.get(movieId).push(doc);
    }

    for (const [movieId, docs] of byMovie.entries()) {
        if (docs.length <= 1) { kept += docs.length; continue; }
        docs.sort((a, b) => {
            const aHasImg = (a.data().imageUrl || "").length > 0 ? 1 : 0;
            const bHasImg = (b.data().imageUrl || "").length > 0 ? 1 : 0;
            if (aHasImg !== bHasImg) return bHasImg - aHasImg;
            const ta = a.data().timestamp?.toMillis?.() || 0;
            const tb = b.data().timestamp?.toMillis?.() || 0;
            return tb - ta;
        });
        const [keepDoc, ...rest] = docs;
        kept++;
        for (const doc of rest) {
            await doc.ref.delete();
            deleted++;
            console.log(
                `  ✗ [dup-movieId=${movieId}] users/${userId}/notifications/${doc.id}` +
                ` (kept ${keepDoc.id})`
            );
        }
    }

    return { deleted, kept };
}

(async () => {
    console.log(`🧹 Cleanup notifications for project: ${projectId}\n`);
    const usersSnapshot = await db.collection("users").get();
    console.log(`Found ${usersSnapshot.size} users.\n`);

    let totalDeleted = 0;
    let totalKept = 0;

    for (const userDoc of usersSnapshot.docs) {
        const { deleted, kept } = await cleanupForUser(userDoc);
        if (deleted > 0) {
            console.log(`User ${userDoc.id}: deleted=${deleted}, kept=${kept}`);
        }
        totalDeleted += deleted;
        totalKept += kept;
    }

    console.log(
        `\n✅ Done. Deleted ${totalDeleted} bad notifications, kept ${totalKept} good ones.`
    );
    process.exit(0);
})().catch((err) => {
    console.error("❌ Cleanup failed:", err);
    process.exit(1);
});
