const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");

admin.initializeApp();

async function resolveDirectChatId(senderId, receiverId, createdAt) {
  const chatsRef = admin.firestore().collection("chats");
  const candidateSnap = await chatsRef.where("participants", "array-contains", senderId).limit(50).get();

  for (const doc of candidateSnap.docs) {
    const participants = Array.isArray(doc.get("participants")) ? doc.get("participants") : [];
    if (participants.length === 2 && participants.includes(receiverId)) {
      return doc.id;
    }
  }

  const chatDoc = await chatsRef.add({
    participants: [senderId, receiverId],
    lastMessage: "Story reply",
    lastMessageType: "story_reply",
    lastMessageTime: createdAt,
    createdAt: createdAt,
    updatedAt: createdAt,
  });

  return chatDoc.id;
}

exports.onCallInviteCreated = onDocumentCreated(
  {
    document: "chats/{chatId}/messages/{messageId}",
    region: "us-central1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data() || {};
    const { chatId } = event.params;
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

// When a story reply is created, mirror it into the chat as a story_reply message
exports.onStoryReplyCreated = onDocumentCreated(
  {
    document: "storyReplies/{replyId}",
    region: "us-central1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data() || {};
    const { replyId } = event.params;
    const senderId = data.senderId || "";
    const storyId = data.storyId || "";
    const receiverId = data.receiverId || data.storyOwnerId || "";
    const replyText = (data.message || data.text || "").toString();
    const createdAt = data.createdAt || Date.now();
    const storyImage = (data.storyImage || data.storyThumbnail || data.mediaPreview || "").toString();
    const storyCaption = (data.storyCaption || data.storyText || data.storyNote || "").toString();

    if (!senderId || !receiverId || !storyId) {
      logger.info("Incomplete story reply, skipping", { replyId });
      return;
    }

    try {
      // Fetch story snapshot for preview fields
      const storySnap = await admin.firestore().collection("stories").doc(storyId).get();
      const storyData = storySnap.exists ? storySnap.data() : {};
      const senderProfile = await admin.firestore().collection("users").doc(senderId).get();

      const chatId = await resolveDirectChatId(senderId, receiverId, createdAt);

      // Create message in chat
      const messagesRef = admin.firestore().collection("chats").doc(chatId).collection("messages");
      const messageDoc = messagesRef.doc(replyId);
      const messagePayload = {
        id: messageDoc.id,
        remoteMessageId: messageDoc.id,
        senderId: senderId,
        receiverId: receiverId,
        messageType: "story_reply",
        storyId: storyId,
        storyImage: storyImage || storyData.thumbUrl || storyData.imageUrl || storyData.imageUri || "",
        storyCaption: storyCaption || storyData.note || storyData.caption || "",
        storyReplyText: replyText,
        replyText: replyText,
        createdAt: createdAt,
        timestamp: createdAt,
        storyOwnerId: receiverId,
        storyOwnerName: storyData.ownerName || "",
        storyThumbnail: storyData.thumbUrl || storyData.imageUrl || storyData.imageUri || "",
        storyImageUrl: storyData.imageUrl || storyData.imageUri || "",
      };

      await messageDoc.set(messagePayload);

      // Update chat preview
      await admin.firestore().collection("chats").doc(chatId).set({
        participants: [senderId, receiverId],
        lastMessage: replyText || "Story reply",
        lastMessageType: "story_reply",
        lastMessageSenderId: senderId,
        lastMessageSenderName: senderProfile.get("displayName") || "Someone",
        lastMessageTime: createdAt,
        timestamp: createdAt,
        updatedAt: createdAt,
      }, { merge: true });

      // Increment replyCount on story (best-effort)
      try {
        const storyRef = admin.firestore().collection("stories").doc(storyId);
        await storyRef.update({ replyCount: admin.firestore.FieldValue.increment(1) });
      } catch (incErr) {
        // ignore if field not present or update fails
        logger.info("Could not increment replyCount", { storyId, error: incErr });
      }

      // Send push to receiver
      const tokensSnap = await admin
        .firestore()
        .collection("users")
        .doc(receiverId)
        .collection("fcmTokens")
        .get();

      const tokens = tokensSnap.docs.map((doc) => doc.id).filter((t) => typeof t === "string");
      if (tokens.length) {
        const senderProfile = await admin.firestore().collection("users").doc(senderId).get();
        const senderName = senderProfile.get("displayName") || "Someone";

        const payload = {
          tokens,
          data: {
            type: "story_reply",
            title: `${senderName} replied to your story`,
            body: replyText ? replyText.substring(0, 120) : (storyCaption ? storyCaption.substring(0, 120) : "New story reply"),
            chatId: chatId,
            storyId: storyId,
            replyId: replyId,
            messageId: replyId,
          },
          android: { priority: "high" },
          apns: { headers: { "apns-priority": "10" } },
        };

        const res = await admin.messaging().sendEachForMulticast(payload);
        logger.info("Story reply push result", { success: res.successCount, failure: res.failureCount });
      }
    } catch (err) {
      logger.error("onStoryReplyCreated error", { error: err, replyId });
    }
  }
);

// Cleanup expired stories (exposed as HTTP function for emulator/manual runs)
const { onRequest } = require("firebase-functions/v2/https");

exports.cleanupExpiredStories = onRequest({ region: "us-central1" }, async (req, res) => {
  const now = Date.now();
  const db = admin.firestore();
  try {
    const q = db.collection("stories").where("expiresAt", "<=", now).limit(200);
    const snap = await q.get();
    if (snap.empty) return res.status(200).send({ cleaned: 0 });

    const batch = db.batch();
    const bucket = admin.storage().bucket();
    for (const doc of snap.docs) {
      const docRef = doc.ref;
      const data = doc.data() || {};

      // delete views subcollection
      const views = await docRef.collection("views").listDocuments();
      for (const v of views) batch.delete(v);

      // attempt to remove storage files under mediaStoragePath if available
      const mediaPath = data.mediaStoragePath || null;
      if (mediaPath) {
        try {
          await bucket.deleteFiles({ prefix: `${mediaPath}/` });
          logger.info("Deleted storage files for story", { storyId: doc.id, prefix: `${mediaPath}/` });
        } catch (storageErr) {
          try {
            const file = bucket.file(mediaPath);
            await file.delete();
            logger.info("Deleted single storage file for story", { storyId: doc.id, path: mediaPath });
          } catch (singleErr) {
            logger.warn("Failed to delete storage files for story", { storyId: doc.id, error: singleErr });
          }
        }
      }

      // remove the story doc
      batch.delete(docRef);
    }
    await batch.commit();
    logger.info("Expired stories cleaned", { count: snap.size });
    return res.status(200).send({ cleaned: snap.size });
  } catch (err) {
    logger.error("cleanupExpiredStories failed", { error: err });
    return res.status(500).send({ error: String(err) });
  }
});
