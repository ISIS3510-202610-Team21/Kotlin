package com.example.spendantt.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationTimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "onReceive action=$action")
        NotificationSyncScheduler.schedulePeriodic(context)
        NotificationSyncScheduler.enqueueImmediate(context, action)
    }

    companion object {
        private const val TAG = "SpendAntNotifSync"
    }
}
