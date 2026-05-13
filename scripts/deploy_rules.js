const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');
const serviceAccount = require(path.resolve(__dirname, '../service-account.json'));

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

async function deploy() {
    try {
        const rules = fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8');
        await admin.securityRules().releaseFirestoreRulesetFromSource(rules);
        console.log("Firestore rules deployed successfully!");
    } catch (e) {
        console.error("Failed to deploy rules:", e);
    }
    process.exit(0);
}

deploy();
