package com.example.teamup.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.teamup.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import com.example.teamup.ui.util.ActiveChat

 // recebe mensagens push do Firebase
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG        = "FCM"
        private const val CHANNEL_ID = "chat"
    }

    // quando token fcm muda
    override fun onNewToken(token: String) {
        Log.d(TAG, "NEW FCM TOKEN -> $token")

    }

     // se receber um push notification verifica se esta fora do chat e se tiver manda a notificaçao
    override fun onMessageReceived(msg: RemoteMessage) {

        /*  id da atividade*/
        val pushedEventId = msg.data["event_id"]?.toIntOrNull()


        if (pushedEventId != null &&
            pushedEventId == ActiveChat.currentEventId) {
            Log.d(TAG, "Ignoring push for event $pushedEventId – user already inside chat.")
            return
        }

        /* constroi e mostra notificaçao*/
        val title = msg.notification?.title ?: getString(R.string.app_name)
        val body  = msg.notification?.body  ?: ""

        createNotificationChannelIfNeeded()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_up)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Random.nextInt(), notification)
    }


    /* One-time channel setup for Android 8+ */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Chat messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Push notifications for new chat messages"
                    enableLights(true)
                    enableVibration(true)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }
}
