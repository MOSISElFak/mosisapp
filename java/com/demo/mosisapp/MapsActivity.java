package com.demo.mosisapp;

import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES;
import static com.google.android.gms.location.GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS;

/* (Notes from Google) @SuppressWarnings("MissingPermission") try @SuppressWarnings({"ResourceType"}) or //noinspection MissingPermission
 * Note: If you're using the v7 appcompat library, your activity should instead extend AppCompatActivity, which is a subclass of FragmentActivity. (For more information, read Adding the App Bar)
 * Note: When you add a fragment to an activity layout by defining the fragment in the layout XML file, you cannot remove the fragment at runtime.
 * For starters, Fragment will be created in XML and set on creation. And AppCompatLib supports the easy action bar
 * ?idea? MainActivity can be merged with this one
 * ?idea? Later, MainActivity will load empty fragment on startup, and after login/register user display map fragment
 */
//setPersistanceEnabled(true) only caches data when there is a Listener attached to that node (when the data has been read at least once).
//keepSynced(true) caches everything from that node, even if there is no listener attached.
//public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , LocationListener
        , ResultCallback<Status>
        ,GoogleMap.OnMapLongClickListener
{
    private final String TAG = "MapsActivity";

    private boolean isGeoActive = false;
    private boolean mLocationUpdated = false; //flag for add places, as to get location
    private boolean mLocationPermissionGranted = true;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION= 1;
    //location access
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastKnownLocation;
    private int DEFAULT_ZOOM = 15;
    private LatLng thisMe;
    //private Location mCurrentLocation;
    private static final long UPDATE_INTERVAL = 20 * 1000;  //20 seconds in milliseconds, inexact //3min?
    private static final long FASTEST_INTERVAL = 10 * 1000; //10sec, update_interval/2, exact     //30sec?

    private final LatLng mDefaultLocation = new LatLng(48.137154, 11.576124); //maps could have default locations based on regions
    private LatLngBounds MUNICH = new LatLngBounds(new LatLng(48.047983,11.363986),new LatLng(48.249102,11.756060));

    private GoogleMap mMap;
    private Marker myMarker;

    //RealtimeDB
    private FirebaseDatabase mFirebaseDatabase;         //main access point
    private DatabaseReference refMyLocation;
    private DatabaseReference refLocation;
    private DatabaseReference refMyFriends;
    private DatabaseReference refUsers;
    private DatabaseReference refFilterResults;

    // Friends and listeners
    private ChildEventListener mChildEventListenerFriends;
    private ChildEventListener mChildEventListenerLocations;
    private ValueEventListener mValueEventListenerFilters;
    private ChildEventListener mChildEventListenerALLUsers;

    private List<String> friends;

    RequestOptions requestOptions;

    // Markers
    private HashMap<Marker, String> hash_marker_id;       // for identifying marker when clicked from map
    private WeakHashMap<String, Marker> hashWeak_id_marker;   // for identifying which marker to update with new location (weak, because it only keeps references)

    // Geofences
    private static final long GEO_DURATION = 2 * 60 * 1000; //in miliseconds
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final int GEOFENCE_REQ_CODE = 16;
    private static final float GEOFENCE_RADIUS = 100.0f; // in meters

    // UDACITY KURS
    protected ArrayList<Geofence> mGeofenceList;

    //Places
    private DatabaseReference refPlaces;
    private HashMap<String, Marker> hash_place_markers;     //pushKey => Marker
    private HashMap<String, LatLng> hash_active_geofences;  //geoKey => position
    private HashMap<String, Circle> hash_circles;           //geoKey => Circle
    private int GEOFENCES_LIMIT = 50;                       //safeguard
    FloatingActionButton fab;

    private PendingIntent geoFencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_easy);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.filter_layout, null))
                        //Alternatively, you can specify a list using setAdapter(). This allows you to back the list with dynamic data (such as from a database) using a ListAdapter
                        .setItems(R.array.add_type_spinner_array, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int index) {
                                String type = getResources().getStringArray(R.array.add_type_spinner_array)[index];
                                Toast.makeText(MapsActivity.this, "Type: "+type, Toast.LENGTH_SHORT).show();
                                searchMapTypeClient(index, type);
                           }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            }
        });

        // Initializations
        friends = new ArrayList<>();
        hash_marker_id = new HashMap<>();
        hashWeak_id_marker = new WeakHashMap<>();
        hash_place_markers = new HashMap<>();
        hash_active_geofences = new HashMap<>();
        hash_circles = new HashMap<>();

        // Initialize RealtimeDB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refMyLocation = mFirebaseDatabase.getReference().child(Constants.LOCATIONS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        refLocation = mFirebaseDatabase.getReference().child(Constants.LOCATIONS);
        refMyFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        refUsers = mFirebaseDatabase.getReference().child(Constants.USERS);
        refPlaces = mFirebaseDatabase.getReference().child(Constants.READPLACE);

        refFilterResults = FirebaseDatabase.getInstance().getReference().child("filter/results").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        //Your GoogleApiClient instance will automatically connect after your activity calls onStart() and disconnect after calling onStop()
        //mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
        //        .addApi(LocationServices.API)
        //        .addConnectionCallbacks(this)
        //        .addOnConnectionFailedListener(this)
        //        .build();
        /**
         * Connects the client to Google Play services. This method returns immediately, and connects to the service in the background.
         * If the connection is successful, onConnected(Bundle) is called and enqueued items are executed.
         * On a failure, onConnectionFailed(ConnectionResult) is called.
         * If the client is already connected or connecting, this method does nothing.
         */
        //mGoogleApiClient.connect(); //calls onConnected when ready
        buildGoogleApiClient();

        requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)                   //DATA(original),RESOURCE(after transformations)
                .placeholder(R.drawable.ic_action_marker_person_color)      //default icon before loading
                .fallback(R.drawable.ic_action_marker_person_color)         //default icon in case of null
                //.fallback(new ColorDrawable(Color.GRAY));
                .priority(Priority.HIGH)
                .circleCrop()
                .override(50, 50);

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Get the geofences used. Geofence data is hard coded in this sample.
        //populateGeofenceList();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Connect the client
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect(); //calls onConnected when ready
        }
    }

    @Override
    protected void onStop() {
        //Disconnect the client
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
           // if (mGoogleApiClient.isConnected()) startLocationUpdates();
        } catch (SecurityException se) {
            Toast.makeText(this, "removeLocationUpdates", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {//java.lang.RuntimeException: Unable to pause activity {com.demo.mosisapp/com.demo.mosisapp.MapsActivity}: java.lang.IllegalStateException: GoogleApiClient is not connected yet.
            if (mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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
    @Override
    @SuppressWarnings("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);//HYBRID =satelite+terrain, TERRAIN =roads
        //googleMap.getUiSettings().setMapToolbarEnabled(false); //for not displaying bottom toolbar

        try {
            // Customise the styling of the base map using a JSON object defined in a raw resource file.
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.silver_map_with_icons));

            if (!success) {
                Log.e("OnMapReady", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("OnMapReady", "Can't find style. Error: ", e);
        }

        //findViewById(R.id.map).setVisibility(View.VISIBLE);
        mMap.setMyLocationEnabled(true); // shows realtime blue dot (me) on map
        //mMap.getUiSettings().setMyLocationButtonEnabled(true); // ze button
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMaxZoomPreference(20);
        mMap.setMinZoomPreference(14);
        mMap.setLatLngBoundsForCameraTarget(MUNICH);

        //if (mLocationPermissionGranted) {
        mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastKnownLocation == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            myMarker = mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .snippet(getString(R.string.default_info_snippet)));
        } else {
            LatLng me = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            thisMe = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            myMarker = mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(me)
                    .snippet(getString(R.string.default_info_snippet))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            //.alpha(0.7f)
        }
        //} else askpermission();
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "onMarkerClick");
                if (hash_marker_id.containsValue(marker)){
                    Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
                    String extra = hash_marker_id.get(marker);
                    intent.putExtra("key_id", extra);
                    startActivity(intent);
                }
                else if (hash_place_markers.containsValue(marker.getId())){ //TODO does marker->place or place->marker
                    hash_place_markers.get(marker.getId()).showInfoWindow();
                }
                marker.showInfoWindow();
                return true;
            }
        });

        //mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
        //{
        //    @Override
        //    public void onInfoWindowClick(Marker marker) {
        //        Log.d("MAP ready: ", "onInfoWindowClick");
        //    }
        //});

        mMap.setOnMapLongClickListener(this);

        //attachListeners();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //Location well = new Location("");
        //well.setLatitude(latLng.latitude);
        //well.setLongitude(latLng.longitude);
        //onLocationChanged(well);
        //float distanceInMeters =  targetLocation.distanceTo(myLocation);
        Toast.makeText(this, latLng.latitude + ": "+latLng.longitude, Toast.LENGTH_SHORT).show();
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListenerFriends != null) {
            //for (int i=0; i<friends.size(); i++)
            //     refLocation.child(friends.get(i)).removeEventListener(mValueEventListener);
            refMyFriends.removeEventListener(mChildEventListenerFriends);
            //mChildEventListenerFriends = null;
        }

        if (mValueEventListenerFilters != null){
            refFilterResults.removeEventListener(mValueEventListenerFilters);
            mValueEventListenerFilters = null;
        }
    }

    private void attachListeners() {
        // IT HAS TO BE CHILD_LISTENER TO DIFFERENTIATE BETWEEN ADDED AND CHANGED
        if (mChildEventListenerLocations == null) { //for this to work, it has to have an additional node <mine>
            mChildEventListenerLocations = new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildAdded", dataSnapshot.getKey());
                    //KEY(coordinates):VALUE(locationbean.class)

                    final LocationBean bean = dataSnapshot.getValue(LocationBean.class);
                    final String id = dataSnapshot.getRef().getParent().getKey(); //UID

                    // You can't change the icon once you've created the marker!
                    // Alternative: add marker, separately download image, find marker from hashmap, create new marker with image and copy data from old marker, and then replace it with the new one.
                    // Procedure for deleting marker: find mMarker, mMap.setMap(null), delete mMarker
                    refUsers.child(id).child("photoUrl")
                            .addListenerForSingleValueEvent(new ValueEventListener()
                                                            {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    Log.d("loadImageMarker", dataSnapshot.getValue(String.class));
                                                                    Glide.with(MapsActivity.this)
                                                                            .asBitmap()
                                                                            .load(dataSnapshot.getValue(String.class))
                                                                            .apply(requestOptions)
                                                                            .into(new SimpleTarget<Bitmap>()
                                                                            {
                                                                                @Override
                                                                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                                                                    MarkerOptions mo = new MarkerOptions();
                                                                                    mo.position(bean.makeCoordinates());
                                                                                    mo.icon(BitmapDescriptorFactory.fromBitmap(resource));
                                                                                    Marker marker = mMap.addMarker(mo); //has to be like this because "You can't change the icon once you've created the marker."
                                                                                    hash_marker_id.put(marker, id);
                                                                                    hashWeak_id_marker.put(id, marker);
                                                                                    Log.d("onResourceReady: ", id);
                                                                                }
                                                                            });
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {
                                                                    Log.d("ERROR: loadImageMarker", databaseError.getMessage());
                                                                }
                                                            }
                            );
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildChanged", dataSnapshot.getKey());
                    //KEY(coordinates):VALUE(locationbean.class)
                    LocationBean bean = dataSnapshot.getValue(LocationBean.class);
                    String id = dataSnapshot.getRef().getParent().getKey();
                    Marker marker = hashWeak_id_marker.get(id);
                    marker.setPosition(bean.makeCoordinates());
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d("locate: onChildRemoved", dataSnapshot.getKey());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Log.d("locate: onChildMoved", dataSnapshot.getKey());
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
                    Log.d("friends: onChildAdded", dataSnapshot.getKey());
                    // KEY(uid):VALUE(true|false)
                    final String id = dataSnapshot.getKey(); //gets friends uid and
                    friends.add(id);
                    // maybe cache images here
                    refLocation.child(id).addChildEventListener(mChildEventListenerLocations);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Log.d("friends: onChildChanged", dataSnapshot.getKey());
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d("friends: onChildRemoved", dataSnapshot.getKey());
                    String id = dataSnapshot.getKey();
                    Marker marker = hashWeak_id_marker.get(id);
                    hashWeak_id_marker.remove(id); //first remove from weak map with references
                    hash_marker_id.remove(marker);
                    refLocation.child(id).removeEventListener(mChildEventListenerLocations);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Log.d("friends: onChildMoved", dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("friends: ERROR", databaseError.getMessage());
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
            case (R.id.myprofile_menu):
                Intent mine = new Intent(MapsActivity.this, MyProfileActivity.class);
                startActivity(mine);
                break;
            case (R.id.addplace_menu):
                if (thisMe != null) {
                    Intent addPlace = new Intent(this, PlaceAddActivity.class);
                    addPlace.putExtra("place_lat", thisMe.latitude);
                    addPlace.putExtra("place_lon", thisMe.longitude);
                    startActivity(addPlace);
                } else
                    Toast.makeText(this, "PlaceAdd not ready", Toast.LENGTH_SHORT).show();
                return true;
            case (R.id.menu_start_geofences):
                //startGeofence();
                //activateGeofences();
                return true;
            case (R.id.menu_hide_geofences):
                //eraseGeofenceCircles(true);
                return true;
            case (R.id.menu_delete_geofences):
                //deactivateGeofences();
                return true;
            case (R.id.menu_show_all_places):
                showAllUsers();
                return true; // absorbed event
            case (R.id.menu_remove_all_places):
                removeAllUsers();
                return true;
            case (R.id.menu_search_date):
                //searchDate();
                return true;
            case (R.id.menu_search_server):
                //searchMapRadiusServer(100,"type","Recycle");
                return true;
            case (R.id.sign_out_menu):
                FirebaseAuth.getInstance().signOut();
                finish();
        }

        return super.onOptionsItemSelected(item);
    }

    //TODO make this dynamic when loaded from db
    private BitmapDescriptor decideIcon(String type) {
        //String[] array = getResources().getStringArray(R.array.add_type_spinner_array);
        switch(type){
            case("Recycle"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_recycle_green_edited);
            case("Drinking fountain"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_heart_greenedit);
            case("Electric car"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_car_greenedit);
            default:
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_recycle_green_edited);
        }
    }

    // if expiration time for places is short, maybe use ChildEventListener?
    //TODO for some reason, you are not getting the last added place, add it manually when return from addplace?
    // error is because of the way cache is saved
    private void showAllPlaces()
    {
        if (!hash_place_markers.isEmpty()) {
            Log.d(TAG, "showAllPlaces: Your places are already loaded");
            return;
        }
        refPlaces.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap: dataSnapshot.getChildren()) {
                    String key = snap.getKey();//should be the unique key
                    PlaceBean place = snap.getValue(PlaceBean.class);
                    if (place == null) return; //skip, this happens when place is new and is not in cache yet. fix:second call;

                    MarkerOptions mo = new MarkerOptions(); //create Marker
                    mo.position(place.location());
                    mo.icon(decideIcon(place.getType()));
                    mo.title(place.getAttribute());
                    mo.snippet(place.getType());
                    Marker marker = mMap.addMarker(mo);
                    hash_place_markers.put(key, marker);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "showAllPlaces:onCancelled", databaseError.toException());
            }
        });
    }

    /*
     * Hides Markers from map
     * Clears list of Markers for places
     */
    private void removeAllPlaces()
    {
        if (isGeoActive) {
            Log.d(TAG, "removeAllPlaces: You must first deactivate Geofences");
            return;
        }

        if (hash_place_markers!=null){
            for (Map.Entry<String, Marker> entry : hash_place_markers.entrySet())
            {
                entry.getValue().remove(); //delete FROM MAP
            }
            hash_place_markers.clear();
        }
    }

    private void showAllUsers()
    {
        if (!hash_marker_id.isEmpty())
        {
            Log.d(TAG, "showAllUsers: You should empty loaded markers first");
            return;
        }

        if (mChildEventListenerALLUsers == null){
            mChildEventListenerALLUsers = new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Log.d(TAG, "Child: "+dataSnapshot.getKey());
                    final String id = dataSnapshot.getKey(); //UID
                    final LocationBean bean = dataSnapshot.child("coordinates").getValue(LocationBean.class);
                    MarkerOptions mo = new MarkerOptions();
                    mo.position(bean.makeCoordinates());
                    mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_marker_person_color));
                    Marker marker = mMap.addMarker(mo);
                    hash_marker_id.put(marker, id);
                    hashWeak_id_marker.put(id, marker);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "mChildEventListenerALLUsers: "+databaseError.getMessage());
                }
            };
        }
        refLocation.addChildEventListener(mChildEventListenerALLUsers);
    }

    private void removeAllUsers()
    {
        if (mChildEventListenerALLUsers!=null){
            refLocation.removeEventListener(mChildEventListenerALLUsers);
            mChildEventListenerALLUsers = null;
        }
        clearPeopleFromMap();
    }

    private void clearPeopleFromMap()
    {
        for (Map.Entry<String, Marker> entry : hashWeak_id_marker.entrySet())
        {
            entry.getValue().remove(); //delete FROM MAP
        }
        hashWeak_id_marker.clear();
        hash_marker_id.clear();
    }

    //depending on number of places, this could be put in a separate node
    private void searchMapTypeClient(int index, String type) {

        // first clean up
        deactivateGeofences();
        removeAllPlaces();

        // simple query, firebase will return only results, will be cached
        Query byType = refPlaces.orderByChild("type").equalTo(type);//maybe do this on server for location specific
        byType.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    String key = snap.getKey();//should be the unique key
                    PlaceBean place = snap.getValue(PlaceBean.class);

                    MarkerOptions mo = new MarkerOptions();
                    mo.position(place.location());
                    mo.icon(decideIcon(place.getType()));
                    mo.title(place.getAttribute());
                    Marker marker = mMap.addMarker(mo);
                    hash_place_markers.put(key, marker);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "searchMapTypeClient:onCancelled", databaseError.toException());
            }
        });
    }

    private void searchDate(){

        Calendar now = Calendar.getInstance();
        int cyear = now.get(Calendar.YEAR);
        int cmonth = now.get(Calendar.MONTH);
        int cday = now.get(Calendar.DAY_OF_MONTH);
        final boolean isSuccess[] = {false};

        DialogInterface.OnClickListener clicker = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == dialog.BUTTON_NEGATIVE){
                    dialog.dismiss();
                }
                else if(which==dialog.BUTTON_POSITIVE){
                    isSuccess[0] = true;
                }
            }
        };

        DatePickerDialog builder = new DatePickerDialog(MapsActivity.this, new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                if (isSuccess[0]){
                    String dateNow = "Date: "+dayOfMonth+"."+month+"."+year;
                    Log.d(TAG,dateNow);
                    searchMapDateClient(dayOfMonth,month,year);
                }
            }
        }, cyear, cmonth, cday);
        builder.setButton(DialogInterface.BUTTON_NEGATIVE,getString(android.R.string.cancel),clicker);
        builder.setButton(DialogInterface.BUTTON_POSITIVE,getString(android.R.string.ok),clicker);
        builder.getDatePicker().setMaxDate(new Date().getTime());
        builder.getDatePicker().setMinDate(new Date(1498889889007L).getTime()); //1.jul.2017=1498889889007
        //now+(1000*60*60*24*7)); //After 7 Days from Now
        builder.show();
    }

    private void searchMapDateClient(int day, int month, int year){
        // first clean up
        deactivateGeofences();
        removeAllPlaces();

        Calendar date = Calendar.getInstance();
        Long now = date.getTimeInMillis();
        date.set(year,month,day);
        Long what = date.getTime().getTime();

        // simple query, firebase will return only results, will be cached
        // startAt = items GREATER THAN OR EQUAL
        // endAt = items LESS THAN OR EQUAL
        Query byDate = refPlaces.orderByChild("date").startAt(what).endAt(now).limitToFirst(20); //maybe do this on server for location specific
        byDate.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "searchMapDateClient: started...");
                if (!dataSnapshot.exists()) {
                    Toast.makeText(MapsActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    String key = snap.getKey();//should be the unique key
                    PlaceBean place = snap.getValue(PlaceBean.class);

                    MarkerOptions mo = new MarkerOptions();
                    mo.position(place.location());
                    mo.icon(decideIcon(place.getType()));
                    mo.title(place.getAttribute());
                    Marker marker = mMap.addMarker(mo);
                    hash_place_markers.put(key, marker);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "searchMapDateClient:onCancelled", databaseError.toException());
            }
        });
    }

    private void searchTime() {
        Calendar now = Calendar.getInstance();
        int chour = now.get(Calendar.HOUR_OF_DAY);
        int cminute = now.get(Calendar.MINUTE);

        final boolean isSuccess[] = {false};

        DialogInterface.OnClickListener clicker = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == dialog.BUTTON_NEGATIVE){
                    dialog.dismiss();
                }
                else if(which==dialog.BUTTON_POSITIVE){
                    isSuccess[0] = true;
                }
            }
        };

        TimePickerDialog builder = new TimePickerDialog(MapsActivity.this, new TimePickerDialog.OnTimeSetListener()
        {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (isSuccess[0]){
                    String timeNow = "Time: "+hourOfDay+"."+minute;
                    Log.d(TAG,timeNow);
                    searchMapTimeClient(hourOfDay,minute,0);
                }
            }
        },chour,cminute,true);
        builder.setButton(DialogInterface.BUTTON_NEGATIVE,getString(android.R.string.cancel),clicker);
        builder.setButton(DialogInterface.BUTTON_POSITIVE,getString(android.R.string.ok),clicker);
        builder.show();
    }

    private void searchMapTimeClient(int hour, int minute, int second){
        // first clean up
        deactivateGeofences();
        removeAllPlaces();

        Calendar date = Calendar.getInstance();
        Long now = date.getTimeInMillis();
        date.set(Calendar.HOUR_OF_DAY,hour);
        date.set(Calendar.MINUTE,minute);
        date.set(Calendar.SECOND,second);
        Long what = date.getTime().getTime(); // first getTime() forces re-calculation, second gets the correct value

        // simple query, firebase will return only results, will be cached
        // startAt = items GREATER THAN OR EQUAL
        // endAt = items LESS THAN OR EQUAL
        Query byDate = refPlaces.orderByChild("date").startAt(what).endAt(now).limitToFirst(20); //maybe do this on server for location specific
        byDate.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "searchMapTimeClient: started...");
                if (!dataSnapshot.exists()) {
                    Toast.makeText(MapsActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    String key = snap.getKey();//should be the unique key
                    PlaceBean place = snap.getValue(PlaceBean.class);

                    MarkerOptions mo = new MarkerOptions();
                    mo.position(place.location());
                    mo.icon(decideIcon(place.getType()));
                    mo.title(place.getAttribute());
                    Marker marker = mMap.addMarker(mo);
                    hash_place_markers.put(key, marker);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "searchMapTimeClient:onCancelled", databaseError.toException());
            }
        });
    }

    private void searchMapRadiusServer(int radius, String filter, String extras){
        HashMap<String, Object> request = new HashMap<>();
        request.put("radius",radius);
        request.put("filter",filter);
        request.put("extras",extras);

        //send request
        DatabaseReference refFilterRequest = FirebaseDatabase.getInstance().getReference().child("filter/requests");
        final String pushKey = refFilterRequest.push().getKey();
        Log.d(TAG, "Sending request: "+pushKey);
        refFilterRequest.child(pushKey).setValue(request).addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override //REQUEST SENT
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    Log.d(TAG,"REQUEST SENT: "+pushKey);

                    if (mValueEventListenerFilters ==null) {
                        mValueEventListenerFilters = new ValueEventListener() {
                            @Override //----LIST----- ITEM = PLACE UNIQUE KEY
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if(dataSnapshot.getChildrenCount()==0){
                                    Log.d(TAG, "No children.");
                                    return;
                                }
                                //check if we got results
                                else if(dataSnapshot.child("result").exists()){
                                    Log.d(TAG, "Result: No results found");
                                    return;
                                }
                                else {
                                    Log.d(TAG, "Results: " + dataSnapshot.getChildrenCount());
                                    final int counter[] = {(int)dataSnapshot.getChildrenCount()};
                                    //if it falls through here, we got results
                                    for (DataSnapshot snap : dataSnapshot.getChildren()) //placeKey:true
                                    {
                                        final String placeKey = snap.getKey();
                                        Log.d(TAG, "List item--place: " + placeKey);

                                        //NOW READ DATA FOR EACH PLACE
                                        refPlaces.child(placeKey).addListenerForSingleValueEvent(new ValueEventListener()
                                        {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                Log.d(TAG, dataSnapshot.getKey());

                                                PlaceBean place = dataSnapshot.getValue(PlaceBean.class);
                                                if (place==null) return; //safeguard
                                                counter[0] = counter[0]+1;
                                                MarkerOptions mo = new MarkerOptions();
                                                mo.position(place.location());
                                                mo.icon(decideIcon(place.getType()));
                                                mo.title(place.getAttribute());
                                                Marker marker = mMap.addMarker(mo);
                                                hash_place_markers.put(placeKey, marker);
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.e(TAG, "searchMapRadiusServer: ERROR = refPlaces" + placeKey, databaseError.toException());
                                            }
                                        });
                                    }
                                    Log.d(TAG, "Displayed: "+counter[0]);
                                    refFilterResults.removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "searchMapRadiusServer: ERROR = refFilterResults", databaseError.toException());
                            }
                        };
                        refFilterResults.addValueEventListener(mValueEventListenerFilters);
                    }
                }
                else {
                    Log.d(TAG,"REQUEST FAILED");
                }
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Toast.makeText(this, "Service running", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        Toast.makeText(this, "Service NOT RUNNING", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onBackPressed() {
        //mGoogleApiClient.disconnect(); //documentation: connect/disconnect is automatic onstart/stop????
        super.onBackPressed();//?
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

        //here you can get last known location
        //mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        // Begin polling for new location updates.
        startLocationUpdates();
        FirebaseMessaging.getInstance().subscribeToTopic("close"); //gets notification from CloudFunction, about user close by
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Reconnecting...", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Reconnecting...", Toast.LENGTH_SHORT).show();
        }
        mGoogleApiClient.connect();
    }

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
        LocationBean beanie = new LocationBean(location.getLatitude(), location.getLongitude());
        //LocationBean beanie = new LocationBean(ServerValue.TIMESTAMP, location.getLatitude(),location.getLongitude());
        refMyLocation.child(Constants.COORDINATES).setValue(beanie);
        // You can now create a LatLng Object for use with maps
        thisMe = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(thisMe));
        myMarker.setPosition(thisMe);
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(0.1f)      //  Set the minimum displacement between location updates in meters (tryout)
                .setInterval(UPDATE_INTERVAL)       // GoogleDoc: 5 seconds would be appropriate for realtime
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
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            } else {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            mLocationPermissionGranted = true;
        }
    }

    /**
     * Handle onNewIntent() to inform the fragment manager that the
     * state is not saved.  If you are handling new intents and may be
     * making changes to the fragment state, you want to be sure to call
     * through to the super-class here first.  Otherwise, if your state
     * is saved but the activity is not stopped, you could get an
     * onNewIntent() call which happens before onResume() and trying to
     * perform fragment operations at that point will throw IllegalStateException
     * because the fragment manager thinks the state is still saved.
     *
     * @param intent
     */
    @Override // when clicked from notifications, as to not destroy the map activity
    protected void onNewIntent(Intent intent) {
        Log.d(TAG,"onNewIntent...");
        Toast.makeText(this, "onNewIntent", Toast.LENGTH_SHORT).show();
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("NotificationMessage")) {
                Toast.makeText(getApplicationContext(), "onNewIntent: " + extras.getString("NotificationMessage"), Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(TAG,"...onNewIntent");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED && requestCode==PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION){
            mLocationPermissionGranted=true;
        }
    }

    // G E O F E N C E S
    // two main functions activateGeofences() and deactivateGeofences()

    @Override // called on LocationServices.GeofencingApi.addGeofences
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if (status.isSuccess()) {
            Toast.makeText(this, "Geofences added!", Toast.LENGTH_SHORT).show();
            isGeoActive = true;
            drawGeofenceCircles();
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
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Draws Circles around ACTIVE geofences
     * loads from hash_place_markers
     */
    private void drawGeofenceCircles() { //TODO test
        Log.d(TAG, "drawGeofenceCircles");

        if (!isGeoActive) return;
        //check if circles are just hidden?
        for (Map.Entry<String, Marker> entry : hash_place_markers.entrySet())
        {
            CircleOptions circleOptions = new CircleOptions()
                    .center(entry.getValue().getPosition())
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(100, 150, 150, 150))
                    .radius(GEOFENCE_RADIUS);
            Circle fence = mMap.addCircle(circleOptions);
            if (fence!=null)
                hash_circles.put(entry.getKey(),fence);
        }
    }

    /*
     * Removes circles of active geofences from map
     * Clears hash_circles
     * DOES NOT REMOVE GEOFENCES!!!
     */
    private void eraseGeofenceCircles(boolean deleteCircles) { //TODO test
        Log.d(TAG,"eraseGeofenceCircles("+deleteCircles+")");

        if (hash_circles.isEmpty()) return;

        for(Map.Entry<String,Circle> entry: hash_circles.entrySet()){
            entry.getValue().remove(); //to erase from Map
        }

        if (deleteCircles)
            hash_circles.clear();
    }

    /*
     *  if Geofences are active, deactivate and clear mGeofenceList
     *  fillGeo
     */
    public void activateGeofences() {
        Log.d(TAG, "activateGeofences");

        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mGeofenceList.isEmpty()){
            Log.e(TAG,"activateGeofences: Your list is empty.");
            fillGeofences();
        }
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that is reused when calling deactivateGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    //TODO save pending intent for reuse?
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            Toast.makeText(this, "FINE_LOCATION disabled!!!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, securityException.getMessage());
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }
    }

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
        return PendingIntent.getService(this,GEOFENCE_REQ_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*
     * Used to populate mGeofenceList from hash_place_markers
     */
    private void fillGeofences()
    {
        if (!mGeofenceList.isEmpty()) {
            Log.d(TAG, "fillGeofences: You need to first remove geofences.");
            return;
        }

        for(Map.Entry<String,Marker> entry: hash_place_markers.entrySet()){
            mGeofenceList.add(new Geofence.Builder() //create Geofence
                    .setRequestId(entry.getKey())
                    .setCircularRegion(entry.getValue().getPosition().latitude,entry.getValue().getPosition().longitude,GEOFENCE_RADIUS)
                    .setExpirationDuration(GEO_DURATION)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build());
        }
    }

    /*
     * Removes registered Geofences from GoogleApiClient
     * mGeoFenceList.clear()
     * isGeoActive = false;
     * eraseGeofenceCircles()
     */
    private void deactivateGeofences(){
        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "deactivateGeofences: GoogleApiClient not connected.");
            return;
        }
        if (!mGeofenceList.isEmpty())
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
                                    Toast.makeText(MapsActivity.this, "Geofences removed!", Toast.LENGTH_SHORT).show();
                                    mGeofenceList.clear();
                                    isGeoActive = false;
                                    eraseGeofenceCircles(true);
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
}
