package com.demo.mosisapp;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// TODO remove sharedPreferences on app logout
// TODO remove profilePic on app logout
// Subscribing your listeners with an activity scope to automatically unregister them when the activity stops

public class MyProfileActivity extends AppCompatActivity implements View.OnClickListener
{
    private TextView screamer; String green="#00C853"; String red="#D32F2F";
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
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference refDB;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference refProfilePics;
    private StorageReference mStorageRefDown, mStorageRefUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        screamer = (TextView)findViewById(R.id.screamer);
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
        ContextWrapper cw = new ContextWrapper(this);
        String a1 = cw.getFilesDir().getPath();         //    /data/data/com.demo.mosisapp/files
        String a2 = cw.getApplicationInfo().dataDir;    //    /data/data/com.demo.mosisapp
        String a3 = Environment.getExternalStorageDirectory().getPath(); //    /storage/sdcard
        String a4 = Environment.getExternalStorageDirectory().getAbsolutePath(); //    /storage/sdcard

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
                bla=(s.toString().trim().length() > 2);
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
                bph=(s.toString().trim().length() > 8);
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

        if (me.getPhotoUrl() != null) loadImage();

        if (me.getDisplayName() != null) et_username.setText(me.getDisplayName());
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
                    // check for permission?
                }
            });
        }
    }

    private void setUp()
    {
        pic.setClickable(false);
        et_username.setEnabled(false);
        et_first.setEnabled(false);
        et_last.setEnabled(false);
        et_phone.setEnabled(false);
        buse=false;bfie=false;blae=false;bphe=false;

        if (me.getPhotoUrl() != null) loadImage();

        if (me.getDisplayName() != null) et_username.setText(me.getDisplayName());
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
        // One of the major differences: getPreferences () returns a file only related to the activity it is opened from.
        // While getDefaultSharedPreferences () returns the application's global preferences.
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putString("username",me.getUsername());
        editor.putString("first",me.getName());
        editor.putString("last",me.getLastName());
        editor.putString("phone",me.getPhone());
        editor.commit();
    }

    private void clearPreferences() {
        SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.clear();
        editor.commit();
    }

    private void MyDebugCheck() {
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        File directory = new File(path);
        File[] files = directory.listFiles();
        System.out.println("Files, Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            System.out.println("Files, FileName:" + files[i].getName());
            //files[i].delete();
        }
        System.out.println("MyDebugCheck completed");
    }

    /**
     * Called when Auth.getPhotoUrl == true
     * Checks if file is already downloaded and displays it. Downloads file if needed.
     */
    private void loadImage() {
        // Check if the file exists
        File img = createImageFile("profile");
        if (img.exists()) {
            pic.setImageURI(Uri.fromFile(img));
        } else {
            // If the file doesn't exist, download it and set
            final Uri address = Uri.fromFile(img);
            StorageReference photoRef = refProfilePics.child("profile");
            screamer.setText("Downloading photo...");screamer.setTextColor(Color.parseColor(red));
            mStorageRefDown = photoRef;
            photoRef.getFile(img).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>()
            {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    mStorageRefDown = null;
                    if (!task.isSuccessful()) {
                        pic.setImageResource(R.drawable.logo_fsociety_smal);screamer.setText("Image downloaded!");screamer.setTextColor(Color.parseColor(green));
                    } else {
                        pic.setImageURI(address);screamer.setText("Image download failed!");screamer.setTextColor(Color.parseColor(red));
                    }
                }
            });
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState(); //mounted
        return (Environment.MEDIA_MOUNTED.equals(state));
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
                takeMyPic();
        }
    }


    /*
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
        //String imageFileName = "profile";
        File storageDir;

        if (isExternalStorageWritable())
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //  /storage/sdcard/Android/data/<package>/files/Pictures/profile.jpg
        else
            storageDir = getFilesDir();
        File image = new File(storageDir, name + ".jpg"); //    /data/data/com.demo.mosisapp/files/profile.jpg
        return image;

    }

    private File createImageFileUnique() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir;
        if (isExternalStorageWritable())
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //  /storage/sdcard/Android/data/<package>/files/Pictures/profile.jpg
        else
            storageDir = getFilesDir();
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        String mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // The Android Camera application encodes the photo in the return Intent delivered to onActivityResult()
    // as a small Bitmap in the extras, under the key "data". It is thumbnail size image.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            pic.setImageBitmap(imageBitmap);
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
                System.out.println(photoURI.toString());
            }
        } finally {
            if (osfile != null)
                osfile.close();
        }
        return Uri.fromFile(image);
    }

    private void updateMeOld() {
        if (!(buse||bfie||blae||bphe)) return;

        final String usern = et_username.getText().toString().trim();
        final String first = et_first.getText().toString().trim();
        final String last = et_last.getText().toString().trim();
        final String phone = et_phone.getText().toString();

        // Update only text fields
        if (imageBitmap == null)
        {
            clearPreferences(); // Update is async, so clearing will prevent reading old data
            ProfileBean profile = new ProfileBean(me.getPhotoUrl().toString(), usern, first, last, phone, null);
            savePreferences(profile);
            screamer.setText("Updating storage data...");screamer.setTextColor(Color.parseColor(red));
            refDB.setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    screamer.setText("Updated storage data!");screamer.setTextColor(Color.parseColor(green));
                }
            });
            screamer.setText("Updating profile...");screamer.setTextColor(Color.parseColor(red));
            me.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(usern)
                    .build())
                    .addOnSuccessListener(new OnSuccessListener<Void>()
                    {
                        @Override
                        public void onSuccess(Void aVoid) {
                            screamer.setText("Profile updated!");screamer.setTextColor(Color.parseColor(green));
                        }
                    });

        }
        else //upload photo and text fields
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
            savePreferences(new ProfileBean(null, usern, first,last,phone, null));
            screamer.setText("Uploading photo...");screamer.setTextColor(Color.parseColor(red));
            // Upload file to Firebase Storage
            StorageReference photoRef = refProfilePics.child("profile");
            mStorageRefUp = photoRef; // backup
            photoRef.putFile(tempUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imageBitmap = null;
                            // When the image has successfully uploaded, we get its download URL
                            @SuppressWarnings("VisibleForTests")
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            mStorageRefUp = null;
                            // Set the download URL and update Auth profile
                            me.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(usern)
                                    .setPhotoUri(downloadUrl)
                                    .build())
                                    .addOnSuccessListener(new OnSuccessListener<Void>()
                                    {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            screamer.setText("Profile updated with image");screamer.setTextColor(Color.parseColor(green));
                                        }
                                    });
                            // Set the download URL in object so that the user can send it to the database
                            ProfileBean profile = new ProfileBean(downloadUrl.toString(), usern, first, last, phone, null);
                            refDB.setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    screamer.setText("storage data updated with image");screamer.setTextColor(Color.parseColor(green));
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MyProfileActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                            screamer.setText("Uploading photo failed");screamer.setTextColor(Color.parseColor(red));
                        }
                    });
        }
    }

    private void updateMe() {
        if (!(buse||bfie||blae||bphe)) return;

        final String usern = et_username.getText().toString().trim();
        final String first = et_first.getText().toString().trim();
        final String last = et_last.getText().toString().trim();
        final String phone = et_phone.getText().toString();

        // Update only text fields
        if (imageBitmap == null) {
            clearPreferences(); // Update is async, so clearing will prevent reading old data
            ProfileBean profile = new ProfileBean(me.getPhotoUrl().toString(), usern, first, last, phone, null);
            savePreferences(profile);
            screamer.setText("Updating storage data...");
            screamer.setTextColor(Color.parseColor(red));
            refDB.setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    screamer.setText("Updated storage data!");
                    screamer.setTextColor(Color.parseColor(green));
                }
            });
            screamer.setText("Updating profile...");
            screamer.setTextColor(Color.parseColor(red));
            if (buse) {
                me.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(usern)
                        .build())
                        .addOnSuccessListener(new OnSuccessListener<Void>()
                        {
                            @Override
                            public void onSuccess(Void aVoid) {
                                screamer.setText("Profile updated!");
                                screamer.setTextColor(Color.parseColor(green));
                            }
                        });
            }
        }
        else //upload photo and text fields
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
            screamer.setText("Uploading photo...");
            screamer.setTextColor(Color.parseColor(red));
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
                            // Set the download URL and update Auth profile
                            me.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(usern)
                                    .setPhotoUri(downloadUrl)
                                    .build())
                                    .addOnSuccessListener(new OnSuccessListener<Void>()
                                    {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            screamer.setText("Profile updated with image");
                                            screamer.setTextColor(Color.parseColor(green));
                                        }
                                    });
                            // Set the download URL in object so that the user can send it to the database
                            ProfileBean profile = new ProfileBean(downloadUrl.toString(), usern, first, last, phone, null);
                            refDB.setValue(profile).addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    screamer.setText("storage data updated with image");
                                    screamer.setTextColor(Color.parseColor(green));
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MyProfileActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                            screamer.setText("Uploading photo failed");
                            screamer.setTextColor(Color.parseColor(red));
                        }
                    });
        }
    }

    /** Uploads/Download continue in the background even after activity lifecycle changes.
     *    If your process is shut down, any uploads/downloads in progress will be interrupted.
     *    However, you can continue uploading once the process restarts by resuming the upload session with the server. (via getUploadSessionUri)
     *    This can save time and bandwidth by not starting the upload from the start of the file.
     *    But I upload one small image and I don't care.
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
