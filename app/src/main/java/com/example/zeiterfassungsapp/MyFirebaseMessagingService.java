package com.example.zeiterfassungsapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Dienst zum Verarbeiten eingehender Firebase-Messaging-Nachrichten.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    // Tag für das Logging
    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Diese Methode wird aufgerufen, wenn eine Nachricht empfangen wird.
     * @param remoteMessage Die empfangene Nachricht.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Behandlung von Benachrichtigungs- und Daten-Nachrichten

        // Überprüft, ob die Nachricht eine Benachrichtigung enthält
        if (remoteMessage.getNotification() != null) {
            // Verarbeiten der Benachrichtigungsnachricht
            String notificationBody = remoteMessage.getNotification().getBody();
            String notificationTitle = remoteMessage.getNotification().getTitle();

            Log.d(TAG, "Notification Title: " + notificationTitle);
            Log.d(TAG, "Notification Body: " + notificationBody);

            // Zeige die Benachrichtigung in der App
            Intent intent = new Intent("MyFirebaseMessage");
            intent.putExtra("title", notificationTitle);
            intent.putExtra("body", notificationBody);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // Zeige die Systembenachrichtigung
            sendNotification(notificationTitle, notificationBody);
        }

        // Überprüft, ob die Nachricht Daten enthält
        if (remoteMessage.getData().size() > 0) {
            // Verarbeiten der Daten-Nachricht
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Extrahiert Daten aus der Nachricht
            String messageData = remoteMessage.getData().get("message");

            // Zeige die Nachricht in der App
            Intent intent = new Intent("MyFirebaseMessage");
            intent.putExtra("data", messageData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Zeigt eine Systembenachrichtigung mit dem gegebenen Titel und Text an.
     * @param title Der Titel der Benachrichtigung.
     * @param body Der Text der Benachrichtigung.
     */
    private void sendNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Kanal-ID für Benachrichtigungen
        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Erstellen des Benachrichtigungs-Builder
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Erstellen des Benachrichtigungskanals für Android Oreo und höher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Senden der Benachrichtigung
        notificationManager.notify(0 /* ID der Benachrichtigung */, notificationBuilder.build());
    }
}
