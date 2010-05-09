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
import com.google.code.morphia.annotations.MongoTransient;
import com.google.code.morphia.annotations.MongoValue;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@MongoDocument
public class User extends AbstractMongoEntity {

    private String             username;
    private String             password;
    private String             gid;
    private String             userinfo; // co sa ma zobrazit v userinfe
    // private ...            userinfoAccessType;
    private String             template; // a ako sa to ma zobrazit

    // TODO mail addr? dalsie?

    private ViewTemplate       view; // userov view
    private String             menu; // userovo menu - zrejme iny objekt ako String
    
    private List<String>       tags;     // given tags
    private List<String>       friends;  // friends and stuff
    private List<String>       ignores;  // users whom this user ignores
    private List<String>       ignoreMail; // users whom this user ignores mail from
    private List<String>       groups;   // user groups this user is part of

    // kackove hospodarstvo :)
    private Long               usedK; // na dnesny den
    private Long               availableK; // na dnesny den
    private Long               kDay;  // v ktorom 24h cykle kacok sme

    public static final String USERNAME = "username";
    public static final String BOOKMARKS = "bookmarks";
    public static final String FRIENDS  = "friends";
    public static final String IGNORES  = "ignores";
    public static final String FOOKS    = "fooks";
    public static final String UPDATES  = "udpates";
    public static final String PASSWORD = "password";
    public static final String USERID   = "gid";
    public static final String ID       = "id";

    private static PasswordService pwdService = new PasswordService();

    @MongoTransient
    private List<Bookmark>     bookmarks;

    public User() {}

    public User(String username, String password, Long id) {
        this.username = username;
        this.password = pwdService.encrypt(password);
        this.gid = id.toString();
    }


    public static User login(String username, String password)
    {
        User u = null;
        boolean pwdOk = false;
        Logger.info("user login");
        try {
           if (username == null || password == null){
               username = "";
               password = "";
           }
            BasicDBObject query = new BasicDBObject().append(USERNAME, username).
                    append(PASSWORD, pwdService.encrypt(password));
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).findOne(query);
            if (iobj ==  null) {
                Logger.info("login failed");
                pwdOk = false;
            } else {
                Logger.info("login successfull:" + iobj.getString("username"));
                pwdOk = true;
                u = MongoDB.getMorphia().fromDBObject(User.class, iobj);
                u.loadUserData(); // ak sa neprihlasuje z RESTu...
            }
        } catch (Exception ex) {
            Logger.info("login failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        if (pwdOk) 
        {
            return u;
        } 
        else 
        {
            return null;
        }
    }

    public void changePwd(Map<String, String> params )
    {
        String oldPwd  = params.get("oldPwd");
        String newPwd1 = params.get("newPwd1");
        String newPwd2 = params.get("newPwd2");
        if (oldPwd!= null && oldPwd.equals(this.password)) {
            if (newPwd1 != null && newPwd2 != null && newPwd1.equals(newPwd2)) {
                this.password = pwdService.encrypt(newPwd1);
                this.update();
            }
        } else {
        // TODO else vynadaj userovi
        }
    }

    // TODO cache invalidate + upload/zmena ikonky
    public void edit(String username, String template, String userinfo)
    {
        boolean changed = false;
        if (username != null) {
            this.username = username;
            changed = true;
        }
        if (template != null) {
            this.template = template;
            changed = true;
        }
        if (userinfo != null) {
            this.userinfo = userinfo;
            changed = true;
        }
        if (changed) {
            this.update();
        }
    }

    // loadni dalsie user data do session pri logine
    public void loadUserData()
    {
        // mail status,
        // list bookmarks,
        // list of friends,
        // and possibly more things
        Cache.set(User.BOOKMARKS + id, getBookmarks());
        // Cache.set(User.FRIENDS + id, getFriends());
        Cache.set(User.IGNORES + id, getIgnores());
        Cache.set(User.FOOKS + id, getFooks());
        Cache.set(User.UPDATES + id, getUpdates());
    }

    public List<Bookmark> getBookmarks()
    {
        return Bookmark.getUserBookmarks(id);
    }

    /*
    public List<Friend> getFriends()
    {
        return Friends.getUserFriends(id);
    }
     */

    public List<ObjectId> getIgnores()
    {
        return new ArrayList<ObjectId>();
    }

    public List<StatusUpdate> getUpdates()
    {
        return new ArrayList<StatusUpdate>();
    }

    public List<ObjectId> getFooks()
    {
        return Fook.getUserFooks(this.getId());
    }

    // false ak uz je username pouzite
    public static boolean usernameAvailable(String username)
    {
        try {
            BasicDBObject query = new BasicDBObject().append(USERNAME, username);
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).findOne(query);
            if (iobj == null) {
                return true;
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @username available");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return false;
        }
        return false;
    }

    public void save()
    {
        try {
             MongoDB.save(this,MongoDB.CUser);
        } catch (Exception ex) {
            Logger.info(ex.toString());
        } 
    }

    public void update()
    {
        try {
             MongoDB.update(this,MongoDB.CUser);
        } catch (Exception ex) {
            Logger.info(ex.toString());
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the id
     */
    // toto by sme mozno mohli vyhodit
    public String getUserid() {
        return getGid();
    }

    // load user by id
    public static User load(String id)
    {
        // coll.find({username:username, password:password})
        // cache?
        // String uname = Cache.get(USERNAME + id, String.class);
        User u = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).
                    findOne(new BasicDBObject().append("_id",new ObjectId(id)));
            if (iobj != null)
                u = (User) MongoDB.getMorphia().
                        fromDBObject(User.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    // load by graph id
    public static User loadByGid(String gid)
    {
        User u = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).
                    findOne(new BasicDBObject().append("gid",gid));
            if (iobj != null)
                u = (User) MongoDB.getMorphia().
                        fromDBObject(User.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    // TODO search, order, start , count
    public static List<User> loadUsers()
    {
        List<User> users = null;
        try {
            BasicDBObject query = new BasicDBObject().append(USERNAME, 1);
            DBCursor iobj = (DBCursor) MongoDB.getDB().
                    getCollection(MongoDB.CUser).find().sort(query);
            if (iobj == null) {
                users = new ArrayList<User>();
            } else {
                users = Lists.transform(iobj.toArray(),
                            MongoDB.getSelf().toUser());
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @loadUsers");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return users;
    }

    // vracia mongo id
    public static String getIdForName(String username)
    {
        String id = Cache.get(ID + username, String.class); //! pozor na cudne username
        if (id != null)
        {
            Logger.info("returning id(1):: " + id);
            return id;
        }
        try {
            BasicDBObject query = new BasicDBObject().append(USERNAME, username );
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).findOne(query);
            if (iobj != null)
            {
                id = iobj.getString("_id");
                Logger.info("returning id(2):: " + id);
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @getIdForName");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        Cache.add(ID + username, id);
        return id;
    }

    public static String getNameForId(String id)
    {
        if (id == null || id.length() < 10) return "";
        String uname = Cache.get(USERNAME + id, String.class);
        if (uname != null)
        {
            Logger.info("returning name(1):: " + uname);
            return uname;
        }
        try {
            BasicDBObject query = new BasicDBObject().append("_id",
                    new ObjectId(id));
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUser).findOne(query);
            if (iobj != null)
            {
                uname = iobj.getString(USERNAME);
                Logger.info("returning name(2):: " + uname);
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @getNameForId for id |" + id + "|");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        Cache.add(USERNAME + id, uname); // TODO expire/invalidate
        return uname;
    }

    public static String viewUsersOnline()
    {
        // pri prihlaseni / kazdej akcii -> Cache(userid,location)
        // + List of current online users
        // potom to tu len pozbierame
        return null;
    }

    // kto koho?
    public static void fook(String get, String id) {

    }

    public static void addFriend(String currUid, String uid) {
        User u = load(currUid);
        if (u != null) {
            if (u.friends == null ) {
                u.friends = new ArrayList<String>();
            } else if (u.friends.contains(uid)) {
                return;
            }
            u.friends.add(uid);
            // + mozno vytvorit nejaky iny zaznam inde?
            u.update();
        }
    }

    public static void addIgnore(String currUid, String uid) {
        User u = load(currUid);
        if (u != null) {
            if (u.ignores == null ) {
                u.ignores = new ArrayList<String>();
            } else if (u.ignores.contains(uid)) {
                return;
            }
            u.ignores.add(uid);
            // + mozno vytvorit nejaky iny zaznam inde?
            u.update();
        }
    }

    public static void addIgnoreMail(String currUid, String uid) {
        User u = load(currUid);
        if (u != null) {
            if (u.ignoreMail == null ) {
                u.ignoreMail = new ArrayList<String>();
            } else if (u.ignoreMail.contains(uid)) {
                return;
            }
            u.ignoreMail.add(uid);
            // + mozno vytvorit nejaky iny zaznam inde?
            u.update();
        }
    }


    /**
     * @return the friends
     */
    public List<String> getFriends() {
        return friends;
    }

    public List<User> listFriends() {
        if (friends == null) {
            return new ArrayList<User>();
        } else {
            return Lists.transform(friends, new ToUser()); //->Singleton
        }
    }

    /**
     * @return the gid
     */
    public String getGid() {
        return gid;
    }

    /**
     * @param gid the gid to set
     */
    public void setGid(String gid) {
        this.gid = gid;
    }

    // transform
    class ToUser implements Function<String, User> {
        public User apply(String arg) {
            return User.load(arg);
        }
    }


    private static final class PasswordService
    {
      MessageDigest md;
      // TODO eventually salt & multiple hashing?
      
      private PasswordService() {
          try{
            md = MessageDigest.getInstance("SHA");
          } catch(NoSuchAlgorithmException e) {}
      }

      private String encrypt(String plaintext) 
      {
        try {
          md.reset();
          md.update(plaintext.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException e) {
        }
        return (new BASE64Encoder()).encode(md.digest());
      }
    }

}
