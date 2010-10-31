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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.util.LinkedList;
import java.util.List;
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
    private ToViewTemplate  viewTemplate;

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
          viewTemplate  = new ToViewTemplate();
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
            db.getCollection(CViewTemplate).ensureIndex(new BasicDBObject("name","1"),"name_1",true);

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

    public static void save(MongoEntity m)
    {
         m.getCollection().insert(morphia.toDBObject(m));
    }

    public static void update(MongoEntity m)
    {
         m.getCollection().save(morphia.toDBObject(m));
    }

    public static void delete(MongoEntity m)
    {
         m.getCollection().remove(morphia.toDBObject(m));
    }

    public static <T> T load(String id, String col, Class<T> entityClass)
    {
        return load(new ObjectId(id), col, entityClass);
    }

    public static <T> T load(ObjectId id, String col, Class<T> entityClass)
    {
        DBObject DBObj = db.getCollection(col).findOne(new BasicDBObject("_id", id));
        return fromDBObject(entityClass, DBObj);
    }

    // TODO variable ordering via BasicDBObject sort as a parameter?
    public static <T> List<T> loadIds(List<ObjectId> fromList, String col,
            Function<DBObject, ? extends T> function)
    {
        DBObject query = new BasicDBObject("_id", new BasicDBObject("$in",
                        fromList.toArray(new ObjectId[fromList.size()])));
        DBCursor iobj = db.getCollection(col).find(query);
        List<T> l = Lists.newArrayListWithCapacity(iobj == null ? 0 : iobj.size());
        if (iobj != null)
            for (DBObject f : iobj) 
                l.add(function.apply(f));
        return l;
    }

    public static <T> List<T> transform( DBCursor fromList,
            Function<DBObject, ? extends T> function) {
        List<T> l = new LinkedList<T>();
        if (fromList != null)
            for (DBObject f : fromList)
                l.add(function.apply(f));
        return l;
    }

    public static <T extends MongoEntity> List<T> transformIds(
                                  List<ObjectId> fromList,
                                  String col,
                                  Class<T> entityClass ) {
        List<T> l = Lists.newArrayListWithCapacity(fromList.size());
        if (fromList != null)
            for (ObjectId f : fromList) {
                T t = load(f, col, entityClass);
                t.enhance();
                l.add(t);
            }
        return l;
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

    public Function<DBObject, ViewTemplate> toViewTemplate()
    {
        return viewTemplate;
    }

    public static <T> T fromDBObject(Class<T> entityClass, DBObject dbObject) {
        return morphia.fromDBObject(entityClass, dbObject,
                morphia.getMapper().createEntityCache());
    }

    public class ToMessage implements Function<DBObject, Message> {
        public Message apply(DBObject arg) {
            return fromDBObject(Message.class, arg).enhance();
        }
    }

    public class ToMessageThread implements Function<DBObject, MessageThread> {
        public MessageThread apply(DBObject arg) {
            return fromDBObject(MessageThread.class, arg).enhance();
        }
    }

    public class ToUser implements Function<DBObject, User> {
        public User apply(DBObject arg) {
            return fromDBObject(User.class, arg).enhance();
        }
    }

    public class ToUserGroup implements Function<DBObject, UserGroup> {
        public UserGroup apply(DBObject arg) {
            return fromDBObject(UserGroup.class, arg).enhance();
        }
    }

    public class ToUserLocation implements Function<DBObject, UserLocation> {
        public UserLocation apply(DBObject arg) {
            return fromDBObject(UserLocation.class, arg).enhance();
        }
    }

    // TODO: caching? to by asi bolo potrebne riesit pred tymto/?
    public class ToNodeContent implements Function<DBObject, NodeContent> {
        public NodeContent apply(DBObject arg) {
            return fromDBObject(NodeContent.class, arg).enhance();
        }
    }

    public class ToTag implements Function<DBObject, Tag> {
        public Tag apply(DBObject arg) {
            return fromDBObject(Tag.class, arg).enhance();
        }
    }

    public class ToBookmark implements Function<DBObject, Bookmark> {
        public Bookmark apply(DBObject arg) {
            return fromDBObject(Bookmark.class, arg).enhance();
        }
    }

    public class ToActivity implements Function<DBObject, Activity> {
        public Activity apply(DBObject arg) {
            return fromDBObject(Activity.class, arg).enhance();
        }
    }

    public class ToFeed implements Function<DBObject, Feed> {
        public Feed apply(DBObject arg) {
            return fromDBObject(Feed.class, arg).enhance();
        }
    }

    public class ToPage implements Function<DBObject, Page> {
        public Page apply(DBObject arg) {
            return fromDBObject(Page.class, arg).enhance();
        }
    }

    public class ToViewTemplate implements Function<DBObject, ViewTemplate> {
        public ViewTemplate apply(DBObject arg) {
            return fromDBObject(ViewTemplate.class, arg).enhance();
        }
    }
}
