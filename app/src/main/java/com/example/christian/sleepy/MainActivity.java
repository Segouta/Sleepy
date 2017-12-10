package com.example.christian.sleepy;

import android.*;
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
import android.util.Xml;
import android.view.View;
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

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    TextView lijst, userNameText;

    String stationInfo = "";
    String stationName = "";

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lijst = findViewById(R.id.lijst);
        lijst.setMovementMethod(new ScrollingMovementMethod());

        mAuth = FirebaseAuth.getInstance();

        setListener();

        userNameText = findViewById(R.id.userNameView);

        userNameText.setText(mAuth.getCurrentUser().getDisplayName());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationChanged(Location location) {
                lijst.setText(location.getLatitude() + " " + location.getLongitude());
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

        getLocation();

        stationListRequest();
//        readXML();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        addToDB();

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
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 10);
            }
            return;
        }
        locationManager.requestLocationUpdates("gps", 500, 0, locationListener);

    }

    public void addToDB() {

        FirebaseUser user = mAuth.getCurrentUser();

        List<String> names = Arrays.asList("Alkmaar", "Allekemaar", "Station Alkmaar Centraal");
        List<String> synonyms = Arrays.asList("Alkmaar", "Allekemaar", "Station Alkmaar Centraal");
        String Lat = Float.toString((float) 54.998009809);
        String Lon = Float.toString((float) 4.68879998);

        StationData data = new StationData(Lat, Lon, names, synonyms);

        mDatabase.child("stations").child("Broek op Langendijk").setValue(data);
    }

    public void getCoordsFromDB() {

        System.out.println("HIIIIIIIIIIIIIIIIIRRRRR");

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db
                stationName = "Haarlem";
                StationData info = dataSnapshot.child("stations").child(stationName).getValue(StationData.class);

                userNameText.setText(info.Lon);
                System.out.println("HIEEEEEEEEEER" + info.Lon);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIR Something went wrong.");
            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    public void readXML() {
        org.jdom2.input.SAXBuilder saxBuilder = new SAXBuilder();
        try {
            toaster("hier");
            org.jdom2.Document doc = saxBuilder.build(new StringReader(stationInfo));
            toaster("hier2");
            String message = doc.getRootElement().getText();
            toaster("hier3");
            toaster(message);

            Log.d("MyApp","I am here");
        } catch (JDOMException e) {
            // handle JDOMException
        } catch (IOException e) {
            // handle IOException
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
