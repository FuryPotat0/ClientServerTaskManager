package main.clientServerConversation;

import java.io.Serializable;

public class UserData implements Serializable {
    private String username;
    private String userPassword;

    public UserData(String username, String userPassword){
        this.username = username;
        this.userPassword = userPassword;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getUsername() {
        return username;
    }
}

