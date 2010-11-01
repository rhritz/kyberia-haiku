/*
    Kyberia Haiku - advanced community web application
    Copyright (C) 2010 Robert Hritz

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package models;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import play.cache.Cache;
import plugins.MongoDB;


@Entity("UserLocation")
public class UserLocation extends MongoEntity {

    public static DBCollection dbcol = null;
    private static final String key  = "user_location_";

    private static final String uls = "user_locations";
    private static final String ulk = "user_location_";
    private static final String userCacheExpiry = "1h";

    private ObjectId       uid;
    private ObjectId       loc;
    private Long           time;

    @Transient
    private String         username;
    
    @Transient
    private String         locname;

    public UserLocation () {}

    public UserLocation(User user,
                        ObjectId location,
                        Long time)
    {
        uid      = user.getId();
        username = user.getUsername();
        loc      = location;
        this.time     = time;
    }

    // TODO vymazat pri logoute + nebude lepsia capped collection?
    public static synchronized void saveVisit(User user, ObjectId location)
    {
        if (location == null || user.isInvisible())
            return;
        UserLocation ul = new UserLocation(user, location, System.currentTimeMillis());
        Cache.set(ulk + user.getIdString(), ul, userCacheExpiry);
        ul.save();
        Bookmark.updateVisit(user.getId(), location.toString());
        SortedSet<UserLocation> ll = Cache.get(uls, SortedSet.class);
        if ( ll == null )
            ll = new TreeSet<UserLocation>(new LocationComparator());
        else if(ll.contains(ul)) // remove old location
            ll.remove(ul);
        ll.add(ul);
        Cache.set( uls, ll);
        // +:
        // - pozri do zoznamu starych lokacii ci niekde nie je, ak je vymaz
        // - pridaj pre aktualnu lokaciu
    }

    // vsetci useri kdekolvek, teraz
    public static SortedSet<UserLocation> getAll()
    {
        SortedSet<UserLocation> ll = Cache.get(uls, SortedSet.class);
        return ll;
    }

    // vsetci useri pre danu lokaciu, teraz - toto zatial neviem ako
    // - nejaka hashmultimap alebo co?
    public static List<UserLocation> getUsersByLocation(String location)
    {
        return new ArrayList<UserLocation>();
    }

    // sucasna location daneho suera
    public static UserLocation getUserLocation(String uid)
    {
        return Cache.get(ulk + uid, UserLocation.class);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != UserLocation.class)
            return false;
        if (((UserLocation) obj).getUsername().equals(this.getUsername()))
            return true;
        else
            return false;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the uid
     */
    public ObjectId getUid() {
        return uid;
    }

    /**
     * @return the loc
     */
    public ObjectId getLoc() {
        return loc;
    }

    /**
     * @return the time
     */
    public Long getTime() {
        return time;
    }

    public Long getTimeAgo() {
        return (System.currentTimeMillis() - time)/1000l;
    }

    private static class LocationComparator implements Comparator<UserLocation>
    {
        public int compare(UserLocation u1, UserLocation u2) {
            return u1.getUsername().compareToIgnoreCase(u2.getUsername());
        }
    }

    @Override
    public UserLocation enhance() {
        return this;
    }

    @Override
    public DBCollection getCollection() {
        return dbcol;
    }

    @Override
    public String key() {
        return key;
    }

}
