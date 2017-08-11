package com.demo.mosisapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuAdapter;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    private FriendsListAdapter mAdapter;

    private ListView mList;
    private Toolbar toolbar;

    AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //refLocation = mFirebaseDatabase.getReference().child("location").child(user.getUid());
        //refFriends = mFirebaseDatabase.getReference().child("friends").child(user.getUid());
        //refUsers = mFirebaseDatabase.getReference().child("users");
        mFirebaseStorage = FirebaseStorage.getInstance();

        // add a local list for uids?
        //List<ProfileBean> profiles = new ArrayList<>();
        //mAdapter = new FriendsListAdapter(this, R.layout.item_profilelist, profiles);
        mAdapter = new FriendsListAdapter(this, R.layout.item_profilelist);
        mList = (ListView)findViewById(R.id.friends_list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnCreateContextMenuListener(this);

        //FirebaseAuth.getInstance().signOut();

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
                            beanie.setReserve(dataSnapshot.getKey());
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
                    // Removal will be detected here too
                    // but I will no longer have access to that friends profile
                    // mAdapter.notifyDataSetChanged();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle(mAdapter.getItem(info.position).getUsername());
        menu.add(0,1,1,"View profile");
        menu.add(0,2,2,"Remove friend");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int pos = info.position;
        if (item.getItemId()==1) {// View
            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra("friend", mAdapter.getItem(pos));
            startActivity(i);
        }
        else if (item.getItemId()==2) { // Delete
            deleteFriend(pos);
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Removes friendship on both sides
     *
     * @param pos is position of friend in list, used to access ProfileBean and users uid
     */
    private void deleteFriend(final int pos) {
        ad = new AlertDialog.Builder(this)
                .setTitle("Delete friend")
                .setMessage("Do you really want to delete this friend?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String secret = mAdapter.getItem(pos).getReserve();
                        if (secret == null) {
                            Toast.makeText(FriendsListActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Remove my entry from their list of friends first as to not block my access
                        DatabaseReference ref = mFirebaseDatabase.getReference().child("friends");
                        ref.child(secret).child(user.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // Now remove from my friends
                                refFriends.child(secret).removeValue().addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(FriendsListActivity.this, "Friend removed!", Toast.LENGTH_SHORT).show();
                                            mAdapter.remove(mAdapter.getItem(pos));
                                            mAdapter.notifyDataSetChanged();
                                            if (mAdapter.getCount()==0) toolbar.setSubtitle("You are now free.");
                                        }
                                        else Toast.makeText(FriendsListActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }})
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
