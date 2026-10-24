package de.emailspeicher.zufallsklingelton

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun interface ReceiverWorkRunner {
    fun run(receiver: BroadcastReceiver, work: () -> Unit)
}

private val asyncReceiverWorkRunner = ReceiverWorkRunner { receiver, work ->
    val pendingResult = receiver.goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            work()
        } finally {
            pendingResult.finish()
        }
    }
}

class BootReceiver(private val workRunner: ReceiverWorkRunner = asyncReceiverWorkRunner) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (isBootCompletedAction(intent?.action)) {
            workRunner.run(this) { RingtoneCoordinator(context).onStartup() }
        }
    }
}

class DateChangedReceiver(private val workRunner: ReceiverWorkRunner = asyncReceiverWorkRunner) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (isDateChangedAction(intent?.action)) {
            workRunner.run(this) { RingtoneCoordinator(context).onDateChanged() }
        }
    }
}

class PhoneStateReceiver(private val workRunner: ReceiverWorkRunner = asyncReceiverWorkRunner) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        workRunner.run(this) {
            val playback = PlaybackStore(context)
            when (phoneStateAction(playback.ringing, state)) {
                PhoneStateAction.REMEMBER_RINGING -> playback.ringing = true
                PhoneStateAction.FINISH_INCOMING_CALL -> {
                    playback.ringing = false
                    RingtoneCoordinator(context).onIncomingCallFinished()
                }
                PhoneStateAction.IGNORE -> Unit
            }
        }
    }
}
