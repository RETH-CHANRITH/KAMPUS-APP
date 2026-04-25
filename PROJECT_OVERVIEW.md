# KAMPUS APP - Complete Project Overview

**Project Type:** Android Compose Application  
**Language:** Kotlin  
**Target SDK:** 36 | **Min SDK:** 26 | **Java Version:** 11  
**Build System:** Gradle with Kotlin DSL

---

## 📁 Project Structure

```
KAMPUS-APP/
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # Gradle properties
├── gradlew / gradlew.bat               # Gradle wrapper
├── local.properties                    # Local build properties
│
├── gradle/                             # Gradle configuration
│   ├── gradle-daemon-jvm.properties
│   ├── libs.versions.toml             # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
│
└── app/                               # Main application module
    ├── build.gradle.kts               # App build configuration
    ├── google-services.json           # Firebase configuration
    ├── proguard-rules.pro             # ProGuard rules
    │
    └── src/main/
        ├── AndroidManifest.xml        # App manifest
        │
        ├── java/com/example/kampus/   # Source code
        │   ├── KampusApp.kt           # Application class
        │   ├── MainActivity.kt         # Main activity
        │   │
        │   ├── navigation/            # Navigation layer
        │   │   ├── NavGraph.kt        # Navigation configuration
        │   │   └── Screen.kt          # Screen route definitions
        │   │
        │   ├── di/                    # Dependency injection (Hilt/Koin)
        │   │   ├── AppModule.kt       # App-level DI
        │   │   ├── FirebaseModule.kt  # Firebase DI
        │   │   ├── SupabaseModule.kt  # Supabase DI
        │   │   └── RepositoryModule.kt# Repository DI
        │   │
        │   ├── domain/                # Domain layer (Clean Architecture)
        │   │   ├── model/             # Domain models
        │   │   │   ├── User.kt
        │   │   │   ├── Post.kt
        │   │   │   ├── Comment.kt
        │   │   │   ├── Message.kt
        │   │   │   ├── Notification.kt
        │   │   │   ├── Event.kt
        │   │   │   └── Group.kt
        │   │   │
        │   │   ├── repository/        # Repository interfaces
        │   │   │   ├── IAuthRepository.kt
        │   │   │   ├── IUserRepository.kt
        │   │   │   ├── IPostRepository.kt
        │   │   │   ├── IChatRepository.kt
        │   │   │   ├── IEventRepository.kt
        │   │   │   └── IGroupRepository.kt
        │   │   │
        │   │   └── usecase/           # Use cases (business logic)
        │   │       ├── LoginUseCase.kt
        │   │       ├── RegisterUseCase.kt
        │   │       ├── CreatePostUseCase.kt
        │   │       ├── LikePostUseCase.kt
        │   │       ├── GetFeedPostsUseCase.kt
        │   │       ├── SendMessageUseCase.kt
        │   │       ├── JoinGroupUseCase.kt
        │   │       ├── GetGroupsUseCase.kt
        │   │       ├── RsvpEventUseCase.kt
        │   │       ├── GetEventsUseCase.kt
        │   │       └── UploadImageUseCase.kt
        │   │
        │   ├── data/                  # Data layer (repositories & sources)
        │   │   ├── remote/            # Remote data sources
        │   │   │   ├── FirebaseDataSource.kt
        │   │   │   ├── FirebaseAuthSource.kt
        │   │   │   └── SupabaseDataSource.kt
        │   │   │
        │   │   └── repository/        # Repository implementations
        │   │       ├── AuthRepositoryImpl.kt
        │   │       ├── UserRepositoryImpl.kt
        │   │       ├── PostRepositoryImpl.kt
        │   │       ├── ChatRepositoryImpl.kt
        │   │       ├── EventRepositoryImpl.kt
        │   │       └── GroupRepositoryImpl.kt
        │   │
        │   ├── service/               # Background services
        │   │   └── FCMService.kt      # Firebase Cloud Messaging
        │   │
        │   ├── utils/                 # Utility functions
        │   │   ├── Constants.kt
        │   │   ├── Resource.kt        # Data wrapper class
        │   │   ├── Extensions.kt      # Extension functions
        │   │   ├── DateUtils.kt       # Date utilities
        │   │   ├── ImageUtils.kt      # Image handling
        │   │   └── RoleUtils.kt       # Role/permission utilities
        │   │
        │   └── ui/                    # UI layer (Jetpack Compose)
        │       ├── theme/             # Theme & styling
        │       │   ├── Color.kt       # Color palette
        │       │   ├── Theme.kt       # Theme configuration
        │       │   └── Type.kt        # Typography
        │       │
        │       ├── components/        # Reusable UI components
        │       │   ├── LoadingIndicator.kt
        │       │   ├── BottomNavBar.kt
        │       │   ├── TopAppBar.kt
        │       │   ├── InputField.kt
        │       │   ├── GradientButton.kt
        │       │   ├── ConfirmDialog.kt
        │       │   ├── AvatarImage.kt
        │       │   └── EmptyState.kt
        │       │
        │       ├── splash/            # Splash screen
        │       │   └── SplashScreen.kt
        │       │
        │       ├── onboarding/        # Onboarding flow
        │       │   ├── OnboardingScreen.kt
        │       │   ├── OnboardingViewModel.kt
        │       │   ├── Onboardingpage.kt
        │       │   └── OnboardingIllustration.kt
        │       │
        │       ├── auth/              # Authentication screens
        │       │   ├── LoginScreen.kt
        │       │   ├── RegisterScreen.kt
        │       │   ├── OtpScreen.kt
        │       │   ├── ForgotPasswordScreen.kt
        │       │   ├── ResetPasswordScreen.kt
        │       │   ├── AuthViewModel.kt
        │       │   ├── AuthState.kt
        │       │   └── AuthColors.kt
        │       │
        │       ├── feed/              # Feed/Home screens
        │       │   ├── FeedScreen.kt (HomeScreen)
        │       │   ├── FeedViewModel.kt
        │       │   ├── CreatePostScreen.kt
        │       │   ├── CreatePostBar.kt
        │       │   ├── PostItem.kt
        │       │   ├── StoryRow.kt
        │       │   ├── MediaCropper.kt
        │       │   ├── PickerModals.kt
        │       │   └── [other feed components]
        │       │
        │       ├── chat/              # Chat screens
        │       │   ├── ChatListScreen.kt
        │       │   ├── ChatScreen.kt
        │       │   ├── ChatViewModel.kt
        │       │   ├── ChatItem.kt
        │       │   ├── MessageBubble.kt
        │       │   └── [other chat UI]
        │       │
        │       ├── events/            # Events screens
        │       │   ├── EventListScreen.kt
        │       │   ├── EventDetailScreen.kt
        │       │   ├── CreateEventScreen.kt
        │       │   ├── CreateEventPostScreen.kt
        │       │   ├── EventViewModel.kt
        │       │   ├── EventItem.kt
        │       │   ├── MediaPickerHelper.kt
        │       │   └── [other event UI]
        │       │
        │       ├── groups/            # Groups screens
        │       │   ├── GroupListScreen.kt
        │       │   ├── GroupDetailScreen.kt
        │       │   ├── CreateGroupScreen.kt
        │       │   ├── GroupViewModel.kt
        │       │   ├── GroupItem.kt
        │       │   └── Groupdata.kt
        │       │
        │       ├── post/              # Post detail screens
        │       │   ├── PostDetailScreen.kt
        │       │   ├── CreatePostScreen.kt
        │       │   ├── PostViewModel.kt
        │       │   └── CommentItem.kt
        │       │
        │       ├── profile/           # User profile screens
        │       │   ├── ProfileScreen.kt
        │       │   ├── EditProfileScreen.kt
        │       │   └── ProfileViewModel.kt
        │       │
        │       ├── notifications/     # Notification screens
        │       │   ├── NotificationScreen.kt
        │       │   ├── NotificationViewModel.kt
        │       │   └── NotificationItem.kt
        │       │
        │       └── admin/             # Admin dashboard
        │           ├── AdminDashboardScreen.kt
        │           ├── ManageUsersScreen.kt
        │           ├── ReportedContentScreen.kt
        │           └── AdminViewModel.kt
        │
        └── res/                       # Android resources
            ├── mipmap-anydpi/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            │
            ├── values/
            │   ├── strings.xml        # String resources
            │   └── themes.xml         # Theme resources
            │
            └── xml/
                ├── backup_rules.xml
                ├── data_extraction_rules.xml
                └── file_paths.xml
```

---

## 📋 Key Kotlin Files by Category

### Core Application (3 files)
- **KampusApp.kt** - Application class
- **MainActivity.kt** - Main activity that sets up Compose UI
- **navigation/NavGraph.kt** - Complete navigation configuration for all screens

### Domain Layer (21 files)

**Models (7):**
- User.kt, Post.kt, Comment.kt, Message.kt, Notification.kt, Event.kt, Group.kt

**Repositories (6):**
- IAuthRepository.kt, IUserRepository.kt, IPostRepository.kt, IChatRepository.kt, IEventRepository.kt, IGroupRepository.kt

**Use Cases (11):**
- LoginUseCase.kt, RegisterUseCase.kt, CreatePostUseCase.kt, LikePostUseCase.kt, GetFeedPostsUseCase.kt, SendMessageUseCase.kt, JoinGroupUseCase.kt, GetGroupsUseCase.kt, RsvpEventUseCase.kt, GetEventsUseCase.kt, UploadImageUseCase.kt

### Data Layer (9 files)

**Remote Sources (3):**
- FirebaseDataSource.kt, FirebaseAuthSource.kt, SupabaseDataSource.kt

**Repository Implementations (6):**
- AuthRepositoryImpl.kt, UserRepositoryImpl.kt, PostRepositoryImpl.kt, ChatRepositoryImpl.kt, EventRepositoryImpl.kt, GroupRepositoryImpl.kt

### Dependency Injection (4 files)
- AppModule.kt, FirebaseModule.kt, SupabaseModule.kt, RepositoryModule.kt

### UI Layer - Theme (3 files)
- Color.kt - Color palette definitions
- Theme.kt - Material3 theme configuration
- Type.kt - Typography settings

### UI Layer - Components (8 files)
- LoadingIndicator.kt, BottomNavBar.kt, TopAppBar.kt, InputField.kt, GradientButton.kt, ConfirmDialog.kt, AvatarImage.kt, EmptyState.kt

### UI Layer - Screens by Feature

**Splash & Onboarding (4):**
- SplashScreen.kt, OnboardingScreen.kt, OnboardingViewModel.kt, OnboardingIllustration.kt, Onboardingpage.kt

**Authentication (8):**
- LoginScreen.kt, RegisterScreen.kt, OtpScreen.kt, ForgotPasswordScreen.kt, ResetPasswordScreen.kt, AuthViewModel.kt, AuthState.kt, AuthColors.kt

**Feed/Home (8):**
- FeedScreen.kt, FeedViewModel.kt, CreatePostScreen.kt, CreatePostBar.kt, PostItem.kt, StoryRow.kt, MediaCropper.kt, PickerModals.kt

**Chat (5):**
- ChatListScreen.kt, ChatScreen.kt, ChatViewModel.kt, ChatItem.kt, MessageBubble.kt

**Events (7):**
- EventListScreen.kt, EventDetailScreen.kt, CreateEventScreen.kt, CreateEventPostScreen.kt, EventViewModel.kt, EventItem.kt, MediaPickerHelper.kt

**Groups (6):**
- GroupListScreen.kt, GroupDetailScreen.kt, CreateGroupScreen.kt, GroupViewModel.kt, GroupItem.kt, Groupdata.kt

**Post Details (4):**
- PostDetailScreen.kt, CreatePostScreen.kt, PostViewModel.kt, CommentItem.kt

**Profile (3):**
- ProfileScreen.kt, EditProfileScreen.kt, ProfileViewModel.kt

**Notifications (3):**
- NotificationScreen.kt, NotificationViewModel.kt, NotificationItem.kt

**Admin (4):**
- AdminDashboardScreen.kt, ManageUsersScreen.kt, ReportedContentScreen.kt, AdminViewModel.kt

### Utilities & Services (7 files)
- Constants.kt, Resource.kt, Extensions.kt, DateUtils.kt, ImageUtils.kt, RoleUtils.kt, FCMService.kt

### Navigation (2 files)
- NavGraph.kt, Screen.kt

---

## 🔧 Build Configuration Details

### Root build.gradle.kts
```
Plugins:
- Android Application
- Kotlin Android
- Kotlin Compose
- Google Play Services (Firebase)
```

### App build.gradle.kts
```
Namespace: com.example.kampus
Compile SDK: 36
Min SDK: 26
Target SDK: 36
Java Version: 11

Key Dependencies:
- Firebase (Analytics, Auth, Firestore, Storage, Messaging)
- Jetpack Compose & Material3
- Coil (Image loading)
- Media3 (Video playback)
- UCrop (Image cropping)
- Google Sign-In
```

### AndroidManifest.xml
```
Application: KampusApp
Main Activity: MainActivity
Theme: Theme.Kampus
Activities:
  - MainActivity (LAUNCHER)
  - UCropActivity (Image editing)
Providers:
  - FileProvider (File sharing)
```

---

## 📊 Project Statistics

- **Total Kotlin Files:** 112
- **UI Screen Files:** ~40+
- **ViewModel Files:** 8+
- **Repository Files:** 12 (6 interfaces + 6 implementations)
- **Use Case Files:** 11
- **Domain Model Files:** 7
- **Component Files:** 8+
- **Service & Utility Files:** 7+

---

## 🎨 Architecture Pattern

**Clean Architecture with MVVM:**
1. **Presentation Layer** → UI, ViewModels, Screens
2. **Domain Layer** → Entities, Repository Interfaces, Use Cases
3. **Data Layer** → Repository Implementations, Remote/Local Data Sources

---

## 🔐 Authentication & Backend

- **Firebase:** Authentication, Firestore (database), Storage, Cloud Messaging
- **Supabase:** Alternative backend option
- **Google Sign-In:** OAuth integration
- **FCM:** Push notifications

---

## 📱 Key Features

✅ User Authentication (Email/Password, Google Sign-In, OTP)  
✅ Social Feed with Posts  
✅ Real-time Messaging/Chat  
✅ Events Management  
✅ Group Management  
✅ User Profiles  
✅ Notifications  
✅ Image Upload & Cropping  
✅ Admin Dashboard  
✅ Content Moderation

---

## 🛠️ Build & Run

- **Gradle Wrapper:** macOS/Linux use `./gradlew`, Windows use `gradlew.bat`
- **Build:** `./gradlew build`
- **Debug APK:** `./gradlew assembleDebug`
- **Release APK:** `./gradlew assembleRelease`

---

## 📝 Notes

- **Total File Count:** 112 Kotlin files
- **Compose Framework:** Used for all UI development
- **State Management:** MutableStateFlow for ViewModels
- **Navigation:** Jetpack Navigation Compose
- **Image Handling:** Coil for async loading, ucrop for cropping
- **Code Quality:** Follows Kotlin conventions and Clean Architecture principles
