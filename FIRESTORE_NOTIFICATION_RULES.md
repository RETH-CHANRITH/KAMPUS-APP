# Firestore Notification Rules (Add-On)

Use this block inside your `service cloud.firestore` rules to support realtime notifications at:

- `users/{userId}/notifications/{notificationId}`

```javascript
match /users/{userId}/notifications/{notificationId} {
  // Only the owner can read notifications.
  allow read: if request.auth != null && request.auth.uid == userId;

  // Allow authenticated users to create notifications for another user.
  // This supports client-generated events (like, follow request, etc.).
  allow create: if request.auth != null
    && request.auth.uid != userId
    && request.resource.data.actorUserId == request.auth.uid
    && request.resource.data.type is string
    && request.resource.data.title is string
    && request.resource.data.body is string
    && request.resource.data.createdAt is int
    && request.resource.data.isRead == false;

  // Only owner can mark read/update.
  allow update: if request.auth != null
    && request.auth.uid == userId;

  // Optional: allow owner delete.
  allow delete: if request.auth != null
    && request.auth.uid == userId;
}
```

## Notes

- This fits the current app flow where clients create follow/like notifications.
- If you later move notification writes to Cloud Functions, you can tighten `create` to server-only.
- Keep your existing `users/{userId}` rules unchanged.
