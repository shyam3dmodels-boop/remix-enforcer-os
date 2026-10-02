package com.example.core.alarm.captcha

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manager implementing the open Urbandroid Sleep as Android CAPTCHA protocol.
 * Ref: https://github.com/urbandroid-team/sleep-captcha-support
 * Allows dispatching and receiving results from standard Sleep as Android CAPTCHA plugins.
 */
class SleepCaptchaProtocolManager(private val context: Context) {

    /**
     * Builds an explicit or implicit intent to launch a Sleep as Android Captcha.
     */
    fun createCaptchaIntent(captchaAction: String = ACTION_START_CAPTCHA): Intent {
        return Intent(captchaAction).apply {
            putExtra(EXTRA_ALARM_MODE, true)
            putExtra(EXTRA_SUPPRESS_FALLBACK, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Validates whether an incoming callback intent represents a passed CAPTCHA.
     */
    fun isCaptchaSolved(intent: Intent?): Boolean {
        if (intent == null) return false
        val isSuccess = intent.getBooleanExtra(EXTRA_RESULT, false)
        Log.d("SleepCaptchaProtocol", "Received CAPTCHA result: $isSuccess")
        return isSuccess
    }

    companion object {
        const val ACTION_START_CAPTCHA = "com.urbandroid.sleep.captcha.intent.action.START"
        const val ACTION_CAPTCHA_ALIVE = "com.urbandroid.sleep.captcha.intent.action.ALIVE"
        const val EXTRA_RESULT = "com.urbandroid.sleep.captcha.intent.extra.RESULT"
        const val EXTRA_ALARM_MODE = "com.urbandroid.sleep.captcha.intent.extra.ALARM_MODE"
        const val EXTRA_SUPPRESS_FALLBACK = "com.urbandroid.sleep.captcha.intent.extra.SUPPRESS_FALLBACK"
    }
}
