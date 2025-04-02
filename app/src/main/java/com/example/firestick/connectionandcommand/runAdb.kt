package com.example.firestick.connectionandcommand

import android.content.Context

fun runAdb(appContext: Context, host: String, command: String): Process {
    val adbPath = "${appContext.applicationInfo.nativeLibraryDir}/libadb.so"
    val commandList = listOf(
        "sh",
        "-c",
        "$adbPath connect $host; $adbPath -s $host $command",
    )

    val processBuilder = ProcessBuilder(commandList).directory(appContext.filesDir).apply {
        redirectErrorStream(true)
        environment().apply {
            put("HOME", appContext.filesDir.path)
            put("TMPDIR", appContext.cacheDir.path)
        }
    }

    return processBuilder.start()
}






