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
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    HelperMethods fb = new HelperMethods();

    TextView dist, userNameText, togoText;

    float selectedDistance;
    boolean alarmSet;
    String distance;
    Boolean shareState;

    Button addButton, alarmButton, logoutButton, backButton, shareButton;
    ProgressBar progressBar;
    Spinner spinner;

    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapter2;

    MultiAutoCompleteTextView searchText;

    StationData data = new StationData();

    private DatabaseReference mDatabase =  FirebaseDatabase.getInstance().getReference();

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

        progressBar = findViewById(R.id.progressBar);
        dist = findViewById(R.id.info);
        userNameText = findViewById(R.id.userNameView);
        togoText = findViewById(R.id.togoText);
        addButton = findViewById(R.id.addButton);
        alarmButton = findViewById(R.id.alarmButton);
        backButton = findViewById(R.id.backButton);
        logoutButton = findViewById(R.id.logOutButton);
        shareButton = findViewById(R.id.shareButton);
        spinner = findViewById(R.id.spinner);
        searchText = findViewById(R.id.searchText);

        setLayout("loading");

        shareState = true;
        alarmSet = false;
        selectedDistance = 500;
        distance = "500m";

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        stationSuggestions = getSuggestions().get(0);
        codes = getSuggestions().get(1);
        userSuggestions = getUserSuggestions().get(0);
        userIds = getUserSuggestions().get(1);

        data.Lon = "0.0000";
        data.Lat = "0.0000";

        mAuth = FirebaseAuth.getInstance();
        Log.d("xdtcfgvh", "onCreate: " + String.valueOf(stationSuggestions.size()));

        mDatabase = FirebaseDatabase.getInstance().getReference();

        dist.setMovementMethod(new ScrollingMovementMethod());
        searchText.setAdapter(adapter);
        searchText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        setListener();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationChanged(Location location) {

                Log.d("hierzo", "hoi");
//                toaster("new");
                Location targetLocation = new Location("");
                targetLocation.setLatitude(Float.parseFloat(data.Lat));
                targetLocation.setLongitude(Float.parseFloat(data.Lon));

                Location phoneLocation = new Location("");
                phoneLocation.setLatitude(location.getLatitude());
                phoneLocation.setLongitude(location.getLongitude());

                float distanceInMeters = phoneLocation.distanceTo(targetLocation);
                int distanceInMetersRound = Math.round(distanceInMeters);
                if (distanceInMeters < 2000) {
                    dist.setText(Integer.toString(Math.round(distanceInMetersRound)) + " m");
                }
                else{
                    dist.setText(Double.toString(round(distanceInMeters / 1000, 1)) + " km");
                }

                if (alarmSet && distanceInMeters < selectedDistance) {
                    toaster("waking user up rn");

//                    TODO: set the wakeup alert here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!=============

                    alarmSet = false;
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        searchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                String toFind = parent.getItemAtPosition(position).toString();

                dist.setVisibility(View.VISIBLE);
                togoText.setVisibility(View.VISIBLE);

                System.out.println("tofind: " + toFind);

                searchText.setText(toFind);

                if (shareState) {
                    getStationInfo(codes.get(stationSuggestions.indexOf(toFind)));
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                } else {
                    getUserFavorites(toFind);
                }

            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                distance = parent.getItemAtPosition(position).toString();
                Float distanceFloat = Float.valueOf(distance.substring(0, distance.length() - 2));
                selectedDistance = distanceFloat;
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getLocation();

        fb.stationListUpdate(this);
        getUsername();
    }

    public void getUsername() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
                String gotUsername = dataSnapshot.child("users").child(mAuth.getUid()).child("username").getValue().toString();

                userNameText.setText("Welcome back, " + gotUsername);

                setLayout("main");

                dist.setVisibility(View.INVISIBLE);
                togoText.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIR Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    public void setLayout(String type) {
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
        }
        else if (type.equals("sleeping")){
            progressBar.setVisibility(View.INVISIBLE);
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
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                getLocation();
                break;
            default:
                break;
        }
    }

    void getLocation() {
        // first check for permissions
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
        FirebaseAuth.getInstance().signOut();
        goToSplashActivity();
    }

    public void goToSplashActivity() {
        startActivity(new Intent(MainActivity.this, SplashActivity.class));
        finish();
    }

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

    private void setListener() {
        // initialize auth listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    // User is signed in
                    Log.d("setListener", "signed in so redirected");
                }
                else {
                   toaster("You were redirected, because you were logged out.");
                    goToSplashActivity();
                }
            }
        };
    }

    public void toaster(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public void getStationInfo(final String stationCode) {


        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
                System.out.println("dit is de code to search: " + stationCode);

                StationData stationData = dataSnapshot.child("stations").child(stationCode).getValue(StationData.class);

                togoText.setVisibility(View.VISIBLE);
                dist.setVisibility(View.VISIBLE);
                data = stationData;
                System.out.println(stationData.Code);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong...");
            }
        };
        mDatabase.addValueEventListener(postListener);

    }

    private static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public void deleteButton(View view) {
        searchText.setText("");
        togoText.setVisibility(View.INVISIBLE);
        dist.setVisibility(View.INVISIBLE);
    }

    public void setClicked(View view) {
        if (data.names != null) {
            setClicked();
        } else {
            toaster("Please select station first");
        }
    }

    public void setClicked() {
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
                        setLayout("sleeping");
                        alarmSet = true;
//                        toaster(String.valueOf(vibeSwitch.isChecked()));
//                        toaster(String.valueOf(soundSwitch.isChecked()));
                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    public void shareClicked(View view) {
        shareState = !shareState;

        if (shareState) {
            searchText.setHint("Wake-up destionation...");
            addButton.setVisibility(View.VISIBLE);
            searchText.setAdapter(adapter);

        }
        else {
            searchText.setHint("Search sleepy user...");
            addButton.setVisibility(View.INVISIBLE);
            searchText.setAdapter(adapter2);

        }

    }

    public void addClicked(View view) {
        if (data.names != null) {
            addClicked();
        } else {
            toaster("Please select station first");
        }
    }

    public void addClicked() {
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
                        Log.d("toast", "werkt niet...");
                        fb.addToFavorites(data.names.get(1), mAuth);

                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    public List<List<String>> getSuggestions() {

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                suggestionList = dataSnapshot.child("suggestions").getValue(Suggestions.class).suggestionList;
                codeList = dataSnapshot.child("suggestions").getValue(Suggestions.class).codeList;
                stationSuggestions = suggestionList;
                codes = codeList;
//                System.out.println("vanaf hier:------------------------------------------------------");
//                for (int i = 0; i < suggestion.suggestionList.size(); i++) {
//                    System.out.println(suggestion.suggestionList.get(i));
//                }
                adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, suggestionList);
                searchText.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIR Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);


        List<List<String>> totList = new ArrayList<List<String>>();
        totList.add(suggestionList);
        totList.add(codeList);

        System.out.println("getSUggestions done");

        return totList;
    }

    public List<List<String>> getUserSuggestions() {

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                suggestionList = dataSnapshot.child("userSuggestions").getValue(Suggestions.class).suggestionList;
                codeList = dataSnapshot.child("userSuggestions").getValue(Suggestions.class).codeList;
                userSuggestions = suggestionList;
                userIds = codeList;

                adapter2 = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_dropdown_item_1line, suggestionList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIR Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);


        List<List<String>> totList = new ArrayList<List<String>>();
        totList.add(suggestionList);
        totList.add(codeList);

        System.out.println("getSUggestions done");

        return totList;
    }

    public void getUserFavorites(final String username) {

        Log.d("getUFavs", username);

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Suggestions userSuggestions = dataSnapshot.child("userSuggestions").getValue(Suggestions.class);
                String userId = userSuggestions.codeList.get(userSuggestions.suggestionList.indexOf(username));
                List<String> userFavorites = dataSnapshot.child("users").child(userId).getValue(UserData.class).favorites;
                System.out.println(userFavorites);

                if (userFavorites != null) {
                    TextView title = findViewById(R.id.favoTitle);
                    title.setText(username + "'s favorite sleep-through stations:");
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.custom_list);
                    dialog.setTitle("Title...");
                    ListView list = (ListView) dialog.findViewById(R.id.listView);
                    ArrayAdapter<String> adapter3 = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, userFavorites);
                    list.setAdapter(adapter3);
                    dialog.show();
                } else {
                    toaster(username + " has no favorites yet!");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);


    }
}
