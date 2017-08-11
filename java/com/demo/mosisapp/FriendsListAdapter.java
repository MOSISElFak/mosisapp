package com.demo.mosisapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FriendsListAdapter extends ArrayAdapter<ProfileBean>
{
    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * objects  The objects to represent in the ListView.
     */
    public FriendsListAdapter(Context context, int resource/*, @NonNull List<ProfileBean> objects*/) {
            super(context, resource/*, objects*/);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_profilelist, parent, false);
        }

        ImageView mImage = (ImageView) convertView.findViewById(R.id.item_plist_icon);
        TextView mUsername = (TextView) convertView.findViewById(R.id.item_plist_username);
        TextView mName = (TextView) convertView.findViewById(R.id.item_plist_name);

        ProfileBean beanie = getItem(position);
        mUsername.setText(beanie.getUsername());
        String name = "("+beanie.getName()+" "+beanie.getLastName()+")";
        mName.setText(name);
        boolean isPhoto = beanie.getPhotoUrl() != null;
        if (isPhoto) {
            Glide.with(getContext())
                    .load(beanie.getPhotoUrl())
                    .into(mImage);
        }
        return convertView;
    }
}
