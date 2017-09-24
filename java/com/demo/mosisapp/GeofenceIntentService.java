package com.demo.mosisapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import static com.google.android.gms.location.GeofenceStatusCodes.getStatusCodeString;

// Note: there is no way to get location of geofence from it
// Note: place type is embedded in requestId
public class GeofenceIntentService extends IntentService
{
    private static final String TAG = "GeofenceIntentService";

    public GeofenceIntentService() {
        super(TAG); // use TAG to name the IntentService worker thread
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
        int geofenceTransition = geofencingEvent.getGeofenceTransition(); //geofenceTransition = 0

        // Check if the transition type
        if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER )
        {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for ( Geofence geofence : triggeringGeofences ) {
                sendNotification( geofence.getRequestId() );
            }
        }
        else {
            Log.e(TAG,getString(R.string.geo_unknown_transition) + geofenceTransition);
        }
    }

    private void sendNotification( String msg ) {
        Log.d(TAG, "sendNotification: " + msg );

        String place[] = msg.split("!"); //should give "Drinking Fountain" and "-K9aB..."

        // Intent to start the main Activity
        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
        notificationIntent.putExtra("GeofenceID",place[1]);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, Constants.GEOFENCE_REQ_CODE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        notificationBuilder.setSmallIcon(R.drawable.ic_action_favourite)
                            //.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            //        R.drawable.ic_action_place_add))
                            .setContentTitle("Place close")
                            .setContentText("Near: "+place[0])
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                            .setAutoCancel(true);// Dismiss notification once the user touches it.

        // Creating and sending Notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        mNotificationManager.notify(MosisApp.getInstance().getNotifRC(),notificationBuilder.build());
    }
}