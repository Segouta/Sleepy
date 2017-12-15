package com.example.christian.sleepy;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    // create all needed variables
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    String email, password, confirmpassword, username, layout;

    TextView emailText, passwordText, confirmPasswordText, usernameText;
    ProgressBar progressBar;
    Button signupButton, loginButton, backButton;

    boolean returnCode;

    @Override
    public void onStart() {
        super.onStart();
        // check if user is signed in (non-null) and update UI accordingly
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // remove the authlistener if it was present on and of activity
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // firebase necessities
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // find all views
        progressBar = findViewById(R.id.progressBarSplash);
        signupButton = findViewById(R.id.signInButton);
        loginButton = findViewById(R.id.logInButton);
        backButton = findViewById(R.id.backButton);
        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        confirmPasswordText = findViewById(R.id.passwordCheckText);
        usernameText = findViewById(R.id.usernameText);

        // set the initial layout of buttons etc
        setVisibility("buttons");

        // initialize auth listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                // check if user is signed in
                if (user != null) {
                    // change display name (for future use)
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username).build();
                    user.updateProfile(profileUpdates);

                    goToMainActivity();
                }
            }
        };
    }

    public void createAccount() {
        // create new account with the email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // sign in success, add user to firebase
                            Log.d("Created User", "createUserWithEmail:success");
                            Toast.makeText(SplashActivity.this, "Created User: " + email,
                                    Toast.LENGTH_SHORT).show();

                            addUserToDB();

                        } else {
                            // if sign in fails, display a message to the user
                            Log.w("Creating user failed", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SplashActivity.this, "Failed to create new user...",
                                    Toast.LENGTH_SHORT).show();

                            // reset layout to try again
                            setVisibility("signup");

                        }
                    }
                });
    }

    public void logIn() {
        // login with the email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // sign in successful, the previously set listener will automatically take over from here
                            Log.d("Signed In", "signInWithEmail:success");

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Logging In Failed", "signInWithEmail:failure", task.getException());
                            Toast.makeText(SplashActivity.this, "Login failed.",
                                    Toast.LENGTH_SHORT).show();
                            setVisibility("login");

                        }
                    }
                });
    }

    public void signClicked(View view) {
        // if the sign up button was clicked, this checks if fields were filled in valid
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
            else if (password.length() < 7) {
                toaster("Password must contain at least 7 characters");
            }
            else {
                // set layout to loading screen and create account
                setVisibility("loading");
                createAccount();
            }
        }
    }

    public void logClicked(View view) {
        // if the login button was clicked, this checks if all fields were filled in valid
        if (layout.equals("buttons")) {
            setVisibility("login");
        }
        else {
            email = emailText.getText().toString();
            password = passwordText.getText().toString();
            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                toaster("Please fill in all fields...");
            } else {
                // set layout to loading screen and log in
                setVisibility("loading");
                logIn();
            }
        }
    }

    public void backClicked(View view) {
        // when the back button was clicked, this loads the appropriate layout with just buttons
        setVisibility("buttons");
    }

    public void goToMainActivity() {
        // does what it's called
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    public void addUserToDB() {
        // adds user to database
        FirebaseUser user = mAuth.getCurrentUser();

        // create new UserData object
        UserData data = new UserData(username, email, password, Calendar.getInstance().getTime(), new ArrayList<String>());

        // store UserData object in firebase
        mDatabase.child("users").child(user.getUid()).setValue(data);

        // updates list with all users and list with corresponding ID's for suggestion textview
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db

                // get Suggestions object from firebase
                Suggestions check = dataSnapshot.child("userSuggestions").getValue(Suggestions.class);

                // if it does not exist, create it and store lists in it, else append to existing lists
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
        // set the layout of the splash activity to the right format with buttons and text fields
        layout = localLayout;
        if (layout.equals("buttons")) {
            progressBar.setVisibility(View.INVISIBLE);
            emailText.setVisibility(View.INVISIBLE);
            passwordText.setVisibility(View.INVISIBLE);
            confirmPasswordText.setVisibility(View.INVISIBLE);
            usernameText.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.INVISIBLE);
        }
        else if (layout.equals("signup")) {
            progressBar.setVisibility(View.INVISIBLE);
            emailText.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            confirmPasswordText.setVisibility(View.VISIBLE);
            usernameText.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.INVISIBLE);
            signupButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }
        else if (layout.equals("login")) {
            progressBar.setVisibility(View.INVISIBLE);
            emailText.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            confirmPasswordText.setVisibility(View.INVISIBLE);
            usernameText.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.VISIBLE);
            signupButton.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }
        else if (layout.equals("loading")) {
            progressBar.setVisibility(View.VISIBLE);
            emailText.setVisibility(View.INVISIBLE);
            passwordText.setVisibility(View.INVISIBLE);
            confirmPasswordText.setVisibility(View.INVISIBLE);
            usernameText.setVisibility(View.INVISIBLE);
            loginButton.setVisibility(View.INVISIBLE);
            signupButton.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.INVISIBLE);
        }
    }

    public void toaster(String message) {
        // toasts
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public boolean usernameExists(String username) {
        // this function checks if a username already exists. If it exists, it returns true, else false
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    returnCode = true;
                } else {
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