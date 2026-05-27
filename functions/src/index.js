const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");

admin.initializeApp();

exports.onCallInviteCreated = onDocumentCreated(
  {
    document: "chats/{chatId}/messages/{messageId}",
    region: "us-central1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data() || {};
    const chatId = event.params.chatId;
    const callerId = data.senderId || "";
    const messageType = (data.messageType || "message").toString().toLowerCase();
    const callType = (data.callType || "voice").toString().toLowerCase();
    const callId = (data.callId || event.params.messageId || "").toString();

    if (!callerId) {
      logger.info("Skipping push because sender id is missing", { chatId, messageType });
      return;
    }

    const chatSnap = await admin.firestore().collection("chats").doc(chatId).get();
    const participants = Array.isArray(chatSnap.get("participants")) ? chatSnap.get("participants") : [];
    const inferredRecipientId = participants.find((participant) => participant && participant !== callerId) || chatSnap.get("otherUserId") || "";
    const calleeId = data.receiverId || data.recipientId || inferredRecipientId;

    if (!calleeId || calleeId === callerId) {
      logger.info("Skipping push because recipient could not be resolved", {
        chatId,
        callerId,
        calleeId,
        messageType,
      });
      return;
    }

    const callerProfile = await admin.firestore().collection("users").doc(callerId).get();
    const callerName = callerProfile.get("displayName") || "Someone";

    const tokensSnap = await admin
      .firestore()
      .collection("users")
      .doc(calleeId)
      .collection("fcmTokens")
      .get();

    const tokens = tokensSnap.docs
      .map((doc) => doc.id)
      .filter((token) => typeof token === "string" && token.length > 20);

    if (!tokens.length) {
      logger.info("No FCM tokens for callee", { calleeId, chatId });
      return;
    }

    const message = {
      tokens,
      data: {
        type: messageType === "call_invite" ? "call_invite" : "chat_message",
        messageType,
        title: messageType === "call_invite" ? callerName : callerName,
        body:
          messageType === "call_invite"
            ? callType === "video"
              ? "Incoming video call"
              : "Incoming voice call"
            : (data.text || data.body || data.message || "New message").toString(),
        chatId,
        senderId: callerId,
        calleeId,
        callType,
        callId,
      },
      android: {
        priority: "high",
      },
      apns: {
        headers: {
          "apns-priority": "10",
        },
      },
    };

    const result = await admin.messaging().sendEachForMulticast(message);
    logger.info("Chat/call push sent", {
      messageType,
      chatId,
      callId,
      successCount: result.successCount,
      failureCount: result.failureCount,
    });

    if (result.failureCount > 0) {
      const invalidTokens = [];
      result.responses.forEach((resp, index) => {
        if (!resp.success) {
          const code = resp.error && resp.error.code ? resp.error.code : "unknown";
          if (
            code.includes("registration-token-not-registered") ||
            code.includes("invalid-registration-token")
          ) {
            invalidTokens.push(tokens[index]);
          }
        }
      });

      if (invalidTokens.length) {
        const batch = admin.firestore().batch();
        invalidTokens.forEach((token) => {
          const ref = admin
            .firestore()
            .collection("users")
            .doc(calleeId)
            .collection("fcmTokens")
            .doc(token);
          batch.delete(ref);
        });
        await batch.commit();
      }
    }
  }
);
