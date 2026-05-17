package com.example.kampus.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ReportGmailerrorred
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private fun defaultAboutActions() = listOf(
    AboutActionItem(title = "Privacy Policy", iconKey = "privacy", actionUrl = "https://kampus.app/privacy"),
    AboutActionItem(title = "Open Source Licenses", iconKey = "licenses", actionUrl = "https://kampus.app/licenses"),
)

private fun defaultContactOptions() = listOf(
    ContactOptionItem(title = "KAMPUS Help Center", subtitle = "Browse articles and guides", iconKey = "description", actionValue = "https://kampus.app/help"),
    ContactOptionItem(title = "Live Chat", subtitle = "Chat with our support team", iconKey = "chat", actionValue = "https://kampus.app/support/chat"),
    ContactOptionItem(title = "Email Support", subtitle = "support@kampus.app", iconKey = "email", actionValue = "support@kampus.app"),
    ContactOptionItem(title = "Phone Support", subtitle = "Call us for assistance", iconKey = "phone", actionValue = "+1-800-555-0199"),
)

private fun defaultFaqTopics() = listOf("Account", "Security", "Payments", "Safety", "Privacy")

@Immutable
data class AboutActionItem(
    val title: String,
    val iconKey: String,
    val actionUrl: String = "",
)

@Immutable
data class ContactOptionItem(
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val actionValue: String = "",
)

@Immutable
data class SupportContentUiState(
    val appName: String = "KAMPUS",
    val appLogoUrl: String = "",
    val appLogoFallbackText: String = "K",
    val aboutVersion: String = "1.0.0",
    val aboutActions: List<AboutActionItem> = defaultAboutActions(),
    val contactOptions: List<ContactOptionItem> = defaultContactOptions(),
    val faqTopics: List<String> = defaultFaqTopics(),
    val reportTechnicalIssueTitle: String = "Report Technical Issue",
    val reportTechnicalIssueHelp: String = "Email or chat with support to report a problem.",
    val appVersionText: String = "1.0.0 (Build 2024.03.21)",
    val checkForUpdatesText: String = "Check for Updates",
)

class SupportContentViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(SupportContentUiState())
    val uiState: StateFlow<SupportContentUiState> = _uiState.asStateFlow()
    private var contentListener: ListenerRegistration? = null

    init {
        observeSupportContent()
    }

    private fun observeSupportContent() {
        contentListener?.remove()
        contentListener = firestore
            .collection("appConfig")
            .document("content")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                _uiState.update { current ->
                    current.copy(
                        appName = snapshot.getString("appName") ?: current.appName,
                        appLogoUrl = snapshot.getString("appLogoUrl") ?: snapshot.getString("logoUrl") ?: current.appLogoUrl,
                        appLogoFallbackText = snapshot.getString("appLogoFallbackText") ?: snapshot.getString("logoText") ?: current.appLogoFallbackText,
                        aboutVersion = snapshot.getString("aboutVersion") ?: snapshot.getString("appVersion") ?: current.aboutVersion,
                        aboutActions = parseAboutActions(snapshot.get("aboutActions")) ?: current.aboutActions,
                        contactOptions = parseContactOptions(snapshot.get("contactOptions")) ?: current.contactOptions,
                        faqTopics = parseFaqTopics(snapshot.get("faqTopics")) ?: current.faqTopics,
                        reportTechnicalIssueTitle = snapshot.getString("reportTechnicalIssueTitle") ?: current.reportTechnicalIssueTitle,
                        reportTechnicalIssueHelp = snapshot.getString("reportTechnicalIssueHelp") ?: current.reportTechnicalIssueHelp,
                        appVersionText = snapshot.getString("appVersionText") ?: current.appVersionText,
                        checkForUpdatesText = snapshot.getString("checkForUpdatesText") ?: current.checkForUpdatesText,
                    )
                }
            }
    }

    private fun parseAboutActions(rawValue: Any?): List<AboutActionItem>? {
        val items = (rawValue as? List<*>)?.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val title = (map["title"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val iconKey = (map["iconKey"] as? String)?.takeIf { it.isNotBlank() } ?: "description"
            val rawUrl = (map["actionUrl"] as? String)?.takeIf { it.isNotBlank() } ?: ""
            val actionUrl = normalizeActionUrl(rawUrl)
            AboutActionItem(title = title, iconKey = iconKey, actionUrl = actionUrl)
        }.orEmpty()

        return items.takeIf { it.isNotEmpty() }
    }

    private fun parseContactOptions(rawValue: Any?): List<ContactOptionItem>? {
        val items = (rawValue as? List<*>)?.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val title = (map["title"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val subtitle = (map["subtitle"] as? String)?.takeIf { it.isNotBlank() } ?: ""
            val iconKey = (map["iconKey"] as? String)?.takeIf { it.isNotBlank() } ?: "chat"
            val rawAction = (map["actionValue"] as? String)?.takeIf { it.isNotBlank() } ?: ""
            val actionValue = normalizeActionValue(rawAction, iconKey)
            ContactOptionItem(title = title, subtitle = subtitle, iconKey = iconKey, actionValue = actionValue)
        }.orEmpty()

        return items.takeIf { it.isNotEmpty() }
    }

    private fun parseFaqTopics(rawValue: Any?): List<String>? {
        val items = (rawValue as? List<*>)?.mapNotNull { entry ->
            (entry as? String)?.takeIf { it.isNotBlank() }
        }.orEmpty()
        return items.takeIf { it.isNotEmpty() }
    }

    private fun normalizeActionValue(raw: String, iconKey: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val low = trimmed.lowercase()
        if (low.startsWith("mailto:") || low.startsWith("tel:") || low.startsWith("http://") || low.startsWith("https://")) return trimmed

        return when {
            iconKey.equals("email", true) || trimmed.contains("@") -> "mailto:${trimmed}"
            iconKey.equals("phone", true) || Regex("^\\+?[0-9][0-9\\-\\s()]{4,}").matches(trimmed) -> "tel:${trimmed.replace(" ", "")}"
            trimmed.startsWith("www.") || trimmed.contains(".") -> "https://${trimmed}"
            else -> trimmed
        }
    }

    private fun normalizeActionUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val low = trimmed.lowercase()
        if (low.startsWith("http://") || low.startsWith("https://")) return trimmed
        return if (trimmed.startsWith("/")) trimmed else if (trimmed.startsWith("www.") || trimmed.contains(".")) "https://${trimmed}" else trimmed
    }

    override fun onCleared() {
        super.onCleared()
        contentListener?.remove()
    }

    companion object {
        fun iconForAbout(key: String) = when (key.lowercase()) {
            "license", "licenses", "opensource" -> Icons.Outlined.Gavel
            "description", "policy", "privacy" -> Icons.Outlined.Description
            else -> Icons.Outlined.Description
        }

        fun iconForContact(key: String) = when (key.lowercase()) {
            "email" -> Icons.Outlined.Email
            "phone" -> Icons.Outlined.Phone
            "report" -> Icons.Outlined.ReportGmailerrorred
            else -> Icons.Outlined.ChatBubbleOutline
        }
    }
}