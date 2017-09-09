package com.demo.mosisapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Map;

/**
 * Adapts <list item> to display profilePic and Username of top scorers
 * For now, this will read only username and pic
 * TODO update Database to differentiate between private and public profile data
 */

public class ScoreboardListAdapter extends ArrayAdapter<ProfileBean>
{
    public ScoreboardListAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getView(position, convertView, parent);
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_scorelist, parent, false);
        }

        TextView mScore = (TextView)convertView.findViewById(R.id.item_scorelist_score);
        final ImageView mImage = (ImageView) convertView.findViewById(R.id.item_scorelist_icon);
        TextView mUsername = (TextView) convertView.findViewById(R.id.item_scorelist_username);

        ProfileBean beanie = getItem(position);
        assert beanie != null; //TODO handle this
        mUsername.setText(beanie.getUsername());
        mScore.setText(beanie.getReserve());
        boolean isPhoto = beanie.getPhotoUrl() != null;
        if (isPhoto) {
            Glide.with(getContext())
                    .load(beanie.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(mImage);
        }
        return convertView;
    }
}
