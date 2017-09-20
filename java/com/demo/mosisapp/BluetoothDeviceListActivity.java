package com.demo.mosisapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

//NOTE: ProgressBar causes "D/Surface: Surface::setBuffersDimensions..." logcat spam
// (also indeterminate or not and horizontal or not and adding actual numbered dimensions or not)
//NOTE: for using Bluetooth you need to have permissions to use BLUETOOTH_ADMIN !!!AND!!! ACCESS_FINE_LOCATION
public class BluetoothDeviceListActivity extends AppCompatActivity
{
    private final String TAG = "BtDeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ProgressBar bar;
    private DeviceArrayAdapter mDevAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_list);
        bar = (ProgressBar)findViewById(R.id.progressBarSearching);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize ArrayAdapter for custom ListView
        mDevAdapter = new DeviceArrayAdapter(this, R.layout.item_paired);
        ListView mDevListView = (ListView) findViewById(R.id.all_devices);
        mDevListView.setAdapter(mDevAdapter);
        mDevListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        doDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG,"doDiscovery");

        // Indicate scanning in the title
        bar.setVisibility(View.VISIBLE);
        setTitle(R.string.scanning);


        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            //int test = ((TextView)v).getId();
            String address =((TextView)v.findViewById(R.id.item_paired_devaddress)).getText().toString();

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG,"BroadcastReceiver,onReceive: DEVICE FOUND: "+device.getName());
                mDevAdapter.add(device);

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"BroadcastReceiver,onReceive: ACTION_DISCOVERY_FINISHED "+mDevAdapter.getCount());

                bar.setVisibility(View.INVISIBLE);

                if (mDevAdapter.getCount() == 0) {
                    setTitle(R.string.none_found);
                    findViewById(R.id.title_all_devices).setVisibility(View.INVISIBLE);
                }
                else {
                    setTitle(R.string.title_other_devices);
                }

                // When discovery starts, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                setTitle(R.string.scanning);
                Log.d(TAG,"BroadcastReceiver,onReceive: ACTION_DISCOVERY_STARTED");
            }
        }
    };
}
