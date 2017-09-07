package com.demo.mosisapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS;
import static com.google.android.gms.location.GeofenceStatusCodes.getStatusCodeString;

public class GeofenceIntentService extends IntentService
{
    private static final String TAG = "GeofenceIntentService";
    public static final int GEOFENCE_NOTIFICATION_ID = 16;

    public GeofenceIntentService() {
        super(TAG); // use TAG to name the IntentService worker thread
        Log.d(TAG, "constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);   //you always get data from intent based on what the service was for
        if (geofencingEvent.hasError()) {
            String errorMessage = getStatusCodeString(geofencingEvent.getErrorCode()); //https://developers.google.com/android/reference/com/google/android/gms/location/GeofencingEvent
            Log.e(TAG, errorMessage);
            return;
        }

        // Retrieve GeofenceTrasition
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check if the transition type
        if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT )
        {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Create a detail message with Geofences received
            String geofenceTransitionDetails = getGeofenceTrasitionDetails(geofenceTransition, triggeringGeofences);

            // Send notification details as a String
            sendNotification( geofenceTransitionDetails );
        }
        else {
            Log.e(TAG,getString(R.string.geo_unknown_transition) + geofenceTransition);
        }
    }

    // this should be in a separate static class so that it will be available from other Activities
    private static String getErrorString(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GEOFENCE_NOT_AVAILABLE:
                return mResources.getString(R.string.geo_not_available);
            case GEOFENCE_TOO_MANY_GEOFENCES:
                return mResources.getString(R.string.geo_too_many);
            case GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return mResources.getString(R.string.geo_too_many_pending);
            default:
                return mResources.getString(R.string.geo_unknown_error);
        }
    }

    // Create a detail message with Geofences received
    private String getGeofenceTrasitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences)
    {
        // get the ID of each geofence triggered; IDs are unique keys associated with each geofence (push on db)
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();

        for ( Geofence geofence : triggeringGeofences ) {
            triggeringGeofencesList.add( geofence.getRequestId() );
        }

        String geofenceStatus = getTransitionString(geofenceTransition);

        return geofenceStatus + ": " + TextUtils.join( ", ", triggeringGeofencesList);
    }

    private String getTransitionString(int geofenceTransition) {
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geo_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geo_transition_exited);
            default:
                return getString(R.string.geo_unknown_transition);
        }
    }

    private void sendNotification( String msg ) {
        Log.d(TAG, "sendNotification: " + msg );
//https://stackoverflow.com/a/16919410
        // Intent to start the main Activity
        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
        //notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |Intent.FLAG_ACTIVITY_NEW_TASK);
/*
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);
        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);
        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(GEOFENCE_NOTIFICATION_ID, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
*/

        PendingIntent pendingIntent = PendingIntent.getActivity(this, GEOFENCE_NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        notificationBuilder.setSmallIcon(R.drawable.ic_action_important)
                            // In a real app, you may want to use a library like Volley
                            // to decode the Bitmap.
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.drawable.ic_action_place_add))
                            .setContentTitle(msg)
                            .setContentText("Geofence Notification!")
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                            .setAutoCancel(true);// Dismiss notification once the user touches it.

        // Creating and sending Notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        mNotificationManager.notify(GEOFENCE_NOTIFICATION_ID,notificationBuilder.build());
    }
}

/*
* you could also sent out broadcast instead of notification
* Intent localIntent = new Intent("myPackageName.BROADCAST_ACTION");
* localIntent.putExtra(Tag, blabablaba);
* LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
*
* and then you handle it in a class that extends BroadcastReceiver
* works best as a nested class on MainActivity
* */

/*
 * If you need to communicate with UI thread add mHandler
@Override
public void onCreate() {
    super.onCreate();
    mHandler = new Handler();!!!
}

@Override
protected void onHandleIntent(Intent intent) {
    mHandler.post(new Runnable() {    !!!!
        @Override
        public void run() {
            Toast.makeText(MyIntentService.this, "Hello Toast!", Toast.LENGTH_LONG).show();
        }
    });
}
 */