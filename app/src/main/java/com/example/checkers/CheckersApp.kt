package com.example.checkers

import android.app.Application
import android.util.Log
import com.example.checkers.game.CheckersEngine
import timber.log.Timber

class CheckersApp : Application() {

    val engine: CheckersEngine = CheckersEngine()

    override fun onCreate() {
        super.onCreate()
        initLogger()
        Timber.i("CheckersApp started (debug=%b)", BuildConfig.DEBUG)
    }

    private fun initLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(CheckersDebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    private class CheckersDebugTree : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "Checkers/${element.className.substringAfterLast('.')}:${element.lineNumber}#${element.methodName}"
        }
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) return
            val logTag = tag ?: "Checkers"
            if (t != null) {
                Log.println(priority, logTag, "$message\n${Log.getStackTraceString(t)}")
            } else {
                Log.println(priority, logTag, message)
            }
        }
    }
}