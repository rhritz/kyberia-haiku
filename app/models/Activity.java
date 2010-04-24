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
import com.google.code.morphia.annotations.MongoCollectionName;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;

// new nodes etc create new activity instances to notify bookmarks etc
// about these new events

@MongoDocument
public class Activity extends AbstractMongoEntity {
    
    private String       oid;  // o ktorom objekte je tato notifikacia
    private Integer        type; // enum? new, delete...? zatial neviem
    private Long           date;
    private String         name; // nazov prispevku, meno usera atd
                                 // pripadne popis aktivity?
    private List<String>   vector;
    // vecna otazka: String alebo oids?
    private List<String>   ids;  // idcka nodov ktore notifikujeme
    private List<String>   uids; // userov ktorych notifikujeme, skrze friend/follow
    private String         parid; // user submission children - len ak parowner != owner
    private String         owner;
    
    @MongoCollectionName 
    private String         actName;

    public Activity () {}

    public Activity (String oid, 
                     Long date,
                     List<String> ids,
                     List<String> uids,
                     List<String> vector,
                     String parid,
                     String owner)
    {
        this.oid  = oid;
        this.date = date;
        this.ids  = ids;
        this.uids  = uids;
        this.vector = vector;
        this.parid  = parid;
        this.owner  = owner;
    }
    
    public static void newNodeActivity(
            String id,
            NodeContent node,
            List<String> parents,
            String ownerid,
            String parOwnerGid
            )
    {
        try {
            Logger.info("@newNodeActivity");
            List<String> friends = User.load(ownerid).getFriends();
            User parent = User.loadByGid(parOwnerGid);
            String parOwnerId = null;
            if (parent != null) {
                parOwnerId = parent.getId();
            }
            // parents==vektor sucasne aj (ak mame vsetkych)
            Activity notif = new Activity(id, System.currentTimeMillis(),
                    parents, friends, parents, parOwnerId,node.getOwner());
            notif.save();
        } catch (Exception ex) {
            Logger.info("newNodeActivity failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public void save()
    {
        try {
            Logger.info("creating new activity");
            MongoDB.save(this, MongoDB.CActivity);
        } catch (Exception ex) {
            Logger.info("activity save failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    // toto
    public static void load()
    {
        
    }

    // ako specifikovat co vlastne chceme? ake su moznosti?
    // TODO toto je asi zle
    public static List<Activity> getActivityList(
            Integer type,
            String parent,
            String owner,
            String parOwner,
            Integer dateFrom,
            Integer dateTo,
            Integer start,
            Integer count)
    {
        // tu si poskladame query z toho, co v parametroch nie je null
        // urobime query a vratime zoznam act
        // ...detto mozno ptorebujeme rovnaku funkciu ktora bude vracta len pocet?
        // asi len type nemoze byt null. ale na druhejs trane, ktovie.
        BasicDBObject query = new BasicDBObject().append("type", type);
        BasicDBObject sort  = new BasicDBObject();
        switch (type) {
            case 0:
                    query.append(owner, start);
                    break;
            case 1:
                    break;
            case 2:
                    break;
        }
        DBCursor iobj = MongoDB.getDB()
            .getCollection(MongoDB.CActivity).find(query).
            sort(sort).limit(count);
        Morphia morphia = MongoDB.getMorphia();
        List<Activity> ll = new LinkedList<Activity>();
        while(iobj.hasNext())
        {
           ll.add(morphia.fromDBObject(Activity.class,
                   (BasicDBObject) iobj.next()));
        }
        return ll;
    }

    // potrebujeme robit query podla bookedId
    public static String viewBookmarkActivity()
    {
        StringBuilder ret = new StringBuilder();
        String bookId = "uuu";
        Integer   dateFrom = 0; // nacitame z bookmarku
        List<Activity> acts = getActivityList(0,bookId,null,null,
                dateFrom,null,null,null);
        for (Activity act : acts)
        {
            // zoznam pribudnutych nodov
            ret.append(act.oid).append(act.date).append(act.name).append("<br>");
        }
        return ret.toString();
    }

    public static void getBookmarkActs(List bmarkIds)
    {
        // pozbieraj data o bookmarkoch - bude sucastou bookmarklistu neskor
    }

    public static List<NodeContent> showFriendsContent(String uid)
    {
        List<NodeContent> ll = new LinkedList<NodeContent>();

        BasicDBObject query = new BasicDBObject().append("uids", uid);
        BasicDBObject sort = new BasicDBObject().append("date", -1);
        DBCursor iobj = MongoDB.getDB()
            .getCollection(MongoDB.CActivity).find(query).
            sort(sort).limit(30);
        Morphia morphia = MongoDB.getMorphia();
        while(iobj.hasNext())
        {
           Activity a = morphia.fromDBObject(Activity.class,
                   (BasicDBObject) iobj.next());
           ll.add(NodeContent.load(a.oid));
        }
        return ll;
    }

}

