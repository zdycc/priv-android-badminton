package com.example.ding_badminton.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ding_badminton.MainActivity

class BookingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_AUTO_BOOKING) return
        val launchIntent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_AUTO_START_BOOKING, true)
            }
        context.startActivity(launchIntent)
    }

    companion object {
        const val ACTION_AUTO_BOOKING = "com.example.ding_badminton.AUTO_BOOKING"
        const val EXTRA_AUTO_START_BOOKING = "extra_auto_start_booking"
    }
}
