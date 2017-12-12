package com.example.christian.sleepy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    HelperMethods fb = new HelperMethods();

    TextView lijst, userNameText;

    List<StationData> DBqueue = new ArrayList<StationData>();

    private DatabaseReference mDatabase;

    List<String> suggestions = new ArrayList<>();
    List<String> codes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        setContentView(R.layout.activity_main);
        lijst = findViewById(R.id.lijst);
        userNameText = findViewById(R.id.userNameView);

        suggestions = fb.getSuggestions().suggestionList;
        codes = fb.getSuggestions().codeList;

        mAuth = FirebaseAuth.getInstance();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        final MultiAutoCompleteTextView searchText = findViewById(R.id.searchText);
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
                Location targetLocation = new Location("");
                targetLocation.setLatitude(52.35526);
                targetLocation.setLongitude(4.94651);

                Location phoneLocation = new Location("");
                phoneLocation.setLatitude(location.getLatitude());
                phoneLocation.setLongitude(location.getLongitude());

                float distanceInMeters = phoneLocation.distanceTo(targetLocation);
                lijst.setText(Float.toString(distanceInMeters));
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
//                System.out.println("-" + toFind + "-");
//                System.out.println(suggestions.indexOf(toFind));
//                System.out.println(codes.get(suggestions.indexOf(toFind)));

                searchText.setText(toFind);
                searchText.setCursorVisible(false);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                StationData data = fb.getStationInfo(codes.get(suggestions.indexOf(toFind)));
                userNameText.setText(data.Lon);
            }
        });

        getLocation();

        if(mDatabase == null)
            Log.d("test", "db is null");

        fb.stationListRequest(this);
        fb.getUsername(mAuth, userNameText);
        fb.getCoordsFromDB();
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

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, locationListener);

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

}
