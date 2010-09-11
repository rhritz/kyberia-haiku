package models;


import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;

@Entity
public class Ignore extends MongoEntity {

    public static void isIgnored(User u1, User u2)
    {

    }

}
