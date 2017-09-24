package com.demo.mosisapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MyProfileActivity extends AppCompatActivity implements View.OnClickListener
{
    private final String TAG = "MyProfileActivity";
    private ImageView pic;
    private EditText et_username;
    private EditText et_first;
    private EditText et_last;
    private EditText et_phone;
    private Button change;
    private boolean isUpdate = false;

    private static final int REQUEST_IMAGE_CAPTURE = 852;
    private Bitmap imageBitmap;
    private boolean bus=true, bfi=true, bla=true, bph=true; // flags for correctly filled fields
    private boolean buse=false, bfie=false, blae=false, bphe=false; // flags for edited fields

    private FirebaseUser me;
    private DatabaseReference refDB;
    private StorageReference refProfilePics;
    private StorageReference mStorageRefDown, mStorageRefUp;

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

        pic.setOnClickListener(this);
        change.setOnClickListener(this);

        me = FirebaseAuth.getInstance().getCurrentUser();
        refDB = FirebaseDatabase.getInstance().getReference().child("users").child(me.getUid());
        refProfilePics = FirebaseStorage.getInstance().getReference().child("profilePics").child(me.getUid());
        mStorageRefDown = null;
        mStorageRefUp = null;

        initSetUp();
        //MyDebugCheck();

        et_username.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et_username.setError(null);
                bus = (s.toString().trim().length() > 5);
                change.setEnabled(isUpdate && bus && bfi && bla && bph);
            }
            @Override
            public void afterTextChanged(Editable s) {
                buse = true;
            }
        });
        et_first.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et_first.setError(null);
                bfi=(s.toString().trim().length() > 1);
                change.setEnabled(isUpdate && bus && bfi && bla && bph);
            }
            @Override
            public void afterTextChanged(Editable s) {
                bfie = true;
            }
        });
        et_last.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et_last.setError(null);
                bla=(s.toString().trim().length() > 1);
                change.setEnabled(isUpdate && bus && bfi && bla && bph);
            }
            @Override
            public void afterTextChanged(Editable s) {
                blae = true;
            }
        });
        et_phone.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et_phone.setError(null);
                bph=(s.length() > 7);
                change.setEnabled(isUpdate && bus && bfi && bla && bph);
            }
            @Override
            public void afterTextChanged(Editable s) {
                bphe = true;
            }
        });
    }

    private void initSetUp() {
        pic.setClickable(false);
        et_username.setEnabled(false);
        et_first.setEnabled(false);
        et_last.setEnabled(false);
        et_phone.setEnabled(false);

        et_username.setText(me.getDisplayName());

        File img = createImageFile("profile");
        SharedPreferences sp = getSharedPreferences("basic", MODE_PRIVATE);
        String savedname = sp.getString("username","boo");

        if (savedname.equals(me.getDisplayName())) {    // Saved data matches logged in user
            // Check if the file exists on device
            if (img.exists()){
                pic.setImageURI(Uri.fromFile(img));
            }
            else{
                downloadImage(img);
            }
        } else {                                        // Saved data DOESN'T match logged in user
            downloadImage(img);
        }

        if (!loadPreferences())
        {
            refDB.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ProfileBean profile = dataSnapshot.getValue(ProfileBean.class);
                    if (profile.getName() != null) et_first.setText(profile.getName());
                    if (profile.getLastName() != null) et_last.setText(profile.getLastName());
                    if (profile.getPhone() != null) et_phone.setText(profile.getPhone());
                    savePreferences(profile);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG,databaseError.getMessage());
                }
            });
        }
    }

    private void downloadImage(File img){
        Log.d(TAG, "in downloadImage");
        // If the file doesn't exist, download it and set
        final Uri address = Uri.fromFile(img);
        StorageReference photoRef = refProfilePics.child("profile");
        Log.d(TAG, "Downloading photo...");
        mStorageRefDown = photoRef;
        photoRef.getFile(img).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                mStorageRefDown = null;
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Image download failed!");
                    pic.setImageResource(R.drawable.logo_fsociety_smal);
                } else {
                    Log.d(TAG, "Image downloaded");
                    pic.setImageURI(address);
                }
            }
        });
    }

    private void setUp()
    {
        Log.d(TAG, "in setUp");
        pic.setClickable(false);
        et_username.setEnabled(false);
        et_first.setEnabled(false);
        et_last.setEnabled(false);
        et_phone.setEnabled(false);
        File img = createImageFile("profile");
        if (img.exists()){ pic.setImageURI(Uri.fromFile(img)); }
        else { downloadImage(img); }
        et_username.setText(me.getDisplayName());
        loadPreferences();
    }

    private boolean loadPreferences() {
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        if(data!=null && (data.contains("first"))) {
            String temp = getString(R.string.default_text);
            et_first.setText(data.getString("first",temp));
            et_last.setText(data.getString("last",temp));
            et_phone.setText(data.getString("phone",temp));
            return true;
        } else
            return false;
    }

    private void savePreferences(ProfileBean me) {
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        data.edit().putString("username",me.getUsername())
                .putString("first",me.getName())
                .putString("last",me.getLastName())
                .putString("phone",me.getPhone())
                .apply();
    }

    private void clearPreferences() {
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        data.edit().clear().apply();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.mine_change_button):
                if (isUpdate) {
                    updateMe();
                    setUp();
                    isUpdate = false;
                    change.setText(getString(R.string.mine_change_label));
                    // Flag reset must be after accessing EditText fields
                    buse=false;
                    bfie=false;
                    blae=false;
                    bphe=false;
                }
                else {
                    isUpdate = true;
                    pic.setClickable(true);
                    et_username.setEnabled(true);
                    et_first.setEnabled(true);
                    et_last.setEnabled(true);
                    et_phone.setEnabled(true);
                    change.setText(getString(R.string.mine_update_label));
                }
                break;
            case (R.id.MyProfilePic):
                checkCameraPermission();
                takeMyPic();
        }
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            if (ContextCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MyProfileActivity.this, new String[]{android.Manifest.permission.CAMERA}, Constants.RC_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED && requestCode==Constants.RC_CAMERA){
            takeMyPic();
        }
    }


    /**
     * Takes a picture, and returns resulting image in Bundle.Extra."data"
     * Thumbnail size image!
     */
    private void takeMyPic() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) { //if you call startActivityForResult() using an intent that no app can handle, your app will crash
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile(String name) {
        return new File(getFilesDir(), name + ".jpg"); //    /data/data/com.demo.mosisapp/files/profile.jpg
    }

    // The Android Camera application encodes the photo in the return Intent delivered to onActivityResult()
    // as a small Bitmap in the extras, under the key "data". It is thumbnail size image.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            pic.setImageBitmap(imageBitmap);
        } else {
            File img = createImageFile("profile");
            if (img.exists()){
                pic.setImageURI(Uri.fromFile(img));
                imageBitmap = null;
            }
        }
    }

    private Uri saveImage(Bitmap imageBitmap, String name) throws IOException {
        FileOutputStream osfile = null;
        File image;
        Uri photoURI;
        try {
            image = createImageFile(name);
            osfile = new FileOutputStream(image);
            boolean bla = imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, osfile);
            if (bla)
            {// Continue only if the File was successfully created
                photoURI = Uri.fromFile(image);
                Log.d(TAG, "photoURI: "+photoURI.toString());
            }
        } finally {
            if (osfile != null)
                osfile.close();
        }
        return Uri.fromFile(image);
    }

    private boolean isPhoneValid(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    private void updateMe() {
        if (!(buse||bfie||blae||bphe)) return;

        View focusView = null;
        boolean cancel = false;
        final String phone = et_phone.getText().toString();
        final String usern = et_username.getText().toString().trim();
        final String first = et_first.getText().toString().trim();
        final String last = et_last.getText().toString().trim();

        if (!isPhoneValid(phone)) {
            Log.d(TAG, "Phone is invalid");
            et_phone.setError(getString(R.string.error_invalid_phone));
            focusView = et_phone;
            cancel = true;
        }
        if (last.length() < 2) { // "Al" is valid
            et_last.setError(getString(R.string.error_short));
            focusView = et_last;
            cancel = true;
        }
        if (first.length() < 2) { // "Al" is valid
            et_first.setError(getString(R.string.error_short));
            focusView = et_first;
            cancel = true;
        }
        if (usern.length() < 6) {
            et_username.setError(getString(R.string.error_short));
            focusView = et_username;
            cancel = true;
        }
        if (cancel) {
            focusView.requestFocus();
            return;
        }

        Log.d(TAG,"buse: "+buse);
        Log.d(TAG,"bfie: "+bfie);
        Log.d(TAG,"blae: "+blae);
        Log.d(TAG,"bphe: "+bphe);

        final HashMap<String, Object> result = new HashMap<>();
        if (buse) result.put("username", usern);
        if (bfie) result.put("name", first);
        if (blae) result.put("lastName", last);
        if (bphe) result.put("phone", phone);

        // Update only text fields
        if (imageBitmap == null)
        {
            clearPreferences(); // Update is async, so clearing will prevent reading old data
            ProfileBean profile = new ProfileBean(me.getPhotoUrl().toString(), usern, first, last, phone, null);
            savePreferences(profile);

            Log.d(TAG, "Updating profile...");
            if (buse) {
                Log.d(TAG,"Updating profile name.");
                me.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(usern)
                        .build())
                        .addOnCompleteListener(new OnCompleteListener<Void>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) Log.d(TAG, "Profile updated!");
                                else Log.e(TAG, "Profile update failed!");
                            }
                        });
            } else {
                Log.d(TAG,"Updating profile skipped");
            }

            Log.d(TAG, "Updating database...");
            if (!result.isEmpty()){
                refDB.updateChildren(result).addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) Log.d(TAG, "Updated database!");
                        else Log.e(TAG, "Database update failed!");
                    }
                });
            } else {
                Log.d(TAG,"Updating database skipped");
            }
        }
        else // Upload photo and text fields
        {
            Uri tempUri;
            // Save the Image to local file
            try {
                tempUri = saveImage(imageBitmap, "profile");
            } catch (IOException e) {
                e.printStackTrace();
                return; //check permissions?
            }
            clearPreferences(); // Update is async, so clearing will prevent reading old data
            savePreferences(new ProfileBean(null, usern, first, last, phone, null));
            Log.d(TAG,"Uploading photo...");
            // Upload file to Firebase Storage
            StorageReference photoRef = refProfilePics.child("profile");
            mStorageRefUp = photoRef; // backup
            photoRef.putFile(tempUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imageBitmap = null;
                            mStorageRefUp = null;
                            // When the image has successfully uploaded, we get its download URL
                            @SuppressWarnings("VisibleForTests")
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            result.put("photoUrl", downloadUrl.toString());
                            // Set the download URL and update Auth profile
                            // Here I could skip adding username
                            me.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(usern)
                                    .setPhotoUri(downloadUrl)
                                    .build())
                                    .addOnSuccessListener(new OnSuccessListener<Void>()
                                    {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG,"Profile updated with image");
                                        }
                                    });
                            // Set the download URL in object so that the user can send it to the database
                            refDB.updateChildren(result).addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) Log.d(TAG,"Updated database!");
                                    else Log.e(TAG, "Database update failed!");
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MyProfileActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                            Log.e(TAG,"Uploading photo failed: "+e.getMessage());
                        }
                    });
        }
    }

    /** Uploads/Download continue in the background even after activity lifecycle changes.
     *    If your process is shut down, any uploads/downloads in progress will be interrupted.
     *    However, you can continue uploading once the process restarts by resuming the upload session with the server. (via getUploadSessionUri)
     *    This can save time and bandwidth by not starting the upload from the start of the file.
     *    But I upload one small image
     */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If there's a download in progress, save the reference so you can query it later
        if ( mStorageRefDown != null) {
            outState.putString("download", mStorageRefDown.toString());
        }
        // If there's an upload in progress, save the reference so you can query it later
        if (mStorageRefUp != null) {
            outState.putString("upload", mStorageRefUp.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // If there was a download in progress, get its reference and create a new StorageReference
        final String stringRefDown = savedInstanceState.getString("download");
        if (stringRefDown == null) {
            return;
        }
        mStorageRefDown = FirebaseStorage.getInstance().getReferenceFromUrl(stringRefDown);

        // Find all DownloadTasks under this StorageReference (in this example, there should be one)
        List<FileDownloadTask> tasksDown = mStorageRefDown.getActiveDownloadTasks();
        if (tasksDown.size() > 0) {
            // Get the task monitoring the download
            FileDownloadTask taskDown = tasksDown.get(0);

            // Add new listeners to the task using an Activity scope
            taskDown.addOnSuccessListener(this, new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot state) {
                    mStorageRefDown = null;
                }
            });
        }

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRefUp = savedInstanceState.getString("upload");
        if (stringRefUp == null) {
            return;
        }
        mStorageRefUp = FirebaseStorage.getInstance().getReferenceFromUrl(stringRefUp);

        // Find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasksUp = mStorageRefUp.getActiveUploadTasks();
        if (tasksUp.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasksUp.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {
                    mStorageRefUp = null;
                }
            });
        }
    }
}
