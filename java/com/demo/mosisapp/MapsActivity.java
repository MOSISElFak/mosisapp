package com.demo.mosisapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Console;
import java.util.Date;

/* (Notes from Google) @SuppressWarnings("MissingPermission") try @SuppressWarnings({"ResourceType"}) or //noinspection MissingPermission
 * Note: If you're using the v7 appcompat library, your activity should instead extend AppCompatActivity, which is a subclass of FragmentActivity. (For more information, read Adding the App Bar)
 * Note: When you add a fragment to an activity layout by defining the fragment in the layout XML file, you cannot remove the fragment at runtime.
 * For starters, Fragment will be created in XML and set on creation. And AppCompatLib supports the easy action bar
 * ?idea? MainActivity can be merged with this one
 * ?idea? Later, MainActivity will load empty fragment on startup, and after login/register user display map fragment
 */
//
//public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private boolean mLocationPermissionGranted = false;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    //location access
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastKnownLocation;
    private int DEFAULT_ZOOM = 15;
    //private Location mCurrentLocation;
    private static final long UPDATE_INTERVAL = 20000; //20 seconds in milliseconds, inexact
    private static final long FASTEST_INTERVAL = 10000; //10sec, update_interval/2, exact

    private final LatLng mDefaultLocation = new LatLng(48.137154, 11.576124); //maps could have default locations based on regions

    private GoogleMap mMap;

    //RealtimeDB
    private FirebaseDatabase mFirebaseDatabase; //main access point
    private DatabaseReference refLocation;      //pointer to root node
    private ChildEventListener mChildEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // easy toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_easy);
        setSupportActionBar(toolbar);

        // Initialize RealtimeDB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refLocation = mFirebaseDatabase.getReference().child("location").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        //Your GoogleApiClient instance will automatically connect after your activity calls onStart() and disconnect after calling onStop()
        mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        /*
        * Connects the client to Google Play services. This method returns immediately, and connects to the service in the background.
        * If the connection is successful, onConnected(Bundle) is called and enqueued items are executed.
        * On a failure, onConnectionFailed(ConnectionResult) is called.
        * If the client is already connected or connecting, this method does nothing.
        */
        mGoogleApiClient.connect(); //calls onConnected when ready
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mGoogleApiClient.isConnected()) startLocationUpdates();
        } catch (SecurityException se) {
            Toast.makeText(this, "removeLocationUpdates", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {//java.lang.RuntimeException: Unable to pause activity {com.demo.mosisapp/com.demo.mosisapp.MapsActivity}: java.lang.IllegalStateException: GoogleApiClient is not connected yet.
            if (mGoogleApiClient.isConnected()) LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        } catch (SecurityException se) {
            Toast.makeText(this, "removeLocationUpdates", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
        //mGoogleApiClient.disconnect();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override @SuppressWarnings("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true); // shows realtime blue dot (me) on map
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // ze button

        mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastKnownLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,DEFAULT_ZOOM));
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));
        } else {
            LatLng me = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(me)
                    .snippet(getString(R.string.default_info_snippet))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    //.alpha(0.7f)
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case (R.id.sign_out_menu):
                //sign out
                FirebaseAuth.getInstance().signOut();
                finish();
                break;
            case (R.id.myprofile_menu):
                Intent mine = new Intent(MapsActivity.this, MyProfileActivity.class);
                startActivity(mine);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //mGoogleApiClient.disconnect(); //documentation: connect/disconnect is automatic onstart/stop????
        Intent escape = new Intent();
        setResult(RESULT_OK, escape);
        finish();
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // calls onMapReady when ready
        // Begin polling for new location updates.
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        LocationBean beanie = new LocationBean(new Date(),location.getLatitude(),location.getLongitude());
        refLocation.setValue(beanie);
        // You can now create a LatLng Object for use with maps
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException se) {
            Log.d("DEBUG", "startLocationUpdates: " + se.getMessage());
        }
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
}
