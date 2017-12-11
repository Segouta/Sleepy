package com.example.christian.sleepy;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    TextView lijst, userNameText;

    String stationInfo = "";
    String stationName = "";

    List<StationData> DBqueue = new ArrayList<StationData>();

    private DatabaseReference mDatabase;

    private String[] COUNTRIES = new String[] {"Belgium", "France", "Italy", "Germany", "Spain"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lijst = findViewById(R.id.lijst);
        lijst.setMovementMethod(new ScrollingMovementMethod());

        mAuth = FirebaseAuth.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        MultiAutoCompleteTextView searchText = findViewById(R.id.searchText);
        searchText.setAdapter(adapter);
        searchText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        setListener();

        userNameText = findViewById(R.id.userNameView);

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
                userNameText.setText(Float.toString(distanceInMeters));
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

        getLocation();

        stationListRequest();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        getUsername();

        if(mDatabase == null)
            Log.d("test", "db is null");

//        addToDB();

        getCoordsFromDB();
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

    public void addToDBqueue(StationData data) {

        DBqueue.add(data);
    }

    public void addToDB(StationData data) {

        mDatabase.child("stations").child(data.Code).setValue(data);
    }

    public void getUsername() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
                String gotUsername = dataSnapshot.child("users").child(mAuth.getUid()).child("username").getValue().toString();

                userNameText.setText(gotUsername);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIR Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    public void getCoordsFromDB() {


        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
//                stationName = "Haarlem";
//                StationData info = dataSnapshot.child("stations").child(stationName).getValue(StationData.class);
//
//                System.out.println(info.Lon);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong...");
            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    public void XMLtoDB() throws XmlPullParserException, IOException {

        System.out.println("---------------------------------------------");

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        StationData toDB = new StationData();
        List<String> namesToAdd = new ArrayList<String>();
        List<String> synonymsToAdd = new ArrayList<String>();

        int counter = 0;

        xpp.setInput(new StringReader (stationInfo));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            String name = xpp.getName();

            if (name != null && name.equals("Code") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    System.out.println(xpp.getText());
                    toDB.Code = xpp.getText();
                }
            }
            else if (name != null && (name.equals("Kort") || name.equals("Middel") || name.equals("Lang")) && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    namesToAdd.add(xpp.getText());
                }
            }
            else if (name != null && name.equals("Synoniem") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    synonymsToAdd.add(xpp.getText());
                }
            }
            else if (name != null && name.equals("Lat") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    toDB.Lat = xpp.getText();
                }
            }
            else if (name != null && name.equals("Lon") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    toDB.Lon = xpp.getText();
                }
            }
            toDB.names = namesToAdd;
            toDB.synonyms = synonymsToAdd;

            if (name != null && name.equals("Station") && eventType == XmlPullParser.END_TAG) {
//                addToDBqueue(toDB);

                addToDB(toDB);
                namesToAdd.clear();
                synonymsToAdd.clear();
            }

            eventType = xpp.next();
        }

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

    public void stationListRequest() {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://christianbijvoets@gmail.com:LBSjuDJ1khvavr8TSq-zAE8uz61NX60ja_VzSWDRFvuq9gk0bJmzFg@webservices.ns.nl/ns-api-stations-v2";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        lijst.setText("Response is: "+ response);
                        stationInfo = response;
                        try {
                            XMLtoDB();
                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                lijst.setText("That didn't work!");
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String credentials = "christianbijvoets@gmail.com:LBSjuDJ1khvavr8TSq-zAE8uz61NX60ja_VzSWDRFvuq9gk0bJmzFg";
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", auth);
                return headers;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
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
