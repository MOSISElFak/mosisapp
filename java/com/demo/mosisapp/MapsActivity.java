package com.demo.mosisapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
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
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.demo.mosisapp.Constants.GEOFENCE_RADIUS;

/* (Notes from Google) @SuppressWarnings("MissingPermission") try @SuppressWarnings({"ResourceType"}) or //noinspection MissingPermission
 * Note: If you're using the v7 appcompat library, your activity should instead extend AppCompatActivity, which is a subclass of FragmentActivity. (For more information, read Adding the App Bar)
 * Note: When you add a fragment to an activity layout by defining the fragment in the layout XML file, you cannot remove the fragment at runtime.
 * For starters, Fragment will be created in XML and set on creation. And AppCompatLib supports the easy action bar
 * ?idea? MainActivity can be merged with this one
 * ?idea? Later, MainActivity will load empty fragment on startup, and after login/register user display map fragment
 */
//setPersistanceEnabled(true) only caches data when there is a Listener attached to that node (when the data has been read at least once).
//keepSynced(true) caches everything from that node, even if there is no listener attached.
//The android geofences get removed every time you reboot your device or every time you toggle the location mode
// Note: Markers can not be "re-added" after calling marker.remove(), so there is no way to "hide" them except deletion.

public class MapsActivity extends AppCompatActivity
implements OnMapReadyCallback
        ,GoogleMap.OnMapLongClickListener
        ,GoogleMap.OnMarkerClickListener, View.OnClickListener
{

    private final String TAG = "MapsActivity";

    private boolean isGeoActive = false;
    private boolean mLocationPermissionGranted = true;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int RC_SCOREBOARD = 321;
    //location access

    private int DEFAULT_ZOOM = 15;
    private final LatLng mDefaultLocation = new LatLng(48.137154, 11.576124); //maps could have default locations based on regions
    private LatLngBounds MUNICH = new LatLngBounds(new LatLng(48.047983, 11.363986), new LatLng(48.249102, 11.756060));

    private GoogleMap mMap;

    private Marker myMarker;
    private LatLng mLastKnownLocation;
    private boolean mapOnline = false;

    //RealtimeDB
    private FirebaseDatabase mFirebaseDatabase;         //main access point
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
    private HashMap<String, Marker> hash_id_marker_users;
    //Places
    private DatabaseReference refPlaces;
    private HashMap<String, Marker> hash_place_markers;     //pushKey => Marker
    private HashMap<String, Circle> hash_circles;           //geoKey => Circle
    FloatingActionButton fab_main, fab1, fab2, fab3, fab_type, fab_date;
    boolean isOpenFabMain = false;
    boolean isOpenFabSearch = false;

    //service
    protected MyResultReceiver resultReceiver;              //listener for SERVICE->CLIENT events
    private boolean isServiceBound = false;                 //flag for bound to service

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "camera_location";

    private DrawerLayout mDrawerLayout;
    private Switch switchGeofences, switchUserfences, switchPeople, switchOnline, switchRadius;

    private int flagRadius = 0;
    private boolean useRadius = false;
    private boolean shouldStop = false;                     //option for stoping background service
    private boolean shouldGeofence = false;
    private boolean shouldUserfence = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Log.d(TAG,"onCreate");

        fab_main = (FloatingActionButton) findViewById(R.id.fab_main);
        fab1 = (FloatingActionButton)findViewById(R.id.fab1);   //search fab
        fab2 = (FloatingActionButton)findViewById(R.id.fab2);
        fab3 = (FloatingActionButton)findViewById(R.id.fab3);
        fab_type = (FloatingActionButton)findViewById(R.id.fab2_type);
        fab_date = (FloatingActionButton)findViewById(R.id.fab3_date);

        fab_main.setOnClickListener(this);
        fab1.setOnClickListener(this); //Search FAB
        fab2.setOnClickListener(this);
        fab3.setOnClickListener(this);
        fab_type.setOnClickListener(this);
        fab_date.setOnClickListener(this);

        // Initializations
        friends = new ArrayList<>();
        hash_marker_id = new HashMap<>();
        hashWeak_id_marker = new WeakHashMap<>();
        hash_place_markers = new HashMap<>();
        hash_circles = new HashMap<>();

        // Initialize RealtimeDB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refLocation = mFirebaseDatabase.getReference().child(Constants.LOCATIONS);
        refMyFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        refUsers = mFirebaseDatabase.getReference().child(Constants.USERS);
        refPlaces = mFirebaseDatabase.getReference().child(Constants.READPLACE);

        refFilterResults = FirebaseDatabase.getInstance().getReference().child("filter/results").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        requestOptions = new RequestOptions()                               // GLIDE options for friends icons
                .diskCacheStrategy(DiskCacheStrategy.ALL)                   //DATA(original),RESOURCE(after transformations)
                .placeholder(R.drawable.ic_action_marker_person_color)      //default icon before loading
                .fallback(R.drawable.ic_action_marker_person_color)         //default icon in case of null
                //.fallback(new ColorDrawable(Color.GRAY));
                .priority(Priority.HIGH)
                .circleCrop()
                .override(50, 50);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        initializeDrawer();
    }

    private void initializeDrawer() {
        // TextViews
        findViewById(R.id.drawer_textview_profile).setOnClickListener(this);
        findViewById(R.id.drawer_textview_score).setOnClickListener(this);
        findViewById(R.id.drawer_textview_logout).setOnClickListener(this);

        shouldUserfence = getSharedPreferences("flags",MODE_PRIVATE).getBoolean("shouldUserfence",false);
        shouldStop = getSharedPreferences("flags",MODE_PRIVATE).getBoolean("shouldStop",false);
        shouldGeofence = getSharedPreferences("flags", MODE_PRIVATE).getBoolean("shouldGeofence",false);
        useRadius = getSharedPreferences("flags",MODE_PRIVATE).getBoolean("useRadius",false);
        flagRadius = getSharedPreferences("flags",MODE_PRIVATE).getInt("flagRadius",1000);

        //Switches
        switchUserfences = (Switch)findViewById(R.id.switch_userfences);
        switchGeofences = (Switch)findViewById(R.id.switch_geofences);
        switchPeople = (Switch)findViewById(R.id.switch_people);
        switchOnline = (Switch)findViewById(R.id.switch_online);
        switchRadius = (Switch)findViewById(R.id.switch_radius);

        switchUserfences.setChecked(shouldUserfence);
        switchGeofences.setChecked(shouldGeofence);
        switchOnline.setChecked(!shouldStop);
        switchRadius.setChecked(useRadius);
        switchPeople.setChecked(false);

        switchUserfences.setOnClickListener(this);
        switchGeofences.setOnClickListener(this);
        switchGeofences.setOnClickListener(this);
        switchPeople.setOnClickListener(this);
        switchOnline.setOnClickListener(this);
        switchRadius.setOnClickListener(this);
    }

    private void animateSearchFabs() {
        if (isOpenFabSearch){
            Log.d(TAG, "closing search fab");
            animateCloseSearch();
        } else {
            Log.d(TAG, "opening search fab");
            animateOpenSearch();
        }
    }

    private void animateMainFabs() {
    if (isOpenFabMain){
        if (isOpenFabSearch) {
            animateCloseSearch();
        }
        Log.d(TAG, "closing -->");
        animateClose();
    }
    else{
        Log.d(TAG, "<-- opening");
        animateOpen();
    }
}

    private void animateCloseSearch() {
        final Animation popOut2 = new AlphaAnimation(1f,0f);
        popOut2.setFillAfter(false);
        popOut2.setDuration(200);
        final Animation popOut3 = new AlphaAnimation(1f,0f);
        popOut3.setFillAfter(false);
        popOut3.setDuration(100);

        fab_date.startAnimation(popOut3);
        findViewById(R.id.fab3_date_label).setVisibility(View.INVISIBLE);
        fab_date.setVisibility(View.INVISIBLE);
        fab3.setVisibility(View.VISIBLE);

        fab_type.startAnimation(popOut2);
        findViewById(R.id.fab2_type_label).setVisibility(View.INVISIBLE);
        fab_type.setVisibility(View.INVISIBLE);
        fab2.setVisibility(View.VISIBLE);

        isOpenFabSearch=false;
    }

    private void animateOpenSearch() {
        final Animation popUp1 = new AlphaAnimation(0f,1f);
        popUp1.setFillAfter(true);
        popUp1.setDuration(100);
        final Animation popUp2 = new AlphaAnimation(0f,1f);
        popUp2.setFillAfter(true);
        popUp2.setDuration(200);

        fab_type.startAnimation(popUp1);
        fab_type.setVisibility(View.VISIBLE);
        findViewById(R.id.fab2_type_label).setVisibility(View.VISIBLE);
        fab2.setVisibility(View.INVISIBLE);

        fab_date.startAnimation(popUp2);
        fab_date.setVisibility(View.VISIBLE);
        findViewById(R.id.fab3_date_label).setVisibility(View.VISIBLE);
        fab3.setVisibility(View.INVISIBLE);
        isOpenFabSearch=true;
    }

    private void animateOpen() {
        final Animation popUp1 = new AlphaAnimation(0f,1f);
        popUp1.setFillAfter(true);
        popUp1.setDuration(400);
        final Animation popUp2 = new AlphaAnimation(0f,1f);
        popUp2.setFillAfter(true);
        popUp2.setDuration(400);
        popUp2.setStartOffset(200);
        final Animation popUp3 = new AlphaAnimation(0f,1f);
        popUp3.setFillAfter(true);
        popUp3.setDuration(400);
        popUp3.setStartOffset(400);

        fab_main.animate().rotation(-45.0f);
        fab1.startAnimation(popUp1);
        fab1.setVisibility(View.VISIBLE);
        fab2.startAnimation(popUp2);
        fab2.setVisibility(View.VISIBLE);
        fab3.startAnimation(popUp3);
        fab3.setVisibility(View.VISIBLE);

        isOpenFabMain = true;
    }

    private void animateClose() {
        final Animation popOut3 = new AlphaAnimation(1f,0f);
        popOut3.setFillAfter(false);
        popOut3.setDuration(300);

        final Animation popOut2 = new AlphaAnimation(1f,0f);
        popOut2.setFillAfter(false);
        popOut2.setDuration(300);
        popOut2.setStartOffset(100);

        final Animation popOut1 = new AlphaAnimation(1f,0f);
        popOut1.setFillAfter(false);
        popOut1.setDuration(300);
        popOut1.setStartOffset(200);

        fab_main.animate().rotation(0);
        fab3.startAnimation(popOut3);
        fab3.setVisibility(View.INVISIBLE);
        fab2.startAnimation(popOut2);
        fab2.setVisibility(View.INVISIBLE);
        fab1.startAnimation(popOut1);
        fab1.setVisibility(View.INVISIBLE);

        isOpenFabMain =false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        // In case it gets changed
        SharedPreferences data = getSharedPreferences("basic", MODE_PRIVATE);
        TextView username = (TextView)findViewById(R.id.drawer_username);
        username.setText(data.getString("username","Unknown"));

        startLocationService(); // startService: [onCreate][onStartCommand]
        bindToService();        // bindService  [onBind] BECAUSE YOU NEED TO REMOVE THE RECEIVER WHEN YOU STOP ACTIVITY
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        getSharedPreferences("flags",MODE_PRIVATE).edit()
                .putInt("flagRadius",flagRadius)
                .putBoolean("shouldStop",shouldStop)
                .putBoolean("useRadius",useRadius)
                .apply();

        //TODO: remove my marker?

        saveGeofenceData();
        unbindFromService();
        detachDatabaseReadListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
        if (shouldStop) {
            Log.d(TAG, "onStop: should stop the service");
            stopLocationService();
        }
        if (mChildEventListenerFriends != null) mChildEventListenerFriends = null;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId()){
            case R.id.drawer_textview_profile:      //View My Profile
                Log.d(TAG, "onClick: View My Profile");
                mDrawerLayout.closeDrawers();
                Intent mine = new Intent(MapsActivity.this, MyProfileActivity.class);
                startActivity(mine);
                break;
            case R.id.drawer_textview_score:        //Show Scoreboard
                Log.d(TAG, "onClick: Show Scoreboard");
                mDrawerLayout.closeDrawers();
                Intent board = new Intent(this,ScoreboardActivity.class);
                startActivityForResult(board, RC_SCOREBOARD);
                break;
            case R.id.drawer_textview_logout:       //Log out
                Log.d(TAG, "onClick: Log out");
                MosisApp.getInstance().logoutFlag = true;
                mDrawerLayout.closeDrawers();
                MapsActivity.this.finish();
                break;
            case R.id.switch_geofences:               //Switches
                Log.d(TAG, "onClick: switch_geofences");
                if (switchGeofences.isChecked()){
                    if (hash_place_markers == null || hash_place_markers.isEmpty()) {
                        Toast.makeText(MapsActivity.this, "Select some places first", Toast.LENGTH_SHORT).show();
                        switchGeofences.setChecked(false);
                    } else {
                        mDrawerLayout.closeDrawers();
                        askGeofencingDialog();
                    }
                } else {
                    shouldGeofence = false;
                    if (isServiceBound){
                        myBinder.stopGeofencing();
                    }
                }
                break;
            case R.id.switch_userfences:
                Log.d(TAG, "onClick: switch_userfences");
                if (switchUserfences.isChecked()){
                    Log.d(TAG, "subscribed to close users");
                    shouldUserfence = true;
                    FirebaseMessaging.getInstance().subscribeToTopic("close"); //gets notification from CloudFunction, about user close by
                } else {
                    Log.d(TAG, "unsubscribed from close users");
                    shouldUserfence = false;
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("close"); //gets notification from CloudFunction, about user close by
                }
                getSharedPreferences("flags",MODE_PRIVATE).edit().putBoolean("shouldUserfence",shouldUserfence).commit();
                break;
            case R.id.switch_people:
                Log.d(TAG, "onClick: switch_people");
                if (switchPeople.isChecked()){
                    Log.d(TAG,"showing all users");
                    showAllUsers();
                } else {
                    Log.d(TAG,"removing users");
                    removeAllUsers();
                }
                mDrawerLayout.closeDrawers();
                break;
            case R.id.switch_online:
                Log.d(TAG, "onClick: switch_online");
                shouldStop = !switchOnline.isChecked();
                Log.d(TAG, "shouldStop="+shouldStop);
                getSharedPreferences("flags",MODE_PRIVATE).edit().putBoolean("shouldStop",shouldStop).commit();
                break;
            case R.id.switch_radius:
                Log.d(TAG, "onClick: switch_radius");
                if (switchRadius.isChecked()){
                    mDrawerLayout.closeDrawers();
                    askRadiusDialog();
                } else {
                    useRadius=false;
                    getSharedPreferences("flags",MODE_PRIVATE).edit().putBoolean("useRadius",useRadius).commit();
                }
                break;
            case R.id.fab_main:
                animateMainFabs();
                break;
            case R.id.fab1:
                animateSearchFabs();
                break;
            case R.id.fab3:                         //FAB - Add person
                Log.d(TAG, "onClick: fab3 - add person");
                Intent addFriend = new Intent(this, BluetoothFriendActivity.class);
                startActivity(addFriend);
                break;
            case R.id.fab2:                         //FAB - Add place
                Log.d(TAG, "onClick: fab2 - add place");
                if (mLastKnownLocation != null) {
                    Intent addPlace = new Intent(this, PlaceAddActivity.class);
                    addPlace.putExtra("place_lat", mLastKnownLocation.latitude);
                    addPlace.putExtra("place_lon", mLastKnownLocation.longitude);
                    startActivity(addPlace);
                } else
                    Toast.makeText(this, "PlaceAdd not ready", Toast.LENGTH_SHORT).show();
                break;
            case R.id.fab2_type:                         //FAB - Search places
                Log.d(TAG, "onClick: fab2_type");
                animateMainFabs();
                searchDialog();
                break;
            case R.id.fab3_date:
                Log.d(TAG, "onClick: fab3_date");
                animateMainFabs();
                searchDate();
                break;
        }
    }

    private void askGeofencingDialog() {
        Log.d(TAG, "askGeofencingDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Place Notifications");
        builder.setMessage("Turn on notifications when you are near selected places?");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "askGeofencingDialog: OK");
                startGeofencing();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "askGeofencingDialog: CANCEL");
                shouldGeofence = false;
                switchGeofences.setChecked(false);
            }
        }).show();
    }

    private void askRadiusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Select Radius");
        //builder.setView(R.layout.dialog_radius);
        final View ad = getLayoutInflater().inflate(R.layout.dialog_radius, null);
        final NumberPicker np = (NumberPicker)ad.findViewById(R.id.radius_picker);
        np.setMaxValue(10);//10km
        np.setMinValue(1); //1km
        builder.setView(ad);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: OK "+which);
                Log.d(TAG, String.valueOf(np.getValue()));
                useRadius=true;
                getSharedPreferences("flags",MODE_PRIVATE).edit().putBoolean("useRadius",useRadius).commit();
                getSharedPreferences("flags",MODE_PRIVATE).edit().putInt("flagRadius",flagRadius).commit();
                switchRadius.setChecked(true);
                flagRadius = np.getValue()*1000;
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: Cancel "+which);
                switchRadius.setChecked(useRadius);
            }
        }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==RC_SCOREBOARD){
            Log.d(TAG, "onActivityResult, RC_SCOREBOARD"); //DOESNT WORK, try adding "stay in history flag"?
        }
    }

    private void startLocationService() {
        Log.d(TAG,"startLocationService");
        resultReceiver = new MyResultReceiver(null);
        Intent i = new Intent(this,MyLocationService.class);
        i.putExtra("receiver", resultReceiver);
        startService(i); //this return immediately
    }
    private void bindToService() {
        Log.d(TAG,"bindToService");
        Intent i = new Intent(this,MyLocationService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE|BIND_ADJUST_WITH_ACTIVITY); //TODO: note added flag
    }

    private void stopLocationService() {
        Log.d(TAG,"stopLocationService");
        if (isServiceBound) unbindFromService();
        Intent i = new Intent(this,MyLocationService.class);
        stopService(i);
    }

    private void unbindFromService() {
        Log.d(TAG,"unbindFromService");
        if (isServiceBound) {
            unbindService(mServiceConnection);
            myBinder = null;
            isServiceBound = false;
            Log.d(TAG,"unbindFromService,...done.");
        }
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
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        mapOnline = true;
        //mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);//HYBRID =satelite+terrain, TERRAIN =roads
        //googleMap.getUiSettings().setMapToolbarEnabled(false); //for not displaying bottom toolbar (navigation and directions)

        try {
            // Customise the styling of the base map using a JSON object defined in a raw resource file.
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.silver_map_with_icons));

            if (!success) {
                Log.e("OnMapReady", "Style parsing failed.");
            }
        } catch ( Resources.NotFoundException e ) {
            Log.e("OnMapReady", "Can't find style. Error: ", e);
        }

        //findViewById(R.id.map).setVisibility(View.VISIBLE);
        mMap.setMyLocationEnabled(true); // shows realtime blue dot (me) on map
        //mMap.getUiSettings().setMyLocationButtonEnabled(true); // ze button
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMaxZoomPreference(20);
        mMap.setMinZoomPreference(14);
        mMap.setLatLngBoundsForCameraTarget(MUNICH);

        if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLastKnownLocation, DEFAULT_ZOOM));
            if (myMarker==null) myMarker = createMyMarker();
            else myMarker.setPosition(mLastKnownLocation);
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM)); //with no location, you get map of earth
        }
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);

        attachListeners();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick");
        if (hash_marker_id.containsValue(marker)){
            Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
            String extra = hash_marker_id.get(marker);
            intent.putExtra("key_id", extra);
            startActivity(intent);
        }
        else {
            marker.showInfoWindow(); //TODO test now if it works
        }
        return true;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //float distanceInMeters =  targetLocation.distanceTo(myLocation);
        Toast.makeText(this, latLng.latitude + ": "+latLng.longitude, Toast.LENGTH_SHORT).show();
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
            case (R.id.menu_start_geofences):
                myBinder.startGeofencing();
                return true;
            case (R.id.menu_hide_geofences):
                //isMyServiceRunning(MyLocationService.class);
                //eraseGeofenceCircles(true);
                fillClientGeofences();
                return true;
            case (R.id.menu_delete_geofences):
                myBinder.stopGeofencing();
                return true;
            case (R.id.menu_show_all_places):
                //showAllUsers();
                showAllPlaces();
                return true; // absorbed event
            case (R.id.menu_remove_all_places):
                //removeAllUsers();
                removeAllPlaces();
                return true;
            case (R.id.menu_search_date):
                searchDate();
                return true;
            case (R.id.menu_search_server):
                searchMapRadiusServer(100,"type","Recycle");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void detachDatabaseReadListener() {
        if (mChildEventListenerFriends != null) {
            refMyFriends.removeEventListener(mChildEventListenerFriends);
        }
        //listeners for each friend's location?
        for (String id: friends) {
            refLocation.child(id).removeEventListener(mChildEventListenerLocations);
        }

        if (mValueEventListenerFilters != null){
            refFilterResults.removeEventListener(mValueEventListenerFilters);
            mValueEventListenerFilters = null;
        }
    }

    private void attachListeners() {
        // IT HAS TO BE CHILD_LISTENER TO DIFFERENTIATE BETWEEN ADDED AND CHANGED
        if (mChildEventListenerLocations == null) { //for this to work, it has to have an additional node <coordinates>
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
                    friends.remove(id);
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

    //this should not be allowed, it defies the point of friendships and privacy
    //THIS IS A BAD IDEA
    private void showAllUsers() {
        Log.d(TAG, "showAllUsers");
        final String myid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (hash_id_marker_users==null){
            hash_id_marker_users = new HashMap<>();
        }
        if (mChildEventListenerALLUsers == null){
            mChildEventListenerALLUsers = new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Log.d(TAG, "Child: "+dataSnapshot.getKey());
                    final String id = dataSnapshot.getKey(); //UID
                    if (myid.equals(id)) return; //skip me
                    if (!hashWeak_id_marker.containsKey(id)) //skip friends
                    {
                        final LocationBean bean = dataSnapshot.child("coordinates").getValue(LocationBean.class);
                        MarkerOptions mo = new MarkerOptions();
                        mo.position(bean.makeCoordinates());
                        mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_green));
                        Marker marker = mMap.addMarker(mo);
                        hash_id_marker_users.put(id, marker);
                    }
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
                    switchPeople.setChecked(false);
                }
            };
        }
        refLocation.addChildEventListener(mChildEventListenerALLUsers);
    }

    private void removeAllUsers() {

        if (mChildEventListenerALLUsers!=null){
            refLocation.removeEventListener(mChildEventListenerALLUsers);
            mChildEventListenerALLUsers = null;
        }
        clearUsersFromMap();
    }

    private void clearUsersFromMap(){

        if (hash_id_marker_users==null || hash_id_marker_users.isEmpty()) return; // No user is displayed

        for (Map.Entry<String, Marker> entry : hash_id_marker_users.entrySet())
        {
            entry.getValue().remove(); //delete FROM MAP
        }
        hash_id_marker_users.clear();
    }


//**********************************************************************************************[ PLACES }**********************
    private BitmapDescriptor decideIcon(String type) {
        switch(type){
            case("Recycle"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_recycle_green_edited);
            case("Fountains"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_heart_greenedit);
            case("Electric vehicles"):
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_car_greenedit);
            default:
                return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_recycle_green_edited);
        }
    }

    // Note: Because of the way firebase cache works, for the last added place you only get ID, but not the data
    private void showAllPlaces() {
        if (!hash_place_markers.isEmpty()) { //some of the places are already in the map, but clear just in case
            removeAllPlaces();
        }
        refPlaces.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap: dataSnapshot.getChildren()) {
                    String key = snap.getKey(); //should be the unique key "-K5p97A..."
                    PlaceBean place = snap.getValue(PlaceBean.class);
                    if (place == null) return; //skip, this happens when place is new and is not in cache yet. fix:second call;

                    MarkerOptions mo = new MarkerOptions(); //create Marker
                    mo.position(place.location());
                    mo.icon(decideIcon(place.getType()));
                    mo.title(place.getType());
                    mo.snippet(place.getAttribute());
                    Marker marker = mMap.addMarker(mo);
                    hash_place_markers.put(key, marker); //"-K5p97A..." : [marker]
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "showAllPlaces:onCancelled", databaseError.toException());
            }
        });
    }

    private void removeAllPlaces() {

        for (Map.Entry<String, Circle> entry : hash_circles.entrySet())
        {
            entry.getValue().remove(); //delete FROM MAP
        }
        hash_circles.clear();
        if (hash_place_markers!=null){
            for (Map.Entry<String, Marker> entry : hash_place_markers.entrySet())
            {
                entry.getValue().remove(); //delete FROM MAP
            }
            hash_place_markers.clear();
        }
    }

    private void searchMapTypeClient(String type) {
        Log.d(TAG,"searchMapTypeClient");

        // first clean up
        //deactivateGeofences();
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
                    Log.d(TAG,"Place found: "+key);
                    PlaceBean place = snap.getValue(PlaceBean.class);
                    if (place==null) return;

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

    private void searchDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        final View bubu = inflater.inflate(R.layout.filter_layout, null);

        ((Spinner)bubu.findViewById(R.id.filter_by_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String source = "add_attr_".concat(Integer.toString(position));
                    Class res = R.array.class;
                    Field field = res.getField(source);
                    int zeId = field.getInt(null);
                    ArrayAdapter<CharSequence> spin_adapter_attr = ArrayAdapter.createFromResource(MapsActivity.this, zeId, android.R.layout.simple_spinner_item);
                    spin_adapter_attr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    ((Spinner) bubu.findViewById(R.id.add_attribute_spinner)).setAdapter(spin_adapter_attr);
                } catch (Exception e) {
                    Log.e("Spinner selection", "Failed to get spinner ID.", e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected");
            }
        });

        builder.setView(bubu)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String type = ((String)((Spinner)bubu.findViewById(R.id.filter_by_spinner)).getSelectedItem());
                        Log.d(TAG, "on OK click "+type);

                        if (type.equalsIgnoreCase("all")){
                            if (useRadius){
                                Log.d(TAG,"searchDialog: ALL, RADIUS: "+flagRadius);
                                searchMapRadiusServer(flagRadius,Constants.ALL,""); //TODO second extras?
                            }else{
                                Log.d(TAG,"searchDialog: ALL, no radius");
                                showAllPlaces();
                            }
                        } else {
                            String attribute = ((String)((Spinner)bubu.findViewById(R.id.add_attribute_spinner)).getSelectedItem());
                            if (attribute.equalsIgnoreCase("all")){
                                if (useRadius){
                                    Log.d(TAG, "searchDialog: CUSTOM searchMapRadiusServer(flagRadius=" + flagRadius + ",type=" + type + ",attribute=" + attribute + ");");
                                    searchMapRadiusServer(flagRadius, Constants.TYPE, type);
                                }
                                else {
                                    Log.d(TAG, "searchDialog: CUSTOM, no radius, no attribute");
                                    searchMapTypeClient(type);
                                }
                            } else {
                                if (useRadius) {
                                    Log.d(TAG, "searchDialog: CUSTOM searchMapRadiusServer(flagRadius=" + flagRadius + ", ATTRIBUTE, "+attribute);
                                    searchMapRadiusServer(flagRadius, Constants.ATTRIBUTE, attribute);
                                }
                                else {
                                    Log.d(TAG, "hacky one");
                                    searchMapRadiusServer(10000, Constants.ATTRIBUTE, attribute);
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
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
                    // Note: DatePicker returns month as 0-11, this is expected and consistent with Calendar class.
                    String dateNow = "Date: "+dayOfMonth+"."+month+"."+year;
                    Log.d(TAG,dateNow);
                    if (useRadius){
                        // server search
                        searchMapDateServer(flagRadius,dayOfMonth,month,year);
                    } else {
                        // client search
                        searchMapDateClient(dayOfMonth, month, year);
                    }
                }
            }
        }, cyear, cmonth, cday);
        builder.setButton(DialogInterface.BUTTON_NEGATIVE,getString(android.R.string.cancel),clicker);
        builder.setButton(DialogInterface.BUTTON_POSITIVE,getString(android.R.string.ok),clicker);
        builder.getDatePicker().setMaxDate(new Date().getTime());
        builder.getDatePicker().setMinDate(new Date(1483228800000L).getTime());
        // 1498889889007 = Sat Jul 01 2017 06:18:09 GMT+0000
        // 1483228800000 = Sun Jan 01 2017 00:00:00 GMT+0000
        // 1498867200000 = Sat Jul 01 2017 00:00:00 GMT+0000
        //now+(1000*60*60*24*7)); //After 7 Days from Now
        builder.show();
    }

    private void searchMapDateClient(int day, int month, int year){
        Log.d(TAG,"searchMapDateClient");
        // first clean up
        //deactivateGeofences();
        removeAllPlaces();

        // NOTE: Calendar counts months as 0-11
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

    private void searchMapDateServer(int radius, int day, int month, int year){
        Log.d(TAG, "searchMapDateServer");
        removeAllPlaces();

        // NOTE: Calendar counts months as 0-11
        Calendar date = Calendar.getInstance();
        Long now = date.getTimeInMillis();
        date.set(year,month,day);
        Long what = date.getTime().getTime();

        searchMapRadiusServer(radius, Constants.DATE, what.toString());
    }

    private void searchMapRadiusServer(int radius, String filter, String extras){
        Log.d(TAG,"searchMapRadiusServer");
        removeAllPlaces();
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

                    if (mValueEventListenerFilters ==null) { // has to be the "long-lasting" listener because data may not exist at the time of attachment
                        mValueEventListenerFilters = new ValueEventListener() {
                            @Override //----LIST----- ITEM = PLACE UNIQUE KEY
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                // failsafe, because on first read it reads 0
                                if(dataSnapshot.getChildrenCount()==0){
                                    Log.d(TAG, "No children.");
                                    return;
                                }
                                //check if we got "results", which is key node set when results found actually are =0
                                else if(dataSnapshot.child("result").exists()){
                                    Toast.makeText(MapsActivity.this, "No results found.", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Result: No results found");
                                    return;
                                }
                                else {
                                    //if it falls through here, we got results (results is a list of placeKey that fit filter)
                                    Log.d(TAG, "Results: " + dataSnapshot.getChildrenCount());
                                    final int counter[] = {(int)dataSnapshot.getChildrenCount()}; // unlikely to overflow int
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

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    public void onGACConnected() {
        Log.d(TAG, "onGACConnected");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // calls onMapReady when ready
    }

    public void newLocationChanged(Double lat, Double lon) {
        // You can now create a LatLng Object for use with maps
        mLastKnownLocation = new LatLng(lat, lon);
        if (mMap!=null && mapOnline) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mLastKnownLocation));
            if (myMarker==null) myMarker = createMyMarker();
            else myMarker.setPosition(mLastKnownLocation);

        }
    }

    private Marker createMyMarker() {
        return mMap.addMarker(new MarkerOptions()
                .title(getString(R.string.default_info_title))
                .position(mLastKnownLocation)
                .snippet(getString(R.string.default_info_snippet))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_location_me_dark)));
                //.alpha(0.7f));
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            mLocationPermissionGranted = true;
        }
    }

    /**
     * Will be called when a user clicks on notification
     * If the activity is already open, will prevent destroying and re-creating the activity
     * @param intent intent that is sent with notifications
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("FriendID")) {
                //This is a friend from [MyNotificationService]
                Log.d(TAG,"onNewIntent: FriendID: "+extras.getString("FriendID"));
            }
            else if(extras.containsKey("GeofenceID")){
                //This is a geofence from [GeofenceIntentService]
                Log.d(TAG,"onNewIntent: GeofenceID: "+extras.getString("GeofenceID"));
                refPlaces.child(extras.getString("GeofenceID")).addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            PlaceBean geof = dataSnapshot.getValue(PlaceBean.class);
                            if (geof==null) return;
                            if (!hash_place_markers.containsKey(dataSnapshot.getKey())){
                                MarkerOptions mo = new MarkerOptions()
                                        .position(new LatLng(geof.getPlatitude(),geof.getPlongitude()))
                                        .icon(decideIcon(geof.getType()))
                                        .title(geof.getAttribute());
                                Marker marker = mMap.addMarker(mo);
                                hash_place_markers.put(dataSnapshot.getKey(), marker);
                            }
                            CircleOptions circleOptions = new CircleOptions()
                                    .center(new LatLng(geof.getPlatitude(),geof.getPlongitude()))
                                    .strokeColor(Color.argb(50, 70, 70, 70))
                                    .fillColor(Color.argb(100, 150, 150, 150))
                                    .radius(GEOFENCE_RADIUS);
                            hash_circles.put(dataSnapshot.getKey(),mMap.addCircle(circleOptions));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG,databaseError.getMessage());
                    }
                });
            }
            else {
                Log.d(TAG,"onNewIntent got some extras");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED && requestCode==PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION){
            mLocationPermissionGranted=true;
        }
    }

    class MyResultReceiver extends ResultReceiver {
        public MyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData)
        {
            if(resultCode == 100){
                Log.d(TAG,"onReceiveResult: connected!");
                if (resultData!=null && resultData.containsKey("loc")) {
                    mLastKnownLocation = resultData.getParcelable("loc");
                    Log.d(TAG, "onReceiveResult: we got the last known location");
                }
                onGACConnected(); //loads map
            }
            else if(resultCode == 101){
                Log.d(TAG,"onReceiveResult: connection suspended, reconnecting");
            }
            else if(resultCode == 102){
                Log.d(TAG,"onReceiveResult: connection failed");
                //error has to handled from activity
                //https://developers.google.com/android/guides/api-client#handle_connection_failures
            }
            else if(resultCode==103){
                Log.d(TAG,"onReceiveResult: location updated");
                if (resultData!=null){
                    newLocationChanged(resultData.getDouble("loc_lat"),resultData.getDouble("loc_lon"));
                }
            }
            else if (resultCode==104){
                Log.d(TAG,"onReceiveResult: geofences");
                if (resultData!=null){
                    Toast.makeText(MapsActivity.this, resultData.getString("error"), Toast.LENGTH_SHORT).show();
                } else {
                    shouldGeofence = true;
                    switchGeofences.setChecked(true);
                    getSharedPreferences("flags", MODE_PRIVATE).edit().putBoolean("shouldGeofence", shouldGeofence).apply();
                }
            }
            else if (resultCode==105){
                shouldGeofence = false;
                switchGeofences.setChecked(false);
            }
            else{
                Log.d(TAG,"onReceiveResult: Result Received "+resultCode);
            }
        }
    }

    // will be called when service is binded
    private MyLocationService.MyBinder myBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected: Service has unexpectedly disconnected");
            isServiceBound = false;
            myBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected");
            myBinder = (MyLocationService.MyBinder) service;
            //mBoundService = myBinder.getService();
            isServiceBound = true;
        }
    };

    // creates geofences in mGeofenceList[Service] from hash_place_markers[Activity] which holds current filltered results
    protected boolean fillClientGeofences() {
        if (!isServiceBound) {
            Log.d(TAG, "fillClientGeofences: No service bound!");
            return false;
        }
        for (Map.Entry<String, Marker> entry : hash_place_markers.entrySet())
        {
            String dummy = entry.getValue().getTitle()+"!"+entry.getKey();
            myBinder.getGeofenceList().add(new Geofence.Builder() //create Geofence
                    .setRequestId(dummy)
                    .setCircularRegion(entry.getValue().getPosition().latitude, entry.getValue().getPosition().longitude, GEOFENCE_RADIUS)
                    .setExpirationDuration(Constants.GEO_DURATION)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build());
        }
        Log.d(TAG, "fillClientGeofences: mGeofenceList filled by Client");
        Log.d(TAG, "fillClientGeofences: mGeofenceList size "+myBinder.getGeofenceList().size());
        return true;
    }

    // will overwrite value if already exists! so no need for extra call for delete
    protected void saveGeofenceData() { //should be about 5KB max
        Log.d(TAG, "saveGeofenceData");
        HashMap<String,LatLng> tosave = new HashMap<>(hash_place_markers.size());
        for(Map.Entry<String,Marker> e: hash_place_markers.entrySet()){
            tosave.put(e.getValue().getTitle()+"!"+e.getKey(),new LatLng(e.getValue().getPosition().latitude,e.getValue().getPosition().longitude));
        }
        SharedPreferences data = getSharedPreferences("localservice", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tosave);
        editor.putString("fences", json);
        editor.commit();
    }

    protected void startGeofencing(){
        Log.d(TAG, "startGeofencing");
        if (!isServiceBound) {
            Toast.makeText(this, "No service available", Toast.LENGTH_SHORT).show();
        }
        if (fillClientGeofences()){
            myBinder.startGeofencing();
        }
        saveGeofenceData();     // service will then reload it on restart
    }
}
//CODES
// 100 - connected : [null | last known location]
// 101 - suspended, reconnecting
// 102 - failed
// 103 - on location changed : two doubles
// 104 - geofences active
// 105 - signal for switch_geofences = false when geofences deleted