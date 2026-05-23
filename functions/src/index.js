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
    if (data.messageType !== "call_invite") return;

    const chatId = event.params.chatId;
    const callerId = data.senderId || "";
    const calleeId = data.receiverId || data.recipientId || "";
    const callType = (data.callType || "voice").toString().toLowerCase();
    const callId = (data.callId || "").toString();
    const callStatus = (data.callStatus || "ringing").toString().toLowerCase();

    if (!calleeId || !callerId || callStatus !== "ringing") {
      logger.info("Skipping call invite push due to missing ids or non-ringing status", {
        chatId,
        callerId,
        calleeId,
        callStatus,
      });
      return;
    }

    const callerProfile = await admin.firestore().collection("users").doc(callerId).get();
    const callerName = callerProfile.get("displayName") || "Incoming call";

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
        type: "call_invite",
        messageType: "call_invite",
        title: callerName,
        body: callType === "video" ? "Incoming video call" : "Incoming voice call",
        chatId,
        callerId,
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
    logger.info("Call invite push sent", {
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
