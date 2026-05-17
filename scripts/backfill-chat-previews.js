// Backfill stale chat preview fields from the latest message in each chat.
// Usage:
// 1. npm install
// 2. export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccount.json"
// 3. node scripts/backfill-chat-previews.js

const admin = require('firebase-admin');

admin.initializeApp({ credential: admin.credential.applicationDefault() });

const db = admin.firestore();

function formatPreviewTimestamp(timestamp) {
  if (!timestamp || Number.isNaN(Number(timestamp))) return 'now';
  const millis = Number(timestamp);
  const diffMinutes = Math.max(0, Math.floor((Date.now() - millis) / 60000));
  if (diffMinutes < 1) return 'now';
  if (diffMinutes < 60) return `${diffMinutes}m`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}h`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}d`;
  return new Date(millis).toLocaleDateString();
}

async function getUserDisplayName(userId) {
  if (!userId) return '';
  const snap = await db.collection('users').doc(userId).get();
  if (!snap.exists) return userId;
  return snap.get('displayName') || snap.get('name') || userId;
}

async function repairChat(chatDoc) {
  const chatId = chatDoc.id;
  const chat = chatDoc.data() || {};
  const participants = Array.isArray(chat.participants) ? chat.participants.filter(Boolean) : [];

  const latestSnap = await db
    .collection('chats')
    .doc(chatId)
    .collection('messages')
    .orderBy('timestamp', 'desc')
    .limit(1)
    .get();

  if (latestSnap.empty) return { chatId, updated: false, reason: 'no_messages' };

  const latestMessage = latestSnap.docs[0].data() || {};
  const latestMessageText = latestMessage.content || latestMessage.text || '';
  const latestSenderId = latestMessage.senderId || '';
  const latestTimestamp = Number(latestMessage.timestamp || 0);

  const senderDisplayName = await getUserDisplayName(latestSenderId);
  const otherUserId = participants.find((uid) => uid && uid !== latestSenderId) || '';
  const otherDisplayName = otherUserId ? await getUserDisplayName(otherUserId) : '';

  const desiredSenderName = latestSenderId && participants.length > 1
    ? senderDisplayName
    : (latestSenderId === chat.lastMessageSenderId ? (chat.lastMessageSenderName || senderDisplayName) : senderDisplayName);

  const updateData = {
    lastMessage: latestMessageText,
    lastMessageTime: latestTimestamp,
    lastMessageSenderId: latestSenderId,
    lastMessageSenderName: desiredSenderName,
    timestamp: formatPreviewTimestamp(latestTimestamp),
  };

  const changed =
    chat.lastMessage !== updateData.lastMessage ||
    Number(chat.lastMessageTime || 0) !== updateData.lastMessageTime ||
    (chat.lastMessageSenderId || '') !== updateData.lastMessageSenderId ||
    (chat.lastMessageSenderName || '') !== updateData.lastMessageSenderName ||
    (chat.timestamp || '') !== updateData.timestamp;

  if (changed) {
    await db.collection('chats').doc(chatId).set(updateData, { merge: true });
  }

  return {
    chatId,
    updated: changed,
    sender: desiredSenderName,
    otherUserId,
    otherDisplayName,
  };
}

async function main() {
  const chatsSnap = await db.collection('chats').get();
  let updated = 0;
  let unchanged = 0;

  for (const chatDoc of chatsSnap.docs) {
    try {
      const result = await repairChat(chatDoc);
      if (result.updated) {
        updated += 1;
        console.log(`[updated] ${result.chatId} -> ${result.sender}`);
      } else {
        unchanged += 1;
      }
    } catch (error) {
      console.error(`[error] ${chatDoc.id}:`, error.message || error);
    }
  }

  console.log(`Done. updated=${updated} unchanged=${unchanged} total=${chatsSnap.size}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
