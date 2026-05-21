const admin = require('firebase-admin');

const projectId =
    process.env.FIREBASE_PROJECT_ID ||
    process.env.GCLOUD_PROJECT ||
    process.env.GOOGLE_CLOUD_PROJECT ||
    'alpha-cinema-39dfb';

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        projectId
    });
}

module.exports = {
    admin,
    db: admin.firestore()
};
