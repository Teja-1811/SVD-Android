package com.svd.svdagencies.ui.admin

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ReportDownloads {
    suspend fun save(activity: Activity, response: ResponseBody, filename: String): File = withContext(Dispatchers.IO) {
        val file = File(activity.getExternalFilesDir(null), filename)
        response.use { body -> body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } } }
        val signature = file.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
        if (String(signature, Charsets.US_ASCII) != "%PDF") {
            file.delete()
            throw IllegalStateException("The server did not return a PDF. Please try again after the website update.")
        }
        file
    }

    fun open(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val share = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(view, "Open or print report").putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(share))
        activity.startActivity(chooser)
    }
}
