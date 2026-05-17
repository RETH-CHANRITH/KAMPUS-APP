package com.example.kampus.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openWebPage(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        showSupportActionError(context)
    }
}

fun openEmail(context: Context, email: String, subject: String = "", body: String = "") {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
        if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
        if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showSupportActionError(context)
    }
}

fun openDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showSupportActionError(context)
    }
}

private fun showSupportActionError(context: Context) {
    Toast.makeText(context, "Unable to open this action", Toast.LENGTH_SHORT).show()
}