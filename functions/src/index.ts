import * as v1 from "firebase-functions/v1";
import { onRequest } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-southeast1";
const BASE_URL = "https://phimapi.com";

// --- API Movies (v2) ---
export const getLatestMovies = onRequest({ cors: true, region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const page = req.query.page || 1;
        const response = await fetch(`${BASE_URL}/danh-sach/phim-moi-cap-nhat?page=${page}`);
        const data = await response.json();
        res.status(200).json(data);
    } catch (error) {
        logger.error("Error in getLatestMovies", error);
        res.status(500).send("Internal Server Error");
    }
});

export const getMoviesByType = onRequest({ cors: true, region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const type = req.query.type || 'phim-bo';
        const page = req.query.page || 1;
        const limit = req.query.limit || 10;
        const url = `${BASE_URL}/v1/api/danh-sach/${type}?page=${page}&limit=${limit}&sort_field=modified.time&sort_type=desc`;
        const response = await fetch(url);
        const data = await response.json();
        res.status(200).json(data);
    } catch (error) {
        logger.error("Error in getMoviesByType", error);
        res.status(500).send("Internal Server Error");
    }
});

export const getMoviesByCategory = onRequest({ cors: true, region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const slug = req.query.slug || 'hanh-dong';
        const page = req.query.page || 1;
        const limit = req.query.limit || 10;
        const url = `${BASE_URL}/v1/api/the-loai/${slug}?page=${page}&limit=${limit}&sort_field=modified.time&sort_type=desc`;
        const response = await fetch(url);
        const data = await response.json();
        res.status(200).json(data);
    } catch (error) {
        logger.error("Error in getMoviesByCategory", error);
        res.status(500).send("Internal Server Error");
    }
});

export const getMovieDetail = onRequest({ cors: true, region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const slug = req.query.slug;
        if (!slug) {
            res.status(400).send("Missing slug");
            return;
        }
        const response = await fetch(`${BASE_URL}/phim/${slug}`);
        const data = await response.json();
        res.status(200).json(data);
    } catch (error) {
        logger.error("Error in getMovieDetail", error);
        res.status(500).send("Internal Server Error");
    }
});

// --- Auth Admin (v2) ---
export const resetPasswordAdmin = onRequest({ cors: true, region: REGION, invoker: "public" }, async (req, res) => {
    // Không cần set Header CORS thủ công vì đã có { cors: true }
    try {
        const { email, newPassword } = req.body;
        if (!email || !newPassword) {
            res.status(400).send("Missing email or newPassword");
            return;
        }

        const user = await admin.auth().getUserByEmail(email);
        await admin.auth().updateUser(user.uid, { password: newPassword });

        res.status(200).send("Password updated successfully");
    } catch (error: any) {
        logger.error("Error resetting password", error);
        res.status(500).send(error.message || "Internal Server Error");
    }
});

// --- Firestore Triggers ---
export const onRatingWritten = v1.region(REGION).firestore
    .document("movies/{movieId}/ratings/{userId}")
    .onWrite(async (change: any, context: any) => {
        const movieId = context.params.movieId;
        try {
            const ratingsSnapshot = await db.collection("movies").doc(movieId).collection("ratings").get();
            let totalRatings = 0;
            let sumScore = 0;
            ratingsSnapshot.forEach((doc) => {
                const rating = doc.data();
                if (typeof rating.score === "number") {
                    sumScore += rating.score;
                    totalRatings++;
                }
            });
            const averageRating = totalRatings > 0 ? (sumScore / totalRatings) : 0;
            await db.collection("movies").doc(movieId).set({
                averageRating: Math.round(averageRating * 10) / 10,
                totalRatings: totalRatings
            }, { merge: true });
        } catch (error) {
            logger.error(`Error updating stats for movie ${movieId}`, error);
        }
    });

// --- MoMo Payment (sandbox) ---
import * as crypto from "crypto";

const MOMO_PARTNER_CODE = "MOMO";
const MOMO_ACCESS_KEY = "F8BBA842ECF85";
const MOMO_SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
const MOMO_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
const MOMO_APP_PARTNER_CODE = "MOMOIQA420180417";
const MOMO_APP_SECRET_KEY = "PPuDXq1KowPT1ftR8DvlQTHhC03aul17";
const MOMO_APP_PAY_ENDPOINT = "https://test-payment.momo.vn/pay/app";
const MOMO_APP_CONFIRM_ENDPOINT = "https://test-payment.momo.vn/pay/confirm";
const MOMO_APP_PUBLIC_KEY = [
  "-----BEGIN PUBLIC KEY-----",
  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkpa+qMXS6O11x7jBGo9W3yxeHEsAdyDE",
  "40UoXhoQf9K6attSIclTZMEGfq6gmJm2BogVJtPkjvri5/j9mBntA8qKMzzanSQaBEbr8FyByHnf226dsL",
  "t1RbJSMLjCd3UC1n0Yq8KKvfHhvmvVbGcWfpgfo7iQTVmL0r1eQxzgnSq31EL1yYNMuaZjpHmQuT2",
  "4Hmxl9W9enRtJyVTUhwKhtjOSOsR03sMnsckpFT9pn1/V9BE2Kf3rFGqc6JukXkqK6ZW9mtmGLSq3",
  "K+JRRq2w8PVmcbcvTr/adW4EL2yc1qk9Ec4HtiDhtSYd6/ov8xLVkKAQjLVt7Ex3/agRPfPrNwIDAQAB",
  "-----END PUBLIC KEY-----",
].join("\n");
const MOMO_PAID_PLANS = new Set(["basic", "couple", "premium"]);

type MomoCreatePaymentBody = {
  amount?: number | string;
  orderInfo?: string;
  plan?: string;
};

type MomoAppPaymentBody = MomoCreatePaymentBody & {
  orderId?: string;
  token?: string;
  phoneNumber?: string;
  env?: string;
};

type MomoOrderDoc = {
  uid: string;
  plan: string;
  amount: number;
  orderInfo: string;
};

function valueAsString(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function normalizeMomoPlan(plan: unknown): string | null {
  const normalized = valueAsString(plan).trim().toLowerCase();
  return MOMO_PAID_PLANS.has(normalized) ? normalized : null;
}

function getBearerToken(authorizationHeader: string | undefined): string | null {
  if (!authorizationHeader?.startsWith("Bearer ")) {
    return null;
  }
  return authorizationHeader.slice("Bearer ".length).trim() || null;
}

function buildMomoIpnRawSignature(data: Record<string, unknown>): string {
  return `accessKey=${MOMO_ACCESS_KEY}` +
    `&amount=${valueAsString(data.amount)}` +
    `&extraData=${valueAsString(data.extraData)}` +
    `&message=${valueAsString(data.message)}` +
    `&orderId=${valueAsString(data.orderId)}` +
    `&orderInfo=${valueAsString(data.orderInfo)}` +
    `&orderType=${valueAsString(data.orderType)}` +
    `&partnerCode=${valueAsString(data.partnerCode)}` +
    `&payType=${valueAsString(data.payType)}` +
    `&requestId=${valueAsString(data.requestId)}` +
    `&responseTime=${valueAsString(data.responseTime)}` +
    `&resultCode=${valueAsString(data.resultCode)}` +
    `&transId=${valueAsString(data.transId)}`;
}

function isValidMomoIpnSignature(data: Record<string, unknown>): boolean {
  const receivedSignature = valueAsString(data.signature);
  if (!receivedSignature) {
    return false;
  }

  const expectedSignature = crypto
    .createHmac("sha256", MOMO_SECRET_KEY)
    .update(buildMomoIpnRawSignature(data))
    .digest("hex");
  const receivedBuffer = Buffer.from(receivedSignature, "utf8");
  const expectedBuffer = Buffer.from(expectedSignature, "utf8");

  return receivedBuffer.length === expectedBuffer.length &&
    crypto.timingSafeEqual(receivedBuffer, expectedBuffer);
}

function normalizeMomoAppOrderId(orderId: unknown): string | null {
  const normalized = valueAsString(orderId).trim();
  const pattern = new RegExp(
    `^${MOMO_APP_PARTNER_CODE}-\\d{13}-` +
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
    "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  );
  return pattern.test(normalized) ? normalized : null;
}

function createMomoAppRsaHash(data: Record<string, unknown>): string {
  return crypto.publicEncrypt(
    {
      key: MOMO_APP_PUBLIC_KEY,
      padding: crypto.constants.RSA_PKCS1_PADDING,
    },
    Buffer.from(JSON.stringify(data), "utf8")
  ).toString("base64");
}

function buildMomoAppPayResponseRawSignature(
  data: Record<string, unknown>
): string {
  return `status=${valueAsString(data.status)}` +
    `&message=${valueAsString(data.message)}` +
    `&amount=${valueAsString(data.amount)}` +
    `&transid=${valueAsString(data.transid)}`;
}

function isValidMomoAppPayResponseSignature(
  data: Record<string, unknown>
): boolean {
  const receivedSignature = valueAsString(data.signature);
  if (!receivedSignature) {
    return false;
  }

  const expectedSignature = crypto
    .createHmac("sha256", MOMO_APP_SECRET_KEY)
    .update(buildMomoAppPayResponseRawSignature(data))
    .digest("hex");
  const receivedBuffer = Buffer.from(receivedSignature, "utf8");
  const expectedBuffer = Buffer.from(expectedSignature, "utf8");

  return receivedBuffer.length === expectedBuffer.length &&
    crypto.timingSafeEqual(receivedBuffer, expectedBuffer);
}

function buildMomoAppConfirmRawSignature(data: {
  partnerRefId: string;
  requestType: string;
  requestId: string;
  momoTransId: string;
}): string {
  return `partnerCode=${MOMO_APP_PARTNER_CODE}` +
    `&partnerRefId=${data.partnerRefId}` +
    `&requestType=${data.requestType}` +
    `&requestId=${data.requestId}` +
    `&momoTransId=${data.momoTransId}`;
}

function signMomoAppConfirm(data: {
  partnerRefId: string;
  requestType: string;
  requestId: string;
  momoTransId: string;
}): string {
  return crypto
    .createHmac("sha256", MOMO_APP_SECRET_KEY)
    .update(buildMomoAppConfirmRawSignature(data))
    .digest("hex");
}

function isValidMomoAppConfirmResponseSignature(
  data: Record<string, unknown>
): boolean {
  const receivedSignature = valueAsString(data.signature);
  const responseData = data.data as Record<string, unknown> | undefined;
  if (!receivedSignature || !responseData) {
    return false;
  }

  const rawSignature = `amount=${valueAsString(responseData.amount)}` +
    `&momoTransId=${valueAsString(responseData.momoTransId)}` +
    `&partnerCode=${valueAsString(responseData.partnerCode)}` +
    `&partnerRefId=${valueAsString(responseData.partnerRefId)}`;
  const expectedSignature = crypto
    .createHmac("sha256", MOMO_APP_SECRET_KEY)
    .update(rawSignature)
    .digest("hex");
  const receivedBuffer = Buffer.from(receivedSignature, "utf8");
  const expectedBuffer = Buffer.from(expectedSignature, "utf8");

  return receivedBuffer.length === expectedBuffer.length &&
    crypto.timingSafeEqual(receivedBuffer, expectedBuffer);
}

function getMomoAppConfirmData(
  data: Record<string, unknown>
): Record<string, unknown> {
  return data.data && typeof data.data === "object" ?
    data.data as Record<string, unknown> :
    {};
}

function maskPhoneNumber(phoneNumber: string): string {
  if (phoneNumber.length <= 7) {
    return "***";
  }
  return `${phoneNumber.slice(0, 4)}***${phoneNumber.slice(-3)}`;
}

function getSubscriptionExpiry(durationMonths = 1): admin.firestore.Timestamp {
  const expiresAt = new Date();
  expiresAt.setMonth(expiresAt.getMonth() + durationMonths);
  return admin.firestore.Timestamp.fromDate(expiresAt);
}

export const createMomoPayment = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        return;
      }

      const idToken = getBearerToken(req.header("Authorization"));
      if (!idToken) {
        res.status(401).json({error: "Missing Firebase auth token"});
        return;
      }

      let decodedToken: admin.auth.DecodedIdToken;
      try {
        decodedToken = await admin.auth().verifyIdToken(idToken);
      } catch (error) {
        logger.warn("Invalid Firebase auth token for MoMo payment", error);
        res.status(401).json({error: "Invalid Firebase auth token"});
        return;
      }

      const {amount, orderInfo, plan} = req.body as MomoCreatePaymentBody;
      const numericAmount = Number(amount);
      const normalizedPlan = normalizeMomoPlan(plan);
      if (!Number.isFinite(numericAmount) || numericAmount <= 0 || !orderInfo) {
        res.status(400).json({error: "Missing or invalid amount/orderInfo"});
        return;
      }
      if (!normalizedPlan) {
        res.status(400).json({error: "Invalid subscription plan"});
        return;
      }

      const requestId = MOMO_PARTNER_CODE + Date.now();
      const orderId = requestId;
      const redirectUrl = "https://alpha-cinema-39dfb.web.app/payment-success";
      const ipnUrl =
        `https://${REGION}-alpha-cinema-39dfb.cloudfunctions.net/momoIpn`;
      const requestType = "captureWallet";
      const extraData = Buffer.from(JSON.stringify({orderId}), "utf8")
        .toString("base64");

      await db.collection("momoOrders").doc(orderId).set({
        uid: decodedToken.uid,
        plan: normalizedPlan,
        amount: numericAmount,
        orderInfo,
        status: "pending",
        provider: "momo",
        paymentMethod: "MoMo",
        requestId,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Build raw signature string (alphabetical order per MoMo docs)
      const rawSignature =
        `accessKey=${MOMO_ACCESS_KEY}` +
        `&amount=${numericAmount}` +
        `&extraData=${extraData}` +
        `&ipnUrl=${ipnUrl}` +
        `&orderId=${orderId}` +
        `&orderInfo=${orderInfo}` +
        `&partnerCode=${MOMO_PARTNER_CODE}` +
        `&redirectUrl=${redirectUrl}` +
        `&requestId=${requestId}` +
        `&requestType=${requestType}`;

      // HMAC SHA256 signature (server-side only)
      const signature = crypto
        .createHmac("sha256", MOMO_SECRET_KEY)
        .update(rawSignature)
        .digest("hex");

      const requestBody = {
        partnerCode: MOMO_PARTNER_CODE,
        accessKey: MOMO_ACCESS_KEY,
        requestId,
        amount: numericAmount,
        orderId,
        orderInfo,
        redirectUrl,
        ipnUrl,
        extraData,
        requestType,
        signature,
        lang: "vi",
      };

      logger.info("MoMo request", {
        orderId,
        amount: numericAmount,
        uid: decodedToken.uid,
        plan: normalizedPlan,
      });

      const momoRes = await fetch(MOMO_ENDPOINT, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(requestBody),
      });

      const momoData = await momoRes.json() as Record<string, unknown>;
      const momoResultCode = Number(momoData.resultCode);
      logger.info("MoMo response", {
        resultCode: momoResultCode,
        orderId,
      });
      if (momoResultCode !== 0) {
        await db.collection("momoOrders").doc(orderId).set({
          status: "failed",
          message: valueAsString(momoData.message) || "MoMo rejected payment",
          momoResult: momoData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
      }

      res.status(200).json(momoData);
    } catch (error) {
      logger.error("Error in createMomoPayment", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  }
);

export const processMomoAppPayment = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        return;
      }

      const idToken = getBearerToken(req.header("Authorization"));
      if (!idToken) {
        res.status(401).json({error: "Missing Firebase auth token"});
        return;
      }

      let decodedToken: admin.auth.DecodedIdToken;
      try {
        decodedToken = await admin.auth().verifyIdToken(idToken);
      } catch (error) {
        logger.warn("Invalid Firebase auth token for MoMo app payment", error);
        res.status(401).json({error: "Invalid Firebase auth token"});
        return;
      }

      const {
        amount,
        orderInfo,
        plan,
        token,
        phoneNumber,
        env,
        orderId,
      } = req.body as MomoAppPaymentBody;
      const numericAmount = Number(amount);
      const normalizedPlan = normalizeMomoPlan(plan);
      const normalizedOrderId = normalizeMomoAppOrderId(orderId);
      const momoToken = valueAsString(token).trim();
      const customerNumber = valueAsString(phoneNumber).trim();

      if (!Number.isFinite(numericAmount) || numericAmount <= 0 || !orderInfo) {
        res.status(400).json({error: "Missing or invalid amount/orderInfo"});
        return;
      }
      if (!normalizedPlan) {
        res.status(400).json({error: "Invalid subscription plan"});
        return;
      }
      if (!normalizedOrderId || !momoToken || !customerNumber) {
        res.status(400).json({error: "Missing MoMo SDK token/order/phone"});
        return;
      }

      const orderRef = db.collection("momoOrders").doc(normalizedOrderId);
      await orderRef.set({
        uid: decodedToken.uid,
        plan: normalizedPlan,
        amount: numericAmount,
        orderInfo,
        status: "processing",
        provider: "momo",
        paymentMethod: "MoMo SDK",
        momoEnvironment: valueAsString(env) || "app",
        customerNumberMasked: maskPhoneNumber(customerNumber),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});

      const hash = createMomoAppRsaHash({
        partnerCode: MOMO_APP_PARTNER_CODE,
        partnerRefId: normalizedOrderId,
        amount: numericAmount,
        partnerTransId: normalizedOrderId,
        description: orderInfo,
      });

      const payRequestBody = {
        customerNumber,
        partnerCode: MOMO_APP_PARTNER_CODE,
        partnerRefId: normalizedOrderId,
        appData: momoToken,
        hash,
        description: orderInfo,
        version: 2,
        amount: numericAmount,
      };

      logger.info("MoMo app payment request", {
        orderId: normalizedOrderId,
        amount: numericAmount,
        uid: decodedToken.uid,
        plan: normalizedPlan,
      });

      const payResponse = await fetch(MOMO_APP_PAY_ENDPOINT, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(payRequestBody),
      });
      const payData = await payResponse.json() as Record<string, unknown>;
      const payStatus = Number(payData.status);

      logger.info("MoMo app payment response", {
        orderId: normalizedOrderId,
        status: payStatus,
      });

      if (payStatus !== 0) {
        await orderRef.set({
          status: "failed",
          message: valueAsString(payData.message) || "MoMo rejected payment",
          momoResult: payData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({
          ...payData,
          orderId: normalizedOrderId,
          resultCode: payStatus,
        });
        return;
      }

      if (!isValidMomoAppPayResponseSignature(payData)) {
        await orderRef.set({
          status: "failed",
          message: "Invalid MoMo app payment signature",
          momoResult: payData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(401).json({
          orderId: normalizedOrderId,
          status: -1,
          resultCode: -1,
          message: "Invalid MoMo app payment signature",
        });
        return;
      }

      if (Number(payData.amount) !== numericAmount) {
        await orderRef.set({
          status: "failed",
          message: "Invalid MoMo app payment amount",
          momoResult: payData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({
          orderId: normalizedOrderId,
          status: -1,
          resultCode: -1,
          message: "Invalid MoMo app payment amount",
        });
        return;
      }

      const momoTransId = valueAsString(payData.transid);
      if (!momoTransId) {
        await orderRef.set({
          status: "failed",
          message: "MoMo response did not include transaction id",
          momoResult: payData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({
          orderId: normalizedOrderId,
          status: -1,
          resultCode: -1,
          message: "MoMo response did not include transaction id",
        });
        return;
      }

      const confirmRequest = {
        partnerRefId: normalizedOrderId,
        requestType: "capture",
        requestId: `${normalizedOrderId}-capture`,
        momoTransId,
      };
      const confirmResponse = await fetch(MOMO_APP_CONFIRM_ENDPOINT, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
          partnerCode: MOMO_APP_PARTNER_CODE,
          partnerRefId: confirmRequest.partnerRefId,
          requestType: confirmRequest.requestType,
          requestId: confirmRequest.requestId,
          momoTransId: confirmRequest.momoTransId,
          customerNumber,
          signature: signMomoAppConfirm(confirmRequest),
        }),
      });
      const confirmData =
        await confirmResponse.json() as Record<string, unknown>;
      const confirmStatus = Number(confirmData.status);
      const confirmPayload = getMomoAppConfirmData(confirmData);

      if (confirmStatus !== 0) {
        await orderRef.set({
          status: "failed",
          message: valueAsString(confirmData.message) ||
            "MoMo confirm payment failed",
          momoResult: payData,
          momoConfirmResult: confirmData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({
          ...confirmData,
          orderId: normalizedOrderId,
          resultCode: confirmStatus,
        });
        return;
      }

      if (!isValidMomoAppConfirmResponseSignature(confirmData)) {
        await orderRef.set({
          status: "failed",
          message: "Invalid MoMo confirm payment signature",
          momoResult: payData,
          momoConfirmResult: confirmData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(401).json({
          orderId: normalizedOrderId,
          status: -1,
          resultCode: -1,
          message: "Invalid MoMo confirm payment signature",
        });
        return;
      }

      if (Number(confirmPayload.amount) !== numericAmount ||
        valueAsString(confirmPayload.partnerRefId) !== normalizedOrderId ||
        valueAsString(confirmPayload.momoTransId) !== momoTransId) {
        await orderRef.set({
          status: "failed",
          message: "MoMo confirm payment data mismatch",
          momoResult: payData,
          momoConfirmResult: confirmData,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({
          orderId: normalizedOrderId,
          status: -1,
          resultCode: -1,
          message: "MoMo confirm payment data mismatch",
        });
        return;
      }

      await db.runTransaction(async (transaction) => {
        const orderSnapshot = await transaction.get(orderRef);
        if (!orderSnapshot.exists) {
          throw new Error("MoMo app order disappeared before capture");
        }

        const order = orderSnapshot.data() as MomoOrderDoc;
        if (order.uid !== decodedToken.uid || order.amount !== numericAmount) {
          throw new Error("MoMo app order owner or amount mismatch");
        }

        const userRef = db.collection("users").doc(order.uid);
        const subscriptionStartedAt =
          admin.firestore.FieldValue.serverTimestamp();
        transaction.set(orderRef, {
          status: "paid",
          message: valueAsString(confirmData.message) ||
            valueAsString(payData.message),
          transId: momoTransId,
          momoResult: payData,
          momoConfirmResult: confirmData,
          paidAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        transaction.set(userRef, {
          uid: order.uid,
          subscriptionPlan: order.plan,
          subscriptionStatus: "active",
          subscriptionStartedAt,
          subscriptionExpiresAt: getSubscriptionExpiry(),
          subscriptionPaymentMethod: "MoMo",
          subscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
      });

      logger.info("MoMo app payment SUCCESS", {
        orderId: normalizedOrderId,
        uid: decodedToken.uid,
      });

      res.status(200).json({
        ...payData,
        orderId: normalizedOrderId,
        status: 0,
        resultCode: 0,
        confirmStatus,
      });
    } catch (error) {
      logger.error("Error in processMomoAppPayment", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  }
);

// MoMo IPN callback - receives payment result from MoMo
export const momoIpn = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        return;
      }

      const ipnBody = req.body as Record<string, unknown>;
      const orderId = valueAsString(ipnBody.orderId);
      const resultCode = Number(ipnBody.resultCode);
      const message = valueAsString(ipnBody.message);
      const amount = Number(ipnBody.amount);
      logger.info("MoMo IPN received", {
        orderId, resultCode, message, amount,
      });

      if (!orderId || !isValidMomoIpnSignature(ipnBody)) {
        logger.warn("Invalid MoMo IPN signature", {orderId});
        res.status(401).send("Invalid signature");
        return;
      }

      const orderRef = db.collection("momoOrders").doc(orderId);
      await db.runTransaction(async (transaction) => {
        const orderSnapshot = await transaction.get(orderRef);
        if (!orderSnapshot.exists) {
          logger.warn("MoMo IPN for unknown order", {orderId});
          return;
        }

        const order = orderSnapshot.data() as MomoOrderDoc;
        if (amount !== order.amount) {
          transaction.set(orderRef, {
            status: "failed",
            message: "Invalid payment amount",
            momoResult: ipnBody,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          }, {merge: true});
          return;
        }

        if (resultCode === 0) {
          const userRef = db.collection("users").doc(order.uid);
          const subscriptionStartedAt =
            admin.firestore.FieldValue.serverTimestamp();
          transaction.set(orderRef, {
            status: "paid",
            message,
            transId: valueAsString(ipnBody.transId),
            momoResult: ipnBody,
            paidAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          }, {merge: true});
          transaction.set(userRef, {
            uid: order.uid,
            subscriptionPlan: order.plan,
            subscriptionStatus: "active",
            subscriptionStartedAt,
            subscriptionExpiresAt: getSubscriptionExpiry(),
            subscriptionPaymentMethod: "MoMo",
            subscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          }, {merge: true});
          logger.info("Payment SUCCESS", {orderId, uid: order.uid});
        } else {
          transaction.set(orderRef, {
            status: "failed",
            message,
            resultCode,
            momoResult: ipnBody,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          }, {merge: true});
          logger.warn("Payment FAILED", {orderId, resultCode});
        }
      });

      // MoMo requires 204 No Content response
      res.status(204).send();
    } catch (error) {
      logger.error("Error in momoIpn", error);
      res.status(204).send();
    }
  }
);

// --- Push Notifications ---
export * from "./notifications";
export * from "./cleanupDupes";

