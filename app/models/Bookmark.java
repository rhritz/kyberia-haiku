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
import com.google.code.morphia.Morphia;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import com.mongodb.QueryBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;

// TODO na druhej strane mozno by bolo dobre ulozit vsetky userove bookmarky
// do jedneho dokumentu;
// i ked sa nam mozno tazsie budu dorabat kategorie (ale mozno nie)
// potom pre kazdy node ukladat last_update_time do caceh a porovnavat
// pri nacitani bookamrkov

@Entity("Bookmark")
public class Bookmark extends MongoEntity {

    private String  destination;     // dest url/path
    private String  alt;             // alt link to diplay next to dest url
    private String  name;            // dest name
    private Integer typ;             // - standard (node) + streams
    private Long    lastVisit;
    // private List    notifications;   // list of activities since last visit(?)
    private ObjectId  uid;

    @Transient
    private Integer numNew;

    private List<String>  tags;

    private static final String USERID = "uid";
    private static final String NAME   = "name";
    private static final String DEST   = "destination";
    
    private static final BasicDBObject sort = new BasicDBObject().append(NAME, 1);

    public Bookmark() {}

    public Bookmark(String dest, ObjectId uid)
    {
        this.destination = dest;
        this.uid         = uid;
        this.name        = NodeContent.load(dest).getName();
        this.lastVisit   = System.currentTimeMillis();
    }

    public static List<Bookmark> getUserBookmarks(String uid)
    {
        List<Bookmark> b =  new LinkedList<Bookmark>();
        LinkedList<String> bkeys = Cache.get("bookmark_" + uid,
                LinkedList.class);
        if (bkeys != null) {
            for (String bkey : bkeys) {
                Bookmark boo = Cache.get("bookmark_" + uid + "_" + bkey,
                        Bookmark.class);
                if (boo == null ) {
                    // invalidated or expired
                    boo = loadAndStore(uid, bkey);
                }
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
                    // TODO cast z tohto mozno neskor do mongojs
                    boo.numNew = loadNotifsForBookmark(boo.destination,
                        boo.lastVisit == null ? 0 : boo.lastVisit );
                    // TODO toto by malo byt nutne len docasne
                    boo.name = NodeContent.load(boo.destination).getName();
                    b.add(boo);
                    String bkey = "bookmark_" + uid + "_" + boo.destination;
                    Cache.add(bkey, boo);
                    bkeys.add(boo.destination);
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
        try {
            BasicDBObject query = new BasicDBObject().append(USERID, uid).
                    append(DEST, dest) ;
            BasicDBObject iobj = (BasicDBObject)
                MongoDB.getDB().getCollection(MongoDB.CBookmark).findOne(query);
            if (iobj !=  null) {
                b = MongoDB.getMorphia().fromDBObject(Bookmark.class,iobj);
                b.numNew = loadNotifsForBookmark(b.destination,
                    b.lastVisit == null ? 0 : b.lastVisit );
                // TODO toto by malo byt nutne len docasne
                b.name = NodeContent.load(b.destination).getName();
                String bkey = "bookmark_" + uid + "_" + b.destination;
                Cache.add(bkey, b);
            }
        } catch (Exception ex) {
            Logger.info("Bookmark.loadAndStore");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return b;
    }

    private static Bookmark getByUserAndDest(String uid, String dest) {
        Bookmark b = Cache.get("bookmark_" + uid + "_" + dest,
                        Bookmark.class);
        if (b != null)
            return b;
        else
            return loadAndStore(uid, dest);
    }

    public static int loadNotifsForBookmark(String nodeId, Long lastVisit)
    {
        int len = 0;
        // Integer clen = Cache.get(ID + username, String.class);
        try {
            BasicDBObject query = new BasicDBObject().append("ids",
                    new ObjectId(nodeId)).append("date",
                    new BasicDBObject("$gt",lastVisit));
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

    // TODO cast z tohto mozno neskor do mongojs
    public static List<NodeContent> getUpdatesForBookmark(
            String nodeId,
            String uid)
    {
        // 1. get bookmark
        List<NodeContent> newNodes = null;
        Bookmark b = getByUserAndDest(uid, nodeId);
        Long lastVisit = b.lastVisit;
        updateVisit(new ObjectId(uid), nodeId);
        // 2. load notifications
        try {
            BasicDBObject query = new BasicDBObject().append("ids",
                    new ObjectId(nodeId)).append("date",
                    new BasicDBObject("$gt",lastVisit));
            // Logger.info("loadNotifsForBookmark::"  + query.toString());
            // BasicDBObject sort = new BasicDBObject().append("date", -1);
            // vlastne chceme natural sort a iba idcka nodes ktore mame zobrazit
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).
                    sort(sort);
            if (iobj !=  null) {
                Logger.info("loadNotifsForBookmark found");
                List<Activity> lll = Lists.transform(iobj.toArray(), 
                        MongoDB.getSelf().toActivity());
                // 3. load nodes we want to show
                // - pozbierajme idcka a zavolajme na to NodeContent.loadMulti()
                List<ObjectId> nodeIds = new LinkedList<ObjectId>();
                for (Activity ac : lll) 
                    nodeIds.add(ac.getOid());
                newNodes = NodeContent.load(nodeIds);
            }
        } catch (Exception ex) {
            Logger.info("loadNotifsForBookmark");
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

    public static void add(String dest, String uid)
    {
        Bookmark b = Cache.get("bookmark_" + uid + "_" + dest, Bookmark.class);
        if (b != null )
            return;
        b = new Bookmark(dest, new ObjectId(uid));
        MongoDB.save(b, MongoDB.CBookmark);
        Cache.delete("bookmark_" + uid);
    }

    public static void delete(String dest, String uid)
    {
        Cache.delete("bookmark_" + uid);
        Cache.delete("bookmark_" + uid + "_" + dest);
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
        // Logger.info("Updating visit::" + "bookmark_" + uid + "_" + location);
        Bookmark b = (Bookmark) Cache.get("bookmark_" + uid + "_" + location);
        if (b != null) {
            b.lastVisit = System.currentTimeMillis();
            Cache.replace("bookmark_" + uid.toString() + "_" + location, b);
            MongoDB.update(b, MongoDB.CBookmark);
        }
    }

    static void invalidate(ObjectId uid, String dest) {
        Cache.delete("bookmark_" + uid + "_" + dest);
    }

    /**
     * @return the uid
     */
    public ObjectId getUid() {
        return uid;
    }
}
