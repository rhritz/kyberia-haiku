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
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import sun.misc.BASE64Encoder;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@Entity("User")
public class User extends MongoEntity {

    public static DBCollection dbcol = null;

    private String             username;
    private String             password;
    private ObjectId           userinfo; // -> userinfo Node
    private String             template; // a ako sa to ma zobrazit

    private String             view;     // userov view
    private Map<String,String> menu;     // userovo menu
    
    private List<String>       tags;     // given tags
    private List<ObjectId>     friends;  // friends and stuff
    private List<ObjectId>     ignores;  // users whom this user ignores
    private List<ObjectId>     ignoreMail; // users whom this user ignores mail from
    private List<ObjectId>     groups;   // user groups this user is part of

    private Integer            dailyK;
    private Integer            k;

    private Boolean            invisible = Boolean.FALSE;

    public static final String USERNAME = "username";
    public static final String BOOKMARKS = "bookmarks";
    public static final String FRIENDS  = "friends";
    public static final String IGNORES  = "ignores";
    public static final String FOOKS    = "fooks";
    public static final String UPDATES  = "udpates";
    public static final String PASSWORD = "password";
    public static final String ID       = "id";

    private static PasswordService pwdService = new PasswordService();

    @Transient
    public HashMap<ObjectId,Boolean>   prepIgnores;
    @Transient
    public HashMap<ObjectId,Boolean>   prepGroups;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = pwdService.encrypt(password);
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
            BasicDBObject query = new BasicDBObject(USERNAME, username).
                    append(PASSWORD, pwdService.encrypt(password));
            DBObject iobj = dbcol.findOne(query);
            if (iobj ==  null) {
                Logger.info("login failed");
                pwdOk = false;
            } else {
                Logger.info("login successfull:" + iobj.get("username").toString());
                pwdOk = true;
                u = MongoDB.fromDBObject(User.class, iobj);
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
        if (oldPwd!= null && password.equals(pwdService.encrypt(oldPwd))) {
            if (newPwd1 != null && newPwd2 != null && newPwd1.equals(newPwd2)) {
                password = pwdService.encrypt(newPwd1);
                update();
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
            this.userinfo = toId(userinfo);
            changed = true;
        }
        if (changed) {
            this.update();
        }
    }

    public void loadUserData()
    {
        // mail status,
        // list bookmarks,
        // and possibly more things

        prepIgnores = new HashMap<ObjectId,Boolean>();
        if (ignores != null)
            for (ObjectId ignore : ignores)
                prepIgnores.put(ignore, Boolean.TRUE);

        prepGroups = new HashMap<ObjectId,Boolean>  () ;
        if (groups != null)
            for (ObjectId group : groups)
                prepGroups.put(group, Boolean.TRUE);
        // foreach friend...

    }

    // false ak uz je username pouzite
    public static boolean usernameAvailable(String username)
    {
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject(USERNAME, username));
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
             Cache.set("user_" + this.getId(), this);
             MongoDB.save(this);
        } catch (Exception ex) {
            Logger.info(ex.toString());
        } 
    }

    public void update()
    {
        try {
             Cache.set("user_" + this.getId(), this);
             MongoDB.update(this);
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

    public static User load(String id)
    {
        if (id == null || id.length() < 10) return null;
        return load(toId(id));
    }

    // load user by id
    public static User load(ObjectId id)
    {
        User u = Cache.get("user_" + id, User.class);
        if (u != null )
            return u;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id",id));
            if (iobj != null) {
                u = MongoDB.fromDBObject(User.class, iobj);
                Cache.set("user_" + id, u);
                Cache.set(ID + u.username, id.toString());
                Cache.set(USERNAME + id, u.username);
            }
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    public static void refreshDailyK(Integer howMuch) {
        try {
            dbcol.update(new BasicDBObject(), new BasicDBObject("$set",
                    new BasicDBObject("dailyK",howMuch)), false, true);
        } catch (Exception ex) {
            Logger.info("tag inc fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }


    public static List<User> loadUsers(String namePart,
                                        Integer start,
                                        Integer count,
                                        String  order)
    {
        List<User> users = null;
        if (namePart != null) {
            // este treba upravit
            BasicDBObject match = new BasicDBObject(USERNAME, namePart);
        }
        try {
            BasicDBObject query = new BasicDBObject(USERNAME, 1);
            DBCursor iobj = dbcol.find().sort(query).
                    skip(start == null ? 0 : start).
                    limit(count == null ? 0 : count);
            users = MongoDB.transform(iobj, MongoDB.getSelf().toUser());
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
        // pozor na cudne usernames
        String id = Cache.get(ID + username, String.class); 
        if (id != null)
            return id;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject(USERNAME, username));
            if (iobj != null)
            {
                id = iobj.get("_id").toString();
                Cache.add(ID + username, id);
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @getIdForName");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return id;
    }

    public static String getNameForId(ObjectId id)
    {
        if (id == null || id.toString().length() < 10)
            return "";
        String uname = Cache.get(USERNAME + id, String.class);
        if (uname != null)
            return uname;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id", id));
            if (iobj != null)
            {
                uname = iobj.get(USERNAME).toString();
                Cache.add(USERNAME + id.toString(), uname);
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @getNameForId for id |" + id + "|");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return uname;
    }

    public void addFriend(ObjectId uid) {
        if (friends == null ) {
            friends = new ArrayList<ObjectId>();
        } else if (friends.contains(uid)) {
            return;
        }
        friends.add(uid);
        update();
    }

    public void removeFriend(ObjectId uid) {
        if (friends != null && friends.contains(uid)) {
            friends.remove(uid);
            update();
        }
    }

    public void addIgnore(ObjectId uid) {
        if (ignores == null ) {
            ignores = new ArrayList<ObjectId>();
        } else if (ignores.contains(uid)) {
            return;
        }
        ignores.add(uid);
        // + mozno vytvorit nejaky iny zaznam inde?
        update();
    }

    public void removeIgnore(ObjectId uid) {
        if (ignores != null && ignores.contains(uid)) {
            ignores.remove(uid);
            update();
        }
    }

    public void addIgnoreMail(ObjectId uid) {
        if (ignoreMail == null ) {
            ignoreMail = new ArrayList<ObjectId>();
        } else if (ignoreMail.contains(uid)) {
            return;
        }
        ignoreMail.add(uid);
        // + mozno vytvorit nejaky iny zaznam inde?
        update();
    }

    public void removeIgnoreMail(ObjectId uid) {
        if (ignoreMail != null && ignoreMail.contains(uid)) {
            ignoreMail.add(uid);
            update();
        }
    }


    /**
     * @return the friends
     */
    protected List<ObjectId> getFriends() {
        return friends;
    }

    public List<User> listFriends() {
        if (friends == null)
            return new ArrayList<User>();
        else
            return Lists.transform(friends, new ToUser());
    }

    public List<User> listIgnores() {
        if (ignores == null)
            return new ArrayList<User>();
        else
            return Lists.transform(ignores, new ToUser());
    }

    /**
     * @return the view
     */
    public String getView() {
        return view;
    }

    void setDailyK(int i) {
        dailyK = i;
    }

    int getDailyK() {
        return dailyK;
    }

    public Boolean isInvisible() {
        return invisible;
    }

    // transform ObjectId to User
    class ToUser implements Function<ObjectId, User> {
        public User apply(ObjectId arg) {
            return User.load(arg);
        }
    }

    private static final class PasswordService
    {
      MessageDigest md;
      BASE64Encoder enc;
      // TODO eventually salt & multiple hashing?
      
      private PasswordService() {
          try{
            md = MessageDigest.getInstance("SHA");
          } catch(NoSuchAlgorithmException e) {}
          enc = new BASE64Encoder();
      }

      private synchronized String encrypt(String plaintext)
      {
        try {
          md.reset();
          md.update(plaintext.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException e) {
        }
        return enc.encode(md.digest());
      }
    }

    public static String addUser(Map<String,String> params)
    {
        ObjectId id = null;
        String username = params.get(USERNAME);
        String password = params.get(PASSWORD);

        if (! User.usernameAvailable(username)) {
            return null;
        }

        try {
            User u = new User(username, password);
            id = u.getId();
            u.save();
        }
        catch(Exception e)
        {
           Logger.info("addUser failed " + e.toString() );
        }
        return id == null ? "" : id.toString();
    }

    @Override
    public User enhance() {
        return this;
    }

    @Override
    public DBCollection getCollection() {
        return dbcol;
    }


}
