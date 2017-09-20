package com.demo.mosisapp;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class DeviceArrayAdapter extends ArrayAdapter<BluetoothDevice>
{
    //Drawable secure;
    //Drawable unsecure;
    private int isecure, iunsecure;

    DeviceArrayAdapter(@NonNull Context context, @LayoutRes int resource) {//}, @NonNull List<BluetoothDevice> objects) {
        super(context, resource);//, objects);

        //unsecure = context.getResources().getDrawable(R.drawable.ic_action_not_secure);
        //secure = context.getResources().getDrawable(R.drawable.ic_action_secure);
        iunsecure = context.getResources().getIdentifier("ic_action_not_secure", "drawable", "com.demo.mosisapp");
        isecure = context.getResources().getIdentifier("ic_action_secure", "drawable","com.demo.mosisapp");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_paired, parent, false);
        }

        ImageView mImage = (ImageView) convertView.findViewById(R.id.item_paired_icon);
        TextView mDevName = (TextView) convertView.findViewById(R.id.item_paired_devname);
        TextView mDevAddress = (TextView) convertView.findViewById(R.id.item_paired_devaddress);

        BluetoothDevice device = getItem(position);

        if (device != null) {
            boolean isPaired = (device.getBondState() != BluetoothDevice.BOND_BONDED);
            mImage.setImageResource(isPaired ? isecure : iunsecure);
            mDevName.setText(device.getName());
            mDevAddress.setText(device.getAddress());
        } else {
            Log.e("DeviceArrayAdapter", "Device is null!");
        }

        return convertView;
    }
}
