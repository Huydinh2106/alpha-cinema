const admin = require("firebase-admin");

admin.initializeApp({
  credential: admin.credential.applicationDefault()
});

async function cleanUpDuplicates() {
  const usersSnapshot = await admin.firestore().collection("users").get();
  let deletedCount = 0;

  for (const userDoc of usersSnapshot.docs) {
    const notificationsRef = userDoc.ref.collection("notifications");
    const notificationsSnapshot = await notificationsRef.where("type", "==", "new_movie").get();
    
    // Group by movieId
    const seen = new Map();
    for (const doc of notificationsSnapshot.docs) {
      const data = doc.data();
      const movieId = data.movieId;
      if (!movieId) continue;
      
      if (!seen.has(movieId)) {
        seen.set(movieId, [doc]);
      } else {
        seen.get(movieId).push(doc);
      }
    }

    // Keep the most recent one (or the one with imageUrl), delete the rest
    for (const [movieId, docs] of seen.entries()) {
      if (docs.length > 1) {
        // Sort by timestamp descending
        docs.sort((a, b) => {
          const timeA = a.data().timestamp?.toMillis() || 0;
          const timeB = b.data().timestamp?.toMillis() || 0;
          return timeB - timeA;
        });

        // Delete all except the first one
        for (let i = 1; i < docs.length; i++) {
          console.log(`Deleting duplicate notification ${docs[i].id} for movie ${movieId} in user ${userDoc.id}`);
          await docs[i].ref.delete();
          deletedCount++;
        }
      }
    }
  }
  
  console.log(`Cleanup complete. Deleted ${deletedCount} duplicate notifications.`);
}

cleanUpDuplicates().catch(console.error);
