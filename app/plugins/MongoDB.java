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
import com.mongodb.ObjectId;
import models.*;
import play.Logger;
import play.Play;

public class MongoDB {
    private static MongoDB  self;
    private ToMessage       message;
    private ToMessageThread messageThread;
    private ToUser          user;
    private ToUserLocation  userLocation;
    private ToNodeContent   nodeContent;
    private ToTag           tag;
    private ToBookmark      bookmark;

    private static DB       db;
    private static Mongo    mongo;
    private static Morphia  morphia;
    private static String   ADDR;
    private static String   PORT;
    private static String   DBNAME;

    // Collection Names
    public static final String CActivity      = "Activity";
    public static final String CBookmark      = "Bookmark";
    public static final String CFook          = "Fook";
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
          userLocation  = new ToUserLocation();
          nodeContent   = new ToNodeContent();
          tag           = new ToTag();
          bookmark      = new ToBookmark();
    }

    public static void start() {
        self = new MongoDB();
        Logger.info("Mongo connecting");
        try {
            /* The Mongo object instance actually represents a pool of connections
                to the database; you will only need one object of class Mongo
                even with multiple threads.
             */
            mongo = new Mongo(ADDR, Integer.parseInt(PORT));
            db = mongo.getDB(DBNAME);
            /*
            Set<String> colls = db.getCollectionNames();
            for (String s : colls) {
                Logger.info(s);
                morphia.map(Class.forName(s));
            }
             */
            morphia = new Morphia();

            morphia.map(Message.class);
            morphia.map(User.class);
            morphia.map(MessageThread.class);
            morphia.map(Bookmark.class);
            morphia.map(Activity.class);
            morphia.map(NodeContent.class);
            morphia.map(UserLocation.class);
            morphia.map(Friend.class);
            morphia.map(Fook.class);
            morphia.map(Ignore.class);
            morphia.map(UserGroup.class);
            morphia.map(Tag.class);
            morphia.map(Vote.class);
            morphia.map(Page.class);
            morphia.map(ViewTemplate.class);
            
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

    // TODO ak sa da generify ak nie tak je to tu asi zbytocne
    public static Object load(String id, String col)
            throws ClassNotFoundException
    {
        BasicDBObject DBObj =
                (BasicDBObject) db.getCollection(col).
                findOne(new BasicDBObject("_id", new ObjectId(id)));

        return morphia.fromDBObject(Class.forName(col), DBObj);
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

    // transformacna funkcia pre Lists.transform
    public class ToMessage implements Function<DBObject, Message> {
        public Message apply(DBObject arg) {
            return morphia.fromDBObject(Message.class,
                   arg);
        }
    }

    public class ToMessageThread implements Function<DBObject, MessageThread> {
        public MessageThread apply(DBObject arg) {
            return morphia.fromDBObject(MessageThread.class,
                    arg);
        }
    }

    public class ToUser implements Function<DBObject, User> {
        public User apply(DBObject arg) {
            return morphia.fromDBObject(User.class,
                   arg);
        }
    }

    public class ToUserLocation implements Function<DBObject, UserLocation> {
        public UserLocation apply(DBObject arg) {
            return morphia.fromDBObject(UserLocation.class,   arg);
        }
    }

    public class ToNodeContent implements Function<DBObject, NodeContent> {
        public NodeContent apply(DBObject arg) {
            return morphia.fromDBObject(NodeContent.class,   arg);
        }
    }

    public class ToTag implements Function<DBObject, Tag> {
        public Tag apply(DBObject arg) {
            return morphia.fromDBObject(Tag.class,
                   arg);
        }
    }

    public class ToBookmark implements Function<DBObject, Bookmark> {
        public Bookmark apply(DBObject arg) {
            return morphia.fromDBObject(Bookmark.class,
                   arg);
        }
    }
}
/*
 TODO
 @OnApplicationStart
public class EnsureGeoMongoIndex extends Job {

        @Override
        public void doJob() {
                DB db = MongoDB.db();
                DBCollection coll = db.getCollection("places");
                coll.ensureIndex(new BasicDBObject("location", "2d"));
        }
}
 */