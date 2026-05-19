import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
    admin.initializeApp();
}

export const cleanupDupes = functions.onRequest({ region: "asia-southeast1" }, async (req, res) => {
    try {
        const usersSnapshot = await admin.firestore().collection("users").get();
        let deletedCount = 0;

        for (const userDoc of usersSnapshot.docs) {
            const notificationsRef = userDoc.ref.collection("notifications");
            const notificationsSnapshot = await notificationsRef.get();
            
            const seen = new Map<string, any[]>();
            for (const doc of notificationsSnapshot.docs) {
                const data = doc.data();
                const title = data.title;
                if (!title || !title.includes("vừa cập bến")) continue;
                
                if (!seen.has(title)) {
                    seen.set(title, [doc]);
                } else {
                    seen.get(title)!.push(doc);
                }
            }

            for (const [_, docs] of seen.entries()) {
                if (docs.length > 1) {
                    // Sắp xếp giảm dần theo thời gian (giữ lại cái mới nhất)
                    docs.sort((a, b) => {
                        const timeA = a.data().timestamp?.toMillis() || 0;
                        const timeB = b.data().timestamp?.toMillis() || 0;
                        return timeB - timeA;
                    });
                    for (let i = 1; i < docs.length; i++) {
                        await docs[i].ref.delete();
                        deletedCount++;
                    }
                }
            }
        }
        res.status(200).send(`Cleanup complete. Deleted ${deletedCount} duplicate notifications.`);
    } catch (e: any) {
        res.status(500).send(`Error: ${e.message}`);
    }
});
