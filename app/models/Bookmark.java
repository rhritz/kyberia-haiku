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
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import com.mongodb.QueryBuilder;
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

    private String destination; // dest url/path
    private String alt; // alt link to diplay next to dest url
    private String name; // dest name.
    // most of the time this is just the name od the dest content
    private Long lastVisit;
    private List notifications; // list of activities since last visit
    private String uid;

    private List<String>  tags;

    private static final String USERID = "uid";
    private static final String NAME   = "name";

    public Bookmark() {}

    public Bookmark(String dest, String uid)
    {
        this.destination = dest;
        this.uid  = uid;
        // this.name = Document.getNameForId(uid);
        this.lastVisit = System.currentTimeMillis();
    }

    public static List<Bookmark> getUserBookmarks(String uid)
    {
        // coll.find({user:userid}
        // each bookmark should, in some way, point to an activity list
        // saying something along the lines of 'give me the number of items I haven't seen yet'
        // or 'this item was last updated after your last visit there'
        List<Bookmark> b = new LinkedList<Bookmark>();
        try {
            BasicDBObject query = new BasicDBObject().append(USERID, uid);
            BasicDBObject sort = new BasicDBObject().append(NAME, 1);
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CBookmark).find(query).
                    sort(sort);
            if (iobj !=  null) {
                Logger.info("user threads found");
                int i = 0;
                while(iobj. hasNext())
                {
                    Bookmark boo = MongoDB.getMorphia().fromDBObject(
                            Bookmark.class,(BasicDBObject) iobj.next());
                    b.add(boo);
                    Cache.add("bookmark_" + uid + "_" + boo.destination, boo);
                }
            }
        } catch (Exception ex) {
            Logger.info("getUserThreads");
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
        try {
            BasicDBObject query = new BasicDBObject().append("ids",
                    nodeId).append("date",
                    new BasicDBObject("$gt",lastVisit));
            Logger.info("loadNotifsForBookmark::"  + query.toString());
            // BasicDBObject sort = new BasicDBObject().append("date", -1);
                    // vlastne chceme natural sort
            /*
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).
                    sort(sort);
             */
            len = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).count();
            Logger.info("You can haz " + len + " new nodez");
            /*
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

    public static String viewUserBookmarks(String uid)
    {
        List<Bookmark> b = getUserBookmarks(uid);
        if (b == null)
        {
            return "";
        }
        StringBuilder s = new StringBuilder();
        for (Bookmark bk : b )
        {
            // opat enjaky reverzny routing v tmpl + zmenit id/gid na id/mongoid
            NodeContent node = NodeContent.load(bk.destination);
            // asi skor naraz pre vsetkych
            // Activity news = Activity.load(bk.destination, bk.lastVisit);
            if (node != null) {
                int news = loadNotifsForBookmark(bk.destination, 
                        bk.lastVisit == null ? 0 : bk.lastVisit );
                s.append("<a href=\"/id/").append(bk.destination).append("\">");
                s.append(node.getName()).append("</a>");
                if (news > 0) {
                    s.append("&nbsp;").append(news).append(" new");
                    /* TODO ak kliknem na toto tak sa mi zobrazia nevidene updates::
                     ?.showActivity(uid,nodeid,lastVisit)
                     - v akej forme? s vektorom a (+) na jeho rozbalenie ?
                     */
                }
                s.append("<br>");
            }
        }
        return s.toString();
    }

    public static void add(String dest, String uid)
    {
        // add a bookmark for user
        // TODO - check ci uz neexistuje - uniq index na (dest, uid) ?
        Bookmark b = new Bookmark(dest, uid);
        MongoDB.save(b, MongoDB.CBookmark);
    }

    public static void delete(String dest, String uid)
    {
        // len ako ho najst
        // MongoDB.delete(Bookmark.find(dest,uid));
    }

    static void updateVisit(String uid, String location) {
        Logger.info("Updating visit::" + "bookmark_" + uid + "_" + location);
        Bookmark b = (Bookmark) Cache.get("bookmark_" + uid + "_" + location);
        if (b != null) {
            b.lastVisit = System.currentTimeMillis();
            MongoDB.update(b, MongoDB.CBookmark);
        }
    }
}
