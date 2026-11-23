package com.ecosystemku.assistant

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

class EcosystemKuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = buildString {
                    append("Thread: ").append(thread.name).append('\n')
                    append(Log.getStackTraceString(throwable))
                }
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                    putExtra(CrashActivity.EXTRA_STACKTRACE, trace)
                }
                startActivity(intent)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (_: Throwable) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
