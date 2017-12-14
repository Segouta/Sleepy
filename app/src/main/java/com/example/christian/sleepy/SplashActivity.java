package com.example.christian.sleepy;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private DatabaseReference mDatabase;

    String email, password, confirmpassword, username;

    TextView emailText, passwordText, confirmPasswordText, usernameText;

    Button signupButton, loginButton, backButton;

    String layout = "";

    boolean returnCode;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();

        signupButton = findViewById(R.id.signInButton);
        loginButton = findViewById(R.id.logInButton);
        backButton = findViewById(R.id.backButton);

        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        confirmPasswordText = findViewById(R.id.passwordCheckText);
        usernameText = findViewById(R.id.usernameText);

        setVisibility("buttons");

        // initialize auth listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username).build();
                    user.updateProfile(profileUpdates);
                    
                    goToMainActivity();

                    // User is signed in
                    Toast.makeText(getApplicationContext(), "redirected cause already logged in", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void createAccount() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Created User", "createUserWithEmail:success");
                            Toast.makeText(SplashActivity.this, "Created User: " + email,
                                    Toast.LENGTH_SHORT).show();

                            addUserToDB();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Creating user failed", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SplashActivity.this, "Failed to create new user...",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    public void logIn() {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Signed In", "signInWithEmail:success");

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Logging In Failed", "signInWithEmail:failure", task.getException());
                            Toast.makeText(SplashActivity.this, "Login failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    public void signClicked(View view) {
        if (layout.equals("buttons")) {
            setVisibility("signup");
        }
        else {
            email = emailText.getText().toString();
            password = passwordText.getText().toString();
            confirmpassword = confirmPasswordText.getText().toString();
            username = usernameText.getText().toString();

            if (email == null || email.isEmpty() || username == null || username.isEmpty() || password == null || password.isEmpty() || confirmpassword == null || confirmpassword.isEmpty()) {
                toaster("Please fill in all fields...");
            }
            else if (!confirmpassword.equals(password)) {
                toaster("Passwords do not match...");
            }
            else if (usernameExists(username)) {
                toaster("Username already exists...");
            }
            else {
                createAccount();
            }
        }
    }

    public void logClicked(View view) {
        if (layout.equals("buttons")) {
            setVisibility("login");
        }
        else {
            email = emailText.getText().toString();
            password = passwordText.getText().toString();
            if (email == null || email.isEmpty() || username == null || username.isEmpty()) {
                toaster("Please fill in all fields...");
            }
            logIn();
        }
    }

    public void backClicked(View view) {
        setVisibility("buttons");
    }

    public void goToMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    public void addUserToDB() {

        FirebaseUser user = mAuth.getCurrentUser();

        UserData data = new UserData(username, email, password, Calendar.getInstance().getTime(), new ArrayList<String>());

        mDatabase.child("users").child(user.getUid()).setValue(data);

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db


                Suggestions check = dataSnapshot.child("userSuggestions").getValue(Suggestions.class);
                if (check == null) {
                    Suggestions userAddData = new Suggestions();
                    List<String> inName = new ArrayList<>();
                    List<String> inId = new ArrayList<>();
                    inName.add(username);
                    inId.add(mAuth.getCurrentUser().getUid());
                    userAddData.suggestionList = inName;
                    userAddData.codeList = inId;
                    mDatabase.child("userSuggestions").setValue(userAddData);
                } else {
                    Suggestions userAddData = new Suggestions();
                    List<String> inName = check.suggestionList;
                    List<String> inId = check.codeList;
                    System.out.println(username);
                    System.out.println(inName);
                    inName.add(username);
                    inId.add(mAuth.getCurrentUser().getUid());
                    userAddData.suggestionList = inName;
                    userAddData.codeList = inId;
                    mDatabase.child("userSuggestions").setValue(userAddData);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addListenerForSingleValueEvent(postListener);

    }

    public void setVisibility(String localLayout) {
        layout = localLayout;
        if (layout.equals("buttons")) {
            emailText.setVisibility(View.INVISIBLE);
            passwordText.setVisibility(View.INVISIBLE);
            confirmPasswordText.setVisibility(View.INVISIBLE);
            usernameText.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.INVISIBLE);
        }
        else if (layout.equals("signup")) {
            emailText.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            confirmPasswordText.setVisibility(View.VISIBLE);
            usernameText.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.INVISIBLE);
            signupButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }
        else if (layout.equals("login")) {
            emailText.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            confirmPasswordText.setVisibility(View.INVISIBLE);
            usernameText.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }
    }

    public void toaster(String message) {
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_SHORT).show();
    }

//    This function checks if a username already exists. If it exists, it returns true, else false.
    public boolean usernameExists(String username) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    returnCode = true;
                } else {
                    // User does not exist. NOW call createUserWithEmailAndPassword
                    returnCode = false;

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return returnCode;
    }
}