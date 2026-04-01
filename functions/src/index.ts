import {onRequest} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

const REGION = "asia-southeast1";
const BASE_URL = "https://phimapi.com";

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


