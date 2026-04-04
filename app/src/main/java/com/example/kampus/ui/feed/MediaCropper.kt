package com.example.kampus.ui.feed

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File

object MediaCropper {

    @SuppressLint("ResourceAsColor")
    fun createCropIntent(context: Context, input: Uri): Intent {
        val outputFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            outputFile,
        )

        val options = UCrop.Options().apply {
            setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
            setCompressionQuality(92)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)

            // Set aspect ratio constraints to reduce layout shifts
            setStatusBarColor(android.R.color.black)
            setToolbarColor(android.R.color.black)
        }

        return UCrop.of(input, outputUri)
            .withOptions(options)
            .getIntent(context)
    }

    fun getOutput(data: Intent?): Uri? = data?.let { UCrop.getOutput(it) }
}
