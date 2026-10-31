package com.mobilecomputing.myapplication

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReportShareHelper {

    fun sharePdfReport(
        context: Context,
        fileName: String,
        generate: (FileOutputStream) -> Unit
    ) {
        try {
            val cacheFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(cacheFile)
            generate(outputStream)
            outputStream.flush()
            outputStream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense & Budget Statement Report")
                putExtra(Intent.EXTRA_TEXT, "Here is the PDF statement report from Expense Tracker.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share or Open PDF Report via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
