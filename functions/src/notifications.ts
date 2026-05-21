import * as functions from "firebase-functions/v2/https";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

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
// Sử dụng onDocumentWritten thay vì onDocumentCreated vì Admin UI dùng
// .set() với document ID cố định (slug), Firestore có thể coi đó là update
// thay vì create → onDocumentCreated sẽ không trigger.
// Logic chống trùng: chỉ gửi thông báo khi document chưa có trường notifiedAt.
export const onNewMovieAdded = onDocumentWritten({
  document: "movies/{movieId}",
  region: REGION
}, async (event) => {
  const movieId = event.params.movieId;
  console.log(`onNewMovieAdded triggered for document: ${movieId}`);

  const afterData = event.data?.after?.data();
  const beforeData = event.data?.before?.data();

  // Không có dữ liệu sau khi ghi = document bị xóa → bỏ qua
  if (!afterData) {
    console.log("Document was deleted, skipping.");
    return;
  }

  // Chống gửi trùng: nếu document đã có trường notifiedAt → đã gửi thông báo rồi → bỏ qua
  if (afterData.notifiedAt) {
    console.log(`Movie ${movieId} already notified at ${afterData.notifiedAt}. Skipping.`);
    return;
  }

  // Nếu beforeData đã tồn tại VÀ đã có notifiedAt → chỉ là update nội dung → bỏ qua
  if (beforeData && beforeData.notifiedAt) {
    console.log(`Movie ${movieId} is being updated (already notified). Skipping.`);
    return;
  }

  // Admin UI lưu phim với trường "title", còn thêm trực tiếp trên Firebase có thể dùng "name"
  const movieTitle = afterData.title || afterData.name || afterData.origin_name || "Phim mới";
  console.log(`Processing new movie: ${movieTitle}`);

  // Lấy poster URL để hiển thị thumbnail trong giao diện thông báo
  const rawPoster = afterData.posterUrl || afterData.poster_url || afterData.thumb_url || "";
  const imageUrl = rawPoster && !rawPoster.startsWith("http") ? `https://phimimg.com/${rawPoster}` : rawPoster;

  const title = `${movieTitle} vừa cập bến!`;
  const body = `Siêu phẩm "${movieTitle}" đã chính thức có mặt trên Alpha Cinema. Vào xem ngay kẻo lỡ!`;
  const type = "new_movie";

  // Đánh dấu đã gửi thông báo để tránh gửi lại khi admin sửa phim
  await admin.firestore().collection("movies").doc(movieId).update({
    notifiedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log(`Marked movie ${movieId} as notified.`);

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
    // Sử dụng movieId làm ID để tránh gửi 2 thông báo cho cùng 1 phim (nếu cập nhật hoặc re-trigger)
    const notifRef = admin.firestore().collection("users").doc(uid).collection("notifications").doc(movieId);
    currentBatch.set(notifRef, {
      title,
      body,
      type,
      movieId: movieId, // Lưu movieId (slug) để mở chi tiết phim
      imageUrl: imageUrl || "", // Poster thumbnail cho giao diện thông báo
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
        // notification.imageUrl: FCM tự hiển thị poster trên thanh trạng thái khi app background.
        notification: { title, body, imageUrl: imageUrl || undefined },
        android: {
          priority: "high",
          notification: {
            channelId: "push_notifications",
            priority: "default",
            // Bổ sung imageUrl trong AndroidNotification để FCM SDK render BigPictureStyle.
            imageUrl: imageUrl || undefined
          }
        },
        // data.imageUrl: dùng khi app foreground, MyFirebaseMessagingService.kt sẽ tự load và set BigPictureStyle.
        data: { type, movieId: movieId, imageUrl: imageUrl || "" },
        tokens: chunk
      };
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(`Batch sent. Success: ${response.successCount}, Failure: ${response.failureCount}`);
    }
  } else {
    console.log("No FCM tokens found to send push notifications.");
  }
});

// Ảnh hiển thị cho thông báo gói sắp hết hạn (BigPicture trên thanh trạng thái + thumb trong list).
const SUBSCRIPTION_REMINDER_IMAGE =
  "https://sf-static.upanhlaylink.com/img/image_2026051904b7f141257cc5aca5bc4506e7bf3924.jpg";

// Map mã gói → tên hiển thị thân thiện trong thông báo.
function planLabel(plan: string): string {
  const key = (plan || "").toLowerCase();
  switch (key) {
    case "basic": return "Cơ Bản";
    case "couple": return "Cặp Đôi";
    case "premium": return "Premium";
    default: return plan ? plan.charAt(0).toUpperCase() + plan.slice(1) : "đăng ký";
  }
}

// 6. Cronjob kiểm tra gói dịch vụ sắp hết hạn (chạy mỗi phút)
export const checkExpiringSubscriptions = onSchedule({
  schedule: "every 1 minutes", // Chạy mỗi phút
  timeZone: "Asia/Ho_Chi_Minh",
  region: REGION
}, async (event) => {
  const now = new Date();

  // Lấy tất cả user. (Lý tưởng nên có filter "subscriptionStatus" == "active" để tiết kiệm read)
  const usersSnapshot = await admin.firestore().collection("users").get();

  // Gom (plan, displayDays) → danh sách FCM token để gửi push cá nhân hoá theo gói.
  // Key có dạng "premium|3", "basic|1", v.v.
  const tokensByPlanAndDays = new Map<string, string[]>();
  const batchArray: admin.firestore.WriteBatch[] = [];
  let currentBatch = admin.firestore().batch();
  let batchCount = 0;

  usersSnapshot.docs.forEach(doc => {
    const userData = doc.data();
    const plan = userData.subscriptionPlan || userData.plan;
    const status = userData.subscriptionStatus;
    let expiryDate: Date | null = null;

    // Kiểm tra trường mới subscriptionExpiresAt hoặc trường cũ expiryDate
    const expiresAt = userData.subscriptionExpiresAt || userData.expiryDate;

    if (expiresAt) {
      if (expiresAt.toDate) {
        expiryDate = expiresAt.toDate(); // Firestore Timestamp
      } else {
        expiryDate = new Date(expiresAt); // ISO string
      }
    }

    // Chỉ xử lý nếu gói có hạn và là gói trả phí đang active
    if (plan && plan !== "free" && expiryDate && status !== "inactive") {
      const diffTime = expiryDate.getTime() - now.getTime();
      // diffDays bây giờ là số thực, vd: 2.99 ngày
      const diffDays = diffTime / (1000 * 60 * 60 * 24);

      let shouldNotify = false;
      let displayDays = 0;
      let flagToUpdate = "";
      let isExpired = false;

      if (diffDays <= 0) {
        // Đã hết hạn -> Cập nhật status thành inactive, plan thành free
        if (!userData.notifiedExpired) {
          shouldNotify = true;
          isExpired = true;
          flagToUpdate = "notifiedExpired";
          
          currentBatch.update(doc.ref, {
            subscriptionStatus: "inactive",
            subscriptionPlan: "free",
            subscriptionExpiresAt: admin.firestore.FieldValue.delete(),
            notifiedExpired: true
          });
          batchCount++;
        }
      } else if (diffDays <= 1) {
        // Dưới 1 ngày, nếu chưa báo mốc 1 ngày
        if (!userData.notified1Day) {
          shouldNotify = true;
          displayDays = 1;
          flagToUpdate = "notified1Day";
        }
      } else if (diffDays <= 3) {
        // Dưới 3 ngày, nếu chưa báo mốc 3 ngày
        if (!userData.notified3Days) {
          shouldNotify = true;
          displayDays = 3;
          flagToUpdate = "notified3Days";
        }
      } else if (diffDays > 3) {
        // Gói còn trên 3 ngày (có thể do user vừa gia hạn) -> Reset flags
        if (userData.notified3Days || userData.notified1Day || userData.notifiedExpired) {
          currentBatch.update(doc.ref, {
            notified3Days: false,
            notified1Day: false,
            notifiedExpired: false
          });
          batchCount++;
        }
      }

      if (shouldNotify) {
        const uid = doc.id;
        const label = planLabel(plan);
        const title = isExpired ? `Gói ${label} đã hết hạn` : `Gói ${label} sắp hết hạn`;
        const body = isExpired 
          ? `Gói ${label} của bạn đã hết hạn. Vui lòng gia hạn để tiếp tục xem phim không giới hạn.`
          : `Gói ${label} của bạn sẽ hết hạn trong dưới ${displayDays} ngày. Gia hạn ngay để không bị gián đoạn trải nghiệm xem phim.`;
        const type = "billing";

        // Thêm thông báo vào subcollection của user
        const notifRef = admin.firestore().collection("users").doc(uid).collection("notifications").doc();
        currentBatch.set(notifRef, {
          title,
          body,
          type,
          plan: plan,
          imageUrl: SUBSCRIPTION_REMINDER_IMAGE,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false
        });
        batchCount++;

        // Đánh dấu đã gửi thông báo cho mốc này trên doc user (nếu chưa phải là hết hạn vì hết hạn đã update ở trên)
        if (!isExpired) {
          currentBatch.update(doc.ref, {
            [flagToUpdate]: true
          });
          batchCount++;
        }

        if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
          // Lưu group riêng cho expired vs expiring
          const groupKey = isExpired ? "expired" : String(displayDays);
          const key = `${(plan as string).toLowerCase()}|${groupKey}`;
          if (!tokensByPlanAndDays.has(key)) tokensByPlanAndDays.set(key, []);
          tokensByPlanAndDays.get(key)!.push(...userData.fcmTokens);
        }
      }

      if (batchCount >= 400) {
        batchArray.push(currentBatch);
        currentBatch = admin.firestore().batch();
        batchCount = 0;
      }
    }
  });

  if (batchCount > 0) {
    batchArray.push(currentBatch);
  }

  // Commit các batch
  for (const batch of batchArray) {
    await batch.commit();
  }

  // Gửi FCM PUSH notification — push từng nhóm (plan, days/expired) để body cá nhân hoá.
  for (const [key, tokens] of tokensByPlanAndDays.entries()) {
    if (tokens.length === 0) continue;
    const [plan, groupKey] = key.split("|");
    const isExpired = groupKey === "expired";
    const displayDays = isExpired ? 0 : Number(groupKey);
    const label = planLabel(plan);
    
    const pushTitle = isExpired ? `Gói ${label} đã hết hạn` : `Gói ${label} sắp hết hạn`;
    const pushBody = isExpired
      ? `Gói ${label} của bạn đã hết hạn. Mở ứng dụng và gia hạn ngay!`
      : `Gói ${label} của bạn sẽ hết hạn trong dưới ${displayDays} ngày. Mở ứng dụng và gia hạn ngay!`;

    const uniqueTokens = Array.from(new Set(tokens));
    const chunkSize = 500;
    for (let i = 0; i < uniqueTokens.length; i += chunkSize) {
      const chunk = uniqueTokens.slice(i, i + chunkSize);
      const message: admin.messaging.MulticastMessage = {
        notification: {
          title: pushTitle,
          body: pushBody,
          imageUrl: SUBSCRIPTION_REMINDER_IMAGE
        },
        android: {
          priority: "high",
          notification: {
            channelId: "push_notifications",
            priority: "default",
            imageUrl: SUBSCRIPTION_REMINDER_IMAGE
          }
        },
        data: {
          type: "billing",
          plan,
          days: String(displayDays),
          imageUrl: SUBSCRIPTION_REMINDER_IMAGE
        },
        tokens: chunk
      };
      await admin.messaging().sendEachForMulticast(message);
      console.log(
        `Sent expiring push (plan=${plan}, days=${displayDays}) to ${chunk.length} devices.`
      );
    }
  }

});
