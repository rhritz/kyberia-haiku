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

import com.google.code.morphia.AbstractMongoEntity;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoTransient;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import play.cache.Cache;
import play.Logger;
import plugins.MongoDB;


@MongoDocument
public class UserLocation extends AbstractMongoEntity {

    @MongoTransient
    private String         username; // for sorting
    private String         userid;
    private String         location;
    @MongoTransient
    private String         locname; // <- nazov lokacie
    private Long           time;

    public UserLocation () {}

    public UserLocation(String userid,
                        String username,
                        String location,
                        Long time)
    {
        this.userid   = userid;
        this.username = username;
        this.location = location;
        this.time     = time;
    }

    // !!! synchronized aby sme nemali problem s insertom do TreeSetu
    // - mozno aj tak budeme, potom to treba zmenit
    // TODO vymazat pri logoute
    // TODO not-node lokacie nejak vyriesit (pridat loc-name a nastavit len to,
    // potom nezapisovat do db? might work.
    // invisible - bude ukazovat ze clovek je online ale inak nic?
    public static synchronized void saveVisit(User user, String location)
    {
        // if location == null nezapisuj do db...
        String userid = user.getId();
        UserLocation ul = new UserLocation(userid, user.getUsername(),
                location, System.currentTimeMillis());
        Cache.set("user_location" + user.getId(), ul); // + expiry
        ul.save();
        Bookmark.updateVisit(user.getId(), location);
        // TODO z tohto asi perzistentny plugin, stale to de/serializovat z cache
        // je blbost
        SortedSet<UserLocation> ll = Cache.get("user_locations",
                SortedSet.class);
        if ( ll==null )
            ll = new TreeSet<UserLocation>(new LocationComparator());
        if ( ll.contains(ul)) // vyhodime staru lokaciu
            ll.remove(ul);
        ll.add(ul);
        Cache.set("user_locations", ll);
    }

    private void save()
    {
        MongoDB.save(this, MongoDB.CUserLocation);
    }

    // vsetci useri kdekolvek, teraz
    public static SortedSet<UserLocation> getAll()
    {
        SortedSet<UserLocation> ll = 
                Cache.get("user_locations", SortedSet.class);
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
        return (UserLocation) Cache.get("user_location" + uid);
    }

    // historia daneho usera - z mongo
    public static List<UserLocation> getUserLocationHistory(
            String uid,
            Integer start,
            Integer count)
    {
        List<UserLocation> r = null;
        try {
            BasicDBObject query = new BasicDBObject().append("userid", uid);
            BasicDBObject sort = new BasicDBObject().append("time", -1); // TODO natural sort
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CUserLocation).
                    find(query).sort(sort).skip(start).limit(count);
            if (iobj ==  null) {
                r = new ArrayList<UserLocation>();
            } else {
                Logger.info("user threads found");
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toUserLocation());
            }
        } catch (Exception ex) {
            Logger.info("getUserThreads");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    // historia danej location - z mongo
    public static List<UserLocation> getLocationHistory(
            String uid,
            Integer start,
            Integer count)
    {
        List<UserLocation> r = null;
        try {
            // hm.. toto moze byt vlastne staticke/singleton...
            BasicDBObject query = new BasicDBObject().append("location", uid);
            BasicDBObject sort = new BasicDBObject().append("time", -1); // TODO natural sort
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CUserLocation).
                    find(query).sort(sort).skip(start).limit(count);
            if (iobj ==  null) {
                r = new ArrayList<UserLocation>();
            } else {
                Logger.info("user threads found");
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toUserLocation());
            }
        } catch (Exception ex) {
            Logger.info("getUserThreads");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    @Override
    public boolean equals(Object obj)
    {
        // ClassCastException ?
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
     * @return the userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
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
}
