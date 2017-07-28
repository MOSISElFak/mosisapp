package com.demo.mosisapp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.Date;

// ProfileBean should be saved as an application variable (?requires internet) or persistently (?different users) for offline access
// Updates could be atomic to save data
public class MyProfileActivity extends AppCompatActivity implements View.OnClickListener
{
    private ImageView pic;
    private EditText et_username;
    private EditText et_first;
    private EditText et_last;
    private EditText et_phone;
    private Button change;
    private boolean isUpdate = false;

    private static final int REQUEST_IMAGE_CAPTURE = 852;
    private Bitmap imageBitmap;

    private FirebaseUser me;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference refDB;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference refProfilePics;

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

        setUp();
    }

    private void setUp()
    {
        pic.setClickable(false);
        et_username.setEnabled(false);
        et_first.setEnabled(false);
        et_last.setEnabled(false);
        et_phone.setEnabled(false);

        if (me.getPhotoUrl() != null) loadImage();
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

private void cleanup() {
        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        File directory = new File(path);
        File[] files = directory.listFiles();
        System.out.println("Files, Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            System.out.println("Files, FileName:" + files[i].getName());
            //files[i].delete();
        }
        System.out.println("cleanup completed");
    }

    /*
     * Called when Auth.getPhotoUrl == true
     * Checks if file is already downloaded and displays it. Downloads file if needed.
     */
    private void loadImage() {
        // Check if the file exists
        File img = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/profile.jpg"); //  /storage/sdcard/Android/data/<package>/files/Pictures/profile.jpg
        if (img.exists()) {
            cleanup();
            pic.setImageURI(Uri.fromFile(img));
        }
        else
        {
            cleanup();
            // If the file doesn't exist, download it and set
            final Uri address = Uri.fromFile(img);
            StorageReference photoRef = refProfilePics.child("profile");
            photoRef.getFile(img).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>()
            {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        if (!task.isSuccessful()) {
                            pic.setImageResource(R.drawable.logo_fsociety_smal);
                        } else {
                            pic.setImageURI(address);
                        }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.mine_change_button):
                if (isUpdate) {
                    if (!checkUpPass()) break;
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

    private File createImageFile() {
        String imageFileName = "profile";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, imageFileName + ".jpg");
        return image;
    }

    private File createImageFileUnique() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

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

    private Uri saveImage(Bitmap imageBitmap) throws IOException {
        FileOutputStream osfile = null;
        File image;
        Uri photoURI;
        try {
            image = createImageFile();
            osfile = new FileOutputStream(image);
            boolean bla = imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, osfile);
            if (bla==true && image!=null)
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

    private boolean checkUpPass() {
        // check username, all else can be null
        String usern = et_username.getText().toString().trim();
        if (me.getDisplayName() == usern || usern.length() < 6) {
            et_username.setError(getString(R.string.error_invalid_username));
            et_username.requestFocus();
            return false;
        } else
            return true;
    }

    private void updateMe() {
        final String usern = et_username.getText().toString().trim();
        final String first = et_first.getText().toString().trim();
        final String last = et_last.getText().toString().trim();
        final String phone = et_phone.getText().toString();

        // Update only text fields
        if (imageBitmap == null)
        {
            me.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(usern)
                    .build())
                    .addOnSuccessListener(new OnSuccessListener<Void>()
                    {
                        @Override
                        public void onSuccess(Void aVoid) {
                            ProfileBean profile = new ProfileBean(me.getPhotoUrl().toString(), usern, first, last, phone);
                            refDB.setValue(profile);
                        }
                    });
        }
        else //upload photo and text fields
        {
            Uri tempUri;
            // Save the Image to local file
            try {
                tempUri = saveImage(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                return; //check permissions?
            }
            // Upload file to Firebase Storage
            StorageReference photoRef = refProfilePics.child("profile");
            photoRef.putFile(tempUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // When the image has successfully uploaded, we get its download URL
                            @SuppressWarnings("VisibleForTests") final
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            // Set the download URL to the message box, so that the user can send it to the database
                            me.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(usern)
                                    .setPhotoUri(downloadUrl)
                                    .build())
                                    .addOnSuccessListener(new OnSuccessListener<Void>()
                                    {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            ProfileBean profile = new ProfileBean(downloadUrl.toString(), usern, first, last, phone);
                                            refDB.setValue(profile);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MyProfileActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
