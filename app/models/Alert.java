package models;

// a class to collect warnings, errors, mail & other notifcations

import java.util.List;

// so we can display them in the notification area

public class Alert {

    // add an alert for current user
    public static void push(String uid, String alert)
    {
        // check if the user has a List already,
        // if not create one
        // add alert to the list
        // -> Session
    }

    // pop & destroy
    public static List<String> pop(String uid)
    {
        // return list of alerts and remove it from the hash/cache
        return null;
    }
}
