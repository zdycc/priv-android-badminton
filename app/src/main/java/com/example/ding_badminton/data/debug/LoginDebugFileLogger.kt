package com.example.ding_badminton.data.debug

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LoginDebugFileLogger {
    private const val FILE_NAME = "login_validation.log"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)

    fun clear(context: Context) {
        runCatching { file(context).writeText("") }
    }

    fun append(context: Context, message: String) {
        val line = "${timeFormat.format(Date())} | $message\n"
        runCatching { file(context).appendText(line) }
    }

    fun filePath(context: Context): String = file(context).absolutePath

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}
