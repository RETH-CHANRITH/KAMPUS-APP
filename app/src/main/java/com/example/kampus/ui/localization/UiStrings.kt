package com.example.kampus.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.kampus.utils.LanguageManager

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-string classes (keeps each constructor under the DEX argument limit)
// ─────────────────────────────────────────────────────────────────────────────

data class CommonStrings(
    val back: String,
    val search: String,
    val dismiss: String,
    val saveChanges: String,
    val all: String,
    val discover: String,
    val you: String,
    val location: String,
    val tapToSelect: String,
    val thisIsPreview: String,
    val appTagline: String,
    val blue: String,
    val purple: String,
    val pink: String,
    val red: String,
    val orange: String,
    val green: String,
    val teal: String,
    val crop: String,
    val change: String,
    val addLocationTitle: String,
    val searchLocations: String,
    val gettingYourLocation: String,
    val myLocation: String,
    val noLocationsFound: String,
    val credits: String,
    val iconsBy: String,
    val imagesBy: String,
    val builtWith: String,
    val copyright: String,
    val madeWithLove: String,
)

data class AuthStrings(
    val welcomeBack: String,
    val loginTitle: String,
    val emailRequired: String,
    val validEmail: String,
    val ruppEmailOnly: String,
    val passwordRequired: String,
    val atLeastSixCharacters: String,
    val rememberMe: String,
    val dontHaveAccount: String,
    val orContinueWith: String,
    val login: String,
    val register: String,
    val emailAddress: String,
    val password: String,
    val forgotPassword: String,
    val signUp: String,
    val signIn: String,
    val createAccount: String,
    val termsOfService: String,
    val privacyPolicy: String,
    val verifyCode: String,
    val didNotReceiveCode: String,
    val resendIn: String,
    val resendCode: String,
    val youWillReceiveCodeShortly: String,
    val allDone: String,
    val passwordResetSuccessfully: String,
    val yourPasswordHasBeenChanged: String,
    val loginWithNewPassword: String,
    val accountSetup: String,
    val accountCannotBeEmpty: String,
    val onlyRuppEmailsAllowed: String,
)

data class NavStrings(
    val home: String,
    val groups: String,
    val events: String,
    val chat: String,
    val messenger: String,
)

data class ProfileStrings(
    val username: String,
    val profilePicture: String,
    val changeProfilePicture: String,
    val usernameLabel: String,
    val emailLabel: String,
    val phoneLabel: String,
    val bioLabel: String,
    val facultyLabel: String,
    val yearLabel: String,
    val locationLabel: String,
    val mutualFriendsLabel: String,
    val selectYear: String,
    val selectCambodiaProvince: String,
    val profileSaved: String,
    val chooseYear: String,
    val chooseLocation: String,
    val followersLabel: String,
    val followingLabel: String,
    val friendsLabel: String,
    val shareProfile: String,
    val aboutSection: String,
    val loadingProfile: String,
    val unknownLocation: String,
)

data class SettingsStrings(
    val settings: String,
    val editProfile: String,
    val notifications: String,
    val privacyAndSecurity: String,
    val account: String,
    val appearance: String,
    val languageAndRegion: String,
    val blockedUsers: String,
    val helpAndSupport: String,
    val about: String,
    val logOut: String,
    val version: String,
    val theme: String,
    val light: String,
    val dark: String,
    val accentColor: String,
    val fontSize: String,
    val preview: String,
    val noBlockedUsers: String,
    val unblock: String,
    val helpTitle: String,
    val reportProblem: String,
    val reportTechnicalIssue: String,
    val reportTechnicalIssueHelp: String,
    val appVersion: String,
    val tapToViewQuestions: String,
    val privacySecurityTitle: String,
    val blockedUsersTitle: String,
    val editProfileTitle: String,
    val notificationsTitle: String,
    val accountTitle: String,
    val aboutTitle: String,
    val contactUs: String,
    val frequentlyAskedQuestions: String,
    val reportAProblem: String,
    val checkForUpdates: String,
    val pushNotifications: String,
    val emailNotifications: String,
    val smsNotifications: String,
    val privateAccount: String,
    val activityStatus: String,
    val allowTagging: String,
    val allowMentions: String,
    val twoFactorAuthentication: String,
    val changePassword: String,
    val loginActivity: String,
    val downloadYourData: String,
    val searchHistory: String,
    val accountInformation: String,
    val linkedAccounts: String,
    val accountActions: String,
    val dangerZone: String,
    val phoneNotSet: String,
    val accountCreated: String,
    val connected: String,
    val notConnected: String,
    val deactivateAccount: String,
    val temporarilyDisableAccount: String,
    val permanentlyDeleteAccount: String,
    val privacy: String,
    val security: String,
    val dataAndHistory: String,
    val syncingPrivacySettings: String,
    val languageSubtitle: String,
    val englishLanguage: String,
    val englishDesc: String,
    val khmerLanguage: String,
    val khmerDesc: String,
    val cancel: String,
    val logoutConfirmTitle: String,
    val logoutConfirmMessage: String,
)

data class SocialStrings(
    val hoursAgo: String,
    val like: String,
    val comment: String,
    val createStory: String,
    val followBack: String,
    val followingStatus: String,
    val posts: String,
    val requests: String,
    val discoverPeople: String,
    val recentActivity: String,
    val noActivityYet: String,
    val suggested: String,
    val newest: String,
    val noUsersToDiscoverYet: String,
    val friendRequestsTitle: String,
    val incoming: String,
    val outgoing: String,
    val accept: String,
    val reject: String,
    val cancelRequest: String,
    val unfollow: String,
    val block: String,
    val noIncomingRequests: String,
    val noOutgoingRequests: String,
    val incomingRequest: String,
    val sentRequest: String,
    val pending: String,
    val accepted: String,
    val rejected: String,
    val blocked: String,
    val unknownUser: String,
    val discoverSuggestedDesc: String,
    val discoverNewDesc: String,
    val discoverAllDesc: String,
    val friend: String,
    val requested: String,
    val follow: String,
    val newPost: String,
    val feeling: String,
    val people: String,
    val addPhotosOrVideos: String,
    val whatsOnYourMind: String,
    val gallery: String,
    val gif: String,
    val video: String,
    val live: String,
    val postAction: String,
    val public: String,
    val friends: String,
    val private: String,
    val unread: String,
    val posted: String,
    val likesLabel: String,
    val commentsLabel: String,
    val createPost: String,
    val postsLabel: String,
    val addMorePhotos: String,
    val chooseYourFeeling: String,
    val searchFeelings: String,
    val tagPeople: String,
    val searchFriends: String,
    val noFriendsFound: String,
    val findGif: String,
    val searchGifs: String,
    val noGifsFound: String,
)

data class EventStrings(
    val searchEvents: String,
    val noEventsFound: String,
    val tryDifferentFilterOrSearch: String,
    val discoverEventsNearYou: String,
    val today: String,
    val thisWeek: String,
    val eventLabel: String,
    val searchEvent: String,
    val aboutThisEvent: String,
    val interestedLabel: String,
    val dateLabel: String,
    val timeLabel: String,
    val imInterested: String,
    val youreInterested: String,
    val featured: String,
    val searchEventsLocations: String,
    val noEventsFound2: String,
    val tryDifferentFilterOrSearch2: String,
    val peopleInterested: String,
    val filterAll: String,
    val filterToday: String,
    val filterThisWeek: String,
    val filterMusic: String,
    val filterTech: String,
    val filterArt: String,
    val filterCampus: String,
    val organizer: String,
)

data class GroupStrings(
    val searchGroups: String,
    val noGroupsFound: String,
    val tryDifferentKeyword: String,
    val myGroups: String,
    val membersLabel: String,
    val joined: String,
    val joinGroup: String,
    val searchGroupsPlaceholder: String,
    val createGroup: String,
)

// ─────────────────────────────────────────────────────────────────────────────
//  UiStrings — holds sub-objects + flat helpers for backward compatibility
//  NOTE: constructor param is `settingsObj` to avoid clashing with the flat
//        helper `val settings: String` that screens already use.
// ─────────────────────────────────────────────────────────────────────────────

data class UiStrings(
    val common: CommonStrings,
    val auth: AuthStrings,
    val nav: NavStrings,
    val profile: ProfileStrings,
    val settingsObj: SettingsStrings,
    val social: SocialStrings,
    val event: EventStrings,
    val group: GroupStrings,
) {
    // ── Common ────────────────────────────────────────────────────────────────
    val back                       get() = common.back
    val search                     get() = common.search
    val dismiss                    get() = common.dismiss
    val saveChanges                get() = common.saveChanges
    val all                        get() = common.all
    val discover                   get() = common.discover
    val you                        get() = common.you
    val location                   get() = common.location
    val tapToSelect                get() = common.tapToSelect
    val thisIsPreview              get() = common.thisIsPreview
    val appTagline                 get() = common.appTagline
    val blue                       get() = common.blue
    val purple                     get() = common.purple
    val pink                       get() = common.pink
    val red                        get() = common.red
    val orange                     get() = common.orange
    val green                      get() = common.green
    val teal                       get() = common.teal
    val crop                       get() = common.crop
    val change                     get() = common.change
    val addLocationTitle           get() = common.addLocationTitle
    val searchLocations            get() = common.searchLocations
    val gettingYourLocation        get() = common.gettingYourLocation
    val myLocation                 get() = common.myLocation
    val noLocationsFound           get() = common.noLocationsFound
    val credits                    get() = common.credits
    val iconsBy                    get() = common.iconsBy
    val imagesBy                   get() = common.imagesBy
    val builtWith                  get() = common.builtWith
    val copyright                  get() = common.copyright
    val madeWithLove               get() = common.madeWithLove
    // ── Auth ──────────────────────────────────────────────────────────────────
    val welcomeBack                get() = auth.welcomeBack
    val loginTitle                 get() = auth.loginTitle
    val emailRequired              get() = auth.emailRequired
    val validEmail                 get() = auth.validEmail
    val ruppEmailOnly              get() = auth.ruppEmailOnly
    val passwordRequired           get() = auth.passwordRequired
    val atLeastSixCharacters       get() = auth.atLeastSixCharacters
    val rememberMe                 get() = auth.rememberMe
    val dontHaveAccount            get() = auth.dontHaveAccount
    val orContinueWith             get() = auth.orContinueWith
    val login                      get() = auth.login
    val register                   get() = auth.register
    val emailAddress               get() = auth.emailAddress
    val password                   get() = auth.password
    val forgotPassword             get() = auth.forgotPassword
    val signUp                     get() = auth.signUp
    val signIn                     get() = auth.signIn
    val createAccount              get() = auth.createAccount
    val termsOfService             get() = auth.termsOfService
    val privacyPolicy              get() = auth.privacyPolicy
    val verifyCode                 get() = auth.verifyCode
    val didNotReceiveCode          get() = auth.didNotReceiveCode
    val resendIn                   get() = auth.resendIn
    val resendCode                 get() = auth.resendCode
    val youWillReceiveCodeShortly  get() = auth.youWillReceiveCodeShortly
    val allDone                    get() = auth.allDone
    val passwordResetSuccessfully  get() = auth.passwordResetSuccessfully
    val yourPasswordHasBeenChanged get() = auth.yourPasswordHasBeenChanged
    val loginWithNewPassword       get() = auth.loginWithNewPassword
    val accountSetup               get() = auth.accountSetup
    val accountCannotBeEmpty       get() = auth.accountCannotBeEmpty
    val onlyRuppEmailsAllowed      get() = auth.onlyRuppEmailsAllowed
    // ── Nav ───────────────────────────────────────────────────────────────────
    val home                       get() = nav.home
    val groups                     get() = nav.groups
    val events                     get() = nav.events
    val chat                       get() = nav.chat
    val messenger                  get() = nav.messenger
    // ── Profile ───────────────────────────────────────────────────────────────
    val username                   get() = profile.username
    val profilePicture             get() = profile.profilePicture
    val changeProfilePicture       get() = profile.changeProfilePicture
    val usernameLabel              get() = profile.usernameLabel
    val emailLabel                 get() = profile.emailLabel
    val phoneLabel                 get() = profile.phoneLabel
    val bioLabel                   get() = profile.bioLabel
    val facultyLabel               get() = profile.facultyLabel
    val yearLabel                  get() = profile.yearLabel
    val locationLabel              get() = profile.locationLabel
    val mutualFriendsLabel         get() = profile.mutualFriendsLabel
    val selectYear                 get() = profile.selectYear
    val selectCambodiaProvince     get() = profile.selectCambodiaProvince
    val profileSaved               get() = profile.profileSaved
    val chooseYear                 get() = profile.chooseYear
    val chooseLocation             get() = profile.chooseLocation
    val followersLabel             get() = profile.followersLabel
    val followingLabel             get() = profile.followingLabel
    val friendsLabel               get() = profile.friendsLabel
    val shareProfile               get() = profile.shareProfile
    val aboutSection               get() = profile.aboutSection
    val loadingProfile             get() = profile.loadingProfile
    val unknownLocation            get() = profile.unknownLocation
    // ── Settings ──────────────────────────────────────────────────────────────
    val settings                   get() = settingsObj.settings
    val editProfile                get() = settingsObj.editProfile
    val notifications              get() = settingsObj.notifications
    val privacyAndSecurity         get() = settingsObj.privacyAndSecurity
    val account                    get() = settingsObj.account
    val appearance                 get() = settingsObj.appearance
    val languageAndRegion          get() = settingsObj.languageAndRegion
    val blockedUsers               get() = settingsObj.blockedUsers
    val helpAndSupport             get() = settingsObj.helpAndSupport
    val about                      get() = settingsObj.about
    val logOut                     get() = settingsObj.logOut
    val version                    get() = settingsObj.version
    val theme                      get() = settingsObj.theme
    val light                      get() = settingsObj.light
    val dark                       get() = settingsObj.dark
    val accentColor                get() = settingsObj.accentColor
    val fontSize                   get() = settingsObj.fontSize
    val preview                    get() = settingsObj.preview
    val noBlockedUsers             get() = settingsObj.noBlockedUsers
    val unblock                    get() = settingsObj.unblock
    val helpTitle                  get() = settingsObj.helpTitle
    val reportProblem              get() = settingsObj.reportProblem
    val reportTechnicalIssue       get() = settingsObj.reportTechnicalIssue
    val reportTechnicalIssueHelp   get() = settingsObj.reportTechnicalIssueHelp
    val appVersion                 get() = settingsObj.appVersion
    val tapToViewQuestions         get() = settingsObj.tapToViewQuestions
    val privacySecurityTitle       get() = settingsObj.privacySecurityTitle
    val blockedUsersTitle          get() = settingsObj.blockedUsersTitle
    val editProfileTitle           get() = settingsObj.editProfileTitle
    val notificationsTitle         get() = settingsObj.notificationsTitle
    val accountTitle               get() = settingsObj.accountTitle
    val aboutTitle                 get() = settingsObj.aboutTitle
    val contactUs                  get() = settingsObj.contactUs
    val frequentlyAskedQuestions   get() = settingsObj.frequentlyAskedQuestions
    val reportAProblem             get() = settingsObj.reportAProblem
    val checkForUpdates            get() = settingsObj.checkForUpdates
    val pushNotifications          get() = settingsObj.pushNotifications
    val emailNotifications         get() = settingsObj.emailNotifications
    val smsNotifications           get() = settingsObj.smsNotifications
    val privateAccount             get() = settingsObj.privateAccount
    val activityStatus             get() = settingsObj.activityStatus
    val allowTagging               get() = settingsObj.allowTagging
    val allowMentions              get() = settingsObj.allowMentions
    val twoFactorAuthentication    get() = settingsObj.twoFactorAuthentication
    val changePassword             get() = settingsObj.changePassword
    val loginActivity              get() = settingsObj.loginActivity
    val downloadYourData           get() = settingsObj.downloadYourData
    val searchHistory              get() = settingsObj.searchHistory
    val accountInformation         get() = settingsObj.accountInformation
    val linkedAccounts             get() = settingsObj.linkedAccounts
    val accountActions             get() = settingsObj.accountActions
    val dangerZone                 get() = settingsObj.dangerZone
    val phoneNotSet                get() = settingsObj.phoneNotSet
    val accountCreated             get() = settingsObj.accountCreated
    val connected                  get() = settingsObj.connected
    val notConnected               get() = settingsObj.notConnected
    val deactivateAccount          get() = settingsObj.deactivateAccount
    val temporarilyDisableAccount  get() = settingsObj.temporarilyDisableAccount
    val permanentlyDeleteAccount   get() = settingsObj.permanentlyDeleteAccount
    val privacy                    get() = settingsObj.privacy
    val security                   get() = settingsObj.security
    val dataAndHistory             get() = settingsObj.dataAndHistory
    val syncingPrivacySettings     get() = settingsObj.syncingPrivacySettings
    val languageSubtitle           get() = settingsObj.languageSubtitle
    val englishLanguage            get() = settingsObj.englishLanguage
    val englishDesc                get() = settingsObj.englishDesc
    val khmerLanguage              get() = settingsObj.khmerLanguage
    val khmerDesc                  get() = settingsObj.khmerDesc
    // ── Social ────────────────────────────────────────────────────────────────
    val hoursAgo                   get() = social.hoursAgo
    val like                       get() = social.like
    val comment                    get() = social.comment
    val createStory                get() = social.createStory
    val followBack                 get() = social.followBack
    val followingStatus            get() = social.followingStatus
    val posts                      get() = social.posts
    val requests                   get() = social.requests
    val discoverPeople             get() = social.discoverPeople
    val recentActivity             get() = social.recentActivity
    val noActivityYet              get() = social.noActivityYet
    val suggested                  get() = social.suggested
    val newest                     get() = social.newest
    val noUsersToDiscoverYet       get() = social.noUsersToDiscoverYet
    val friendRequestsTitle        get() = social.friendRequestsTitle
    val incoming                   get() = social.incoming
    val outgoing                   get() = social.outgoing
    val accept                     get() = social.accept
    val reject                     get() = social.reject
    val cancelRequest              get() = social.cancelRequest
    val unfollow                   get() = social.unfollow
    val block                      get() = social.block
    val noIncomingRequests         get() = social.noIncomingRequests
    val noOutgoingRequests         get() = social.noOutgoingRequests
    val incomingRequest            get() = social.incomingRequest
    val sentRequest                get() = social.sentRequest
    val pending                    get() = social.pending
    val accepted                   get() = social.accepted
    val rejected                   get() = social.rejected
    val blocked                    get() = social.blocked
    val unknownUser                get() = social.unknownUser
    val discoverSuggestedDesc      get() = social.discoverSuggestedDesc
    val discoverNewDesc            get() = social.discoverNewDesc
    val discoverAllDesc            get() = social.discoverAllDesc
    val friend                     get() = social.friend
    val requested                  get() = social.requested
    val follow                     get() = social.follow
    val newPost                    get() = social.newPost
    val feeling                    get() = social.feeling
    val people                     get() = social.people
    val addPhotosOrVideos          get() = social.addPhotosOrVideos
    val whatsOnYourMind            get() = social.whatsOnYourMind
    val gallery                    get() = social.gallery
    val gif                        get() = social.gif
    val video                      get() = social.video
    val live                       get() = social.live
    val postAction                 get() = social.postAction
    val public                     get() = social.public
    val friends                    get() = social.friends
    val private                    get() = social.private
    val unread                     get() = social.unread
    val posted                     get() = social.posted
    val likesLabel                 get() = social.likesLabel
    val commentsLabel              get() = social.commentsLabel
    val createPost                 get() = social.createPost
    val postsLabel                 get() = social.postsLabel
    val addMorePhotos              get() = social.addMorePhotos
    val chooseYourFeeling          get() = social.chooseYourFeeling
    val searchFeelings             get() = social.searchFeelings
    val tagPeople                  get() = social.tagPeople
    val searchFriends              get() = social.searchFriends
    val noFriendsFound             get() = social.noFriendsFound
    val findGif                    get() = social.findGif
    val searchGifs                 get() = social.searchGifs
    val noGifsFound                get() = social.noGifsFound
    // ── Event ─────────────────────────────────────────────────────────────────
    val searchEvents               get() = event.searchEvents
    val noEventsFound              get() = event.noEventsFound
    val tryDifferentFilterOrSearch get() = event.tryDifferentFilterOrSearch
    val discoverEventsNearYou      get() = event.discoverEventsNearYou
    val today                      get() = event.today
    val thisWeek                   get() = event.thisWeek
    val eventLabel                 get() = event.eventLabel
    val searchEvent                get() = event.searchEvent
    val aboutThisEvent             get() = event.aboutThisEvent
    val interestedLabel            get() = event.interestedLabel
    val dateLabel                  get() = event.dateLabel
    val timeLabel                  get() = event.timeLabel
    val imInterested               get() = event.imInterested
    val youreInterested            get() = event.youreInterested
    val featured                   get() = event.featured
    val searchEventsLocations      get() = event.searchEventsLocations
    val noEventsFound2             get() = event.noEventsFound2
    val tryDifferentFilterOrSearch2 get() = event.tryDifferentFilterOrSearch2
    val peopleInterested           get() = event.peopleInterested
    val filterAll                  get() = event.filterAll
    val filterToday                get() = event.filterToday
    val filterThisWeek             get() = event.filterThisWeek
    val filterMusic                get() = event.filterMusic
    val filterTech                 get() = event.filterTech
    val filterArt                  get() = event.filterArt
    val filterCampus               get() = event.filterCampus
    val organizer                  get() = event.organizer
    // ── Group ─────────────────────────────────────────────────────────────────
    val searchGroups               get() = group.searchGroups
    val noGroupsFound              get() = group.noGroupsFound
    val tryDifferentKeyword        get() = group.tryDifferentKeyword
    val myGroups                   get() = group.myGroups
    val membersLabel               get() = group.membersLabel
    val joined                     get() = group.joined
    val joinGroup                  get() = group.joinGroup
    val searchGroupsPlaceholder    get() = group.searchGroupsPlaceholder
    val createGroup                get() = group.createGroup
    val cancel                   get() = settingsObj.cancel
    val logoutConfirmTitle       get() = settingsObj.logoutConfirmTitle
    val logoutConfirmMessage     get() = settingsObj.logoutConfirmMessage
}

// ─────────────────────────────────────────────────────────────────────────────
//  Composable helper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberUiStrings(): UiStrings {
    val languageCode by LanguageManager.languageCode.collectAsState()
    return remember(languageCode) { stringsFor(languageCode) }
}

fun stringsFor(languageCode: String): UiStrings =
    if (languageCode == LanguageManager.KHMER) khmerStrings() else englishStrings()

// ─────────────────────────────────────────────────────────────────────────────
//  English
// ─────────────────────────────────────────────────────────────────────────────

private fun englishStrings() = UiStrings(
    common = CommonStrings(
        back = "Back", search = "Search", dismiss = "Dismiss",
        saveChanges = "Save Changes", all = "All", discover = "Discover",
        you = "You", location = "Location", tapToSelect = "Tap to select",
        thisIsPreview = "This is a preview of how your posts will look with the selected theme and colors.",
        appTagline = "Mini Social Media\nfor Campus Students",
        blue = "Blue", purple = "Purple", pink = "Pink", red = "Red",
        orange = "Orange", green = "Green", teal = "Teal",
        crop = "Crop", change = "Change",
        addLocationTitle = "Add Location", searchLocations = "Search locations...",
        gettingYourLocation = "Getting your location...", myLocation = "My Location",
        noLocationsFound = "No locations found", credits = "Credits",
        iconsBy = "- Icons by Lucide Icons", imagesBy = "- Images by Unsplash",
        builtWith = "- Built with Android Compose", copyright = "© 2026 KAMPUS Inc.",
        madeWithLove = "Made with love for connecting people",
    ),
    auth = AuthStrings(
        welcomeBack = "Welcome back 👋", loginTitle = "Login to your\nAccount",
        emailRequired = "Email is required", validEmail = "Enter a valid email",
        ruppEmailOnly = "Only RUPP emails (@rupp.edu.kh) allowed",
        passwordRequired = "Password is required", atLeastSixCharacters = "At least 6 characters",
        rememberMe = "Remember me", dontHaveAccount = "Don't have an account? ",
        orContinueWith = "or continue with", login = "Login", register = "Register",
        emailAddress = "Email address", password = "Password",
        forgotPassword = "Forgot Password?", signUp = "Sign Up", signIn = "Sign In",
        createAccount = "Create Account", termsOfService = "Terms of Service",
        privacyPolicy = "Privacy Policy", verifyCode = "Verify Code",
        didNotReceiveCode = "Didn't receive the code?", resendIn = "Resend in",
        resendCode = "Resend Code",
        youWillReceiveCodeShortly = "You'll receive a 6-digit code shortly",
        allDone = "All Done! 🎉", passwordResetSuccessfully = "Password Reset Successfully",
        yourPasswordHasBeenChanged = "Your password has been changed.",
        loginWithNewPassword = "You can now login with your new password.",
        accountSetup = "Account Setup", accountCannotBeEmpty = "Account cannot be empty",
        onlyRuppEmailsAllowed = "Only RUPP emails (@rupp.edu.kh) allowed",
    ),
    nav = NavStrings(
        home = "Home", groups = "Groups", events = "Events",
        chat = "Chat", messenger = "messenger",
    ),
    profile = ProfileStrings(
        username = "Username", profilePicture = "Profile picture",
        changeProfilePicture = "Change profile picture",
        usernameLabel = "Username", emailLabel = "Email", phoneLabel = "Phone",
        bioLabel = "Bio", facultyLabel = "Faculty", yearLabel = "Year",
        locationLabel = "Location", mutualFriendsLabel = "mutual friends",
        selectYear = "Select year", selectCambodiaProvince = "Select Cambodia province",
        profileSaved = "Profile saved", chooseYear = "Choose year",
        chooseLocation = "Choose location", followersLabel = "Followers",
        followingLabel = "Following", friendsLabel = "Friends",
        shareProfile = "Share Profile", aboutSection = "About",
        loadingProfile = "Loading profile...", unknownLocation = "Unknown",
    ),
    settingsObj = SettingsStrings(
        settings = "Settings", editProfile = "Edit Profile",
        notifications = "Notifications", privacyAndSecurity = "Privacy & Security",
        account = "Account", appearance = "Appearance",
        languageAndRegion = "Language & Region", blockedUsers = "Blocked Users",
        helpAndSupport = "Help & Support", about = "About", logOut = "Log Out",
        version = "Version", theme = "Theme", light = "Light", dark = "Dark",
        accentColor = "Accent Color", fontSize = "Font Size", preview = "Preview",
        noBlockedUsers = "No blocked users", unblock = "Unblock",
        helpTitle = "Help & Support", reportProblem = "Report a Problem",
        reportTechnicalIssue = "Report Technical Issue",
        reportTechnicalIssueHelp = "Let us know if something isn't working",
        appVersion = "App Version", tapToViewQuestions = "Tap to view related questions",
        privacySecurityTitle = "Privacy & Security", blockedUsersTitle = "Blocked Users",
        editProfileTitle = "Edit Profile", notificationsTitle = "Notifications",
        accountTitle = "Account", aboutTitle = "About", contactUs = "Contact Us",
        frequentlyAskedQuestions = "Frequently Asked Questions",
        reportAProblem = "Report a Problem", checkForUpdates = "Check for Updates",
        pushNotifications = "Push Notifications", emailNotifications = "Email Notifications",
        smsNotifications = "SMS Notifications", privateAccount = "Private Account",
        activityStatus = "Activity Status", allowTagging = "Allow Tagging",
        allowMentions = "Allow Mentions",
        twoFactorAuthentication = "Two-Factor Authentication",
        changePassword = "Change Password", loginActivity = "Login Activity",
        downloadYourData = "Download Your Data", searchHistory = "Search History",
        accountInformation = "Account Information", linkedAccounts = "Linked Accounts",
        accountActions = "Account Actions", dangerZone = "Danger Zone",
        phoneNotSet = "Not set", accountCreated = "Account Created",
        connected = "Connected", notConnected = "Not connected",
        deactivateAccount = "Deactivate Account",
        temporarilyDisableAccount = "Temporarily disable your account",
        permanentlyDeleteAccount = "Permanently delete your account and data",
        privacy = "Privacy", security = "Security", dataAndHistory = "Data & History",
        syncingPrivacySettings = "Syncing your privacy settings...",
        languageSubtitle = "Choose the app language",
        englishLanguage = "English", englishDesc = "Use the app in English",
        khmerLanguage = "Khmer", khmerDesc = "Use the app in Khmer",
        cancel = "Cancel",
        logoutConfirmTitle = "Log out?",
        logoutConfirmMessage = "Are you sure you want to log out?",
    ),
    social = SocialStrings(
        hoursAgo = "hours ago", like = "Like", comment = "Comment",
        createStory = "Create story", followBack = "Follow Back",
        followingStatus = "Following", posts = "Posts", requests = "Requests",
        discoverPeople = "Discover People", recentActivity = "Recent Activity",
        noActivityYet = "No activity yet. Your posts, event actions, and shares will appear here.",
        suggested = "Suggested", newest = "New",
        noUsersToDiscoverYet = "No users to discover yet",
        friendRequestsTitle = "Friend Requests", incoming = "Incoming",
        outgoing = "Outgoing", accept = "Accept", reject = "Reject",
        cancelRequest = "Cancel Request", unfollow = "Unfollow", block = "Block",
        noIncomingRequests = "No incoming requests",
        noOutgoingRequests = "No outgoing requests",
        incomingRequest = "Incoming request", sentRequest = "Sent request",
        pending = "Pending", accepted = "Accepted", rejected = "Rejected",
        blocked = "Blocked", unknownUser = "Unknown user",
        discoverSuggestedDesc = "People you may know based on mutual friends and interests",
        discoverNewDesc = "New members who recently joined the community",
        discoverAllDesc = "Browse all users on the platform",
        friend = "Friend", requested = "Requested", follow = "Follow",
        newPost = "New post", feeling = "Feeling", people = "People",
        addPhotosOrVideos = "Add photos or videos",
        whatsOnYourMind = "What's on your mind?",
        gallery = "Gallery", gif = "GIF", video = "Video", live = "Live",
        postAction = "Post", public = "Public", friends = "Friends",
        private = "Private", unread = "Unread", posted = "Posted",
        likesLabel = "likes", commentsLabel = "comments", createPost = "Create Post",
        postsLabel = "posts", addMorePhotos = "Add more photos",
        chooseYourFeeling = "Choose your feeling", searchFeelings = "Search feelings...",
        tagPeople = "Tag people", searchFriends = "Search friends...",
        noFriendsFound = "No friends found", findGif = "Find GIF",
        searchGifs = "Search GIFs...", noGifsFound = "No GIFs found for",
    ),
    event = EventStrings(
        searchEvents = "Search events, locations…", noEventsFound = "No events found",
        tryDifferentFilterOrSearch = "Try a different filter or search",
        discoverEventsNearYou = "Discover events near you",
        today = "Today", thisWeek = "This Week", eventLabel = "Event",
        searchEvent = "Search event...", aboutThisEvent = "About this Event",
        interestedLabel = "interested", dateLabel = "Date", timeLabel = "Time",
        imInterested = "I'm Interested", youreInterested = "You're Interested",
        featured = "⭐ Featured", searchEventsLocations = "Search events, locations…",
        noEventsFound2 = "No events found",
        tryDifferentFilterOrSearch2 = "Try a different filter or search",
        peopleInterested = "people interested",
        filterAll = "All", filterToday = "Today", filterThisWeek = "This Week",
        filterMusic = "Music", filterTech = "Tech", filterArt = "Art",
        filterCampus = "Campus", organizer = "Organizer",
    ),
    group = GroupStrings(
        searchGroups = "Search groups…", noGroupsFound = "No groups found",
        tryDifferentKeyword = "Try a different keyword", myGroups = "My Groups",
        membersLabel = "members", joined = "Joined", joinGroup = "Join Group",
        searchGroupsPlaceholder = "Search groups…", createGroup = "Create Group",
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
//  Khmer
// ─────────────────────────────────────────────────────────────────────────────

private fun khmerStrings() = UiStrings(
    common = CommonStrings(
        back = "ត្រឡប់ក្រោយ", search = "ស្វែងរក", dismiss = "បិទ",
        saveChanges = "រក្សាទុកការផ្លាស់ប្តូរ", all = "ទាំងអស់", discover = "ស្វែងរក",
        you = "អ្នក", location = "ទីតាំង", tapToSelect = "ចុចដើម្បីជ្រើស",
        thisIsPreview = "នេះជាការមើលជាមុននៃរបៀបបង្ហាញប្រកាសរបស់អ្នក។",
        appTagline = "បណ្ដាញសង្គមខ្នាតតូច\nសម្រាប់និស្សិត",
        blue = "ខៀវ", purple = "ស្វាយ", pink = "ផ្កាឈូក", red = "ក្រហម",
        orange = "ទឹកក្រូច", green = "បៃតង", teal = "បៃតងខៀវ",
        crop = "កាត់", change = "ផ្លាស់ប្តូរ",
        addLocationTitle = "បន្ថែមទីតាំង", searchLocations = "ស្វែងរកទីតាំង...",
        gettingYourLocation = "កំពុងទាញយកទីតាំងរបស់អ្នក...",
        myLocation = "ទីតាំងរបស់ខ្ញុំ", noLocationsFound = "រកមិនឃើញទីតាំង",
        credits = "កិត្តិយស", iconsBy = "- រូបតំណាងដោយ Lucide Icons",
        imagesBy = "- រូបភាពដោយ Unsplash",
        builtWith = "- បង្កើតជាមួយ Android Compose",
        copyright = "រក្សាសិទ្ធិឆ្នាំ 2026 KAMPUS Inc.",
        madeWithLove = "បង្កើតដោយក្តីស្រឡាញ់ដើម្បីភ្ជាប់ទំនាក់ទំនងមនុស្ស",
    ),
    auth = AuthStrings(
        welcomeBack = "ស្វាគមន៍ត្រឡប់មកវិញ 👋",
        loginTitle = "ចូលទៅកាន់\nគណនីរបស់អ្នក",
        emailRequired = "ត្រូវការអ៊ីមែល", validEmail = "បញ្ចូលអ៊ីមែលត្រឹមត្រូវ",
        ruppEmailOnly = "អនុញ្ញាតតែអ៊ីមែល RUPP (@rupp.edu.kh)",
        passwordRequired = "ត្រូវការពាក្យសម្ងាត់",
        atLeastSixCharacters = "យ៉ាងហោចណាស់ 6 តួអក្សរ",
        rememberMe = "ចងចាំខ្ញុំ", dontHaveAccount = "មិនទាន់មានគណនី? ",
        orContinueWith = "ឬបន្តដោយ", login = "ចូលគណនី", register = "ចុះឈ្មោះ",
        emailAddress = "អាសយដ្ឋានអ៊ីមែល", password = "ពាក្យសម្ងាត់",
        forgotPassword = "ភ្លេចពាក្យសម្ងាត់?", signUp = "ចុះឈ្មោះ",
        signIn = "ចូលគណនី", createAccount = "បង្កើតគណនី",
        termsOfService = "លក្ខខណ្ឌប្រើប្រាស់",
        privacyPolicy = "គោលការណ៍ឯកជនភាព", verifyCode = "ផ្ទៀងផ្ទាត់កូដ",
        didNotReceiveCode = "មិនទទួលបានកូដទេ?", resendIn = "ផ្ញើម្ដងទៀតក្នុង",
        resendCode = "ផ្ញើកូដម្ដងទៀត",
        youWillReceiveCodeShortly = "អ្នកនឹងទទួលបានកូដ 6 ខ្ទង់ក្នុងពេលឆាប់ៗ",
        allDone = "រួចរាល់ហើយ! 🎉",
        passwordResetSuccessfully = "កំណត់ពាក្យសម្ងាត់ឡើងវិញដោយជោគជ័យ",
        yourPasswordHasBeenChanged = "ពាក្យសម្ងាត់របស់អ្នកត្រូវបានផ្លាស់ប្តូរ។",
        loginWithNewPassword = "ឥឡូវអ្នកអាចចូលប្រើពាក្យសម្ងាត់ថ្មីបាន។",
        accountSetup = "ការកំណត់គណនី",
        accountCannotBeEmpty = "គណនីមិនអាចទទេបាន",
        onlyRuppEmailsAllowed = "អនុញ្ញាតតែអ៊ីមែល RUPP (@rupp.edu.kh)",
    ),
    nav = NavStrings(
        home = "ដើម", groups = "ក្រុម", events = "ព្រឹត្តិការណ៍",
        chat = "ជជែក", messenger = "សារ",
    ),
    profile = ProfileStrings(
        username = "ឈ្មោះអ្នកប្រើ", profilePicture = "រូបប្រវត្តិរូប",
        changeProfilePicture = "ប្តូររូបប្រវត្តិរូប",
        usernameLabel = "ឈ្មោះអ្នកប្រើ", emailLabel = "អ៊ីមែល",
        phoneLabel = "ទូរស័ព្ទ", bioLabel = "ជីវប្រវត្តិ",
        facultyLabel = "មហាវិទ្យាល័យ", yearLabel = "ឆ្នាំ",
        locationLabel = "ទីតាំង", mutualFriendsLabel = "មិត្តភក្ដិរួម",
        selectYear = "ជ្រើសរើសឆ្នាំ",
        selectCambodiaProvince = "ជ្រើសរើសខេត្តកម្ពុជា",
        profileSaved = "បានរក្សាទុកប្រវត្តិរូប",
        chooseYear = "ជ្រើសរើសឆ្នាំ", chooseLocation = "ជ្រើសរើសទីតាំង",
        followersLabel = "អ្នកតាមដាន", followingLabel = "កំពុងតាមដាន",
        friendsLabel = "មិត្តភក្ដិ", shareProfile = "ចែករំលែកប្រវត្តិរូប",
        aboutSection = "អំពី", loadingProfile = "កំពុងផ្ទុកប្រវត្តិរូប...",
        unknownLocation = "មិនស្គាល់ទីតាំង",
    ),
    settingsObj = SettingsStrings(
        settings = "ការកំណត់", editProfile = "កែប្រែប្រវត្តិរូប",
        notifications = "ការជូនដំណឹង",
        privacyAndSecurity = "ឯកជនភាព និងសុវត្ថិភាព",
        account = "គណនី", appearance = "រចនាប័ទ្ម",
        languageAndRegion = "ភាសា និងតំបន់",
        blockedUsers = "អ្នកប្រើត្រូវបានបិទ",
        helpAndSupport = "ជំនួយ និងការគាំទ្រ",
        about = "អំពី", logOut = "ចាកចេញ", version = "កំណែ",
        theme = "ស្បែក", light = "ភ្លឺ", dark = "ងងឹត",
        accentColor = "ពណ៌សង្កត់", fontSize = "ទំហំអក្សរ",
        preview = "មើលជាមុន",
        noBlockedUsers = "មិនមានអ្នកប្រើត្រូវបានបិទ", unblock = "ដោះបិទ",
        helpTitle = "ជំនួយ និងការគាំទ្រ",
        reportProblem = "រាយការណ៍បញ្ហា",
        reportTechnicalIssue = "រាយការណ៍បញ្ហាបច្ចេកទេស",
        reportTechnicalIssueHelp = "ប្រាប់យើង ប្រសិនបើអ្វីមួយមិនដំណើរការ",
        appVersion = "កំណែកម្មវិធី",
        tapToViewQuestions = "ចុចដើម្បីមើលសំណួរពាក់ព័ន្ធ",
        privacySecurityTitle = "ឯកជនភាព និងសុវត្ថិភាព",
        blockedUsersTitle = "អ្នកប្រើត្រូវបានបិទ",
        editProfileTitle = "កែប្រែប្រវត្តិរូប",
        notificationsTitle = "ការជូនដំណឹង",
        accountTitle = "គណនី", aboutTitle = "អំពី",
        contactUs = "ទាក់ទងយើង",
        frequentlyAskedQuestions = "សំណួរដែលសួរញឹកញាប់",
        reportAProblem = "រាយការណ៍បញ្ហា",
        checkForUpdates = "ពិនិត្យមើលការអាប់ដេត",
        pushNotifications = "ការជូនដំណឹង Push",
        emailNotifications = "ការជូនដំណឹងតាមអ៊ីមែល",
        smsNotifications = "ការជូនដំណឹងតាម SMS",
        privateAccount = "គណនីឯកជន",
        activityStatus = "ស្ថានភាពសកម្មភាព",
        allowTagging = "អនុញ្ញាតឱ្យ Tag",
        allowMentions = "អនុញ្ញាតឱ្យ Mention",
        twoFactorAuthentication = "ការផ្ទៀងផ្ទាត់ពីរជាន់",
        changePassword = "ប្តូរពាក្យសម្ងាត់",
        loginActivity = "សកម្មភាពចូលគណនី",
        downloadYourData = "ទាញយកទិន្នន័យរបស់អ្នក",
        searchHistory = "ប្រវត្តិស្វែងរក",
        accountInformation = "ព័ត៌មានគណនី",
        linkedAccounts = "គណនីភ្ជាប់",
        accountActions = "សកម្មភាពគណនី",
        dangerZone = "តំបន់គ្រោះថ្នាក់",
        phoneNotSet = "មិនបានកំណត់",
        accountCreated = "បង្កើតគណនី",
        connected = "បានភ្ជាប់", notConnected = "មិនទាន់ភ្ជាប់",
        deactivateAccount = "បិទដំណើរការគណនី",
        temporarilyDisableAccount = "បិទគណនីបណ្តោះអាសន្ន",
        permanentlyDeleteAccount = "លុបទិន្នន័យ និងគណនីជារៀងរហូត",
        privacy = "ឯកជនភាព", security = "សុវត្ថិភាព",
        dataAndHistory = "ទិន្នន័យ និងប្រវត្តិ",
        syncingPrivacySettings = "កំពុងធ្វើសមកាលកម្មការកំណត់ឯកជនភាព...",
        languageSubtitle = "ជ្រើសរើសភាសាសម្រាប់កម្មវិធី",
        englishLanguage = "អង់គ្លេស", englishDesc = "ប្រើកម្មវិធីជាភាសាអង់គ្លេស",
        khmerLanguage = "ខ្មែរ", khmerDesc = "ប្រើកម្មវិធីជាភាសាខ្មែរ",
        cancel = "បោះបង់",
        logoutConfirmTitle = "ចាកចេញមែនទេ?",
        logoutConfirmMessage = "តើអ្នកប្រាកដថាចង់ចាកចេញពីគណនីមែនទេ?",
    ),
    social = SocialStrings(
        hoursAgo = "ម៉ោងមុន", like = "ចូលចិត្ត", comment = "មតិយោបល់",
        createStory = "បង្កើតស្តូរី", followBack = "តាមដានវិញ",
        followingStatus = "កំពុងតាមដាន", posts = "ប្រកាស", requests = "សំណើ",
        discoverPeople = "ស្វែងរកមនុស្ស", recentActivity = "សកម្មភាពថ្មីៗ",
        noActivityYet = "មិនទាន់មានសកម្មភាពនៅឡើយ។",
        suggested = "ណែនាំ", newest = "ថ្មី",
        noUsersToDiscoverYet = "មិនទាន់មានអ្នកប្រើសម្រាប់ស្វែងរក",
        friendRequestsTitle = "សំណើមិត្តភក្ដិ",
        incoming = "ចូលមក", outgoing = "ចេញទៅ",
        accept = "ទទួលយក", reject = "បដិសេធ",
        cancelRequest = "បោះបង់សំណើ", unfollow = "ឈប់តាមដាន", block = "ប្លុក",
        noIncomingRequests = "មិនមានសំណើចូលមក",
        noOutgoingRequests = "មិនមានសំណើចេញទៅ",
        incomingRequest = "សំណើចូលមក", sentRequest = "សំណើបានផ្ញើ",
        pending = "កំពុងរង់ចាំ", accepted = "បានទទួលយក",
        rejected = "បានបដិសេធ", blocked = "បានប្លុក",
        unknownUser = "អ្នកប្រើមិនស្គាល់",
        discoverSuggestedDesc = "មនុស្សដែលអ្នកប្រហែលស្គាល់ដោយផ្អែកលើមិត្តភក្ដិរួម",
        discoverNewDesc = "សមាជិកថ្មីដែលទើបចូលរួមសហគមន៍",
        discoverAllDesc = "រកមើលអ្នកប្រើទាំងអស់លើវេទិកា",
        friend = "មិត្តភក្ដិ", requested = "បានស្នើ", follow = "តាមដាន",
        newPost = "ប្រកាសថ្មី", feeling = "អារម្មណ៍", people = "មនុស្ស",
        addPhotosOrVideos = "បន្ថែមរូបថត ឬវីដេអូ",
        whatsOnYourMind = "តើមានអ្វីក្នុងចិត្តរបស់អ្នក?",
        gallery = "ដាច់ឆលន្ទ", gif = "GIF", video = "វីដេអូ", live = "ライវ",
        postAction = "ផ្សាយ", public = "សាធារណៈ", friends = "មិត្តភក្ដិ",
        private = "ឯកជន", unread = "មិនបាននៃ", posted = "បានបង្ហោះ",
        likesLabel = "ចូលចិត្ត", commentsLabel = "មតិយោបល់",
        createPost = "បង្កើតការបង្ហោះ", postsLabel = "ប្រកាស",
        addMorePhotos = "បន្ថែមរូបថតបន្ថែម",
        chooseYourFeeling = "ជ្រើសរើសអារម្មណ៍របស់អ្នក",
        searchFeelings = "ស្វែងរកអារម្មណ៍...",
        tagPeople = "Tag មនុស្ស", searchFriends = "ស្វែងរកមិត្តភក្ដិ...",
        noFriendsFound = "រកមិនឃើញមិត្តភក្ដិ",
        findGif = "ស្វែងរក GIF", searchGifs = "ស្វែងរក GIFs...",
        noGifsFound = "រកមិនឃើញ GIFs សម្រាប់",
    ),
    event = EventStrings(
        searchEvents = "ស្វែងរកព្រឹត្តិការណ៍ ឬទីតាំង…",
        noEventsFound = "រកមិនឃើញព្រឹត្តិការណ៍",
        tryDifferentFilterOrSearch = "សាកល្បងតម្រង ឬការស្វែងរកផ្សេងទៀត",
        discoverEventsNearYou = "ស្វែងរកព្រឹត្តិការណ៍ដែលនៅជិតអ្នក",
        today = "ថ្ងៃនេះ", thisWeek = "សប្តាហ៍នេះ",
        eventLabel = "ព្រឹត្តិការណ៍",
        searchEvent = "ស្វែងរកព្រឹត្តិការណ៍...",
        aboutThisEvent = "អំពីព្រឹត្តិការណ៍នេះ",
        interestedLabel = "ចាប់អារម្មណ៍",
        dateLabel = "កាលបរិច្ឆេទ", timeLabel = "ម៉ោង",
        imInterested = "ខ្ញុំចាប់អារម្មណ៍",
        youreInterested = "អ្នកបានចាប់អារម្មណ៍",
        featured = "⭐ លេចធ្លោ",
        searchEventsLocations = "ស្វែងរកព្រឹត្តិការណ៍ ទីតាំង…",
        noEventsFound2 = "រកមិនឃើញព្រឹត្តិការណ៍",
        tryDifferentFilterOrSearch2 = "សាកល្បងតម្រង ឬស្វែងរកផ្សេង",
        peopleInterested = "នាក់ចាប់អារម្មណ៍",
        filterAll = "ទាំងអស់", filterToday = "ថ្ងៃនេះ",
        filterThisWeek = "សប្តាហ៍នេះ", filterMusic = "តន្ត្រី",
        filterTech = "បច្ចេកវិទ្យា", filterArt = "សិល្បៈ",
        filterCampus = "សាលា", organizer = "អ្នករៀបចំ",
    ),
    group = GroupStrings(
        searchGroups = "ស្វែងរកក្រុម…",
        noGroupsFound = "រកមិនឃើញក្រុម",
        tryDifferentKeyword = "សាកល្បងពាក្យស្វែងរកផ្សេងទៀត",
        myGroups = "ក្រុមរបស់ខ្ញុំ", membersLabel = "សមាជិក",
        joined = "បានចូលរួម", joinGroup = "ចូលរួមក្រុម",
        searchGroupsPlaceholder = "ស្វែងរកក្រុម…",
        createGroup = "បង្កើតក្រុម",
    ),
)