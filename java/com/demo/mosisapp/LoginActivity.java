package com.demo.mosisapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static com.google.firebase.database.DatabaseReference.goOffline;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    //TODO: add autocomplete for emails?
    //TODO: decide on error reporting to user - setError or mStatus.setText?
    private String myTag = "wassermelone";
    private boolean isLogin = true;
    private boolean mail_set = false;
    private boolean pass_set = false;
    private boolean usern_set = false;
    private boolean letMeOut = true;
    boolean forgetmenot = false;

    private FirebaseAuth mFirebaseAuth;

    // UI references.
    private EditText mEditText_mail;
    private EditText mEditText_pass;
    private EditText mEditText_user;
    private ProgressDialog progressDialog;
    private TextView mStatus_register;
    private TextView mStatus_signin;
    private Button sign_in_button;
    private Button register_button;
    private TextView forgot_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEditText_mail = (EditText) findViewById(R.id.login_email);
        mEditText_pass = (EditText) findViewById(R.id.login_password);
        mEditText_user = (EditText) findViewById(R.id.register_username);
        mStatus_register = (TextView)findViewById(R.id.register_status);
        mStatus_signin = (TextView)findViewById(R.id.sign_in_status);
        sign_in_button = (Button)findViewById(R.id.sign_in_button);
        register_button = (Button)findViewById(R.id.register_button);
        forgot_password = (TextView)findViewById(R.id.forgot_password);

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
                    mStatus_register.setVisibility(View.GONE);
                    mStatus_signin.setVisibility(View.GONE);
                    if (s.toString().trim().length() > 0) {
                            mail_set = true;
                            if (pass_set && isLogin) sign_in_button.setEnabled(true);
                            else if (!isLogin && pass_set && usern_set)
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
                mStatus_register.setVisibility(View.GONE);
                mStatus_signin.setVisibility(View.GONE);
                if (s.toString().trim().length() > 0) {
                   pass_set = true;
                   if (mail_set && isLogin) sign_in_button.setEnabled(true);
                    else if (!isLogin && mail_set && usern_set) register_button.setEnabled(true);
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
                if (s.toString().trim().length() > 0) {
                    usern_set = true;
                    if (!isLogin && mail_set && pass_set) register_button.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

    }

    @Override
    public void onBackPressed() { //this is just for fun, returns to choice login/register
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
            findViewById(R.id.login_flow).setVisibility(View.GONE);
            findViewById(R.id.register_form).setVisibility(View.GONE);
            findViewById(R.id.main_form).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
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
                findViewById(R.id.main_form).setVisibility(View.GONE);
                findViewById(R.id.login_flow).setVisibility(View.VISIBLE);
                findViewById(R.id.register_form).setVisibility(View.VISIBLE);
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                forgot_password.setVisibility(View.GONE);
                mEditText_mail.setText(""); //in case user changes modes login/register
                mEditText_pass.setText("");
                mEditText_user.setText("");
                break;
            case(R.id.register_button):
                attemptLogin();
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
                                mEditText_pass.setVisibility(View.VISIBLE);
                                forgot_password.setVisibility(View.VISIBLE);
                                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                                findViewById(R.id.forgot_continue_button).setVisibility(View.GONE);
                                findViewById(R.id.message_to_user).setVisibility(View.GONE);
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                            }
                            showProgressDialog(false);
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
        mEditText_user.setError(null);

        //how secure this really is, add .trim()?
        String mail = mEditText_mail.getText().toString();
        String pass = mEditText_pass.getText().toString();

        //test for simple input errors
        if (!TextUtils.isEmpty(pass) && !isPasswordValid(pass)) {
            mEditText_pass.setError(getString(R.string.error_invalid_password));
            focusView = mEditText_pass;
            cancel = true;
        }

        if (!isLogin && !(mEditText_user.getText().toString().length() > 5)) {
            mEditText_user.setError(getString(R.string.error_username_short));
            focusView = mEditText_user;
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
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off login attempt
            showProgressDialog(true);
            //I don't really need user data because auth persists across activities
            if (isLogin) {
                mFirebaseAuth.signInWithEmailAndPassword(mail, pass)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                showProgressDialog(false);
                                if (task.isSuccessful())
                                {
                                    FirebaseUser user = mFirebaseAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Signed in, " + user.getEmail() + "!", Toast.LENGTH_SHORT).show();
                                    updateUI();
                                }
                                else
                                {
                                    String msg = "Something went wrong. Please try again.";
                                    if (task.getException() instanceof FirebaseAuthInvalidUserException) msg="This email is not registered.";
                                    else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        msg="Wrong password.";
                                        mEditText_pass.setError(getString(R.string.error_incorrect_password));
                                        mEditText_pass.setText("");
                                        mEditText_pass.requestFocus();
                                    }

                                    Log.w(myTag, task.getException().getMessage(), task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    mStatus_signin.setText(msg); //"Wrong email or password"
                                    mStatus_signin.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            } else {//is not login xD
                mFirebaseAuth.createUserWithEmailAndPassword(mail, pass)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                showProgressDialog(false);
                                if (task.isSuccessful())
                                {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(myTag, "createUserWithEmail:success");

                                    // Add username to Auth profile
                                    mFirebaseAuth.getCurrentUser().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(mEditText_user.getText().toString().trim()).build())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d("Display name: ", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                                }
                                            }});

                                    // Create DB reference
                                    // This could be moved to onCreate, as a backup/check to see if it exists on Login too?
                                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    final DatabaseReference refUsers = FirebaseDatabase.getInstance().getReference().child("users");

                                    try { // this is just for trying out continuation, can be simpler
                                        refUsers.child(user.getUid()).child("username").setValue(mEditText_user.getText().toString().trim())
                                                .continueWithTask(new Continuation<Void, Task<Void>>()
                                                {
                                                    @Override
                                                    public Task<Void> then(@NonNull Task<Void> t) throws Exception {
                                                        Task bla = refUsers.child(user.getUid()).child("provider").setValue("email");
                                                        return bla;
                                                    }
                                                })
                                                .addOnSuccessListener(new OnSuccessListener<Void>()
                                                {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //goOffline();
                                                        updateUI();
                                                    }
                                                });
                                    } catch (Exception e) {
                                        Toast.makeText(LoginActivity.this,"Continuation failed",Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                                else
                                {
                                    // If sign in fails, display a message to the user.
                                    String msg = "Something went wrong. Please try again.";
                                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                        msg = "This email is already registered";
                                        mEditText_pass.setError(msg);       //TODO: this can be left for displaying error instead of status_text
                                        mEditText_pass.requestFocus();
                                    }
                                    else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                        msg = "Please use a stronger password";
                                        mEditText_pass.setError(msg);       //TODO: this can be left for displaying error instead of status_text
                                        mEditText_pass.requestFocus();
                                    }
                                    Log.w(myTag, task.getException().getMessage(), task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    mStatus_register.setText(msg); //"Authentication failed"    //TODO: this can be left for displaying error instead of status_text
                                    mStatus_register.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        }
    }

    /*
     * can only go back with RESULT_OK
     * all other cases are authentication errors
     * will do for now, maybe name change
     */
    private void updateUI() {
        Intent results = new Intent();
        setResult(RESULT_OK, results);
        finish();
    }


}

