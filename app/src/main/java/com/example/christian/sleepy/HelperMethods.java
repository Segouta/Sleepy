package com.example.christian.sleepy;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelperMethods {

    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private String stationInfo = "";

    private StationData stationData = new StationData();

    private Suggestions suggestions = new Suggestions();
    private Suggestions s = new Suggestions();

    private List<String> suggestionList = new ArrayList<>();
    private List<String> codeList = new ArrayList<>();

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

    public void getUsername(final FirebaseAuth mAuth, final TextView userNameText) {
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

    public void addToDB(StationData data) {

        mDatabase.child("stations").child(data.Code).setValue(data);
    }

    public void stationListRequest(Context context) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url ="http://christianbijvoets@gmail.com:LBSjuDJ1khvavr8TSq-zAE8uz61NX60ja_VzSWDRFvuq9gk0bJmzFg@webservices.ns.nl/ns-api-stations-v2";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        // lijst.setText("Response is: "+ response);
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
                Log.d("helpers", "did not work...");
                // lijst.setText("That didn't work!");
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

    public List<List<String>> getSuggestions() {

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                suggestionList = dataSnapshot.child("suggestions").getValue(Suggestions.class).suggestionList;
                codeList = dataSnapshot.child("suggestions").getValue(Suggestions.class).codeList;
//                System.out.println("vanaf hier:------------------------------------------------------");
//                for (int i = 0; i < suggestion.suggestionList.size(); i++) {
//                    System.out.println(suggestion.suggestionList.get(i));
//                }
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

        return totList;
    }




}
