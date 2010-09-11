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
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import play.Logger;
import play.cache.Cache;
import play.mvc.Scope.RenderArgs;
import plugins.MongoDB;

@Entity("Feed")
public class Feed extends MongoEntity{

    private        String         name;
    private        ObjectId       owner;
    private        List<ObjectId> nodes;
    private        Integer        maxNodes;
    private        String         subClass;

    // + pripadne neskor permisions atd
    // + unique index na name? alebo cez ID
    @Transient
    private         List<NodeContent> content;
    @Transient
    private        String         ownerName; //?

    // pridaj 'clanok' do feedu
    public static void addToFeed(String feedName, ObjectId contentId)
    {
        // test na pocet
    }

    public static Feed loadContent(ObjectId feedId)
    {
        Feed toLoad = load(feedId);
        if (toLoad != null) 
            toLoad.loadContent();
        return toLoad;
    }

    public void loadContent()
    {
        content = NodeContent.load(nodes);
    }

    public static Feed load(ObjectId id)
    {
        // Logger.info("About to load node " + id);
        Feed n = Cache.get("feed_" + id, Feed.class);
        if (n != null )
            return n;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CFeed).
                    findOne(new BasicDBObject().append("_id",id));
            if (iobj !=  null) {
                n = MongoDB.getMorphia().fromDBObject(Feed.class,
                           (BasicDBObject) iobj);
                Cache.add("feed_" + id, n);
            }
        } catch (Exception ex) {
            Logger.info("load feed");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    static List<Feed> load(List<ObjectId> feedIds) {
        List<Feed> feeds = null;
        try {
            DBObject query = new BasicDBObject("_id",
                    new BasicDBObject().append("$in",
                    feedIds.toArray(new ObjectId[feedIds.size()])));
            DBCursor iobj = MongoDB.getDB().getCollection(MongoDB.CFeed).
                    find(query);
            if (iobj !=  null)
                feeds = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toFeed()); //? .toFeedWithContent()
        } catch (Exception ex) {
            Logger.info("load feeds::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return feeds;
    }

    String getName() {
        return name;
    }
    ////////////////////////////////////////////////////////////////////////////
    void getData(   Map<String, String> params,
                    HashMap request,
                    HashMap session,
                    User    user,
                    RenderArgs renderArgs) {
        // renderArgs.put("data",data)...
    }

    void init(Page page) {
    }

    static Feed loadSubclass(ObjectId feedId, Page page) {
        Feed feed = null;
        try {
            feed = load(feedId);
            Class c = Class.forName(feed.subClass);
            feed = sc(c,feed,page);
        } catch (ClassNotFoundException ex) {
            Logger.info("loadSubclass::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return feed;
    }

    private static <T extends Feed> T sc(Class<T> c, Feed fu, Page page) {
        T le = c.cast(fu);
        le.init(page);
        return c.cast(fu);
    }

}
