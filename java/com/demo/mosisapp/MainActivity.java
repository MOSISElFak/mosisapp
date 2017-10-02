package com.demo.mosisapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity
{
    private final String TAG = "MainActivity";
    private static final int RC_MAIN = 124;

    //firebase authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int RC_SIGN_IN = 123; //request code
    boolean trickster = false;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //initalize Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();

        if (mAuthStateListener==null) {
            Log.d(TAG, "onAuthStateChanged: create");
            mAuthStateListener = new FirebaseAuth.AuthStateListener()
            {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    Log.d("MainActivity", "onAuthStateChanged");
                    //check if user is logged in...
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null)
                    {
                        Log.d(TAG, "onAuthStateChanged: signed_in:" + user.getUid());
                        MosisApp.getInstance().logoutFlag = false;
                        Intent go = new Intent(MainActivity.this, MapsActivity.class);
                        go.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(go, RC_MAIN);
                    }
                    else
                    {
                        // user is signed out, show logInFlow
                        Log.d(TAG, "onAuthStateChanged: signed out");
                        Intent login = new Intent(MainActivity.this, LoginActivity.class);
                        login.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivityForResult(login, RC_SIGN_IN);
                    }
                }
            };
        }
    }

    private void checkPermission() {
        Log.d(TAG,"checkPermission in MapsActivity");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.RC_LOCATION);
            }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG,"onRequestPermissionsResult");
        if(requestCode==Constants.RC_LOCATION){
            if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED){
                Intent go = new Intent(MainActivity.this, MapsActivity.class);
                go.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(go, RC_MAIN);
            }
            Toast.makeText(getApplicationContext(), "Permission for location denied", Toast.LENGTH_SHORT).show();
            finish(); // just leave
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"onActivityResult");
        if (requestCode == RC_SIGN_IN){
            Log.d(TAG, "onActivityResult: RC_SIGN_IN");
            if (resultCode== Activity.RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
                // and then, onAuthStateListener will handle this and start activity
            }else {
                Log.d(TAG, "Sign in canceled!");
                trickster = true;   // so that we can actually leave
                finish(); // after this it calls onResume->onPause->onAuthStateChanged->onAuthStateChanged: signed out AGAIN
            }
        }
        else if (requestCode == RC_MAIN) {
            Log.d(TAG, "onActivityResult: RC_MAIN");
            if (MosisApp.getInstance().logoutFlag) {
                FirebaseAuth.getInstance().signOut();
            }
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (trickster || MosisApp.getInstance().leaveMapsFlag) { // So that we can leave, because AuthStateListener is causing restart of LoginActivity
            if (mAuthStateListener != null) {
                mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
            }
            MosisApp.getInstance().leaveMapsFlag = false;
            finish();
            return;
        }
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
