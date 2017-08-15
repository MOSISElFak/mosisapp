package com.demo.mosisapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity
{
    // Layout
    private ImageView mPic;
    private TextView mUsername;
    private TextView mName;
    private TextView mLast;
    private TextView mPhone;

    ProfileBean friend = null;
    String friendid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        friend = (ProfileBean) getIntent().getSerializableExtra("friend");
        friendid = getIntent().getStringExtra("key_id");
        if (friendid != null)
            downloadProfile(friendid);
        else if (friend != null)
            loadProfile();
        else {
            Log.d("ProfileActivity", "no extras found");
            finish();
        }
        mPic = (ImageView)findViewById(R.id.profile_pic);
        mUsername = (TextView)findViewById(R.id.profile_username);
        mName = (TextView)findViewById(R.id.profile_name);
        mLast = (TextView)findViewById(R.id.profile_last);
        mPhone = (TextView)findViewById(R.id.profile_phone);


    }

    private void downloadProfile(String friendid) {
        FirebaseDatabase.getInstance()
                .getReference(Constants.USERS).child(friendid)
                .addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        friend = dataSnapshot.getValue(ProfileBean.class);
                        loadProfile();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ProfileActivity",databaseError.getMessage());
                        finish();
                    }
                });
    }

    private void loadProfile() {
        //Glide.with(this)
        //        .load(friend.getPhotoUrl())
        //        .into(mPic);

        GlideApp.with(this)
                .load(friend.getPhotoUrl())
                .onlyRetrieveFromCache(true) // We have to see it to click on it, ensuring it will be in cache
                .into(mPic);

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
