import * as v1 from "firebase-functions/v1";
import { onRequest } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const rtdb = admin.database();

const REGION = "asia-southeast1";
const BASE_URL = "https://phimapi.com";
const WEB_APP_ORIGIN = "https://alpha-cinema-39dfb.web.app";
const ANDROID_PACKAGE_NAME = "com.example.alphacinema";

type PhimApiCategory = {
    name?: string;
};

type PhimApiMovie = {
    name?: string;
    slug?: string;
    origin_name?: string;
    content?: string;
    poster_url?: string;
    thumb_url?: string;
    year?: number | string;
    episode_current?: string;
    quality?: string;
    category?: PhimApiCategory[];
};

type PhimApiMovieDetailResponse = {
    status?: boolean;
    movie?: PhimApiMovie | null;
};

type MoviePreviewRequest = {
    query: Record<string, unknown>;
    path?: string;
    originalUrl?: string;
    url?: string;
};

function escapeHtml(value: string): string {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function stripHtml(value: string): string {
    return value
        .replace(/<[^>]*>/g, " ")
        .replace(/\s+/g, " ")
        .trim();
}

function truncateText(value: string, maxLength: number): string {
    if (value.length <= maxLength) {
        return value;
    }
    return `${value.slice(0, Math.max(0, maxLength - 1)).trim()}…`;
}

function normalizePhimImageUrl(url: string | undefined): string {
    const trimmed = valueAsString(url).trim();
    if (!trimmed) {
        return "";
    }
    return trimmed.startsWith("http") ? trimmed : `https://phimimg.com/${trimmed}`;
}

function decodePathSegment(segment: string): string {
    try {
        return decodeURIComponent(segment);
    } catch {
        return segment;
    }
}

function extractMovieSlugFromPath(path: string): string | null {
    const cleanPath = path.split("?")[0];
    const segments = cleanPath.split("/")
        .filter((segment) => segment.trim().length > 0)
        .map(decodePathSegment);
    if (segments[0] === "movie") {
        return segments[1] || null;
    }
    return segments[0] || null;
}

function getMovieShareSlug(req: MoviePreviewRequest): string | null {
    const querySlug = valueAsString(req.query.slug).trim();
    if (querySlug) {
        return querySlug;
    }

    const pathSources = [
        valueAsString(req.path),
        valueAsString(req.originalUrl),
        valueAsString(req.url),
    ];
    return pathSources
        .map(extractMovieSlugFromPath)
        .find((slug) => slug !== null && slug.trim().length > 0) || null;
}

function buildMoviePreviewHtml(params: {
    canonicalUrl: string;
    deepLink: string;
    title: string;
    description: string;
    imageUrl: string;
    subtitle: string;
}): string {
    const title = escapeHtml(params.title);
    const description = escapeHtml(params.description);
    const canonicalUrl = escapeHtml(params.canonicalUrl);
    const deepLink = escapeHtml(params.deepLink);
    const imageUrl = escapeHtml(params.imageUrl);
    const subtitle = escapeHtml(params.subtitle);
    const imageTags = imageUrl ? `
    <meta property="og:image" content="${imageUrl}">
    <meta property="og:image:secure_url" content="${imageUrl}">
    <meta name="twitter:image" content="${imageUrl}">` : "";
    const imageMarkup = imageUrl ? `<img src="${imageUrl}" alt="${title}">` : "";

    return `<!doctype html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${description}">
    <link rel="canonical" href="${canonicalUrl}">
    <meta property="og:type" content="video.movie">
    <meta property="og:site_name" content="Alpha Cinema">
    <meta property="og:title" content="${title}">
    <meta property="og:description" content="${description}">
    <meta property="og:url" content="${canonicalUrl}">${imageTags}
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${title}">
    <meta name="twitter:description" content="${description}">
    <meta property="al:android:app_name" content="Alpha Cinema">
    <meta property="al:android:package" content="${ANDROID_PACKAGE_NAME}">
    <meta property="al:android:url" content="${deepLink}">
    <style>
        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            background: #050505;
            color: #fff;
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }
        main {
            width: min(92vw, 720px);
            padding: 32px 0;
        }
        img {
            width: 100%;
            aspect-ratio: 16 / 9;
            object-fit: cover;
            border-radius: 14px;
            background: #181818;
        }
        h1 {
            margin: 18px 0 6px;
            font-size: clamp(28px, 5vw, 44px);
            line-height: 1.08;
        }
        p {
            margin: 0 0 20px;
            color: #cfcfcf;
            line-height: 1.5;
        }
        a {
            display: inline-block;
            padding: 12px 18px;
            border-radius: 999px;
            background: #f6e29a;
            color: #080808;
            font-weight: 700;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <main>
        ${imageMarkup}
        <h1>${title}</h1>
        <p>${subtitle || description}</p>
        <a href="${deepLink}">Mở trong Alpha Cinema</a>
    </main>
</body>
</html>`;
}

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

export const movieSharePreview = onRequest({ region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const slug = getMovieShareSlug(req);
        if (!slug) {
            res.status(400).send("Missing movie slug");
            return;
        }

        const response = await fetch(`${BASE_URL}/phim/${encodeURIComponent(slug)}`);
        if (!response.ok) {
            res.status(response.status).send("Movie not found");
            return;
        }

        const data = await response.json() as PhimApiMovieDetailResponse;
        const movie = data.movie;
        if (!movie?.name) {
            res.status(404).send("Movie not found");
            return;
        }

        const canonicalUrl = `${WEB_APP_ORIGIN}/movie/${encodeURIComponent(slug)}`;
        const deepLink = `alphacinema://movie/${encodeURIComponent(slug)}`;
        const metadataParts = [
            movie.origin_name,
            valueAsString(movie.year),
            movie.quality,
            movie.episode_current,
            ...(movie.category ?? []).map((category) => category.name),
        ].map((value) => valueAsString(value).trim()).filter(Boolean);
        const subtitle = metadataParts.join(" · ");
        const description = truncateText(
            stripHtml(valueAsString(movie.content)) ||
                subtitle ||
                `Xem ${movie.name} trên Alpha Cinema.`,
            180
        );
        const imageUrl = normalizePhimImageUrl(movie.thumb_url) ||
            normalizePhimImageUrl(movie.poster_url);

        res.set("Cache-Control", "public, max-age=300, s-maxage=3600");
        res.set("Content-Type", "text/html; charset=utf-8");
        res.status(200).send(buildMoviePreviewHtml({
            canonicalUrl,
            deepLink,
            title: movie.name,
            description,
            imageUrl,
            subtitle,
        }));
    } catch (error) {
        logger.error("Error in movieSharePreview", error);
        res.status(500).send("Internal Server Error");
    }
});

// --- Watch Party Share Preview (v2) ---

function extractWatchPartyRoomIdFromPath(path: string): string | null {
    const cleanPath = path.split("?")[0];
    const segments = cleanPath.split("/")
        .filter((segment) => segment.trim().length > 0)
        .map(decodePathSegment);
    if (segments[0] === "watchparty") {
        return segments[1] || null;
    }
    return segments[0] || null;
}

function getWatchPartyRoomId(req: MoviePreviewRequest): string | null {
    const queryRoomId = valueAsString(req.query.roomId).trim();
    if (queryRoomId) {
        return queryRoomId;
    }

    const pathSources = [
        valueAsString(req.path),
        valueAsString(req.originalUrl),
        valueAsString(req.url),
    ];
    return pathSources
        .map(extractWatchPartyRoomIdFromPath)
        .find((id) => id !== null && id.trim().length > 0) || null;
}

function buildWatchPartyPreviewHtml(params: {
    canonicalUrl: string;
    deepLink: string;
    title: string;
    description: string;
    imageUrl: string;
    subtitle: string;
}): string {
    const title = escapeHtml(params.title);
    const description = escapeHtml(params.description);
    const canonicalUrl = escapeHtml(params.canonicalUrl);
    const deepLink = escapeHtml(params.deepLink);
    const imageUrl = escapeHtml(params.imageUrl);
    const subtitle = escapeHtml(params.subtitle);
    const imageTags = imageUrl ? `
    <meta property="og:image" content="${imageUrl}">
    <meta property="og:image:secure_url" content="${imageUrl}">
    <meta name="twitter:image" content="${imageUrl}">` : "";
    const imageMarkup = imageUrl ? `<img src="${imageUrl}" alt="${title}">` : "";

    return `<!doctype html>
<html lang="vi">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${description}">
    <link rel="canonical" href="${canonicalUrl}">
    <meta property="og:type" content="website">
    <meta property="og:site_name" content="Alpha Cinema">
    <meta property="og:title" content="${title}">
    <meta property="og:description" content="${description}">
    <meta property="og:url" content="${canonicalUrl}">${imageTags}
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${title}">
    <meta name="twitter:description" content="${description}">
    <meta property="al:android:app_name" content="Alpha Cinema">
    <meta property="al:android:package" content="${ANDROID_PACKAGE_NAME}">
    <meta property="al:android:url" content="${deepLink}">
    <style>
        body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            background: #050505;
            color: #fff;
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }
        main {
            width: min(92vw, 720px);
            padding: 32px 0;
        }
        img {
            width: 100%;
            aspect-ratio: 16 / 9;
            object-fit: cover;
            border-radius: 14px;
            background: #181818;
        }
        h1 {
            margin: 18px 0 6px;
            font-size: clamp(28px, 5vw, 44px);
            line-height: 1.08;
        }
        p {
            margin: 0 0 20px;
            color: #cfcfcf;
            line-height: 1.5;
        }
        a {
            display: inline-block;
            padding: 12px 18px;
            border-radius: 999px;
            background: #f6e29a;
            color: #080808;
            font-weight: 700;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <main>
        ${imageMarkup}
        <h1>${title}</h1>
        <p>${subtitle || description}</p>
        <a href="${deepLink}">Mở trong Alpha Cinema</a>
    </main>
</body>
</html>`;
}

export const watchPartySharePreview = onRequest({ region: REGION, maxInstances: 10 }, async (req, res) => {
    try {
        const roomId = getWatchPartyRoomId(req);
        if (!roomId) {
            res.status(400).send("Missing room ID");
            return;
        }

        const rtdb = admin.database();
        const snapshot = await rtdb.ref(`watchParty/${roomId}`).get();
        if (!snapshot.exists()) {
            res.status(404).send("Phòng không tồn tại hoặc đã kết thúc");
            return;
        }

        const roomData = snapshot.val() as Record<string, unknown>;
        const movieTitle = valueAsString(roomData.movieTitle).trim();
        const moviePosterUrl = valueAsString(roomData.moviePosterUrl).trim();
        const hostName = valueAsString(roomData.hostName).trim();
        const movieSlug = valueAsString(roomData.movieSlug).trim();

        // Count members
        const membersData = roomData.members as Record<string, unknown> | undefined;
        const memberCount = membersData ? Object.keys(membersData).length : 0;

        const title = movieTitle
            ? `Xem chung: ${movieTitle}`
            : "Phòng xem chung Alpha Cinema";
        const subtitle = [
            hostName ? `Chủ phòng: ${hostName}` : "",
            memberCount > 0 ? `${memberCount}/5 thành viên` : "",
        ].filter(Boolean).join(" · ");
        const description = movieTitle
            ? `Tham gia xem chung "${movieTitle}" trên Alpha Cinema.`
            : "Tham gia phòng xem chung trên Alpha Cinema.";

        // Use movie poster as image; if movie has a slug, try thumb from phimimg
        let imageUrl = "";
        if (moviePosterUrl) {
            imageUrl = normalizePhimImageUrl(moviePosterUrl);
        } else if (movieSlug) {
            // Fallback: try to fetch movie info from phimapi for the thumb
            try {
                const movieRes = await fetch(`${BASE_URL}/phim/${encodeURIComponent(movieSlug)}`);
                if (movieRes.ok) {
                    const movieData = await movieRes.json() as PhimApiMovieDetailResponse;
                    imageUrl = normalizePhimImageUrl(movieData.movie?.thumb_url) ||
                        normalizePhimImageUrl(movieData.movie?.poster_url);
                }
            } catch {
                // Ignore – no image fallback
            }
        }

        const canonicalUrl = `${WEB_APP_ORIGIN}/watchparty/${encodeURIComponent(roomId)}`;
        const deepLink = `alphacinema://watchparty/${encodeURIComponent(roomId)}`;

        res.set("Cache-Control", "public, max-age=30, s-maxage=60");
        res.set("Content-Type", "text/html; charset=utf-8");
        res.status(200).send(buildWatchPartyPreviewHtml({
            canonicalUrl,
            deepLink,
            title,
            description,
            imageUrl,
            subtitle,
        }));
    } catch (error) {
        logger.error("Error in watchPartySharePreview", error);
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

type PaidPlanId = "basic" | "couple" | "premium";

type PlanCatalogEntry = {
  amount: number;
  durationMonths: number;
  watchPartyMaxMembers: number;
};

const PLAN_CATALOG: Record<PaidPlanId, PlanCatalogEntry> = {
  basic: {
    amount: 29000,
    durationMonths: 1,
    watchPartyMaxMembers: 0,
  },
  couple: {
    amount: 59000,
    durationMonths: 1,
    watchPartyMaxMembers: 2,
  },
  premium: {
    amount: 99000,
    durationMonths: 1,
    watchPartyMaxMembers: 10,
  },
};

const MOMO_PAID_PLANS = new Set(Object.keys(PLAN_CATALOG));
const WATCH_PARTY_ROOM_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

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
  durationMonths?: number;
  orderInfo: string;
};

type CreateWatchPartyRoomBody = {
  movieSlug?: string;
  movieTitle?: string;
  moviePosterUrl?: string;
  episodeId?: string | null;
  episodeName?: string | null;
};

type JoinWatchPartyRoomBody = {
  roomId?: string;
};

type LeaveWatchPartyRoomBody = {
  roomId?: string;
};

function valueAsString(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function normalizeMomoPlan(plan: unknown): string | null {
  const normalized = valueAsString(plan).trim().toLowerCase();
  return MOMO_PAID_PLANS.has(normalized) ? normalized : null;
}

function getPlanCatalogEntry(plan: string): PlanCatalogEntry | null {
  return Object.prototype.hasOwnProperty.call(PLAN_CATALOG, plan) ?
    PLAN_CATALOG[plan as PaidPlanId] :
    null;
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

function getOrderDurationMonths(order: MomoOrderDoc): number {
  return order.durationMonths ??
    getPlanCatalogEntry(order.plan)?.durationMonths ??
    1;
}

function timestampToMillis(value: unknown): number | null {
  if (value instanceof admin.firestore.Timestamp) {
    return value.toMillis();
  }
  if (value instanceof Date) {
    return value.getTime();
  }
  if (typeof value === "object" && value !== null) {
    const timestampLike = value as { toMillis?: () => number };
    if (typeof timestampLike.toMillis === "function") {
      const millis = timestampLike.toMillis();
      return Number.isFinite(millis) ? millis : null;
    }
  }
  return null;
}

function activePaidPlanFromProfile(
  profileData: Record<string, unknown>
): PaidPlanId | null {
  const plan = normalizeMomoPlan(profileData.subscriptionPlan);
  const status = valueAsString(profileData.subscriptionStatus)
    .trim()
    .toLowerCase();
  const expiresAtMillis = timestampToMillis(profileData.subscriptionExpiresAt);
  if (!plan || status !== "active" || !expiresAtMillis ||
      expiresAtMillis <= Date.now()) {
    return null;
  }
  return plan as PaidPlanId;
}

function watchPartyMaxMembersForProfile(
  profileData: Record<string, unknown>
): number {
  const activePlan = activePaidPlanFromProfile(profileData);
  if (!activePlan) {
    return 0;
  }
  return getPlanCatalogEntry(activePlan)?.watchPartyMaxMembers ?? 0;
}

async function verifyFirebaseAuthHeader(
  authorizationHeader: string | string[] | undefined
): Promise<admin.auth.DecodedIdToken | null> {
  const header = Array.isArray(authorizationHeader) ?
    authorizationHeader[0] :
    authorizationHeader;
  const idToken = getBearerToken(header);
  if (!idToken) {
    return null;
  }
  try {
    return await admin.auth().verifyIdToken(idToken);
  } catch (error) {
    logger.warn("Invalid Firebase auth token", error);
    return null;
  }
}

function normalizeWatchPartyRoomId(roomId: unknown): string | null {
  const normalized = valueAsString(roomId).trim().toUpperCase();
  return /^[A-Z0-9]{6}$/.test(normalized) ? normalized : null;
}

function generateWatchPartyRoomId(): string {
  return Array.from({length: 6}, () =>
    WATCH_PARTY_ROOM_ID_CHARS[
      Math.floor(Math.random() * WATCH_PARTY_ROOM_ID_CHARS.length)
    ]
  ).join("");
}

async function createUniqueWatchPartyRoomId(): Promise<string> {
  for (let attempt = 0; attempt < 8; attempt++) {
    const roomId = generateWatchPartyRoomId();
    const snapshot = await rtdb.ref(`watchParty/${roomId}`).get();
    if (!snapshot.exists()) {
      return roomId;
    }
  }
  throw new Error("Unable to generate unique watch party room ID");
}

function displayNameForMember(
  decodedToken: admin.auth.DecodedIdToken,
  profileData: Record<string, unknown>
): string {
  return valueAsString(profileData.displayName).trim() ||
    valueAsString(decodedToken.name).trim() ||
    "Người dùng";
}

function photoUrlForMember(
  decodedToken: admin.auth.DecodedIdToken,
  profileData: Record<string, unknown>
): string {
  return valueAsString(profileData.photoUrl).trim() ||
    valueAsString(decodedToken.picture).trim();
}

export const createWatchPartyRoom = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({error: "Method Not Allowed"});
        return;
      }

      const decodedToken = await verifyFirebaseAuthHeader(
        req.headers.authorization
      );
      if (!decodedToken) {
        res.status(401).json({error: "Invalid Firebase auth token"});
        return;
      }

      const userSnapshot = await db.collection("users")
        .doc(decodedToken.uid)
        .get();
      const profileData =
        (userSnapshot.data() ?? {}) as Record<string, unknown>;
      const maxMembers = watchPartyMaxMembersForProfile(profileData);
      if (maxMembers <= 0) {
        res.status(403).json({
          error: "Gói Couple hoặc Premium mới có thể tạo phòng xem chung",
        });
        return;
      }

      const body = req.body as CreateWatchPartyRoomBody;
      const roomId = await createUniqueWatchPartyRoomId();
      const now = Date.now();
      const hostName = displayNameForMember(decodedToken, profileData);
      const hostPhotoUrl = photoUrlForMember(decodedToken, profileData);
      const roomRef = rtdb.ref(`watchParty/${roomId}`);
      await roomRef.set({
        roomId,
        hostId: decodedToken.uid,
        hostName,
        movieSlug: valueAsString(body.movieSlug).trim(),
        movieTitle: valueAsString(body.movieTitle).trim(),
        moviePosterUrl: valueAsString(body.moviePosterUrl).trim(),
        episodeId: valueAsString(body.episodeId).trim(),
        episodeName: valueAsString(body.episodeName).trim(),
        playbackState: "paused",
        currentTimeSec: 0,
        playStartedAt: 0,
        lastUpdated: admin.database.ServerValue.TIMESTAMP,
        createdAt: now,
        maxMembers,
        members: {
          [decodedToken.uid]: {
            uid: decodedToken.uid,
            displayName: hostName,
            photoUrl: hostPhotoUrl,
          },
        },
      });

      res.status(200).json({
        roomId,
        maxMembers,
        message: "Created",
      });
    } catch (error) {
      logger.error("Error in createWatchPartyRoom", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  }
);

export const joinWatchPartyRoom = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({error: "Method Not Allowed"});
        return;
      }

      const decodedToken = await verifyFirebaseAuthHeader(
        req.headers.authorization
      );
      if (!decodedToken) {
        res.status(401).json({error: "Invalid Firebase auth token"});
        return;
      }

      const body = req.body as JoinWatchPartyRoomBody;
      const roomId = normalizeWatchPartyRoomId(body.roomId);
      if (!roomId) {
        res.status(400).json({error: "Invalid room ID"});
        return;
      }

      const roomRef = rtdb.ref(`watchParty/${roomId}`);
      const roomSnapshot = await roomRef.get();
      if (!roomSnapshot.exists()) {
        res.status(404).json({error: "Phòng không tồn tại"});
        return;
      }

      const maxMembers =
        Number(roomSnapshot.child("maxMembers").val()) || 2;
      const userSnapshot = await db.collection("users")
        .doc(decodedToken.uid)
        .get();
      const profileData =
        (userSnapshot.data() ?? {}) as Record<string, unknown>;
      const member = {
        uid: decodedToken.uid,
        displayName: displayNameForMember(decodedToken, profileData),
        photoUrl: photoUrlForMember(decodedToken, profileData),
      };

      let roomFull = false;
      let roomMissing = false;
      const transactionResult = await roomRef.transaction(
        (currentRoom) => {
          if (!currentRoom || typeof currentRoom !== "object") {
            roomMissing = true;
            return;
          }
          const room = currentRoom as Record<string, unknown>;
          const currentMembers = room.members;
          const members = currentMembers &&
            typeof currentMembers === "object" ?
            currentMembers as Record<string, unknown> :
            {};
          if (Object.prototype.hasOwnProperty.call(members, decodedToken.uid)) {
            return room;
          }
          if (Object.keys(members).length >= maxMembers) {
            roomFull = true;
            return;
          }
          return {
            ...room,
            members: {
              ...members,
              [decodedToken.uid]: member,
            },
          };
        },
        undefined,
        false
      );

      if (!transactionResult.committed && roomMissing) {
        res.status(404).json({error: "Phòng không tồn tại"});
        return;
      }
      if (!transactionResult.committed && roomFull) {
        res.status(409).json({
          error: `Phòng đã đầy (${maxMembers}/${maxMembers} người)`,
        });
        return;
      }
      if (!transactionResult.committed) {
        res.status(409).json({error: "Không thể tham gia phòng"});
        return;
      }

      res.status(200).json({
        roomId,
        maxMembers,
        message: "Joined",
      });
    } catch (error) {
      logger.error("Error in joinWatchPartyRoom", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  }
);

export const leaveWatchPartyRoom = onRequest(
  {cors: true, region: REGION, invoker: "public"},
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        res.status(405).json({error: "Method Not Allowed"});
        return;
      }

      const decodedToken = await verifyFirebaseAuthHeader(
        req.headers.authorization
      );
      if (!decodedToken) {
        res.status(401).json({error: "Invalid Firebase auth token"});
        return;
      }

      const body = req.body as LeaveWatchPartyRoomBody;
      const roomId = normalizeWatchPartyRoomId(body.roomId);
      if (!roomId) {
        res.status(400).json({error: "Invalid room ID"});
        return;
      }

      const roomRef = rtdb.ref(`watchParty/${roomId}`);
      const roomSnapshot = await roomRef.get();
      if (!roomSnapshot.exists()) {
        res.status(200).json({roomId, maxMembers: null, message: "Left"});
        return;
      }

      const hostId = valueAsString(roomSnapshot.child("hostId").val());
      if (hostId === decodedToken.uid) {
        await roomRef.remove();
      } else {
        await roomRef.child("members").child(decodedToken.uid).remove();
      }

      res.status(200).json({roomId, maxMembers: null, message: "Left"});
    } catch (error) {
      logger.error("Error in leaveWatchPartyRoom", error);
      res.status(500).json({error: "Internal Server Error"});
    }
  }
);

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
      const planEntry = normalizedPlan ?
        getPlanCatalogEntry(normalizedPlan) :
        null;
      if (!Number.isFinite(numericAmount) || numericAmount <= 0 || !orderInfo) {
        res.status(400).json({error: "Missing or invalid amount/orderInfo"});
        return;
      }
      if (!normalizedPlan || !planEntry) {
        res.status(400).json({error: "Invalid subscription plan"});
        return;
      }
      if (numericAmount !== planEntry.amount) {
        res.status(400).json({error: "Invalid subscription plan amount"});
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
        durationMonths: planEntry.durationMonths,
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
      const planEntry = normalizedPlan ?
        getPlanCatalogEntry(normalizedPlan) :
        null;

      if (!Number.isFinite(numericAmount) || numericAmount <= 0 || !orderInfo) {
        res.status(400).json({error: "Missing or invalid amount/orderInfo"});
        return;
      }
      if (!normalizedPlan || !planEntry) {
        res.status(400).json({error: "Invalid subscription plan"});
        return;
      }
      if (numericAmount !== planEntry.amount) {
        res.status(400).json({error: "Invalid subscription plan amount"});
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
        durationMonths: planEntry.durationMonths,
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
          subscriptionExpiresAt: getSubscriptionExpiry(
            getOrderDurationMonths(order)
          ),
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
            subscriptionExpiresAt: getSubscriptionExpiry(
              getOrderDurationMonths(order)
            ),
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
