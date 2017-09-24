package com.demo.mosisapp;

import android.*;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.demo.mosisapp.Constants.RC_CAMERA;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    private final String TAG = "LoginActivity";

    private final int REQUEST_IMAGE_CAPTURE = 66;
    private boolean isLogin = true;     // flags for flow control
    private boolean letMeOut = true;
    private boolean forgetmenot = false;
    private boolean mail_set = false;   // flags for enabling buttons
    private boolean pass_set = false;
    private boolean usern_set = false;
    private boolean real_set = false;
    private boolean last_set = false;
    private boolean phone_set = false;
    private boolean pic_set = false;

    private FirebaseAuth mFirebaseAuth;

    // UI references.
    private EditText mEditText_mail;
    private EditText mEditText_pass;
    private EditText mEditText_user;
    private EditText mEditText_realname;
    private EditText mEditText_lastname;
    private EditText mEditText_phone;
    private ProgressDialog progressDialog;
    private TextView mStatus_register;
    private TextView mStatus_signin;
    private Button sign_in_button;
    private Button register_button;
    private TextView forgot_password;

    private ImageView mPic;
    private Bitmap imageBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mPic = (ImageView)findViewById(R.id.inner_logo);
        mPic.setOnClickListener(this);
        mPic.setClickable(false);
        mEditText_mail = (EditText) findViewById(R.id.login_email);
        mEditText_pass = (EditText) findViewById(R.id.login_password);
        mEditText_user = (EditText) findViewById(R.id.register_username);
        mEditText_realname = (EditText)findViewById(R.id.register_realname);
        mEditText_lastname = (EditText)findViewById(R.id.register_lastname);
        mEditText_phone = (EditText)findViewById(R.id.register_phone);
        mStatus_register = (TextView)findViewById(R.id.register_status);
        mStatus_signin = (TextView)findViewById(R.id.sign_in_status);
        sign_in_button = (Button)findViewById(R.id.sign_in_button);
        register_button = (Button)findViewById(R.id.register_button);
        forgot_password = (TextView)findViewById(R.id.forgot_password);

        mEditText_pass.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (isLogin) sign_in_button.callOnClick();
                return false;
            }
        });

        findViewById(R.id.goto_login_button).setOnClickListener(this);
        findViewById(R.id.goto_register_button).setOnClickListener(this);
        sign_in_button.setOnClickListener(this);
        register_button.setOnClickListener(this);
        forgot_password.setOnClickListener(this);
        findViewById(R.id.forgot_continue_button).setOnClickListener(this);

        sign_in_button.setEnabled(false);
        register_button.setEnabled(false);

        mFirebaseAuth = FirebaseAuth.getInstance();

        //now the additional tweaks
        mEditText_mail.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                mEditText_mail.setError(null);
                mStatus_register.setVisibility(View.GONE);
                mStatus_signin.setVisibility(View.GONE);
                if (s.length() > 0) {
                        mail_set = true;
                        if (pass_set && isLogin) sign_in_button.setEnabled(true);
                        else if (!isLogin && pass_set && usern_set && real_set && last_set && phone_set && pic_set)
                            register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mEditText_pass.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditText_pass.setError(null);
                mStatus_register.setVisibility(View.GONE);
                mStatus_signin.setVisibility(View.GONE);
                if (s.length() > 0) {
                   pass_set = true;
                   if (mail_set && isLogin) sign_in_button.setEnabled(true);
                   else if (!isLogin && mail_set && usern_set && real_set && last_set && phone_set && pic_set)
                       register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText_user.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditText_user.setError(null);
                if (s.length() > 0) {
                    usern_set = true;
                    if (!isLogin && mail_set && pass_set && real_set && last_set && phone_set && pic_set)
                        register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText_realname.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditText_realname.setError(null);
                if (s.length() > 0) {
                    real_set = true;
                    if (!isLogin && mail_set && pass_set && usern_set && last_set && phone_set && pic_set)
                        register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText_lastname.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditText_lastname.setError(null);
                if (s.length() > 0) {
                    last_set = true;
                    if (!isLogin && mail_set && pass_set && usern_set && real_set && phone_set && pic_set)
                        register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText_phone.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditText_phone.setError(null);
                if (s.length() > 0) {
                    phone_set = true;
                    if (!isLogin && mail_set && pass_set && usern_set && real_set && last_set && pic_set)
                        register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onBackPressed() { //returns to choice login/register
        if (letMeOut) super.onBackPressed();
        else if(forgetmenot) {
            forgetmenot = false;
            forgot_password.setVisibility(View.VISIBLE);
            findViewById(R.id.login_password_wrap).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.forgot_continue_button).setVisibility(View.GONE);
            findViewById(R.id.message_to_user).setVisibility(View.GONE);
        }
        else {
            letMeOut = true;
            isLogin = false;
            pic_set = false;
            mStatus_register.setVisibility(View.GONE);
            findViewById(R.id.login_flow).setVisibility(View.GONE);
            findViewById(R.id.register_form).setVisibility(View.GONE);
            findViewById(R.id.main_form).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            TextView tv = (TextView) findViewById(R.id.message_to_user);
            tv.setVisibility(View.GONE);
            tv.setText(R.string.message_forgot);
            mPic.setClickable(false);
            mPic.setImageResource(R.drawable.logo_fsociety_smal);
        }
    }

    private void showProgressDialog(boolean is) {
        if (is) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
        } else {
            if (progressDialog!=null) progressDialog.dismiss();
        }
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPhoneValid(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    private boolean isPasswordValid(String password) {
        return (password.length() > 5);
    }

    //LOGIN_FLOW/LOGIN_FORM/REGISTER_FORM + sign_in_register_button
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case (R.id.goto_login_button):
                isLogin = true;
                letMeOut = false;
                findViewById(R.id.main_form).setVisibility(View.GONE);
                findViewById(R.id.login_flow).setVisibility(View.VISIBLE);
                forgot_password.setVisibility(View.VISIBLE);
                mEditText_mail.setText(""); //in case user changes modes login/register
                mEditText_pass.setText("");
                mEditText_user.setText("");
                break;
            case(R.id.goto_register_button):
                isLogin = false;
                letMeOut = false;
                mPic.setImageResource(R.drawable.frame);
                mPic.setClickable(true);
                findViewById(R.id.main_form).setVisibility(View.GONE);
                findViewById(R.id.login_flow).setVisibility(View.VISIBLE);
                findViewById(R.id.register_form).setVisibility(View.VISIBLE);
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                forgot_password.setVisibility(View.GONE);
                mEditText_mail.setText(""); //in case user changes modes login/register
                mEditText_pass.setText("");
                mEditText_user.setText("");
                mEditText_realname.setText("");
                mEditText_lastname.setText("");
                mEditText_phone.setText("");
                break;
            case(R.id.register_button):
                attemptRegister();
                break;
            case(R.id.sign_in_button):
                attemptLogin();
                break;
            case(R.id.forgot_password):
                forgetmenot = true;
                forgot_password.setVisibility(View.GONE);
                findViewById(R.id.login_password_wrap).setVisibility(View.GONE);
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                findViewById(R.id.forgot_continue_button).setVisibility(View.VISIBLE);
                findViewById(R.id.message_to_user).setVisibility(View.VISIBLE);
                break;
            case(R.id.forgot_continue_button):
                resetPassword();
                break;
            case(R.id.inner_logo):
                checkCameraPermission();
                takeMyPic();
                break;
        }
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            if (ContextCompat.checkSelfPermission(LoginActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(LoginActivity.this, new String[]{android.Manifest.permission.CAMERA}, Constants.RC_CAMERA);
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

    private void resetPassword() {
        boolean cancel = false;
        String mail = mEditText_mail.getText().toString();
        if (TextUtils.isEmpty(mail)) {
            mEditText_mail.setError(getString(R.string.error_field_required));
            cancel = true;
        } else if (!isEmailValid(mail)) {
            mEditText_mail.setError(getString(R.string.error_invalid_email));
            cancel = true;
        }
        if (cancel) mEditText_mail.requestFocus();
        else {
            showProgressDialog(true);
            mFirebaseAuth.sendPasswordResetEmail(mail)
                    .addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                forgetmenot = false;
                                letMeOut = false;
                                isLogin = true;

                                mEditText_pass.setText("");
                                //mEditText_pass.setVisibility(View.VISIBLE);
                                forgot_password.setVisibility(View.VISIBLE);
                                findViewById(R.id.login_password_wrap).setVisibility(View.VISIBLE);
                                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                                findViewById(R.id.forgot_continue_button).setVisibility(View.GONE);
                                TextView tv = (TextView) findViewById(R.id.message_to_user);
                                tv.setText(R.string.message_email_sent);
                                tv.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                            }
                            showProgressDialog(false);
                        }
                    });
        }
    }

    private void attemptRegister() {
        View focusView = null;
        boolean cancel = false;

        if (!pic_set) {
            mStatus_register.setText(getString(R.string.error_invalid_picture));
            mStatus_register.setVisibility(View.VISIBLE);
            Log.d(TAG, "Picture not set");
            return;
        } else Log.d(TAG, "Picture set");

        mEditText_mail.setError(null);
        mEditText_pass.setError(null);
        mEditText_user.setError(null);
        mEditText_realname.setError(null);
        mEditText_lastname.setError(null);
        mEditText_phone.setError(null);

        //how secure this really is, add .trim()?
        String mail = mEditText_mail.getText().toString();
        String pass = mEditText_pass.getText().toString();
        final String nuser = mEditText_user.getText().toString().trim();
        final String nfirst = mEditText_realname.getText().toString().trim();
        final String nlast = mEditText_lastname.getText().toString().trim();
        final String phone = mEditText_phone.getText().toString().trim();

        // Field checks
        if (!isPhoneValid(phone)){
            mEditText_phone.setError(getString(R.string.error_invalid_phone));
            focusView = mEditText_phone;
            cancel = true;
            Log.d(TAG, "Phone failed");
        }
        if (nlast.length() < 2) { // "Al" is valid
            mEditText_lastname.setError(getString(R.string.error_short));
            focusView = mEditText_lastname;
            cancel = true;
            Log.d(TAG, "Last name failed");
        }
        if (nfirst.length() < 2) { // "Al" is valid
            mEditText_realname.setError(getString(R.string.error_short));
            focusView = mEditText_realname;
            cancel = true;
            Log.d(TAG, "First name failed");
        }
        if (nuser.length() < 6) {
            mEditText_user.setError(getString(R.string.error_short));
            focusView = mEditText_user;
            cancel = true;
            Log.d(TAG, "Username failed");
        }
        if (!TextUtils.isEmpty(pass) && !isPasswordValid(pass)) {
            mEditText_pass.setError(getString(R.string.error_invalid_password),null); //null prevents exclamation mark blocking the eye
            focusView = mEditText_pass;
            cancel = true;
            Log.d(TAG, "Password failed");
        }
        if (!isEmailValid(mail)) {
            mEditText_mail.setError(getString(R.string.error_invalid_email), null);
            focusView = mEditText_mail;
            cancel = true;
            Log.d(TAG, "Email failed");
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
            Log.d(TAG, "Registration: canceled");
        }
        else
        {
            showProgressDialog(true);
            final Uri tempUri;
            // Save the Image to local file
            try {
                tempUri = saveImage(imageBitmap);
                Log.d(TAG, "save image returned");
            } catch ( IOException e ) {
                e.printStackTrace();
                return; //check permissions?
            }
            mFirebaseAuth.createUserWithEmailAndPassword(mail, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull final Task<AuthResult> task) {
                            showProgressDialog(false);
                            if (task.isSuccessful())
                            {
                                Log.d(TAG, "createUserWithEmailAndPassword: success!");
                                final String uuid = task.getResult().getUser().getUid();
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                StorageReference photoRef = FirebaseStorage.getInstance().getReference().child("profilePics").child(uuid).child("profile");
                                photoRef.putFile(tempUri)
                                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                                        {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                imageBitmap = null;
                                                // When the image has successfully uploaded, we get its download URL
                                                @SuppressWarnings("VisibleForTests")
                                                final Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                                // Post full ProfileBean to Database
                                                final DatabaseReference refUsers = FirebaseDatabase.getInstance().getReference().child("users");
                                                ProfileBean beanie = new ProfileBean(downloadUrl.toString(), nuser, nfirst, nlast, phone, null);
                                                refUsers.child(uuid).setValue(beanie)
                                                        .continueWithTask(new Continuation<Void, Task<Void>>()
                                                        {
                                                            // Set the download URL and update Auth profile
                                                            @Override
                                                            public Task<Void> then(@NonNull Task<Void> t) throws Exception {
                                                                return FirebaseAuth.getInstance().getCurrentUser()
                                                                        .updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(nuser).setPhotoUri(downloadUrl).build());
                                                            }
                                                        })
                                                        .addOnCompleteListener(new OnCompleteListener<Void>()
                                                        {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    updateUI();
                                                                    Log.d("Display name: ", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                                                }
                                                                else {
                                                                    Log.e(TAG,"Something failed.");
                                                                }
                                                            }
                                                        });
                                            }
                                        }).addOnFailureListener(new OnFailureListener()
                                            {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    showProgressDialog(false);
                                                    String msg = "Uploading photo failed. Try again.";
                                                    mStatus_register.setText(msg);
                                                    mStatus_register.setVisibility(View.VISIBLE);
                                                    Log.e(TAG, msg+e.getMessage());
                                                }
                                            });

                                Log.d(TAG, "Saving Shared Prefs");
                                SharedPreferences data = getSharedPreferences("basic", Activity.MODE_PRIVATE);
                                data.edit().putString("username", nuser)
                                        .putString("first", nfirst)
                                        .putString("last", nlast)
                                        .putString("phone", phone)
                                        .apply();
                            } else {
                                showProgressDialog(false);
                                Log.e(TAG, "createUserWithEmailAndPassword: failed");
                                // If sign in fails, display a message to the user.
                                String msg = "Please check your internet connection.";
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    msg = "This email is already registered";
                                    mEditText_mail.setError(msg);
                                    mEditText_mail.requestFocus();
                                } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                    msg = "Please use a stronger password";
                                    mEditText_pass.setError(msg);
                                    mEditText_pass.requestFocus();
                                }
                                Log.e(TAG, task.getException().getMessage(), task.getException());
                                mStatus_register.setText(msg);
                                mStatus_register.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    private void attemptLogin()
    {
        View focusView = null;
        boolean cancel = false;

        mEditText_mail.setError(null);
        mEditText_pass.setError(null);

        //how secure this really is, add .trim()?
        String mail = mEditText_mail.getText().toString();
        String pass = mEditText_pass.getText().toString();

        //test for simple input errors
        if (!TextUtils.isEmpty(pass) && !isPasswordValid(pass)) {
            mEditText_pass.setError(getString(R.string.error_invalid_password), null);
            focusView = mEditText_pass;
            cancel = true;
        }

        if (TextUtils.isEmpty(mail)) {
            mEditText_mail.setError(getString(R.string.error_field_required));
            focusView = mEditText_mail;
            cancel = true;
        } else if (!isEmailValid(mail)) {
            mEditText_mail.setError(getString(R.string.error_invalid_email));
            focusView = mEditText_mail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
            Log.d(TAG, "Login: canceled");
        } else {
            // Show a progress spinner, and kick off login attempt
            showProgressDialog(true);
            // I don't really need user data because auth persists across activities
            mFirebaseAuth.signInWithEmailAndPassword(mail, pass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful())
                            {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                Log.d(TAG, "User "+user.getUid()+" ("+user.getDisplayName()+") has logged in");
                                SharedPreferences sp = getSharedPreferences("basic", Activity.MODE_PRIVATE);
                                if (!(user.getDisplayName().equals(sp.getString("username","boo")))){
                                    // Saved data DOESN'T match logged in user
                                    sp.edit().remove("first")
                                            .remove("last")
                                            .remove("phone")
                                            .apply();
                                }
                                updateUI(); //calls finish()
                            }
                            else
                            {
                                String msg = "Something went wrong. Please check your internet connection.";
                                if (task.getException() instanceof FirebaseAuthInvalidUserException)
                                    msg="This email is not registered.";
                                else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    msg="Wrong password.";
                                    mEditText_pass.setError(getString(R.string.error_incorrect_password));
                                    mEditText_pass.setText("");
                                    mEditText_pass.requestFocus();
                                }

                                Log.e(TAG, task.getException().getMessage(), task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                mStatus_signin.setText(msg); //"Wrong email or password"
                                mStatus_signin.setVisibility(View.VISIBLE);
                            }
                            showProgressDialog(false);
                        }
                    });
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

    // The Android Camera application encodes the photo in the return Intent delivered to onActivityResult()
    // as a small Bitmap in the extras, under the key "data". It is thumbnail size image.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            mPic.setImageBitmap(imageBitmap);
            pic_set = true;
            mStatus_register.setVisibility(View.GONE);
            if (!isLogin && mail_set && pass_set && usern_set && real_set && last_set && phone_set)
                register_button.setEnabled(true);
        } else {
            pic_set = false;
        }
    }

    private Uri saveImage(Bitmap imageBitmap) throws IOException {
        Log.d(TAG, "in saveImage");
        FileOutputStream osfile = null;
        File image;
        Uri photoURI;
        try {
            image = new File(getFilesDir(), "profile.jpg"); //    /data/data/com.demo.mosisapp/files/profile.jpg
            osfile = new FileOutputStream(image);
            boolean bla = imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, osfile);
            Log.d(TAG, "compression status = "+bla);
            if (bla)
            {// Continue only if the File was successfully created
                photoURI = Uri.fromFile(image);
                Log.d(TAG, photoURI.toString());
            }
        } finally {
            if (osfile != null)
                osfile.close();
        }
        return Uri.fromFile(image);
    }

    /**
     * can only go back with RESULT_OK
     * all other cases are authentication errors
     */
    private void updateUI() {
        showProgressDialog(false);
        Intent results = new Intent();
        setResult(RESULT_OK, results);
        finish();
    }

    @Override
    protected void onDestroy() {
        showProgressDialog(false);
        super.onDestroy();
    }
}

