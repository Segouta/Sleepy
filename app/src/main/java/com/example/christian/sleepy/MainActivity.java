package com.example.christian.sleepy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // setup firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase =  FirebaseDatabase.getInstance().getReference();

    // setup gps location
    private LocationManager locationManager;
    private LocationListener locationListener;

    // "attach" helpermethods
    HelperMethods fb = new HelperMethods();

    // setup variables
    TextView dist, userNameText, togoText;

    float selectedDistance;
    boolean alarmSet;
    String distance;
    Boolean shareState;

    Button addButton, alarmButton, logoutButton, backButton, shareButton, resetButton;
    ProgressBar progressBar;
    Spinner spinner;
    MultiAutoCompleteTextView searchText;

    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapter2;

    StationData data = new StationData();

    // setup lists
    List<String> suggestionList = new ArrayList<>();
    List<String> codeList = new ArrayList<>();
    List<String> stationSuggestions = new ArrayList<>();
    List<String> userSuggestions = new ArrayList<>();
    List<String> userIds = new ArrayList<>();
    List<String> codes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assign all views
        progressBar = findViewById(R.id.progressBar);
        dist = findViewById(R.id.info);
        userNameText = findViewById(R.id.userNameView);
        togoText = findViewById(R.id.togoText);
        addButton = findViewById(R.id.addButton);
        alarmButton = findViewById(R.id.alarmButton);
        backButton = findViewById(R.id.backButton);
        logoutButton = findViewById(R.id.logOutButton);
        resetButton = findViewById(R.id.resetButton);
        shareButton = findViewById(R.id.shareButton);
        spinner = findViewById(R.id.spinner);
        searchText = findViewById(R.id.searchText);

        // set layout to loading spinner
        setLayout("loading");

        // initialize variables
        shareState = true;
        alarmSet = false;
        selectedDistance = 500;
        distance = "500m";

        // setup inputmanager
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // initialize the suggestion lists for the search text field
        stationSuggestions = getSuggestions().get(0);
        codes = getSuggestions().get(1);
        userSuggestions = getUserSuggestions().get(0);
        userIds = getUserSuggestions().get(1);

        // initialize longitude and latitude
        data.Lon = "0.0000";
        data.Lat = "0.0000";

        // setup firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // set adapters and scrolling
        dist.setMovementMethod(new ScrollingMovementMethod());
        searchText.setAdapter(adapter);
        searchText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // check if logged in
        setListener();

        // initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // this listener checks what happens with the locationservice
        locationListener = new LocationListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationChanged(Location location) {
                // this method is triggered when a user is detected in new location
                Log.d("gps", "updated");

                // get coordinates to get to
                Location targetLocation = new Location("");
                targetLocation.setLatitude(Float.parseFloat(data.Lat));
                targetLocation.setLongitude(Float.parseFloat(data.Lon));

                // get coordinates of user
                Location phoneLocation = new Location("");
                phoneLocation.setLatitude(location.getLatitude());
                phoneLocation.setLongitude(location.getLongitude());

                // calculate distance and round and display in right manner
                float distanceInMeters = phoneLocation.distanceTo(targetLocation);
                int distanceInMetersRound = Math.round(distanceInMeters);
                if (distanceInMeters < 2000) {
                    dist.setText(Integer.toString(Math.round(distanceInMetersRound)) + " m");
                }
                else{
                    dist.setText(Double.toString(round(distanceInMeters / 1000, 1)) + " km");
                }

                // if the distance is closer then user wants it and the alarm is set, fire the alarm
                if (alarmSet && distanceInMeters < selectedDistance) {
                    alertArrival();
                    alarmSet = false;
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // necessity
            }

            @Override
            public void onProviderEnabled(String provider) {
                // necessity
            }

            @Override
            public void onProviderDisabled(String provider) {

                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        // this is the listener for the clicks on the items of the suggestions from the textview
        searchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                // store and display the station or user we selected
                String toFind = parent.getItemAtPosition(position).toString();
                searchText.setText(toFind);

                // if were searching for stations (shareState == true), get stationInfo of that station
                if (shareState) {
                    dist.setVisibility(View.VISIBLE);
                    togoText.setVisibility(View.VISIBLE);
                    getStationInfo(codes.get(stationSuggestions.indexOf(toFind)));

                    // hide keyboard
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                } else {
                    // else, get selected user's favorites
                    getUserFavorites(toFind);
                }

            }
        });

        // this is the listener for the clicks on the items of the spinner that selects distance to be waked up
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // get distance, convert it to a float and store
                distance = parent.getItemAtPosition(position).toString();
                Float distanceFloat = Float.valueOf(distance.substring(0, distance.length() - 2));
                selectedDistance = distanceFloat;
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // necessity
            }
        });

        // get the current location
        getLocation();

        // call the method that updates the station database if that is not up to date
        fb.stationListUpdate(this);

        // get the current user's username to display
        getUsername();
    }

    public void getUsername() {
        // gets the current user's username
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
                String gotUsername = dataSnapshot.child("users").child(mAuth.getUid()).child("username").getValue().toString();

                // set personal welcome message
                userNameText.setText("Welcome back, " + gotUsername);

                // set layout from loading to normal, everything is loaded
                setLayout("main");

                // set to right visibility
                dist.setVisibility(View.INVISIBLE);
                togoText.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    public void setLayout(String type) {
        // sets the layout to the wanted configuration
        if (type.equals("loading")){
            progressBar.setVisibility(View.VISIBLE);
            dist.setVisibility(View.INVISIBLE);
            userNameText.setVisibility(View.INVISIBLE);
            togoText.setVisibility(View.INVISIBLE);
            addButton.setVisibility(View.INVISIBLE);
            spinner.setVisibility(View.INVISIBLE);
            searchText.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.INVISIBLE);
            alarmButton.setVisibility(View.INVISIBLE);
            shareButton.setVisibility(View.INVISIBLE);
            resetButton.setVisibility(View.INVISIBLE);
        }
        else if (type.equals("main")){
            progressBar.setVisibility(View.INVISIBLE);
            dist.setVisibility(View.VISIBLE);
            userNameText.setVisibility(View.VISIBLE);
            togoText.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
            searchText.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            alarmButton.setVisibility(View.VISIBLE);
            shareButton.setVisibility(View.VISIBLE);
            resetButton.setVisibility(View.INVISIBLE);
        }
        else if (type.equals("sleeping")){
            progressBar.setVisibility(View.INVISIBLE);
            dist.setVisibility(View.VISIBLE);
            userNameText.setVisibility(View.INVISIBLE);
            togoText.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.INVISIBLE);
            spinner.setVisibility(View.INVISIBLE);
            searchText.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.INVISIBLE);
            backButton.setVisibility(View.INVISIBLE);
            alarmButton.setVisibility(View.INVISIBLE);
            shareButton.setVisibility(View.INVISIBLE);
            resetButton.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // gets permissions for gps
        switch (requestCode) {
            case 10:
                getLocation();
                break;
            default:
                break;
        }
    }

    void getLocation() {
        // first check for permissions, then get location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d("hierdan", "hierja");
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 10);
            }
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, locationListener);

    }

    public void logOutClicked(View view) {
        // if a user clicks the logout, the user gets logged out and returns to splash activity
        FirebaseAuth.getInstance().signOut();
        goToSplashActivity();
    }

    public void goToSplashActivity() {
        // does what it's called
        startActivity(new Intent(MainActivity.this, SplashActivity.class));
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // check if user is signed in (non-null) and set auth
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        // if activity stops, remove listener
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void setListener() {
        // initialize auth listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    // User is signed in, everything alright
                    Log.d("setListener", "signed in so redirected");
                }
                else {
                    // user is not logged in and redirected to splash page
                    toaster("You are logged out.");
                    goToSplashActivity();
                }
            }
        };
    }

    public void toaster(String message) {
        // toasts string
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public void getStationInfo(final String stationCode) {
        // this method gets the stationinfo of the stationCode provided

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get StationData object from db
                StationData stationData = dataSnapshot.child("stations").child(stationCode).getValue(StationData.class);

                // set visibilities and store stationData
                togoText.setVisibility(View.VISIBLE);
                dist.setVisibility(View.VISIBLE);
                data = stationData;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong...");
            }
        };
        mDatabase.addValueEventListener(postListener);

    }

    private static double round (double value, int precision) {
        // rounds off floats to wanted precision, used for distance display
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public void deleteButton(View view) {
        // when delete button clicked, clear text and set visibility correct
        searchText.setText("");
        togoText.setVisibility(View.INVISIBLE);
        dist.setVisibility(View.INVISIBLE);
    }

    public void setClicked(View view) {
        // if the set alarm is clicked, check if a station was selected
        if (data.names != null) {
            setClicked();
        } else {
            toaster("Please select station first");
        }
    }

    public void setClicked() {
        // if a station was selected, create alertdialog for user to confirm choices
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Summary")
                .setMessage("Traveling to " + data.names.get(1) + ", waking up " + distance + " before arrival.")
                .setPositiveButton("Set alarm!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // if confirmed, set layout to sleeping mode and make alarmSet true
                        setLayout("sleeping");
                        alarmSet = true;
                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing, just closes dialog
                    }
                })
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    public void shareClicked(View view) {
        // if sharebutton is clicked, it switches the function of the search bar and its suggestions
        shareState = !shareState;

        // set to correct values and visibilities
        if (shareState) {
            searchText.setHint("Wake-up destionation...");
            searchText.setText("");
            togoText.setVisibility(View.VISIBLE);
            dist.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.VISIBLE);
            searchText.setAdapter(adapter);
        }
        else {
            searchText.setHint("Search sleepy user...");
            searchText.setText("");
            togoText.setVisibility(View.INVISIBLE);
            dist.setVisibility(View.INVISIBLE);
            addButton.setVisibility(View.INVISIBLE);
            searchText.setAdapter(adapter2);
        }
    }

    public void addClicked(View view) {
        // if add favorites was clicked, check if a station was selected
        if (data.names != null) {
            addClicked();
        } else {
            toaster("Please select station first");
        }
    }

    public void addClicked() {
        // if a station was selected, create dialog
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Confirm add")
                .setMessage("Are you sure you want to add " + data.names.get(1) + " to your favorites?")
                .setPositiveButton("Add!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        toaster("Added  to your favorites!");

                        // add the station to the favorites of the current user
                        fb.addToFavorites(data.names.get(1), mAuth);

                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing, just close dialog
                    }
                })
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    public List<List<String>> getSuggestions() {
        // retrieve the suggestionlist for the stations and the corresponding station codes from firebase
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get suggestionList and codeList and assign them to two different lists for external use
                suggestionList = dataSnapshot.child("suggestions").getValue(Suggestions.class).suggestionList;
                codeList = dataSnapshot.child("suggestions").getValue(Suggestions.class).codeList;
                stationSuggestions = suggestionList;
                codes = codeList;

                // set the suggestions for the search bar
                adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, suggestionList);
                searchText.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);

        // safe the two lists in another list to use them in the return
        List<List<String>> totList = new ArrayList<List<String>>();
        totList.add(suggestionList);
        totList.add(codeList);

        return totList;
    }

    public List<List<String>> getUserSuggestions() {
        // this method gets the usernames for giving suggestions in the search bar
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // retrieve the suggestionlist for the users and the corresponding user id's from firebase
                suggestionList = dataSnapshot.child("userSuggestions").getValue(Suggestions.class).suggestionList;
                codeList = dataSnapshot.child("userSuggestions").getValue(Suggestions.class).codeList;
                userSuggestions = suggestionList;
                userIds = codeList;

                // set the suggestions for the search bar
                adapter2 = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, suggestionList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);

        // safe the two lists in another list to use them in the return
        List<List<String>> totList = new ArrayList<List<String>>();
        totList.add(suggestionList);
        totList.add(codeList);

        return totList;
    }

    public void getUserFavorites(final String username) {
        // get the selected user's favorites and display them in a dialog
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // first get the suggestions, get the userID, get the favorites
                Suggestions userSuggestions = dataSnapshot.child("userSuggestions").getValue(Suggestions.class);
                String userId = userSuggestions.codeList.get(userSuggestions.suggestionList.indexOf(username));
                List<String> userFavorites = dataSnapshot.child("users").child(userId).getValue(UserData.class).favorites;

                // setup the whole pop up with the list containing the favorites
                if (userFavorites != null) {
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.custom_list);
                    dialog.setTitle("Favorites");
                    TextView title = (TextView) dialog.findViewById(R.id.favoTitle);
                    title.setText(username + "'s favorite sleep-through stations:");
                    ListView list = (ListView) dialog.findViewById(R.id.listView);
                    ArrayAdapter<String> adapter3 = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, userFavorites);
                    list.setAdapter(adapter3);
                    dialog.show();
                } else {
                    // if no favorites, toast message
                    toaster(username + " has no favorites yet!");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addListenerForSingleValueEvent(postListener);


    }

    public void alertArrival() {
        // on arrival, let the vibrator buzz and play some sounds
        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.sound);

        // vibration pattern
        long[] pattern = {50, 100, 50, 100, 300, 100, 300, 100};

        // start sound
        mp.start();

        // start vibrations
        v.vibrate(pattern, 0);

        // create dialog
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("WAKE UP!")
                .setMessage("You're almost at " + data.names.get(1) + " station!")
                .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // stop vibrator and sound and set to main view again
                        setLayout("main");
                        v.cancel();
                        mp.stop();

                    }
                })
                .setIcon(android.R.drawable.stat_sys_warning)
                .show();
    }

    public void resetClicked(View view) {
        // when in sleeping mode, reset everything when reset button is clicked
        setLayout("main");
        alarmSet = false;
        toaster("Alarm reset");
    }
}
