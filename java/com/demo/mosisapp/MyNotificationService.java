package com.demo.mosisapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/*
* For receiving and displaying notifications sent from Cloud Messaging
*/
public class MyNotificationService extends FirebaseMessagingService
{

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            showNotification(remoteMessage.getData().get("closeone"), remoteMessage.getData().get("closetime"));
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
        }
    }

    private void showNotification(String userid, String time) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("NotificationMessage",userid);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 17 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Just around the corner... " + time)
                .setSmallIcon(R.mipmap.ic_launcher_person)
                .setContentText("is " + userid)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(17 /* ID of notification */, notificationBuilder.build());
    }
}
/*
var payload = {
            data: {
                    closeone : childKey,
                    closetime : admin.database.ServerValue.TIMESTAMP.toString()
                   }
    };
*/
