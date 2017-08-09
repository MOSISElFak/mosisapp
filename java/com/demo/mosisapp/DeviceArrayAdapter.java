package com.demo.mosisapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeviceArrayAdapter extends ArrayAdapter<BluetoothDevice>
{
    Drawable secure;
    Drawable unsecure;
    int isecure, iunsecure;

    public DeviceArrayAdapter(@NonNull Context context, @LayoutRes int resource) {//}, @NonNull List<BluetoothDevice> objects) {
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

        boolean isPaired = (device.getBondState() != BluetoothDevice.BOND_BONDED);
        mImage.setImageResource(isPaired?isecure:iunsecure);
        //Glide.with(photoImageView.getContext())
        //        .load(message.getPhotoUrl())
        //        .into(photoImageView);
        mDevName.setText(device.getName());
        mDevAddress.setText(device.getAddress());

        return convertView;
    }
}
