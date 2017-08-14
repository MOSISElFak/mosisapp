package com.demo.mosisapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
//import static com.bumptech.glide.request.RequestOptions.fitCenterTransform;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
    private static final long UPDATE_INTERVAL = 20000;  //20 seconds in milliseconds, inexact
    private static final long FASTEST_INTERVAL = 10000; //10sec, update_interval/2, exact

    private final LatLng mDefaultLocation = new LatLng(48.137154, 11.576124); //maps could have default locations based on regions

    private GoogleMap mMap;
    private Marker myMarker;

    //RealtimeDB
    private FirebaseDatabase mFirebaseDatabase;         //main access point
    private DatabaseReference refMyLocation;
    private DatabaseReference refLocation;
    private DatabaseReference refMyFriends;

    // Friends and listeners
    private ChildEventListener mChildEventListenerFriends;
    private ChildEventListener mChildEventListenerLocations;
    private ValueEventListener mValueEventListener;
    private List<String> friends;

    // Markers
    private HashMap<Marker, String> hash_marker_id;       // for identifying marker when clicked from map
    private WeakHashMap<String, Marker> hash_id_marker;   // for identifying which marker to update with new location (weak, because it only keeps references)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_easy);
        setSupportActionBar(toolbar);

        // Initializations
        friends = new ArrayList<>();
        hash_marker_id = new HashMap<>();
        hash_id_marker = new WeakHashMap<>();

        // Initialize RealtimeDB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refMyLocation = mFirebaseDatabase.getReference().child(Constants.LOCATIONS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        refLocation = mFirebaseDatabase.getReference().child(Constants.LOCATIONS);
        refMyFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        //Your GoogleApiClient instance will automatically connect after your activity calls onStart() and disconnect after calling onStop()
        mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        /**
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
        detachDatabaseReadListener();
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

        //mMap.setMyLocationEnabled(true); // shows realtime blue dot (me) on map
        //mMap.getUiSettings().setMyLocationButtonEnabled(true); // ze button

        mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastKnownLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,DEFAULT_ZOOM));
            myMarker = mMap.addMarker(new MarkerOptions()
                        .title(getString(R.string.default_info_title))
                        .position(mDefaultLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .snippet(getString(R.string.default_info_snippet)));
        } else {
            LatLng me = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            myMarker = mMap.addMarker(new MarkerOptions()
                        .title(getString(R.string.default_info_title))
                        .position(me)
                        .snippet(getString(R.string.default_info_snippet))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        //.alpha(0.7f)
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d("MAP ready", "onMarkerClick");
                //Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
                String extra = hash_marker_id.get(marker);
                //intent.putExtra("key_id", extra);
                //startActivity(intent);
                return true;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
        {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Log.d("MAP ready: ","onInfoWindowClick");
            }
        });

        attachListeners();
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListenerFriends!=null) {
            //for (int i=0; i<friends.size(); i++)
            //    refLocation.child(friends.get(i)).removeEventListener(mValueEventListener);
            refMyFriends.removeEventListener(mChildEventListenerFriends);
            //mChildEventListenerFriends = null;
        }
    }

    private void attachListeners()
    {
        // IT HAS TO BE CHILD_LISTENER TO DIFFERENTIATE BETWEEN ADDED AND CHANGED
        if (mChildEventListenerLocations==null) { //for this to work, it has to have an additional node <mine>
            mChildEventListenerLocations = new ChildEventListener()
            {
                String url;
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildAdded",dataSnapshot.getKey());
                    //KEY(mine):VALUE(locationbean.class)
                    if (dataSnapshot.getKey().equalsIgnoreCase("photourl")) //you got an image
                    {
                        url = dataSnapshot.getValue(String.class);
                        Log.d("locate: onChildAdded",url);
                    }
                    else
                    {
                        LocationBean bean = dataSnapshot.getValue(LocationBean.class);
                        String id = dataSnapshot.getRef().getParent().getKey(); //UID

                        MarkerOptions mo = new MarkerOptions();
                        mo.position(bean.getCoordinates());
                        mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_about));
                        Marker marker = mMap.addMarker(mo);
                        hash_marker_id.put(marker,id);
                        hash_id_marker.put(id,marker);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildChanged",dataSnapshot.getKey());
                    //KEY(mine):VALUE(locationbean.class)
                    LocationBean bean = dataSnapshot.getValue(LocationBean.class);
                    String id = dataSnapshot.getRef().getParent().getKey();
                    Marker marker = hash_id_marker.get(id);
                    marker.setPosition(bean.getCoordinates());
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d("locate: onChildRemoved",dataSnapshot.getKey());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildMoved",dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("locate: ERROR", databaseError.getMessage());
                }
            };
        }

        if (mChildEventListenerFriends == null) { // looks at friends
            mChildEventListenerFriends = new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) { //when a friend entry is found, attach a listener to it
                    Log.d("friends: onChildAdded",dataSnapshot.getKey());
                    // KEY(uid):VALUE(true|false)
                    final String id = dataSnapshot.getKey(); //gets friends uid and
                    friends.add(id);
                    // maybe cache images here
                    refLocation.child(id).addChildEventListener(mChildEventListenerLocations);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Log.d("friends: onChildChanged",dataSnapshot.getKey());
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d("friends: onChildRemoved",dataSnapshot.getKey());
                    String id = dataSnapshot.getKey();
                    Marker marker = hash_id_marker.get(id);
                    hash_id_marker.remove(id); //first remove from weak map with references
                    hash_marker_id.remove(marker);
                    refLocation.child(id).removeEventListener(mChildEventListenerLocations);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Log.d("friends: onChildMoved",dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("friends: ERROR", databaseError.getMessage());
                }
            };
            refMyFriends.addChildEventListener(mChildEventListenerFriends);
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
        myMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        LocationBean beanie = new LocationBean(location.getLatitude(),location.getLongitude());
        //LocationBean beanie = new LocationBean(ServerValue.TIMESTAMP, location.getLatitude(),location.getLongitude());
        refMyLocation.setValue(beanie);
        // You can now create a LatLng Object for use with maps
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        myMarker.setPosition(latLng);

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
            //requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, locationListener);
        } catch (SecurityException se) {
            Log.d("DEBUG", "startLocationUpdates: " + se.getMessage());
        }
    }

    public void checkLocationPermission() { //this is nescessary, should be checked on app(main activity) start
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
}
