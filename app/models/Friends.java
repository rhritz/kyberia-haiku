package models;

import com.mongodb.ObjectId;
import java.util.LinkedList;
import java.util.List;

class Friends {

    static List<Friend> getUserFriends(String id) {
        // throw new UnsupportedOperationException("Not yet implemented");
        // v konecnom doeslku mozno len List v User
        return new LinkedList<Friend>();
    }

    boolean isFriend(ObjectId who, ObjectId toWhom)
    {
        // na toto budeme chciet odpvodat hlavne z cache
        return true;
    }

    // alebo zeby skro class UserRelations? ani ne.

}
