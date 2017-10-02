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
* MapsActivity HAS TO BE SINGLETOP in order to call onNewIntent and get data
*/
public class MyNotificationService extends FirebaseMessagingService
{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            showNotification(remoteMessage.getData().get("closeone"), //friends id
                    remoteMessage.getData().get("closename"));         //friends username
        }

        // Check if message contains a notification payload.
        //if (remoteMessage.getNotification() != null) {
        //}
    }

    private void showNotification(String userid, String name) {
        int friendRC = MosisApp.getInstance().getNotifRCFriend();
        Intent intent = new Intent(this, MapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("FriendID",userid);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, friendRC, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Just around the corner... ")
                .setSmallIcon(R.drawable.ic_action_hifriend)
                .setContentText("is " + name)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 100, 100, 300, 300, 300, 100, 100, 100})
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(friendRC, notificationBuilder.build());
    }
}
/*
var payload = {
            data: {
                    closeone : childKey,
                    closename : "friends id"
                    closetime : admin.database.ServerValue.TIMESTAMP.toString()
                   }
    };
*/
