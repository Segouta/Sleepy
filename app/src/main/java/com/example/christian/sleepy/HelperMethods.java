package com.example.christian.sleepy;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelperMethods {

    // create necessary variables
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private String stationInfo = "";

    private int updateAmount = 4;
    private boolean update = false;
    private long millisecPerDay = 86400000;

    private Suggestions suggestions = new Suggestions();
    private List<String> suggestionList = new ArrayList<>();
    private List<String> codeList = new ArrayList<>();

    public void addToDB(StationData data) {
        // this function adds StationData object to firebase
        mDatabase.child("stations").child(data.Code).setValue(data);
    }

    public void stationListUpdate(final Context context) {
        // this function will be called every time a user starts the app but will fire stationListRequest
        // only when the last update from the NS API to the firebase was longer then "updateAmount" days
        // ago, to prevent NS API from overrequesting and from the app doing to much work

        final Date now = Calendar.getInstance().getTime();

        // get last update time from database and calculate difference with current time
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // if a time is not  in the firebase (first time app is used ever) create Date object set to current
                if (dataSnapshot.child("updateTime").getValue(Date.class) == null) {
                    mDatabase.child("updateTime").setValue(now);
                    update = true;
                } else {
                    // else, get difference with last update and accordingly make update required or not
                    Date then = dataSnapshot.child("updateTime").getValue(Date.class);
                    if(Math.abs((now.getTime()-then.getTime())/millisecPerDay) > updateAmount) {
                        update = true;
                        mDatabase.child("updateTime").setValue(now);
                    } else {
                        update = false;
                    }
                }
                // if update was set true an update is required
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
        // this method allows user to get the information from the NS API to the database

        // instantiate the requestQueue
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://christianbijvoets@gmail.com:LBSjuDJ1khvavr8TSq-zAE8uz61NX60ja_VzSWDRFvuq9gk0bJmzFg@webservices.ns.nl/ns-api-stations-v2";

        // request a string response from the provided URL
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // safe entire xml as string
                        stationInfo = response;
                        try {
                            // convert response to settable data for the firebase database
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
            }
        }) {
            // and provide the request with the correct headers, in this case username and password for NS API
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
        // this method parses the xml response from the NS and stores it in the database

        // setting up the xml parser
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        // setting up StationData Object and lists
        StationData toDB = new StationData();
        List<String> namesToAdd = new ArrayList<String>();
        List<String> synonymsToAdd = new ArrayList<String>();

        // start reading the xml string
        xpp.setInput(new StringReader(stationInfo));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // iterate through the whole string till its done storing each part in a String to use
            String name = xpp.getName();

            // store code
            if (name != null && name.equals("Code") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    toDB.Code = xpp.getText();
                }
            }
            // store names
            else if (name != null && (name.equals("Kort") || name.equals("Middel") || name.equals("Lang")) && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    suggestionList.add(xpp.getText());
                    codeList.add(toDB.Code);
                    namesToAdd.add(xpp.getText());
                }
            }
            // store synonyms
            else if (name != null && name.equals("Synoniem") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {
                    suggestionList.add(xpp.getText());
                    codeList.add(toDB.Code);
                    synonymsToAdd.add(xpp.getText());
                }
            }
            // store latitude
            else if (name != null && name.equals("Lat") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    toDB.Lat = xpp.getText();
                }
            }
            // store longitude
            else if (name != null && name.equals("Lon") && eventType == XmlPullParser.START_TAG) {
                if (xpp.next() == XmlPullParser.TEXT) {

                    toDB.Lon = xpp.getText();
                }
            }

            // add all to the StationData object
            toDB.names = namesToAdd;
            toDB.synonyms = synonymsToAdd;

            // if a station is done in the xml, store it in the database
            if (name != null && name.equals("Station") && eventType == XmlPullParser.END_TAG) {
                addToDB(toDB);
                namesToAdd.clear();
                synonymsToAdd.clear();
            }

            // go to next part of the xml
            eventType = xpp.next();
        }
        suggestions.suggestionList = suggestionList;
        suggestions.codeList = codeList;

        // also, add all the possible names and corresponding station codes to another list for suggestions
        addSuggestionstoDB();
    }

    public void addSuggestionstoDB() {
        // add the station suggestions to the database to be found by the user in the edittext
        mDatabase.child("suggestions").setValue(suggestions);
    }

    public void addToFavorites(final String toAdd, final FirebaseAuth mAuth) {
        // this method adds a station to the current user's favorites in firebase
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // get current user's UserData Object from db
                UserData data = dataSnapshot.child("users").child(mAuth.getCurrentUser().getUid()).getValue(UserData.class);

                // if the list is empty, create it and add the station to be favorited, put it back in firebase
                if (data.favorites == null) {
                    List<String> inName = new ArrayList<>();
                    inName.add(toAdd);
                    data.favorites = inName;
                    mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).setValue(data);
                } else {
                    // if the list was not empty, get list and add to that list, put it back in firebase
                    List<String> inName = data.favorites;
                    // but only add it if it did not exist already
                    if (!inName.contains(toAdd)) {
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
