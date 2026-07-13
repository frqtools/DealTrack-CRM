package com.frqtools.dealtrackcrm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frqtools.dealtrackcrm.MainActivity
import com.frqtools.dealtrackcrm.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("DealTrackFCM", "New FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("DealTrackFCM", "FCM Message Received: From=${remoteMessage.from}")
        Log.d("DealTrackFCM", "FCM Data Payload: ${remoteMessage.data}")
        Log.d("DealTrackFCM", "FCM Notification Payload: Title=${remoteMessage.notification?.title}, Body=${remoteMessage.notification?.body}, ImageUrl=${remoteMessage.notification?.imageUrl}")

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "DealTrack CRM Alert"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""
        
        // Match both 'image' and 'imageUrl' keys for maximum compatibility
        val imageUrl = data["image"] ?: data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString()
        val clickAction = data["click_action"] ?: data["clickAction"]
        val ctaText = data["cta_text"] ?: data["ctaText"]
        val ctaLink = data["cta_link"] ?: data["ctaLink"]
        val imageClickAction = data["image_click_action"] ?: data["imageClickAction"]

        Log.d("DealTrackFCM", "Parsed Fields: Title='$title', Body='$body', ImageUrl='$imageUrl', ClickAction='$clickAction', CTALink='$ctaLink', CTAText='$ctaText'")

        sendLocalNotification(this, title, body, imageUrl, clickAction, ctaText, ctaLink, imageClickAction)
    }

    companion object {
        fun sendLocalNotification(
            context: Context,
            title: String,
            body: String,
            imageUrl: String?,
            clickAction: String?,
            ctaText: String?,
            ctaLink: String?,
            imageClickAction: String?
        ) {
            val channelId = "dealtrack_broadcast_channel"
            val channelName = "Broadcast Alerts"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Manual broadcast messages with optional actions and images"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            // Setup primary notification click redirection
            val mainRedirect = imageClickAction ?: clickAction ?: "dealtrack://home"
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("click_action", mainRedirect)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                mainIntent,
                flags
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)

            if (!imageUrl.isNullOrEmpty()) {
                Thread {
                    val bitmap = getBitmapFromUrl(imageUrl)
                    if (bitmap != null) {
                        Log.d("DealTrackFCM", "Successfully downloaded image bitmap. Attaching big picture style.")
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                        builder.setLargeIcon(bitmap)
                    } else {
                        Log.e("DealTrackFCM", "Failed to download image bitmap from URL: $imageUrl")
                    }
                    postNotificationWithCTA(context, builder, ctaText, ctaLink, flags, manager)
                }.start()
            } else {
                postNotificationWithCTA(context, builder, ctaText, ctaLink, flags, manager)
            }
        }

        private fun postNotificationWithCTA(
            context: Context,
            builder: NotificationCompat.Builder,
            ctaText: String?,
            ctaLink: String?,
            flags: Int,
            manager: NotificationManager
        ) {
            if (!ctaText.isNullOrEmpty() && !ctaLink.isNullOrEmpty()) {
                Log.d("DealTrackFCM", "Adding CTA button Action: Text='$ctaText', Link='$ctaLink'")
                val actionIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("cta_link", ctaLink)
                }
                val actionPendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt() + 1,
                    actionIntent,
                    flags
                )
                builder.addAction(R.mipmap.ic_launcher_round, ctaText, actionPendingIntent)
            }

            manager.notify(System.currentTimeMillis().toInt(), builder.build())
            Log.d("DealTrackFCM", "Notification posted successfully.")
        }

        private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
            return try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.instanceFollowRedirects = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Log.e("DealTrackFCM", "Error downloading image from URL: $imageUrl", e)
                null
            }
        }
    }
}
