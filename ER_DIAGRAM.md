# ER Diagram (Mermaid)

Below is an ER diagram summarizing the app's main data entities (Supabase tables + Firestore collections) and their relationships.

```mermaid
erDiagram
    USER {
        string id PK "Firestore uid / auth id"
        string displayName
        string handle
        string email
        string avatarEmoji
        boolean isVerified
        boolean isOnline
        timestamp createdAt
    }

    POST {
        bigint id PK
        text author_id FK "references USER.id"
        text author
        text content
        bigint timestamp
        integer likes
        integer comments
        integer shares
        text visibility
        text[] tags
        bigint shared_original_post_id FK "self reference"
        bigint created_at
        bigint updated_at
    }

    EVENT {
        uuid id PK
        text owner_id FK "references USER.id"
        text title
        text description
        text location
        text image_url
        boolean allow_guest
        timestamptz start_date
        timestamptz end_date
        timestamptz created_at
    }

    CHAT {
        string id PK
        text participants[] "array of USER.id"
        text lastMessage
        bigint updatedAt
    }

    MESSAGE {
        string id PK
        string chat_id FK "references CHAT.id"
        string senderId FK "references USER.id"
        string receiverId FK "references USER.id"
        text body
        bigint createdAt
        text messageType
    }

    STORY {
        string id PK
        string ownerId FK "references USER.id"
        text caption
        bigint createdAt
        bigint expiresAt
    }

    FRIEND_REQUEST {
        string id PK
        string fromUserId FK "references USER.id"
        string toUserId FK "references USER.id"
        string status
        timestamp createdAt
    }

    FRIEND {
        string id PK
        string userId FK "owner user document id"
        string friendUserId FK "references USER.id"
        timestamp addedAt
    }

    %% Relationships
    USER ||--o{ POST : "authors"
    USER ||--o{ EVENT : "owns"
    USER ||--o{ FRIEND_REQUEST : "sends/receives"
    USER ||--o{ FRIEND : "has"
    USER ||--o{ CHAT : "participates"
    CHAT ||--o{ MESSAGE : "contains"
    USER ||--o{ MESSAGE : "sends/receives"
    USER ||--o{ STORY : "owns"
    POST }o--|| POST : "shared_from"

    %% Notes
    %% - Supabase: tables `posts` and `events` (see supabase/migrations)
    %% - Firestore: `users` documents with subcollections (friends, friendRequests, fcmTokens), `chats/{chatId}/messages`, `stories` and `storyReplies`.

```

If you'd like, I can:
- generate a PNG/SVG export of this Mermaid diagram and add it to the repo
- refine entity attributes or add more entities (groups, notifications, profiles)
