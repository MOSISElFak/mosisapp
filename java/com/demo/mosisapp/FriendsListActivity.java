package com.demo.mosisapp;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuAdapter;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
    //FIREBASE AUTHENTICATION
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    //FIREBASE REALTIME DATABASE
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference refLocation;
    private DatabaseReference refFriends;
    private DatabaseReference refUsers;
    private ChildEventListener mChildEventListener;
    //FIREBASE STORAGE
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;

    private FriendsListAdapter mAdapter;

    private ListView mList;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = FirebaseAuth.getInstance().getCurrentUser();
        String name = user.getDisplayName();
        String uuid = user.getUid();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //refLocation = mFirebaseDatabase.getReference().child("location").child(user.getUid());
        //refFriends = mFirebaseDatabase.getReference().child("friends").child(user.getUid());
        //refUsers = mFirebaseDatabase.getReference().child("users");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = mFirebaseStorage.getReference().child("profilePics");

        mAdapter = new FriendsListAdapter(this, R.layout.item_profilelist);
        mList = (ListView)findViewById(R.id.friends_list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChildEventListener == null)
            attachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachDatabaseReadListener();
        mAdapter.clear();
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener!=null) {
            refFriends.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void attachDatabaseReadListener() {
        toolbar.setSubtitle("Populating...");
        //Map<String, Object> td = (HashMap<String,Object>) dataSnapshot.getValue();

        refFriends = mFirebaseDatabase.getReference().child("friends").child(user.getUid());
        refUsers = mFirebaseDatabase.getReference().child("users");

        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener()
            {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    // KEY(uid):VALUE(true|false)
                    String friendsid = dataSnapshot.getKey();
                    refUsers.child(friendsid).addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            ProfileBean beanie = dataSnapshot.getValue(ProfileBean.class);
                            mAdapter.add(beanie);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            String error = databaseError.getMessage();
                            toolbar.setSubtitle(error);
                        }
                    });

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
                    String error = databaseError.getMessage();
                    toolbar.setSubtitle(error);
                }
            };
            refFriends.addChildEventListener(mChildEventListener);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String temp = ((TextView)view.findViewById(R.id.item_plist_username)).getText().toString();
        toolbar.setSubtitle(temp);

        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra("friend", mAdapter.getItem(position));
        startActivity(i);
        //mAdapter.getItem(position)....;

        //Intent intent = new Intent(...);
        //String EXTRA_MID = "myuid";
        //intent.putExtra(EXTRA_MID, temp);
        //startActivity(intent);
    }
}
