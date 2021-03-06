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
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;
import static com.google.common.base.Preconditions.checkNotNull;

// TODO kategorie + tagy + alt linky
// ? pre kazdy node ukladat last_update_time do cache a porovnavat
// pri nacitani bookmarkov

@Entity("Bookmark")
public class Bookmark extends MongoEntity {

    public static DBCollection dbcol = null;
    private static final String key = "bookmark_";

    private ObjectId  dest;
    private String    alt;             // alt link to diplay next to dest url
    private String    name;            // dest name
    private String    typ;
    private Long      lastVisit;
    // private List    notifications;   // list of activities since last visit(?)
    private ObjectId  uid;

    @Transient
    private Integer numNew;

    private List<String>  tags;

    private static final String USERID = "uid";
    private static final String NAME   = "name";
    private static final String DEST   = "dest";

    private static final BasicDBObject sort = new BasicDBObject(NAME, 1);

    public Bookmark() {}

    public Bookmark(ObjectId dest, User user, String type)
    {

        this.dest  = checkNotNull(dest);
        uid        = checkNotNull(user).getId();
        name       = checkNotNull(NodeContent.load(dest, user)).getName();
        lastVisit  = System.currentTimeMillis();
        typ        = checkNotNull(type);
    }

    public static List<Bookmark> getUserBookmarks(String uid)
    {
        List<Bookmark> b =  new LinkedList<Bookmark>();
        List<String> bkeys = Cache.get("bookmark_" + uid, LinkedList.class);
        if (bkeys != null) {
            Logger.info("user bookmarks cached");
            for (String bkey : bkeys) {
                Bookmark boo = Cache.get(key(uid,bkey), Bookmark.class);
                // invalidated or expired
                if (boo == null )
                    boo = loadAndStore(uid, bkey);
                b.add(boo);
            }
            return b;
        }
        try {
            BasicDBObject query = new BasicDBObject(USERID, toId(uid));
            DBCursor iobj = dbcol.find(query).sort(sort);
            if (iobj !=  null) {
                Logger.info("user bookmarks found");
                int i = 0;
                bkeys = new LinkedList<String>();
                while(iobj.hasNext())
                {
                    Bookmark boo = MongoDB.fromDBObject(Bookmark.class, iobj.next());
                    boo.numNew = boo.loadNotifsForBookmark();
                    // TODO toto by malo byt nutne len docasne
                    b.add(boo);
                    Cache.add(key(uid, boo.dest.toString()), boo);
                    bkeys.add(boo.dest.toString());
                }
                Cache.add("bookmark_" + uid, bkeys);
            }
        } catch (Exception ex) {
            Logger.info("getUserBookmarks");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return b;
    }

    // load one bookmark
    private static Bookmark loadAndStore(String uid, String dest) {
        Bookmark b = null;
        Logger.info("Bookmark.loadAndStore :: " + key(uid,dest));
        try {
            BasicDBObject query = new BasicDBObject(USERID, toId(uid)).
                    append(DEST, toId(dest));
            DBObject iobj = dbcol.findOne(query);
            if (iobj !=  null) {
                b = MongoDB.fromDBObject(Bookmark.class,iobj);
                b.numNew = b.loadNotifsForBookmark();
                Cache.add(key(uid,dest), b);
            }
        } catch (Exception ex) {
            Logger.info("Bookmark.loadAndStore");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return b;
    }

    public static Bookmark getByUserAndDest(ObjectId uid, String dest) {
        Bookmark b = Cache.get(key(uid.toString(), dest), Bookmark.class);
        if (b != null)
            return b;
        else
            return loadAndStore(uid.toString(), dest);
    }

    // TODO
    public static Bookmark getByUserAndDest(ObjectId uid, ObjectId dest) {
        Bookmark b = Cache.get(key(uid.toString(), dest.toString()), Bookmark.class);
        if (b != null)
            return b;
        else
            return loadAndStore(uid.toString(), dest.toString());
    }

    public int loadNotifsForBookmark()
    {
        int len = 0;
        // Integer clen = Cache.get(ID + username, String.class);
        try {
            BasicDBObject query = new BasicDBObject(typ == null ? "ids" : typ, dest).
                    append("date",  new BasicDBObject("$gt",
                    lastVisit == null ? 0 : lastVisit ));
            // Logger.info("loadNotifsForBookmark::"  + query.toString());
            len = Activity.dbcol.find(query).count();
        } catch (Exception ex) {
            Logger.info("loadNotifsForBookmark");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return len;
    }

    // List of Bookmarks by destination id
    public static List<Bookmark> getByDest(ObjectId dest)
    {
        List<Bookmark> b = null;
        try {
            DBCursor iobj = dbcol.find(new BasicDBObject(DEST, dest));
            // Logger.info("getByDest::" + iobj);
            b = MongoDB.transform(iobj, MongoDB.getSelf().toBookmark());
        } catch (Exception ex) {
            Logger.info("getByDest::" + dest);
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return b;
    }

    public static void add(ObjectId dest, User user, String type)
    {
        ObjectId uid = user.getId();
        Bookmark b = Cache.get(key(uid.toString(),dest.toString()), Bookmark.class);
        if (b != null)
            return;
        b = new Bookmark(dest, user, type);
        b.save();
        Cache.delete(key + uid);
    }

    public static void delete(String dest, String uid) // -> ObjectId
    {
        Cache.delete(key + uid);
        Cache.delete(key(uid,dest));
        try {
            dbcol.remove(new BasicDBObject(USERID, uid).append(DEST, dest));
        } catch (Exception ex) {
            Logger.info("Bookmark.delete");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public static void updateVisit(ObjectId uid, String location) {
        String key = key(uid.toString(),location);
        Logger.info("Updating visit::" + key);
        Bookmark b = Cache.get(key, Bookmark.class);
        if (b != null) {
            // Logger.info("Updating visit - really");
            b.lastVisit = System.currentTimeMillis();
            b.numNew    = 0;
            Cache.replace(key, b);
            b.update();
        }
    }

    static void invalidate(ObjectId uid, String dest) {
        Cache.delete(key(uid.toString(),dest));
    }

    /**
     * @return the uid
     */
    public ObjectId getUid() {
        return uid;
    }

    private static String key(String uid, String dest) {
        return key + uid + "_" + dest;
    }

    /**
     * @return the lastVisit
     */
    public Long getLastVisit() {
        return lastVisit;
    }

    /**
     * @param lastVisit the lastVisit to set
     */
    public void setLastVisit(Long lastVisit) {
        this.lastVisit = lastVisit;
    }

    /**
     * @return the typ
     */
    public String getTyp() {
        return typ;
    }

    /**
     * @param typ the typ to set
     */
    public void setTyp(String typ) {
        this.typ = typ;
    }

    @Override
    public Bookmark enhance() {
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
