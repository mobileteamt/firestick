package com.example.firestick.connectionandcommand

import android.content.Context
import android.util.Log

fun runAdb(appContext: Context, host: String, command: String): String {
    return try {
        val adbPath = "${appContext.applicationInfo.nativeLibraryDir}/libadb.so"
        val commandList = listOf(
            "sh",
            "-c",
            "$adbPath connect $host; $adbPath -s $host $command"
        )

        val processBuilder = ProcessBuilder(commandList).directory(appContext.filesDir).apply {
            redirectErrorStream(true)
            environment().apply {
                put("HOME", appContext.filesDir.path)
                put("TMPDIR", appContext.cacheDir.path)
            }
        }

        val process = processBuilder.start()

        // Read output and error
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        // Wait for process to complete
        val exitCode = process.waitFor()

        // Log and return output or error
        if (exitCode == 0) {
            Log.d("ADB_COMMAND", "Success: $output")
            output
        } else {
            Log.e("ADB_COMMAND", "Error: $error")
            "Error executing ADB command: $error"
        }

    } catch (e: Exception) {
        Log.e("ADB_COMMAND", "Exception: ${e.message}")
        "Exception: ${e.message}"
    }
}




