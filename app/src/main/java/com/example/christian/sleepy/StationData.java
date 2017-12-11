package com.example.christian.sleepy;

import java.util.List;

/**
 * Created by Christian on 7-12-2017.
 */

public class StationData {

    public String Code;
    public String Lat;
    public String Lon;
    public List<String> names;
    public List<String> synonyms;

    public StationData() {}

    public StationData(String Code, String Lat, String Lon, List<String> names, List<String> synonyms) {
        this.Code = Code;
        this.Lat = Lat;
        this.Lon = Lon;
        this.names = names;
        this.synonyms = synonyms;

    }
}