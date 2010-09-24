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
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;

// TODO kategorie + tagy + alt linky
// ? pre kazdy node ukladat last_update_time do cache a porovnavat
// pri nacitani bookmarkov

@Entity("Bookmark")
public class Bookmark extends MongoEntity {

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

    private static final BasicDBObject sort = new BasicDBObject().append(NAME, 1);

    public Bookmark() {}

    public Bookmark(ObjectId dest, ObjectId uid, String type)
    {
        this.dest       = dest;
        this.uid        = uid;
        name       = NodeContent.load(dest).getName();
        lastVisit  = System.currentTimeMillis();
        typ = type;
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
            BasicDBObject query = new BasicDBObject().append(USERID,
                    new ObjectId(uid));
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CBookmark).find(query).
                    sort(sort);
            if (iobj !=  null) {
                Logger.info("user bookmarks found");
                int i = 0;
                bkeys = new LinkedList<String>();
                while(iobj. hasNext())
                {
                    Bookmark boo = MongoDB.getMorphia().fromDBObject(
                            Bookmark.class,(BasicDBObject) iobj.next());
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
            BasicDBObject query = new BasicDBObject().append(USERID, new ObjectId(uid)).
                    append(DEST, new ObjectId(dest)) ;
            BasicDBObject iobj = (BasicDBObject)
                MongoDB.getDB().getCollection(MongoDB.CBookmark).findOne(query);
            if (iobj !=  null) {
                b = MongoDB.getMorphia().fromDBObject(Bookmark.class,iobj);
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

    private static Bookmark getByUserAndDest(ObjectId uid, String dest) {
        Bookmark b = Cache.get(key(uid.toString(), dest), Bookmark.class);
        if (b != null)
            return b;
        else
            return loadAndStore(uid.toString(), dest);
    }

    public int loadNotifsForBookmark()
    {
        int len = 0;
        // Integer clen = Cache.get(ID + username, String.class);
        try {
            BasicDBObject query = new BasicDBObject().
                    append(typ == null ? "ids" : typ, dest).
                    append("date",  new BasicDBObject("$gt",
                    lastVisit == null ? 0 : lastVisit ));
            // Logger.info("loadNotifsForBookmark::"  + query.toString());
            len = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).count();
        } catch (Exception ex) {
            Logger.info("loadNotifsForBookmark");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return len;
    }

    public static List<NodeContent> getUpdatesForBookmark(
            String nodeId,
            ObjectId uid)
    {
        // 1. get bookmark
        List<NodeContent> newNodes = null;
        Bookmark b = getByUserAndDest(uid, nodeId);
        Long lastVisit = b.lastVisit;
        updateVisit(uid, nodeId);
        // 2. load notifications
        try {
            BasicDBObject query = new BasicDBObject().
                    append(b.typ == null ? "ids" : b.typ,
                    new ObjectId(nodeId)).append("date",
                    new BasicDBObject("$gt",lastVisit));
            // Logger.info("getUpdatesForBookmark::"  + query.toString());
            // BasicDBObject sort = new BasicDBObject().append("date", -1);
            // vlastne chceme natural sort a iba idcka nodes ktore mame zobrazit
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).
                    sort(sort);
            if (iobj !=  null) {
                // Logger.info("getUpdatesForBookmark found");
                List<Activity> lll = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toActivity());
                // 3. load nodes we want to show
                List<ObjectId> nodeIds = new LinkedList<ObjectId>();
                for (Activity ac : lll)
                    nodeIds.add(ac.getOid());
                newNodes = NodeContent.load(nodeIds);
            }
        } catch (Exception ex) {
            Logger.info("getUpdatesForBookmark");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return newNodes;
    }

    // List of Bookmarks by destination id
    public static List<Bookmark> getByDest(ObjectId dest)
    {
        List<Bookmark> b = null;
        try {
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CBookmark).
                    find(new BasicDBObject().append(DEST, dest));
            // Logger.info("getByDest::" + iobj);
            if (iobj !=  null)
                b = Lists.transform(iobj.toArray(),
                            MongoDB.getSelf().toBookmark());
            else
                b =  new LinkedList<Bookmark>();
        } catch (Exception ex) {
            Logger.info("getByDest::" + dest);
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return b;
    }

    public static void add(String dest, String uid, String type)
    {
        Bookmark b = Cache.get(key(uid,dest), Bookmark.class);
        if (b != null )
            return;
        b = new Bookmark(new ObjectId(dest), new ObjectId(uid), type);
        MongoDB.save(b, MongoDB.CBookmark);
        Cache.delete("bookmark_" + uid);
    }

    public static void delete(String dest, String uid) // -> ObjectId
    {
        Cache.delete("bookmark_" + uid);
        Cache.delete(key(uid,dest));
        try {
            BasicDBObject query = new BasicDBObject().append(USERID, uid)
                    .append(DEST, dest);
            Logger.info(query.toString());
            MongoDB.getDB().getCollection(MongoDB.CBookmark).remove(query);
        } catch (Exception ex) {
            Logger.info("Bookmark.delete");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    static void updateVisit(ObjectId uid, String location) {
        String key = key(uid.toString(),location);
        Logger.info("Updating visit::" + key);
        Bookmark b = Cache.get(key, Bookmark.class);
        if (b != null) {
            // Logger.info("Updating visit - really");
            b.lastVisit = System.currentTimeMillis();
            b.numNew    = 0;
            Cache.replace(key, b);
            MongoDB.update(b, MongoDB.CBookmark);
        }
    }

    static void invalidate(ObjectId uid, String dest) {
        Logger.info("Invalidate visit::" + key(uid.toString(),dest));
        Cache.delete(key(uid.toString(),dest));
    }

    /**
     * @return the uid
     */
    public ObjectId getUid() {
        return uid;
    }

    private static String key(String uid, String dest) {
        return "bookmark_" + uid + "_" + dest;
    }
}
