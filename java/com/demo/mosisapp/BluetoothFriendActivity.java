package com.demo.mosisapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;

public class BluetoothFriendActivity extends AppCompatActivity implements View.OnClickListener
{
    private final String TAG = "BluetoothFriendActivity";
    // Intent request codes
    private final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private final int REQUEST_ENABLE_DISCOVERY = 122;
    private final int RC_MULTIPLE = 45;

    private static final int DISCOVER_TIME = 5*60; //5 minutes

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothFriendService btService;

    private String mOldDeviceName;
    private String mNewDeviceName;
    //Name of the connected device
    private String mConnectedDeviceName = null;

    private FirebaseDatabase mFirebaseDatabase; //main access point
    private DatabaseReference refRequest;
    private String secret;
    private String theirSecret;

    private TextView status_received;
    private Drawable defaultPic;
    private ImageView mPic;
    private TextView mName1;
    private TextView mName2;

    private boolean flag_requester = false;
    private boolean flag_bt = false;
    private Menu bt_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_friend);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 1. getAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "No Bluetooth device detected", Toast.LENGTH_SHORT).show();
            finish();
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refRequest = mFirebaseDatabase.getReference().child("requests");

        //UI components
        status_received = (TextView)findViewById(R.id.bt_status_received);
        Button mBtnFindPeople = (Button) findViewById(R.id.bt_request_send);
        Button mBtnAcceptRequest = (Button) findViewById(R.id.request_accept);
        Button mBtnDenyRequest = (Button) findViewById(R.id.request_deny);
        mBtnFindPeople.setOnClickListener(this);
        mBtnAcceptRequest.setOnClickListener(this);
        mBtnDenyRequest.setOnClickListener(this);

        mPic = (ImageView)findViewById(R.id.request_pic);
        mName1 = (TextView)findViewById(R.id.request_name1);
        mName2 = (TextView)findViewById(R.id.request_name2);

        // Because simple does not work
        defaultPic = mPic.getDrawable();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
        // If BT is not on, request that it be enabled.
        // setupThis() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            turnOnDiscoverable();
        } else if (btService == null) {
            flag_bt = true;
            setupThis();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothFriendService.STATE_NONE) {
                // Start the Bluetooth chat services
                btService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");

        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        if (mOldDeviceName != null) mBluetoothAdapter.setName(mOldDeviceName);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        if (btService != null) {
            btService.stop();
        }
        setOldDeviceName();
        if (mBluetoothAdapter!=null){
            mBluetoothAdapter.disable();
        }
        File image = new File(getCacheDir(), "someone" + ".jpg");
        if (image.exists()) image.delete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        bt_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.bt_on_off_item:
                Log.d(TAG, "clicked on bt icon");
                if (flag_bt){
                    flag_bt = false;
                    item.setIcon(R.drawable.ic_action_bluetooth);
                    findViewById(R.id.bt_received_request).setVisibility(View.GONE);
                    findViewById(R.id.bt_send_request).setVisibility(View.GONE);
                    status_received.setText("We need bluetooth enabled to find other users.");
                } else {
                    turnOnDiscoverable();
                }
                return true;
        }
        return false;
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        Toolbar bar = (Toolbar)findViewById(R.id.toolbar);
        bar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        Toolbar bar = (Toolbar)findViewById(R.id.toolbar);
        bar.setSubtitle(subTitle);
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     * Asking for discoverable implies "bluetooth enabled" permission
     */
    private void turnOnDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVER_TIME);
            startActivityForResult(discoverableIntent,REQUEST_ENABLE_DISCOVERY);
        }
    }

    private void askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED))
            {
                ActivityCompat.requestPermissions(BluetoothFriendActivity.this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN
                        },
                        RC_MULTIPLE);
            }
        }
    }

    /**
     * Changes bluetooth name to "MA: username"
     * Starts Bluetooth service
     */
    private void setupThis() {

        if (mNewDeviceName == null) { setNewDeviceName(); }

        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BluetoothFriendService(this, mHandler);
    }

    public void setNewDeviceName() {
        mOldDeviceName = mBluetoothAdapter.getName();
        Log.d(TAG, "mOldDeviceName = "+mOldDeviceName);
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        if (data != null) {
            mNewDeviceName = data.getString("username", mOldDeviceName);
        } else {
            mNewDeviceName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
        Log.d(TAG,"mNewDeviceName = "+mNewDeviceName);
        mBluetoothAdapter.setName(mNewDeviceName);
    }

    private void setOldDeviceName() {
        if (mOldDeviceName!=null && mBluetoothAdapter!=null) {
            mBluetoothAdapter.setName(mOldDeviceName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RC_MULTIPLE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    boolean all = true;
                    for (int i:grantResults) {
                        all = (all && (i == PackageManager.PERMISSION_GRANTED));
                    }
                    if (all) {
                        // permission was granted, yay!
                        Toast.makeText(getApplicationContext(), "Permission GRANTED", Toast.LENGTH_SHORT).show();
                        setupThis();
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bt_request_send: // Without pairing for now
                // in case we are connected
                disconnectDevice();
                flag_requester = true;
                // start searching for other devices
                Intent serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                break;
            case R.id.request_accept:
                acceptRequest();
                break;
            case R.id.request_deny:
                denyRequest();
                break;
        }
    }

    private void clearRequestUI(){
        // Because simple does not work.
        mPic.setImageDrawable(defaultPic);
        mName1.setText(getString(R.string.username));
        mName2.setText(getString(R.string.full_name_filler));
        findViewById(R.id.bt_received_request).setVisibility(View.GONE);
    }

    private void disconnectDevice() {
        Log.d(TAG,"disconnectDevice");
        if (btService.getState() != BluetoothFriendService.STATE_NONE)
            btService.stop();
        secret = null;
        theirSecret = null;

        clearRequestUI();

        if (btService.getState() == BluetoothFriendService.STATE_NONE)
            btService.start();
    }

    /**
     * ***SENDER
     * Generate pushID = key
     * Send out request
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void sendRequest() {
        Log.d(TAG,"sendRequest");
        if (btService.getState() != BluetoothFriendService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = refRequest.push(); // generates the unique push key
        secret = ref.getKey(); // gets that key
        ref.setValue(FirebaseAuth.getInstance().getCurrentUser().getUid()) //pushKey -> myUid
                .addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void aVoid) {
                byte[] message = secret.getBytes();
                btService.write(message);
                Log.d(TAG,"sent>> " + secret);
                status_received.setText("Request sent, waiting...");
            }
        });
    }

    /**
     * ***RECEIVER
     * Reads profile of the requested friend
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void receivedRequest(String key) {
        secret = key;
        Log.d(TAG,"received>> " + key);
        status_received.setText("Request received. Loading...");

        final boolean areFriends[] = {false};
        FirebaseDatabase.getInstance().getReference().child(Constants.FRIENDS).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(theirSecret)
                .addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Log.d(TAG, "Already friends.");
                    areFriends[0] = true;
                    status_received.setText("Already friends");
                    if (btService.getState() == BluetoothFriendService.STATE_CONNECTED) {
                        btService.write(Constants.ACCEPTED.getBytes());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG,databaseError.getMessage());
            }
        });
        if (areFriends[0]) return;

        // Read users UID from key
        refRequest.child(key).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                theirSecret = (String) dataSnapshot.getValue();

                // Load profile from the person that requested friendship and display it
                DatabaseReference ref = mFirebaseDatabase.getReference().child("users").child(theirSecret);
                ref.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {

                            final ProfileBean received = dataSnapshot.getValue(ProfileBean.class);

                            final File image = new File(getCacheDir(), "someone" + ".jpg");
                            if (image.exists()) image.delete();
                            FirebaseStorage.getInstance().getReferenceFromUrl(received.getPhotoUrl()).getFile(image).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                    String temp = status_received.getText().toString();
                                    status_received.setText(temp.concat(" Done!"));

                                    mName1.setText(received.getUsername());
                                    mName2.setText("(" + received.getName() + " " + received.getLastName() + ")");
                                    if (task.isSuccessful()) {
                                        mPic.setImageURI(Uri.fromFile(image));
                                        Log.d(TAG,"Successfullly downloaded image");
                                    } else {
                                        Log.e(TAG,"Failed to download image");
                                    }
                                    findViewById(R.id.bt_received_request).setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
                status_received.setText("Invalid request. Dismissed.");
                disconnectDevice();
            }
        });
    }

    /**
     * ***RECEIVER
     * Changes <key > uuid> to myUid
     * Sends out accepted signal
     * Adds user to friends list
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void acceptRequest() {
        if (btService.getState() != BluetoothFriendService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (theirSecret == null) { // something is wrong
            Log.e(TAG,"No profile links found.");
            return;
        }

        refRequest.child(secret).setValue(FirebaseAuth.getInstance().getCurrentUser().getUid()).addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void aVoid) {
                btService.write(Constants.ACCEPTED.getBytes());
                Log.d(TAG,"Request accepted");
                status_received.setText("Request accepted.");
            }
        });

        mFirebaseDatabase.getReference()
                .child("friends")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(theirSecret)
                .setValue(true)
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Friend added!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG,"Friend added!");
                        clearRequestUI();
                    }
                });
    }

    /**
     * Get their uuid from key
     * Adds user to friends list
     * Delete request entry
     *
     * secret = key = pushID
     * their_secret = uuid from the user that ACCEPTED the request
     */
    private void acceptedRequest() {
        refRequest.child(secret).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                theirSecret = (String) dataSnapshot.getValue();

                mFirebaseDatabase.getReference()
                        .child("friends")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(theirSecret)
                        .setValue(true)
                        .addOnSuccessListener(new OnSuccessListener<Void>()
                        {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getApplicationContext(), "Friend added!", Toast.LENGTH_SHORT).show();

                                status_received.setText("Response: accepted");
                                Log.d(TAG,"Response: accepted " + theirSecret);
                                refRequest.child(secret).setValue(null);
                                secret = null;
                                theirSecret = null;
                                disconnectDevice();
                            }
                        });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG,databaseError.getMessage());
                disconnectDevice();
            }
        });
    }

    /**
     * ***RECEIVER
     * Delete request entry
     * Send out denied message
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void denyRequest() {
        btService.write(Constants.DENIED.getBytes());
        Log.d(TAG,"denied");
        status_received.setText("Request denied.");
        refRequest.child(secret).setValue(null);
        secret = null;
        theirSecret = null;
        clearRequestUI();
    }

    /**
     * Register denial
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void deniedRequest() {
        status_received.setText("Response: denied");
        secret = null;
        theirSecret = null;
        disconnectDevice();
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothFriendService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to) +" "+ mConnectedDeviceName);
                            if (flag_requester) {
                                sendRequest();
                                flag_requester = false;
                            }
                            break;
                        case BluetoothFriendService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothFriendService.STATE_LISTEN:
                        case BluetoothFriendService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE: //we are sending a message
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG,writeMessage);
                    break;
                case Constants.MESSAGE_READ: //we received a message
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if (readMessage.equals(Constants.ACCEPTED)) { acceptedRequest(); }
                    else if (readMessage.equals(Constants.DENIED)) { deniedRequest(); }
                    else if (readMessage.equals(Constants.AREFRIENDS)) {
                        status_received.setText("Already friends.");
                        disconnectDevice();
                    }
                    else { receivedRequest(readMessage); }
                    Log.d(TAG, mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME: // when devices are connected
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Log.d(TAG,"Connected to " + mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST: // reports connection issues
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    Log.d(TAG,msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_DISCOVERY:
                // When the request to enable Bluetooth discovery returns
                if (resultCode==RESULT_CANCELED) {
                    Log.d(TAG,"Enable discovery result CANCEL.");
                    //finish();
                    flag_bt = false;
                    bt_menu.getItem(0).setIcon(R.drawable.ic_action_bluetooth);
                    findViewById(R.id.bt_received_request).setVisibility(View.GONE);
                    findViewById(R.id.bt_send_request).setVisibility(View.GONE);
                    status_received.setText("We need bluetooth enabled to find other users.");
                } else {
                    Log.d(TAG, "Enable discovery result OK.");
                    flag_bt = true;
                    bt_menu.getItem(0).setIcon(R.drawable.ic_action_bluetooth_searching);
                    findViewById(R.id.bt_send_request).setVisibility(View.VISIBLE);
                    status_received.setText(R.string.bt_requests_message);
                    setupThis();
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
        }
    }


    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link BluetoothDeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btService.connect(device, secure);
    }
}
