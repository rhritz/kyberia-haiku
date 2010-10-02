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
package plugins;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.google.code.morphia.Morphia;

// mapovane models
import com.google.common.base.Function;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import models.*;
import play.Logger;
import play.Play;

public class MongoDB {
    private static MongoDB  self;
    private ToMessage       message;
    private ToMessageThread messageThread;
    private ToPage          page;
    private ToUser          user;
    private ToUserGroup     userGroup;
    private ToUserLocation  userLocation;
    private ToNodeContent   nodeContent;
    private ToTag           tag;
    private ToBookmark      bookmark;
    private ToActivity      activity;
    private ToFeed          feed;

    private static DB       db;
    private static Mongo    mongo;
    private static Morphia  morphia;
    private static String   ADDR;
    private static String   PORT;
    private static String   DBNAME;

    // Collection Names
    public static final String CActivity      = "Activity";
    public static final String CBookmark      = "Bookmark";
    public static final String CFeed          = "Feed";
    public static final String CFriend        = "Friend";
    public static final String CIgnore        = "Ignore";
    public static final String CMessage       = "Message";
    public static final String CMessageThread = "MessageThread";
    public static final String CNode          = "Node";
    public static final String CPage          = "Page";
    public static final String CTag           = "Tag";
    public static final String CTagNodeUser   = "TagNodeUser";
    public static final String CUser          = "User";
    public static final String CUserGroup     = "UserGroup";
    public static final String CUserLocation  = "UserLocation";
    public static final String CVote          = "Vote";
    public static final String CViewTemplate  = "ViewTemplate";

    static {
        if (Play.configuration.containsKey("mongodb.addr"))
            ADDR = Play.configuration.getProperty("mongodb.addr");
        else
            ADDR = "127.0.0.1";
        if (Play.configuration.containsKey("mongodb.port"))
            PORT = Play.configuration.getProperty("mongodb.port");
        else
            PORT = "27017";
        if (Play.configuration.containsKey("mongodb.dbname"))
            DBNAME = Play.configuration.getProperty("mongodb.dbname");
        else
            DBNAME = "local";
    }

    private MongoDB() {
          message       = new ToMessage();
          messageThread = new ToMessageThread();
          user          = new ToUser();
          userGroup     = new ToUserGroup();
          userLocation  = new ToUserLocation();
          nodeContent   = new ToNodeContent();
          tag           = new ToTag();
          bookmark      = new ToBookmark();
          activity      = new ToActivity();
          page          = new ToPage();
          feed          = new ToFeed();
    }

    public static void start() {
        self = new MongoDB();
        Logger.info("Mongo connecting");
        try {
            mongo = new Mongo(ADDR, Integer.parseInt(PORT));
            db = mongo.getDB(DBNAME);
            
            morphia = new Morphia();

            // morphia.mapPackage("models");
            morphia.map(Message.class);            
            morphia.map(User.class);            
            morphia.map(MessageThread.class);
            morphia.map(Bookmark.class);
            morphia.map(Activity.class);
            morphia.map(NodeContent.class);
            morphia.map(UserLocation.class);
            morphia.map(Friend.class);
            morphia.map(Ignore.class);
            morphia.map(UserGroup.class);
            morphia.map(Tag.class);
            morphia.map(Vote.class);
            morphia.map(Page.class);
            morphia.map(ViewTemplate.class);
            morphia.map(Feed.class);

            User.dbcol = db.getCollection(CUser);
            NodeContent.dbcol = db.getCollection(CNode);
            MessageThread.dbcol = db.getCollection(CMessageThread);
            Bookmark.dbcol = db.getCollection(CBookmark);
            Activity.dbcol = db.getCollection(CActivity);
            
            UserLocation.dbcol = db.getCollection(CUserLocation);
          //  Friend.dbcol = db.getCollection(CFriend);
           // Ignore.dbcol = db.getCollection(CIgnore);
            UserGroup.dbcol = db.getCollection(CUserGroup);
            Tag.dbcol = db.getCollection(CTag);
            Vote.dbcol = db.getCollection(CVote);
            Page.dbcol = db.getCollection(CPage);
            ViewTemplate.dbcol = db.getCollection(CViewTemplate);
            Message.dbcol = db.getCollection(CMessage);
            Feed.dbcol = db.getCollection(CFeed);

            db.getCollection(CNode).ensureIndex(new BasicDBObject("created","-1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("owner","1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("par","1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("tags","1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("k","1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("mk","1"));
            db.getCollection(CNode).ensureIndex(new BasicDBObject("name","1"));

            db.getCollection(CUser).ensureIndex(new BasicDBObject("username","1"),"username_1",true);

            db.getCollection(CMessageThread).ensureIndex(new BasicDBObject("users","1"));
            
            db.getCollection(CTag).ensureIndex(new BasicDBObject("tag","1"),"tag_1",true);

            db.getCollection(CMessage).ensureIndex(new BasicDBObject("threadid","1"));
            db.getCollection(CMessage).ensureIndex(new BasicDBObject("created","-1")); // alebo composite asi skor?
            db.getCollection(CMessage).ensureIndex(new BasicDBObject("from","1"));
            db.getCollection(CMessage).ensureIndex(new BasicDBObject("to","1")); // + "key" : { "from" : 1, "sent" : -1 }, "name" : "from_1_sent_-1" }

            db.getCollection(CActivity).ensureIndex(new BasicDBObject("owner","1"));
            db.getCollection(CActivity).ensureIndex(new BasicDBObject("parid","1"));
            db.getCollection(CActivity).ensureIndex(new BasicDBObject("oid","1"));
            db.getCollection(CActivity).ensureIndex(new BasicDBObject("date","-1"));
            db.getCollection(CActivity).ensureIndex(new BasicDBObject("ids","1"));
            db.getCollection(CActivity).ensureIndex(new BasicDBObject("uids","1"));

            db.getCollection(CFeed).ensureIndex(new BasicDBObject("name","1"),"name_1",true);
            db.getCollection(CPage).ensureIndex(new BasicDBObject("name","1"),"name_1",true);

            // db.getCollection(CActivity).ensureIndex(new BasicDBObject("username","1"),"username_1",true);
            // db.getCollection(CBookmark).ensureIndex({destination: 1, uid:1}, {unique: true});

            // find().sort({$natural:-1}) <-- sortovanie an natural colls, mozno aj idne funguje takto?
            // http://www.mongodb.org/display/DOCS/Capped+Collections
            
        } catch (Exception e) {
            Logger.info("Brekeke @ mongo:: " + e.toString());
        }
    }

    public static DB getDB() {
        return db;
    }

    protected static Mongo getMongo() {
        return mongo;
    }

    public static void shutdown() {
        Logger.info("Mongo stopping");
    }

    public static Morphia getMorphia() {
        return morphia;
    }

    public static void save(Object m, String col)
    {
         DBObject mDBObj = morphia.toDBObject(m);
         db.getCollection(col).insert(mDBObj);
    }

    public static void update(Object m, String col)
    {
        DBObject mDBObj = morphia.toDBObject(m);
        db.getCollection(col).save(mDBObj);
    }

    public static void delete(Object m, String col)
    {
        DBObject mDBObj = morphia.toDBObject(m);
        db.getCollection(col).remove(mDBObj);
    }

    public static <T> T load(String id, String col, Class<T> entityClass)
            throws ClassNotFoundException
    {
        DBObject DBObj = db.getCollection(col).
                findOne(new BasicDBObject("_id", new ObjectId(id)));
        return fromDBObject(entityClass, DBObj);
    }

    public static DBCollection getCollection(String name)
    {
        return db.getCollection(name);
    }

    public static MongoDB getSelf()
    {
        return self;
    }

    public Function<DBObject, Message> toMessage()
    {
        return message;
    }

    public Function<DBObject, MessageThread> toMessageThread()
    {
        return messageThread;
    }

    public Function<DBObject, User> toUser()
    {
        return user;
    }

    public Function<DBObject, UserGroup> toUserGroup()
    {
        return userGroup;
    }

    public Function<DBObject, UserLocation> toUserLocation()
    {
        return userLocation;
    }

    public Function<DBObject, NodeContent> toNodeContent()
    {
        return nodeContent;
    }

    public Function<DBObject, Tag> toTag()
    {
        return tag;
    }

    public Function<DBObject, Bookmark> toBookmark()
    {
        return bookmark;
    }

    public Function<DBObject, Activity> toActivity()
    {
        return activity;
    }

    public Function<DBObject, Feed> toFeed()
    {
        return feed;
    }

    public Function<DBObject, Page> toPage()
    {
        return page;
    }

    public static <T> T fromDBObject(Class<T> entityClass, DBObject dbObject) {
        return morphia.fromDBObject(entityClass, dbObject,
                morphia.getMapper().createEntityCache());
    }

    // transformacna funkcia pre Lists.transform
    public class ToMessage implements Function<DBObject, Message> {
        public Message apply(DBObject arg) {
            Message m = fromDBObject(Message.class, arg);
            m.fromUser = User.getNameForId(m.from);
            m.toUser   = User.getNameForId(m.to);
            return m;
        }
    }

    public class ToMessageThread implements Function<DBObject, MessageThread> {
        public MessageThread apply(DBObject arg) {
            return fromDBObject(MessageThread.class, arg);
        }
    }

    public class ToUser implements Function<DBObject, User> {
        public User apply(DBObject arg) {
            return fromDBObject(User.class, arg);
        }
    }

    public class ToUserGroup implements Function<DBObject, UserGroup> {
        public UserGroup apply(DBObject arg) {
            return fromDBObject(UserGroup.class, arg);
        }
    }

    public class ToUserLocation implements Function<DBObject, UserLocation> {
        public UserLocation apply(DBObject arg) {
            return fromDBObject(UserLocation.class, arg);
        }
    }

    // TODO: caching? to by asi bolo potrebne riesit pred tymto/?
    public class ToNodeContent implements Function<DBObject, NodeContent> {
        public NodeContent apply(DBObject arg) {
            NodeContent n = fromDBObject(NodeContent.class, arg);
            n.ownerName = User.getNameForId(n.getOwner());
            n.loadRights();
            if (n.par != null )
                n.parName  = NodeContent.load(n.par).name;
            else
                n.parName  = "";
            return n;
        }
    }

    public class ToTag implements Function<DBObject, Tag> {
        public Tag apply(DBObject arg) {
            return fromDBObject(Tag.class, arg);
        }
    }

    public class ToBookmark implements Function<DBObject, Bookmark> {
        public Bookmark apply(DBObject arg) {
            return fromDBObject(Bookmark.class, arg);
        }
    }

    public class ToActivity implements Function<DBObject, Activity> {
        public Activity apply(DBObject arg) {
            return fromDBObject(Activity.class, arg);
        }
    }

    public class ToFeed implements Function<DBObject, Feed> {
        public Feed apply(DBObject arg) {
            Feed applied = fromDBObject(Feed.class, arg);
         //   applied.loadContent();
            return applied;
        }
    }

    public class ToPage implements Function<DBObject, Page> {
        public Page apply(DBObject arg) {
            return fromDBObject(Page.class, arg);
        }
    }
}
