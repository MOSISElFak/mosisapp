package com.demo.mosisapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS;

public class MyLocationService extends Service
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status>
{
    private final String TAG = "MyLocationService";
    private IBinder mBinder = new MyBinder();
    protected ResultReceiver resultReceiver;

    //GOOGLE API CLIENT
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private final long UPDATE_INTERVAL = 30 * 1000;  // 30 seconds in milliseconds, inexact
    private final long FASTEST_INTERVAL = 15 * 1000; // 15 seconds, update_interval/2, exact
    private DatabaseReference refMyLocation;

    protected ArrayList<Geofence> mGeofenceList;

    protected PendingResult<LocationSettingsResult> result;   //used for checkLocationSettings
    private LocationSettingsRequest.Builder builder;

    //Control fields
    private boolean shouldNotif = true; //in case service was restarted, to send "connected!" signal to start map
    private boolean shouldUserfence;    //in case service was restarted, re-subscribe to topic
    private boolean shouldGeofence;     //in case service was restarted, re-start geofences

    private void loadFlags(){
        SharedPreferences sp = getSharedPreferences("flags",MODE_PRIVATE);
        shouldUserfence = sp.getBoolean("shouldUserfence",false);
        shouldGeofence = sp.getBoolean("shouldGeofence",false);
    }

    //<editor-fold desc="Lifecycle">
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        loadFlags();
        try {
            refMyLocation = FirebaseDatabase.getInstance().getReference().child(Constants.LOCATIONS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            if (shouldUserfence) FirebaseMessaging.getInstance().subscribeToTopic("close");
            buildGoogleApiClient();
            mGoogleApiClient.connect(); // calls onConnected when ready
        }
        catch ( NullPointerException ex ){
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        // called by activity
        if (intent != null && intent.hasExtra("receiver")) {
            resultReceiver = intent.getParcelableExtra("receiver");
            Log.d(TAG, "got receiver");

        }
        else if (intent!=null && intent.hasExtra("broadcast")) {
            Log.d(TAG, "from broadcast");
            if (resultReceiver!=null){  // UI is connected
                startLocationUpdates(); // this will call load geofences
            }
        }
        else { // it was auto-reset
            shouldNotif = true;
            Log.d(TAG, "NO receiver");
        }

        return Service.START_STICKY; // Doesn't work on KitKat4.4 or AOTP 5.1
    }

    @Override // Will be bound only by activity
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        if (shouldNotif && mGoogleApiClient.isConnected()){
            Bundle location = new Bundle();
            LatLng mLastKnownLocation = getLastLocation();
            if (mLastKnownLocation!=null){
                String msg = "Last Known Location: " +
                        Double.toString(mLastKnownLocation.latitude) + "," +
                        Double.toString(mLastKnownLocation.longitude);
                Log.d(TAG, msg);
                location.putParcelable("loc",mLastKnownLocation);
            }
            resultReceiver.send(100,location);
            shouldNotif = false;
        }
        return mBinder;
    }

    class MyBinder extends Binder {

        GoogleApiClient getGAC() {
            return mGoogleApiClient;
        }
        void startGeofencing() { activateGeofences(); }
        void stopGeofencing() { deactivateGeofences(); }
        ArrayList<Geofence> getGeofenceList() {
            if (mGeofenceList==null){
                mGeofenceList = new ArrayList<>();
            }
            return mGeofenceList;
        }
        LatLng getLastKnownLocation(){ return getLastLocation(); }
    }

    @Override
    public boolean onUnbind(Intent intent) { //user unbinds on Activity pause
        Log.d(TAG, "onUnbind");
        resultReceiver = null;
        shouldNotif = true;     //reset notification for next connection
        return false; //for each next binding same object of the service will be returned because I bind only once (true -> onRebind)
    }

    /**
     * This is called if the service is currently running and the user has
     * removed a task that comes from the service's application.
     * (swiped away from list of recent applications)
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        //check if this is android kit kat 4.4.2 and set up restart of service
        //https://issuetracker.google.com/issues/36986292
        //https://issuetracker.google.com/issues/36986118
        //also on 5.1 Alcatel OneTouchPop
        if (android.os.Build.VERSION.RELEASE.startsWith("4.4") || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            //setup restart of service
            cleanup();

            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());

            PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE); //41521206
            alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000,//1second
                    restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        cleanup();
        super.onDestroy();
    }

    //</editor-fold>

    //<editor-fold desc="Location">

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected success!");

        if(resultReceiver!=null && shouldNotif) {// if UI is connected
            Bundle location = new Bundle();
            LatLng mLastKnownLocation = getLastLocation();
            if (mLastKnownLocation!=null){
                String msg = "Last Known Location: " +
                        Double.toString(mLastKnownLocation.latitude) + "," +
                        Double.toString(mLastKnownLocation.longitude);
                Log.d(TAG, msg);
                location.putParcelable(Constants.RECEIVER_NEW_LOCATION,mLastKnownLocation);
            }
            resultReceiver.send(100,location);
            shouldNotif = false;
        }
        // Begin polling for new location updates.
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        if(resultReceiver!=null) {
            resultReceiver.send(101, null);

            if (i == CAUSE_SERVICE_DISCONNECTED) {
                Toast.makeText(this, "Disconnected. Reconnecting...", Toast.LENGTH_SHORT).show();
            } else if (i == CAUSE_NETWORK_LOST) {
                Toast.makeText(this, "Network lost. Reconnecting...", Toast.LENGTH_SHORT).show();
            }
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");
        if(resultReceiver!=null) {
            resultReceiver.send(102, null);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(MyLocationService.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    protected LatLng getLastLocation(){
        Log.d(TAG,"getLastLocation");
        //here you can get last known location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher{
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                Log.e(TAG, "getLastLocation: permission problem");
            } else {
                Location mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastKnownLocation != null) {
                    return new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                }
            }
        } else {
            Location mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastKnownLocation != null) {
                return new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            }
        }
        return null;
    }

    protected void startLocationUpdates() {
        Log.d(TAG,"startLocationUpdates");
        // Create the location request
        if (mLocationRequest==null) {
            Log.d(TAG,"new mLocationRequest");
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    //.setSmallestDisplacement(0.1f)      //  Set the minimum displacement between location updates in meters (tryout)
                    .setInterval(UPDATE_INTERVAL)       // GoogleDoc: 5 seconds would be appropriate for realtime
                    .setFastestInterval(FASTEST_INTERVAL);
        }
        if (builder==null) {
            Log.d(TAG,"new builder");
            builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest)
                    .setAlwaysShow(true); //should show up whenever location settings are not appropriate (AOTP nope)
        }

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>()
        {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationResult) {
                Log.d(TAG, "result: onResult");
                final Status status = locationResult.getStatus();
                //LocationSettingsStates state = locationResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here.
                        // Request location updates
                        try {
                            Log.d(TAG, "result: onResult LocationSettingsStatusCodes.SUCCESS");
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MyLocationService.this); //calls onLocationChanged
                            if (shouldGeofence) loadSavedGeofences(); // if location was off/on we need to re-register geofences
                        } catch (SecurityException se) {
                            Log.e(TAG, "startLocationUpdates: " + se.getMessage());
                            se.printStackTrace();
                            Log.e(TAG, "Maybe missing permission?");
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d(TAG, "result: onResult LocationSettingsStatusCodes.RESOLUTION_REQUIRED");
                        // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                        // UserDialog can only be called from activity, so we notify activity
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().

                        if (resultReceiver!=null){
                            Bundle b = new Bundle();
                            b.putParcelable("resolution",status);
                            resultReceiver.send(106,b);
                            Log.d(TAG,"sent forResolution");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                        Log.e(TAG, "result: onResult LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE");
                        Log.e(TAG,"Probably dismissed with DON'T NOTIFY AGAIN");
                        break;
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TAG, msg);
        // Update DB with new location
        LocationBean beanie = new LocationBean(location.getLatitude(), location.getLongitude());
        refMyLocation.child(Constants.COORDINATES).setValue(beanie);
        //Notify UI about new location
        if (resultReceiver!=null) {
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            Bundle b = new Bundle();
            b.putParcelable(Constants.RECEIVER_NEW_LOCATION,latLng);
            resultReceiver.send(103,b);
        }
    }

    private void cleanup() {
        // after this, the service is destroyed one way or another
        if (mGoogleApiClient == null) return;

        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) { // this is extra, geofences are removed on disconnect
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
            mGoogleApiClient.disconnect();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Geofencing">

    private GeofencingRequest getGeofencingRequest() {
        Log.d(TAG, "getGeofencingRequest");
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "getGeofencePendingIntent");
        Intent intent = new Intent(this, GeofenceIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences()
        return PendingIntent.getService(this,Constants.GEOFENCE_REQ_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void activateGeofences() {
        Log.d(TAG, "activateGeofences");

        if (!mGoogleApiClient.isConnected()) {
           Log.d(TAG, getString(R.string.not_connected));
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            Log.e(TAG, securityException.getMessage());
        }
    }

    @Override   // called on LocationServices.GeofencingApi.addGeofences
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if (status.isSuccess()) {
            Log.d(TAG, "Geofences added!");
            if (resultReceiver!=null){
                resultReceiver.send(104,null);
            }
        } else {
            String errorMessage;
            switch (status.getStatusCode()){
                case GEOFENCE_NOT_AVAILABLE:
                    errorMessage = getResources().getString(R.string.geo_not_available);break;
                case GEOFENCE_TOO_MANY_GEOFENCES:
                    errorMessage = getResources().getString(R.string.geo_too_many);break;
                case GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    errorMessage = getResources().getString(R.string.geo_too_many_pending);break;
                default:
                    errorMessage = getResources().getString(R.string.geo_unknown_error);
            }
            if (resultReceiver!=null){
                Bundle error = new Bundle();
                error.putString("error",errorMessage);
                resultReceiver.send(104,error);
            }
            Log.e(TAG,errorMessage);
        }
    }

    private void deleteSavedGeofences() {
        Log.d(TAG, "deleteSavedGeofences");

        SharedPreferences data = getSharedPreferences("localservice", Activity.MODE_PRIVATE);
        if (data.contains("fences")){
            Log.d(TAG, "deleteSavedGeofences: fences found");
            data.edit().clear().commit(); // it has to be commit
            getSharedPreferences("flags",MODE_PRIVATE).edit().putBoolean("shouldGeofence",false).commit();
            if (resultReceiver!=null){
                resultReceiver.send(105,null);
            }
        }
        else {
            Log.d(TAG, "deleteSavedGeofences: No fences found saved");
        }
    }

    /**
     * Reads shared preferences "localservice" for saved "fences"
     * Unpacks "fences" to mGeofenceList and activates them
     *
     * param mGeofenceList : fills for geofencing
     */
    private void loadSavedGeofences() {
        Log.d(TAG, "loadSavedGeofences");

        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG,getString(R.string.not_connected));
            return;
        }

        SharedPreferences data = getSharedPreferences("localservice", Activity.MODE_PRIVATE);
        if (data.contains("fences")) {
            Log.d(TAG, "loadGeofences: fences found");
            String json = data.getString("fences", null);
            if (json==null){
                Log.e(TAG, "Unable to read fences data. json = null");
                deleteSavedGeofences(); //cleanup
                return;
            }
            HashMap<String,LatLng> toshow = new Gson().fromJson(json, new TypeToken<HashMap<String, LatLng>>(){}.getType());
            Log.d(TAG,"JSON:");
            Log.d(TAG,json);
            if (toshow.isEmpty()){
                Log.e(TAG, "Unable to read fences data. hash is empty");
                deleteSavedGeofences(); //cleanup
                return;
            }
            if (mGeofenceList == null) {
                mGeofenceList = new ArrayList<>();
            }
            for (Map.Entry<String, LatLng> entry : toshow.entrySet()) {
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(entry.getKey())
                        .setCircularRegion(entry.getValue().latitude, entry.getValue().longitude, Constants.GEOFENCE_RADIUS)
                        .setExpirationDuration(Constants.GEO_DURATION)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build());
            }
            Log.d(TAG, "loadGeofences: mGeofenceList loaded with " + mGeofenceList.size());
            activateGeofences();
        }
        else {
            Log.d(TAG, "loadGeofences: No fences found saved");
        }
    }

    /**
     * Stops geofencing if mGAC is connected
     * Removes saved geofences so that they don't get reactivated on service re-start
     *
     * param mGeofenceList : used for building deactivation list
     */
    private void deactivateGeofences() {
        Log.d(TAG, "deactivateGeofences");
        deleteSavedGeofences();

        if (mGoogleApiClient.isConnected() && mGeofenceList!=null && !mGeofenceList.isEmpty())
        {
            try {
                ArrayList<String> geoIds = new ArrayList<>(); // has to be a list
                for (Geofence geo:mGeofenceList) {
                    geoIds.add(geo.getRequestId());
                }
                LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geoIds)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Geofences deactivated successfully");
                                    mGeofenceList.clear();
                                }
                                else
                                    Log.e(TAG,status.getStatusMessage());
                            }
                        });
            } catch (SecurityException securityException) {
                Log.e(TAG,securityException.getMessage());
            }
        }
    }

    //</editor-fold>
}

//CODES
// 100 - connected : [null | last known location]
// 101 - suspended, reconnecting
// 102 - failed
// 103 - on location changed : LatLng
// 104 - geofences active
// 105 - signal for switch_geofences = false when geofences deleted

// Multiple clients can connect to the service at once.
// However, the system calls your service's onBind() method to retrieve the IBinder only when the first client binds.
// The system then delivers the same IBinder to any additional clients that bind, without calling onBind() again.
