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
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import plugins.MongoDB;

@Entity("Feed")
public class Feed extends MongoEntity{

    public static DBCollection dbcol = null;
    private static final String key  = "feed_";

    protected        String         name;
    protected        ObjectId       owner;
    protected        List<ObjectId> nodes;
    protected        Integer        maxNodes;
    protected        String         subClass;
    protected        String         dataName;

    // + pripadne neskor permisions atd
    @Transient
    private         List<NodeContent> content;

    public Feed () { name = getClass().getCanonicalName(); }
    
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
        Feed n = Cache.get(key + id, Feed.class);
        if (n != null )
            return n;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id",id));
            if (iobj !=  null) {
                n = MongoDB.fromDBObject(Feed.class, iobj);
                Cache.add(key + id, n);
            }
        } catch (Exception ex) {
            Logger.info("load feed");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    String getName() {
        return name;
    }
    ////////////////////////////////////////////////////////////////////////////
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        // mali by sme vyhodit error?
        Logger.info("Hello from Feed base class. You really shouldn't see this.");
    }

    public void init(Page page) {}

    static <T extends Feed> T getByName(String name, Page page) {
        T le = null;
        try {
            Class<T> fu = (Class<T>) Class.forName(name);
            le = fu.newInstance();
            le.init(page);
        } catch (Exception ex) {
            Logger.info("Feed.getByName::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return le;
    }

    static Feed loadSubclass(ObjectId feedId, Page page) {
        Feed feed = null;
        try {
            feed = load(feedId);
            Class c = Class.forName("models." + feed.subClass);
            feed = sc(c,feed,page);
        } catch (ClassNotFoundException ex) {
            Logger.info("Feed.loadSubclass::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return feed;
    }

    // TODO toto sa urcite da spravit elegantnejsie
    private static <T extends Feed> T sc(Class<T> c, Feed fu, Page page) {
        T le = null;
        try {
            le = c.newInstance();
            le.id = fu.id;
            le.name = fu.name;
            le.maxNodes = fu.maxNodes;
            le.owner = fu.owner;
            le.init(page);
        } catch (Exception ex) {
            Logger.info("Feed.sc::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return le;
    }

   /*
    Dirty hacks ftw!
    list Java files (not classes, since they are not going to be there)
    inside a given package directory, and use them as a list of available feeds
    adapted from Jon Peck http://jonpeck.com
    adapted from http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
   */
  public static List<Class> getClasses(String pckgname) {
    List<Class> classes=new LinkedList<Class>();
    try {
      File directory=new File(Thread.currentThread().getContextClassLoader()
              .getResource('/'+pckgname.replace('.', '/')).getFile());
        if(directory.exists()) {
          String[] files=directory.list();
          for( int i=0 ; i < files.length; i++)
            if(files[i].endsWith(".java"))
              classes.add(Class.forName(pckgname+'.'+
                files[i].substring(0, files[i].length()-5)));
        } else 
          Logger.info(pckgname + " does not appear to be a valid package");
    } catch(Exception x) {
        Logger.info(pckgname + " does not appear to be a valid package");
        x.printStackTrace();
        Logger.info(x.toString());
    }
    return classes;
  }

    @Override
    public Feed enhance() {
                 //   .loadContent();
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
