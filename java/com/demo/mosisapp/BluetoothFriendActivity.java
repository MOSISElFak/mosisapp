package com.demo.mosisapp;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
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

// TODO what if they're already friends
public class BluetoothFriendActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "BluetoothFriendActivity";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 121;
    private static final int REQUEST_ENABLE_DISCOVERY = 122;

    private static final String DENIED = "DENIED";
    private static final String ACCEPTED = "ACCEPTED";

    private static final int DISCOVER_TIME = 300;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 151;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothFriendService btService;

    private Switch main_switch;
    private TextView status;
    private TextView status_message;
    private String mOldDeviceName;
    private String mNewDeviceName;
    //Name of the connected device
    private String mConnectedDeviceName = null;

    private Button mBtnConnect;
    private Button mBtnDisconnect;
    private Button mBtnSendRequest;
    private Button mBtnAcceptRequest;
    private Button mBtnDenyRequest;

    private FirebaseDatabase mFirebaseDatabase; //main access point
    private DatabaseReference refRequest;
    private String secret;
    private String theirSecret;

    private ImageView mPic;
    private TextView mName1;
    private TextView mName2;
    private Drawable defaultPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_friend);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 1. getAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "No Bluetooth device detected",Toast.LENGTH_SHORT).show();
            finish();
        }

        status = (TextView)findViewById(R.id.bt_status);
        status_message = (TextView)findViewById(R.id.bt_status_message);
/*
        main_switch = (Switch)findViewById(R.id.bt_main_switch);
        main_switch.setChecked(mBluetoothAdapter.isEnabled());
        main_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    status.setText("Starting bluetooth...");
                    if (!mBluetoothAdapter.isEnabled()) {
                        //turnOnBluetooth();
                        turnOnDiscoverable();
                    } else
                        setupThis();
                } else {
                    status.setText("Bluetooth off. Bluetooth must be enabled to continue.");
                    if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.disable();
                }
            }
        });
*/
        mBtnSendRequest=(Button)findViewById(R.id.request_send);
        mBtnAcceptRequest=(Button)findViewById(R.id.request_accept);
        mBtnDenyRequest=(Button)findViewById(R.id.request_deny);
        mBtnConnect=(Button)findViewById(R.id.bt_connect);
        mBtnDisconnect=(Button)findViewById(R.id.bt_disconnect);

        mBtnSendRequest.setOnClickListener(this);
        mBtnAcceptRequest.setOnClickListener(this);
        mBtnDenyRequest.setOnClickListener(this);
        mBtnConnect.setOnClickListener(this);
        mBtnDisconnect.setOnClickListener(this);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        refRequest = mFirebaseDatabase.getReference().child("requests");

        mPic = (ImageView)findViewById(R.id.request_pic);
        mName1 = (TextView)findViewById(R.id.request_name1);
        mName2 = (TextView)findViewById(R.id.request_name2);

        // Because simple does not work
        defaultPic = mPic.getDrawable();
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupThis() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            turnOnDiscoverable();
            // Otherwise, setup the chat session
        } else if (btService == null) {
            setupThis();
        }
    }

    public void setNewDeviceName() {
        mOldDeviceName = mBluetoothAdapter.getName();
        String mydeviceaddress = mBluetoothAdapter.getAddress();
        String username = "MA: ";
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        if (data != null && (data.contains("username")))
            username += data.getString("username", mOldDeviceName);
        else username += mOldDeviceName;

        String temp = username + " : " + mydeviceaddress;
        status.setText(temp);
        this.mNewDeviceName = username;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNewDeviceName == null) setNewDeviceName();
        mBluetoothAdapter.setName(mNewDeviceName);

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

        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        if (mOldDeviceName != null) mBluetoothAdapter.setName(mOldDeviceName);
        // TODO change name if you turn off bluetooth
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregisterReceiver(mBroadcastReceiver);
        if (btService != null) {
            btService.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                Toast.makeText(this, "Well you clicked.",Toast.LENGTH_SHORT).show();
                return true;
            /*
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.disconnect: {
                disconnectDevice();
                return true;
            }
            */
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

    private void turnOnBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void turnOnDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVER_TIME);
            startActivityForResult(discoverableIntent,REQUEST_ENABLE_DISCOVERY);
        }
    }

    private void setupThis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(BluetoothFriendActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
        mOldDeviceName = mBluetoothAdapter.getName();
        //String mydeviceaddress = mBluetoothAdapter.getAddress(); // >API23 numbers are assigned random or default 00:20:00...
        //String mydeviceaddress = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "bluetooth_address");
        String username = "MA: ";
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        if (data != null && (data.contains("username")))
            username += data.getString("username", mOldDeviceName);
        else username += FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        mBluetoothAdapter.setName(username);
        String temp = "ME: username + ";
        status.setText(temp);

        // Initialize the BluetoothChatService to perform bluetooth connections
        btService = new BluetoothFriendService(this, mHandler);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    boolean a1 = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (a1) {
                        // permission was granted, yay!
                        Toast.makeText(getApplicationContext(), "Permission GRANTED", Toast.LENGTH_SHORT).show();
                    }
                    setupThis();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(),"Permission denied", Toast.LENGTH_SHORT).show();
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
            case R.id.bt_connect: // Without pairing for now
                //Intent serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
                //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                Intent serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                break;
            case R.id.bt_disconnect:
                disconnectDevice();
                break;
            case R.id.request_send:
                sendRequest();
                break;
            case R.id.request_accept:
                acceptRequest();
                break;
            case R.id.request_deny:
                denyRequest();
                break;
        }
    }

    private void disconnectDevice() {
        if (btService.getState() != BluetoothFriendService.STATE_NONE)
            btService.stop();
        secret = null;
        theirSecret = null;

        // Because simple does not work.
        mPic.setImageDrawable(defaultPic);
        mName1.setText(getString(R.string.username));
        mName2.setText(getString(R.string.full_name_filler));

        mBtnAcceptRequest.setVisibility(View.INVISIBLE);
        mBtnDenyRequest.setVisibility(View.INVISIBLE);

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
        if (btService.getState() != BluetoothFriendService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = refRequest.push(); // generates the unique push key
        secret = ref.getKey(); // gets that key
        ref.setValue(FirebaseAuth.getInstance().getCurrentUser().getUid()) //pushKey > myUid
                .addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void aVoid) {
                byte[] message = secret.getBytes();
                btService.write(message);
                status_message.setText("sent>> " + secret);
                status.setText("Sent request, waiting...");
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
        status.setText("received>> " + key);

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

                            status_message.setText("Loading request...");
                            final ProfileBean received = dataSnapshot.getValue(ProfileBean.class);

                            final File image = new File(getCacheDir(), "someone" + ".jpg");
                            if (image.exists()) image.delete();
                            FirebaseStorage.getInstance().getReferenceFromUrl(received.getPhotoUrl()).getFile(image).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                    status_message.setText("Loading request... Done.");
                                    mName1.setText(received.getUsername());
                                    mName2.setText("(" + received.getName() + " " + received.getLastName() + ")");
                                    mBtnAcceptRequest.setVisibility(View.VISIBLE);
                                    mBtnDenyRequest.setVisibility(View.VISIBLE);

                                    if (task.isSuccessful()) {
                                        mPic.setImageURI(Uri.fromFile(image));
                                        status.setText("Successfullly downloaded image");
                                    } else {
                                        status.setText("Failed to download image");
                                    }
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        status.setText(databaseError.getCode());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                status.setText(databaseError.getCode());
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

        if (theirSecret == null) {
            status_message.setText("No profile links found.");
            return; // something is wrong
        }

        refRequest.child(secret).setValue(FirebaseAuth.getInstance().getCurrentUser().getUid()).addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void aVoid) {
                String message = "accepted";
                btService.write(message.getBytes());
                status_message.setText("Request accepted");
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

                                status.setText("Response: accepted " + theirSecret);
                                refRequest.child(secret).setValue(null);
                                secret = null;
                                theirSecret = null;
                            }
                        });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                status.setText(databaseError.getCode());
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
        String message = "denied";
        btService.write(message.getBytes());
        status.setText("denied");
        refRequest.child(secret).setValue(null);
        secret = null;
        theirSecret = null;
    }

    /**
     * Register denial
     *
     * secret = key = pushID
     * their_secret = uuid from the user that sent the request
     */
    private void deniedRequest() {
        status.setText("Response: denied");
        secret = null;
        theirSecret = null;
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
                            setStatus(getString(R.string.title_connected_to) + mConnectedDeviceName);
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
                    status_message.setText(writeMessage);
                    break;
                case Constants.MESSAGE_READ: //we received a message
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.equalsIgnoreCase("accepted")) acceptedRequest();
                    else if (readMessage.equalsIgnoreCase("denied")) deniedRequest();
                    else receivedRequest(readMessage);
                    status_message.setText(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    status_message.setText("Connected to " + mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    status_message.setText(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    status.setText("Bluetooth enabled. Result OK");
                    setupThis();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    status.setText("Bluetooth must be enabled to continue. CANCEL");
                }
                break;
            case REQUEST_ENABLE_DISCOVERY:
                // When the request to enable Bluetooth discovery returns
                if (resultCode!=RESULT_CANCELED) {
                    status.setText("Enable discovery result OK.");
                    setupThis();
                } else {
                    status.setText("Enable discovery result CANCEL.");
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
