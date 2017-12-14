package com.example.christian.sleepy;

import android.app.Dialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelperMethods {

    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private String stationInfo = "";

//    Only update if firebase is 4 days not updated, to prevent NS API from overrequest.
    int updateAmount = 4;
    boolean update = false;
    long millisecPerDay = 86400000;

    private Suggestions suggestions = new Suggestions();

    private List<String> suggestionList = new ArrayList<>();
    private List<String> codeList = new ArrayList<>();

    public void addToDB(StationData data) {

        mDatabase.child("stations").child(data.Code).setValue(data);
    }

    public void stationListUpdate(final Context context) {
        final Date now = Calendar.getInstance().getTime();

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("updateTime").getValue(Date.class) == null) {
                    mDatabase.child("updateTime").setValue(now);
                    update = true;

                } else {
                    Date then = dataSnapshot.child("updateTime").getValue(Date.class);
                    if(Math.abs((now.getTime()-then.getTime())/millisecPerDay) > updateAmount) {
                        update = true;
                        mDatabase.child("updateTime").setValue(now);
                    } else {
                        update = false;
                    }
                }
                if (update == true) {
                    stationListRequest(context);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addListenerForSingleValueEvent(postListener);
    }

    public void stationListRequest(Context context) {

        System.out.println("=====================================FB updating");

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://christianbijvoets@gmail.com:LBSjuDJ1khvavr8TSq-zAE8uz61NX60ja_VzSWDRFvuq9gk0bJmzFg@webservices.ns.nl/ns-api-stations-v2";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        // lijst.setText("Response is: "+ response);
                        stationInfo = response;
                        Log.d("hoi", "onResponse: done");
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
                Log.d("helpers", "did not work...");
                // lijst.setText("That didn't work!");
            }
        }) {
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

    public void XMLtoDB() throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        StationData toDB = new StationData();
        List<String> namesToAdd = new ArrayList<String>();
        List<String> synonymsToAdd = new ArrayList<String>();


        xpp.setInput(new StringReader(stationInfo));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            String name = xpp.getName();

            if (name != null && name.equals("Code") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
//                    System.out.println(xpp.getText());
                    toDB.Code = xpp.getText();
                }
            }
            else if (name != null && (name.equals("Kort") || name.equals("Middel") || name.equals("Lang")) && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    suggestionList.add(xpp.getText());
                    codeList.add(toDB.Code);
                    namesToAdd.add(xpp.getText());
                }
            }
            else if (name != null && name.equals("Synoniem") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    suggestionList.add(xpp.getText());
                    codeList.add(toDB.Code);
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
        suggestions.suggestionList = suggestionList;
        suggestions.codeList = codeList;

        addSuggestionstoDB();
    }

    public void addSuggestionstoDB() {

        mDatabase.child("suggestions").setValue(suggestions);
    }

    public void addToFavorites(final String toAdd, final FirebaseAuth mAuth) {

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get from db

                UserData data = dataSnapshot.child("users").child(mAuth.getCurrentUser().getUid()).getValue(UserData.class);

                if (data.favorites == null) {
                    System.out.println("nothin here yet");
                    List<String> inName = new ArrayList<>();
                    inName.add(toAdd);
                    data.favorites = inName;
                    mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).setValue(data);

                } else {
                    System.out.println("already things here");
                    List<String> inName = data.favorites;
                    if (inName.contains(toAdd)) {
                        System.out.println("already in favs");
                    } else {
                        inName.add(toAdd);
                        data.favorites = inName;
                        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).setValue(data);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Something went wrong.");
            }
        };
        mDatabase.addListenerForSingleValueEvent(postListener);

    }

}
