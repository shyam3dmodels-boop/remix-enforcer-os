package com.example.core.claw

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Telephony & SMS Dispatcher for PrivateAgent.
 *
 * Implements:
 * 1. Direct phone call placement / dialer opening.
 * 2. SMS message prefilling and dispatching.
 */
class PrivateAgentTelephonyDispatcher private constructor(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Dials or initiates a phone call to the requested number.
     */
    fun callNumber(phoneNumber: String, directCall: Boolean = false): Boolean {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (cleanNumber.isBlank()) {
            Log.w(TAG, "Cannot call empty phone number")
            return false
        }

        return try {
            val action = if (directCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(intent)
            Log.i(TAG, "Dispatched phone call intent to: $cleanNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch call intent", e)
            // Fallback to dialer if direct call permission not granted
            try {
                val fallback = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appContext.startActivity(fallback)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback dialer failed", ex)
                false
            }
        }
    }

    /**
     * Opens SMS app pre-populated with recipient and message body.
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(intent)
            Log.i(TAG, "Dispatched SMS intent to $cleanNumber: \"$message\"")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch SMS intent", e)
            false
        }
    }

    companion object {
        private const val TAG = "PrivateAgentTelephony"

        @Volatile
        private var instance: PrivateAgentTelephonyDispatcher? = null

        fun getInstance(context: Context): PrivateAgentTelephonyDispatcher {
            return instance ?: synchronized(this) {
                instance ?: PrivateAgentTelephonyDispatcher(context).also { instance = it }
            }
        }
    }
}
