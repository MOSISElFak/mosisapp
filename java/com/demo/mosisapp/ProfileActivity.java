package com.demo.mosisapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity
{
    // Layout
    private ImageView mPic;
    private TextView mUsername;
    private TextView mName;
    private TextView mLast;
    private TextView mPhone;

    ProfileBean friend = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        friend = (ProfileBean) getIntent().getSerializableExtra("friend");
        if (friend==null) finish();

        mPic = (ImageView)findViewById(R.id.profile_pic);
        mUsername = (TextView)findViewById(R.id.profile_username);
        mName = (TextView)findViewById(R.id.profile_name);
        mLast = (TextView)findViewById(R.id.profile_last);
        mPhone = (TextView)findViewById(R.id.profile_phone);

        loadProfile();
    }

    private void loadProfile() {
        Glide.with(this).load(friend.getPhotoUrl()).into(mPic);

        mUsername.setText(friend.getUsername());
        mName.setText(friend.getName());
        mLast.setText(friend.getLastName());
        mPhone.setText(friend.getPhone());
    }

    public void onClick(View view) {
        if (view.getId()==R.id.profile_phone){
            //String phone = "tel:"+ friend.getPhone();
            //Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(phone));
            //startActivity(i);
        }
    }
}
