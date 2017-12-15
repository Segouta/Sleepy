/**
 * Created by Christian on 7-12-2017
 */

package com.example.christian.sleepy;

import java.util.Date;
import java.util.List;

public class UserData {

    public String username;
    public String email;
    public String password;
    public Date subDate;
    List<String> favorites;

    public UserData() {}

    public UserData(String username, String email, String password, Date subDate, List<String> favorites) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.subDate = subDate;
        this.favorites = favorites;

    }
}
