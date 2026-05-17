// One-shot script to upload sample support content to Firestore.
// Usage:
// 1. npm install firebase-admin
// 2. Create a service account JSON and set:
//    export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccount.json"
// 3. node scripts/upload_support_content.js

const admin = require('firebase-admin');

const content = {
  appName: "Kampus",
  appLogoUrl: "https://your-cdn-or-storage-path/kampus-logo.png",
  appLogoFallbackText: "K",
  aboutVersion: "v1.2.3",
  aboutActions: [
    { title: "Terms of Service", iconKey: "document", actionUrl: "https://example.com/terms" },
    { title: "Privacy Policy", iconKey: "privacy", actionUrl: "https://example.com/privacy" }
  ],
  contactOptions: [
    { title: "Email Support", subtitle: "support@example.com", iconKey: "email", actionValue: "mailto:support@example.com" },
    { title: "Call Sales", subtitle: "+1 555 1234", iconKey: "phone", actionValue: "tel:+15551234" },
    { title: "Help Center", subtitle: "Browse articles", iconKey: "description", actionValue: "https://example.com/help" }
  ],
  faqTopics: ["Reset password: https://example.com/faq/reset-password", "Billing: https://example.com/faq/billing"],
  reportTechnicalIssueTitle: "Report an issue",
  reportTechnicalIssueHelp: "Please include steps to reproduce and screenshots.",
  appVersionText: "App version",
  checkForUpdatesText: "Check for updates",
  lastUpdated: new Date().toISOString(),
};

admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();

async function upload() {
  const ref = db.collection('appConfig').doc('content');
  await ref.set(content, { merge: true });
  console.log('Uploaded support content to appConfig/content');
}

upload().catch(err => { console.error(err); process.exit(1); });
