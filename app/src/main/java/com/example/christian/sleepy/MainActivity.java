package com.example.christian.sleepy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
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

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    HelperMethods fb = new HelperMethods();

    TextView lijst, userNameText, togoText;

    float selectedDistance;
    boolean alarmSet;
    String distance;
    Boolean shareState;

    Button addButton;

    Switch vibeSwitch, soundSwitch;

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

        shareState = true;
        alarmSet = false;
        selectedDistance = 500;
        distance = "500m";

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        setContentView(R.layout.activity_main);
        lijst = findViewById(R.id.info);
        userNameText = findViewById(R.id.userNameView);
        togoText = findViewById(R.id.togoText);

        addButton = findViewById(R.id.addButton);

        lijst.setVisibility(View.INVISIBLE);
        togoText.setVisibility(View.INVISIBLE);
        addButton.setVisibility(View.VISIBLE);

        stationSuggestions = getSuggestions().get(0);
        codes = getSuggestions().get(1);

        userSuggestions = getUserSuggestions().get(0);
        userIds = getUserSuggestions().get(1);


        spinner = findViewById(R.id.spinner);



        vibeSwitch = findViewById(R.id.vibeSwitch);
        soundSwitch = findViewById(R.id.soundSwitch);

        data.Lon = "0.0000";
        data.Lat = "0.0000";

        mAuth = FirebaseAuth.getInstance();
        Log.d("xdtcfgvh", "onCreate: " + String.valueOf(stationSuggestions.size()));


        searchText = findViewById(R.id.searchText);
        mDatabase = FirebaseDatabase.getInstance().getReference();


        lijst.setMovementMethod(new ScrollingMovementMethod());
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
                    lijst.setText(Integer.toString(Math.round(distanceInMetersRound)) + " m");
                }
                else{
                    lijst.setText(Double.toString(round(distanceInMeters / 1000, 1)) + " km");
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

                lijst.setVisibility(View.VISIBLE);
                togoText.setVisibility(View.VISIBLE);

                System.out.println("tofind: " + toFind);

                searchText.setText(toFind);
                getStationInfo(codes.get(stationSuggestions.indexOf(toFind)));
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);


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

        if(mDatabase == null)
            Log.d("test", "db is null");

//        fb.stationListRequest(this);
        fb.getUsername(mAuth, userNameText);

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

                    Toast.makeText(getApplicationContext(), "redirected cause already logged in", Toast.LENGTH_SHORT).show();

                }
                else {
                    Toast.makeText(getApplicationContext(), "redirected cause not logged in", Toast.LENGTH_SHORT).show();
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
                lijst.setVisibility(View.VISIBLE);
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
        lijst.setVisibility(View.INVISIBLE);
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

        toaster ("hoi");
        Log.d("hoi", "hoi");

        if (shareState) {
            searchText.setHint("Wake-up destionation...");
            addButton.setVisibility(View.VISIBLE);
            searchText.setAdapter(adapter);
//            searchText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }
        else {
            searchText.setHint("Search sleepy user...");
            addButton.setVisibility(View.INVISIBLE);
            searchText.setAdapter(adapter2);
//            searchText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
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
//                System.out.println("vanaf hier:------------------------------------------------------");
//                for (int i = 0; i < suggestion.suggestionList.size(); i++) {
//                    System.out.println(suggestion.suggestionList.get(i));
//                }
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
}
