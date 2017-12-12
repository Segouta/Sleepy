package com.example.christian.sleepy;

import java.util.List;

/**
 * Created by Christian on 12-12-2017.
 */

public class Suggestions {

    List<String> suggestionList;
    List<String> codeList;

    public Suggestions() {}

    public Suggestions(List<String> suggestionList, List<String> codeList) {
        this.suggestionList = suggestionList;
        this.codeList = codeList;
    }
}
