# Chat Preview Backfill

Run this utility when older chat rows still show a stale sender name.

```bash
cd scripts
npm install
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccount.json"
node backfill-chat-previews.js
```

It scans every document under `chats`, reads the latest message, and rewrites the chat preview fields so the list matches the real sender again.
