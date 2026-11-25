package com.example.ding_badminton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ding_badminton.alarm.BookingAlarmReceiver
import com.example.ding_badminton.ui.theme.Ding_badmintonTheme
import com.example.ding_badminton.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Ding_badminton)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val autoStartBooking = intent?.getBooleanExtra(BookingAlarmReceiver.EXTRA_AUTO_START_BOOKING, false) ?: false
        setContent {
            Ding_badmintonTheme {
                AppRoot(autoStartBooking = autoStartBooking)
            }
        }
    }
}