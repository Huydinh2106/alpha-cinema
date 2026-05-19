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

// --- Push Notifications ---
export * from "./notifications";
export * from "./cleanupDupes";
