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
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.annotations.MongoTransient;
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

@MongoDocument
public class Bookmark extends AbstractMongoEntity {

    private String  destination;     // dest url/path
    private String  alt;             // alt link to diplay next to dest url
    private String  name;            // dest name
    private Integer typ;             // - standard (node) + streams
    private Long    lastVisit;
    private List    notifications;   // list of activities since last visit(?)
    private String  uid;

    @MongoTransient
    private Integer numNew;

    private List<String>  tags;

    private static final String USERID = "uid";
    private static final String NAME   = "name";
    private static final String DEST   = "destination";
    
    private static final BasicDBObject sort = new BasicDBObject().append(NAME, 1);

    public Bookmark() {}

    public Bookmark(String dest, String uid)
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
                Bookmark boo = Cache.get(bkey, Bookmark.class);
                // tu je pripadne miesto na kontrolu, ci je aktualny
                // - tak ci tak sa musi robit invalidate pri upd Activities
                // pripadne umiestnit do nejakeho zoznamu zoznam aktualnych/
                // neaktulnych bookmarkov?
                b.add(boo);
            }
            return b;
        }
        bkeys = new LinkedList<String>();
        try {
            BasicDBObject query = new BasicDBObject().append(USERID, uid);
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CBookmark).find(query).
                    sort(sort);
            if (iobj !=  null) {
                Logger.info("user bookmarks found");
                int i = 0;
                while(iobj. hasNext())
                {
                    Bookmark boo = MongoDB.getMorphia().fromDBObject(
                            Bookmark.class,(BasicDBObject) iobj.next());
                    boo.numNew = loadNotifsForBookmark(boo.destination,
                        boo.lastVisit == null ? 0 : boo.lastVisit );
                    // TODO toto by malo byt nutne len docasne
                    boo.name = NodeContent.load(boo.destination).getName();
                    b.add(boo);
                    String bkey = "bookmark_" + uid + "_" + boo.destination;
                    Cache.add(bkey, boo);
                    bkeys.add(bkey);
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

    //
    // alebo aj nie static
    // - v kazdom pripade ked ulozime notification, mozeme akutalny stav
    // notifikovanej nody ulozit do cache, co nam vyrazne zlacni
    // pohlady do bookmarkov - povedzme s expiry na 5 minut?
    // + ked updatujeme nieco s touto nodou tak ho vyhodime z cache
    public static int loadNotifsForBookmark(String nodeId, Long lastVisit)
    {
        // pppommmallleeee
        // List<Activity> b = new LinkedList<Activity>();
        int len = 0;
        // Integer clen = Cache.get(ID + username, String.class);
        try {
            BasicDBObject query = new BasicDBObject().append("ids",
                    nodeId).append("date",
                    new BasicDBObject("$gt",lastVisit));
            // Logger.info("loadNotifsForBookmark::"  + query.toString());
            // BasicDBObject sort = new BasicDBObject().append("date", -1);
                    // vlastne chceme natural sort
            /*
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).
                    sort(sort);
             */
            len = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).count();
            // Logger.info("You can haz " + len + " new nodez");
            /* TODO zoznam novych nodes
            if (iobj !=  null) {
                Logger.info("loadNotifsForBookmark found");
                while(iobj.hasNext())
                {
                    b.add(MongoDB.getMorphia().fromDBObject(Activity.class,
                           (BasicDBObject) iobj.next()));
                }
            }*/
        } catch (Exception ex) {
            Logger.info("loadNotifsForBookmark");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        // anyway tu na konci mame co sme chceli
        return len;
    }

    public static void add(String dest, String uid)
    {
        Bookmark b = Cache.get("bookmark_" + uid + "_" + dest, Bookmark.class);
        if (b != null )
            return;
        b = new Bookmark(dest, uid);
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

    static void updateVisit(String uid, String location) {
        // Logger.info("Updating visit::" + "bookmark_" + uid + "_" + location);
        Bookmark b = (Bookmark) Cache.get("bookmark_" + uid + "_" + location);
        if (b != null) {
            b.lastVisit = System.currentTimeMillis();
            Cache.replace("bookmark_" + uid + "_" + location, b);
            MongoDB.update(b, MongoDB.CBookmark);
        }
    }
}
