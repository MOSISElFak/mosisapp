package com.demo.mosisapp;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// ProfileBean should be saved as an application variable (?requires internet) or persistently (?different users) for offline access
// Updates could be atomic to save data
public class MyProfileActivity extends AppCompatActivity implements View.OnClickListener
{
    ImageView pic;
    EditText et_username;
    EditText et_first;
    EditText et_last;
    EditText et_phone;
    Button change;
    boolean isUpdate = false;

    FirebaseUser me;
    DatabaseReference refDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        pic = (ImageView) findViewById(R.id.MyProfilePic);
        et_username = (EditText) findViewById(R.id.mine_usern);
        et_first = (EditText) findViewById(R.id.mine_name);
        et_last = (EditText) findViewById(R.id.mine_last);
        et_phone = (EditText)findViewById(R.id.mine_phone);
        change = (Button) findViewById(R.id.mine_change_button);

        //pic.setOnClickListener(this);
        change.setOnClickListener(this);

        me = FirebaseAuth.getInstance().getCurrentUser();
        refDB = FirebaseDatabase.getInstance().getReference().child("users").child(me.getUid());

        setUp();
    }

    private void setUp()
    {
        et_username.setEnabled(false);
        et_first.setEnabled(false);
        et_last.setEnabled(false);
        et_phone.setEnabled(false);

        if (me.getPhotoUrl() != null) pic.setImageURI(me.getPhotoUrl()); // TODO: set this up
        if (me.getDisplayName() != null) et_username.setText(me.getDisplayName());
        refDB.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ProfileBean profile = dataSnapshot.getValue(ProfileBean.class);
                if (profile.getName()!=null) et_first.setText(profile.getName());
                if (profile.getLastName()!=null) et_last.setText(profile.getLastName());
                if (profile.getPhone()!=null) et_phone.setText(profile.getPhone());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // check for permission?
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.mine_change_button):
                if (isUpdate) {
                    UpdateMe();
                    setUp();
                    isUpdate = false;
                    change.setText(getString(R.string.mine_change_label));
                }
                else {
                    isUpdate = true;
                    et_username.setEnabled(true);
                    et_first.setEnabled(true);
                    et_last.setEnabled(true);
                    et_phone.setEnabled(true);
                    change.setText(getString(R.string.mine_update_label));
                }
                break;
        }
    }

    private void UpdateMe() {
        final String usern = et_username.getText().toString().trim();
        final String first = et_first.getText().toString().trim();
        final String last = et_last.getText().toString().trim();
        final String phone = et_phone.getText().toString();

        // check username, all else can be null
        if (me.getDisplayName() != usern && usern.length()>5)
        me.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(usern).build())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ProfileBean profile = new ProfileBean(usern,first,last,phone);
                            refDB.setValue(profile);
                        }
                        else {
                            et_username.setError(getString(R.string.error_invalid_username));
                        }
                    }});
    }
}
