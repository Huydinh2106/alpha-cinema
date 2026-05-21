import * as functions from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";

const REGION = "asia-southeast1";

/**
 * Lấy FCM token của người dùng từ Firestore.
 */
async function getUserTokens(uid: string): Promise<string[]> {
  const doc = await admin.firestore().collection("users").doc(uid).get();
  const data = doc.data() ?? {};
  return data.fcmTokens ?? [];
}

/**
 * Hàm gửi Push Notification.
 */
async function sendPush(tokens: string[], payload: admin.messaging.MulticastMessage) {
  if (tokens.length === 0) return;
  await admin.messaging().sendEachForMulticast(payload);
}

// 1. Cập nhật Series – Khi có tập mới
export const notifySeriesUpdate = functions.onCall({ region: REGION }, async (request) => {
  const { uid, seriesName, episode, seriesId } = request.data;
  const tokens = await getUserTokens(uid);
  
  const message: admin.messaging.MulticastMessage = {
    notification: {
      title: `Tập ${episode} của ${seriesName} đã có!`,
      body: `Xem ngay để không bỏ lỡ câu chuyện hấp dẫn.`,
    },
    android: {
      notification: { channelId: "push_notifications" },
    },
    data: {
      type: "series_update",
      seriesId: seriesId ?? "",
      episode: String(episode),
    },
    tokens: tokens
  };
  
  await sendPush(tokens, message);
  return { success: true };
});

// 2. Gợi ý cá nhân hoá
export const notifyPersonalRecommendation = functions.onCall({ region: REGION }, async (request) => {
  const { uid, movieTitle, movieId } = request.data;
  const tokens = await getUserTokens(uid);
  
  const message: admin.messaging.MulticastMessage = {
    notification: {
      title: `Gợi ý dành riêng cho bạn`,
      body: `Dựa trên sở thích của bạn, chúng tôi gợi ý phim: "${movieTitle}".`,
    },
    android: {
      notification: { channelId: "push_notifications" },
    },
    data: {
      type: "recommendation",
      movieId: movieId ?? "",
    },
    tokens: tokens
  };
  
  await sendPush(tokens, message);
  return { success: true };
});

// 3. Cảnh báo bảo mật – Đăng nhập lạ
export const notifySecurityAlert = functions.onCall({ region: REGION }, async (request) => {
  const { uid, deviceName, location } = request.data;
  const tokens = await getUserTokens(uid);
  
  const message: admin.messaging.MulticastMessage = {
    notification: {
      title: `Cảnh báo bảo mật`,
      body: `Tài khoản của bạn vừa đăng nhập thành công trên thiết bị ${deviceName} tại ${location}.`,
    },
    android: {
      notification: {
        channelId: "push_notifications",
        priority: "high"
      },
    },
    data: {
      type: "security_alert",
      device: deviceName ?? "",
    },
    tokens: tokens
  };
  
  await sendPush(tokens, message);
  return { success: true };
});

// 4. Thông báo thanh toán – Gia hạn, thất bại
export const notifyBilling = functions.onCall({ region: REGION }, async (request) => {
  const { uid, status, plan, expiry } = request.data;
  const tokens = await getUserTokens(uid);
  
  const title = status === "success" 
    ? `Thanh toán thành công` 
    : status === "failure" 
      ? `Thanh toán thất bại` 
      : `Gói ${plan} sắp hết hạn`;
      
  const body = status === "success"
    ? `Gói ${plan} đã được kích hoạt. Hạn dùng đến ${expiry}.`
    : status === "failure"
      ? `Không thể gia hạn gói do thanh toán bị từ chối. Vui lòng cập nhật phương thức thanh toán.`
      : `Gói Premium của bạn sẽ hết hạn trong ${expiry} ngày tới. Gia hạn ngay!`;

  const message: admin.messaging.MulticastMessage = {
    notification: { title, body },
    android: {
      notification: { 
        channelId: "push_notifications",
        priority: "high"
      },
    },
    data: { 
      type: "billing", 
      plan: plan ?? "", 
      status: status ?? "" 
    },
    tokens: tokens
  };
  
  await sendPush(tokens, message);
  return { success: true };
});

// 5. Trigger tự động khi có phim mới được thêm vào
export const onNewMovieAdded = onDocumentCreated({
    document: "movies/{movieId}",
    region: REGION
}, async (event) => {
    console.log(`onNewMovieAdded triggered for document: ${event.params.movieId}`);
    const movieData = event.data?.data();
    if (!movieData) {
        console.log("No movie data found.");
        return;
    }

    const movieTitle = movieData.name || movieData.title || movieData.origin_name || "Phim mới";
    console.log(`Processing movie: ${movieTitle}`);
    
    // Đổi format thông báo cho hấp dẫn hơn và chắc chắn có tên phim
    const title = `${movieTitle} vừa cập bến! 🍿`;
    const body = `Siêu phẩm "${movieTitle}" đã chính thức có mặt trên Alpha Cinema. Vào xem ngay kẻo lỡ!`;
    const type = "new_movie";

    // Lấy tất cả user
    const usersSnapshot = await admin.firestore().collection("users").get();
    console.log(`Found ${usersSnapshot.size} users to notify.`);
    
    const tokens: string[] = [];
    const batchArray: admin.firestore.WriteBatch[] = [];
    let currentBatch = admin.firestore().batch();
    let batchCount = 0;

    usersSnapshot.docs.forEach(doc => {
        const uid = doc.id;
        const userData = doc.data();
        
        // Lưu thông báo vào subcollection "notifications" của TỪNG user
        const notifRef = admin.firestore().collection("users").doc(uid).collection("notifications").doc();
        currentBatch.set(notifRef, {
            title,
            body,
            type,
            movieId: event.params.movieId, // Lưu movieId để mở chi tiết phim
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false
        });
        batchCount++;

        if (batchCount === 400) {
            batchArray.push(currentBatch);
            currentBatch = admin.firestore().batch();
            batchCount = 0;
        }

        if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
            tokens.push(...userData.fcmTokens);
        }
    });

    if (batchCount > 0) {
        batchArray.push(currentBatch);
    }
    
    // Thực thi các batch write để lưu vào Firestore
    console.log(`Saving notifications to Firestore in ${batchArray.length} batches...`);
    for (const batch of batchArray) {
        await batch.commit();
    }
    console.log("Firestore notifications saved.");

    // Gửi Push Notification qua FCM
    if (tokens.length > 0) {
        const uniqueTokens = Array.from(new Set(tokens));
        console.log(`Sending push notifications to ${uniqueTokens.length} unique tokens...`);
        const chunkSize = 500;
        for (let i = 0; i < uniqueTokens.length; i += chunkSize) {
            const chunk = uniqueTokens.slice(i, i + chunkSize);
            const message: admin.messaging.MulticastMessage = {
                notification: { title, body },
                android: { 
                    priority: "high", // Đánh thức thiết bị khỏi Doze mode
                    notification: { channelId: "push_notifications", priority: "default" } 
                },
                data: { type, movieId: event.params.movieId },
                tokens: chunk
            };
            const response = await admin.messaging().sendEachForMulticast(message);
            console.log(`Batch sent. Success: ${response.successCount}, Failure: ${response.failureCount}`);
        }
    } else {
        console.log("No FCM tokens found to send push notifications.");
    }
});

// 6. Cronjob kiểm tra gói dịch vụ sắp hết hạn (chạy lúc 08:00 sáng mỗi ngày)
export const checkExpiringSubscriptions = onSchedule({
    schedule: "every day 08:00",
    timeZone: "Asia/Ho_Chi_Minh",
    region: REGION
}, async (event) => {
    const now = new Date();
    
    const usersSnapshot = await admin.firestore().collection("users").get();
    
    const tokens: string[] = [];
    const batchArray: admin.firestore.WriteBatch[] = [];
    let currentBatch = admin.firestore().batch();
    let batchCount = 0;

    usersSnapshot.docs.forEach(doc => {
        const userData = doc.data();
        const plan = userData.plan || userData.subscriptionPlan;
        let expiryDate: Date | null = null;
        
        if (userData.expiryDate) {
            if (userData.expiryDate.toDate) {
                expiryDate = userData.expiryDate.toDate(); // Firestore Timestamp
            } else {
                expiryDate = new Date(userData.expiryDate); // ISO string
            }
        }
        
        if (plan && plan !== "free" && expiryDate) {
            const diffTime = expiryDate.getTime() - now.getTime();
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            
            // Cảnh báo trước 3 ngày và 1 ngày
            if (diffDays === 3 || diffDays === 1) {
                const uid = doc.id;
                const title = "Gói sắp hết hạn";
                const body = `Gói ${plan.toUpperCase()} của bạn sẽ hết hạn sau ${diffDays} ngày. Gia hạn ngay để không bị gián đoạn.`;
                const type = "billing";

                const notifRef = admin.firestore().collection("users").doc(uid).collection("notifications").doc();
                currentBatch.set(notifRef, {
                    title,
                    body,
                    type,
                    plan: plan, // Lưu thông tin gói để điều hướng
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    isRead: false
                });
                batchCount++;
                
                if (batchCount === 400) {
                    batchArray.push(currentBatch);
                    currentBatch = admin.firestore().batch();
                    batchCount = 0;
                }

                if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
                    tokens.push(...userData.fcmTokens);
                }
            }
        }
    });

    if (batchCount > 0) {
        batchArray.push(currentBatch);
    }
    
    for (const batch of batchArray) {
        await batch.commit();
    }

    if (tokens.length > 0) {
        const uniqueTokens = Array.from(new Set(tokens));
        const chunkSize = 500;
        for (let i = 0; i < uniqueTokens.length; i += chunkSize) {
            const chunk = uniqueTokens.slice(i, i + chunkSize);
            const message: admin.messaging.MulticastMessage = {
                notification: { title: "Gói sắp hết hạn", body: "Gói dịch vụ của bạn sắp hết hạn. Mở ứng dụng để xem chi tiết và gia hạn ngay!" },
                android: { 
                    priority: "high", // Đánh thức thiết bị khỏi Doze mode
                    notification: { channelId: "push_notifications", priority: "default" } 
                },
                data: { type: "billing" },
                tokens: chunk
            };
            await admin.messaging().sendEachForMulticast(message);
        }
    }
});
