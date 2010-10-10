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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@Entity("UserGroup")
public class UserGroup extends MongoEntity {

    public static DBCollection dbcol = null;
    
    String         name;
    ObjectId       owner;
    List<ObjectId> masters;
    List<ObjectId> members;
    List<ObjectId> extend; // usergroup inheritance - just a thought for now
    ObjectId oid;  // ak sa vztahuje k nejakemu contentu, tak tu - just a thought for now

    public UserGroup () {}

    public UserGroup (ObjectId uid,
            String name,
            List<ObjectId> members)
    {
        this.owner   = uid;
        this.name    = name;
        this.members = members;
    }

    public static UserGroup create(String uid,
            String name,
            List<ObjectId> members)
    {
            UserGroup u = new UserGroup(new ObjectId(uid), name, members);
            MongoDB.save(u, MongoDB.CUserGroup);
            return u;
    }

    public static UserGroup load(String groupid) {
        return load(new ObjectId(groupid));
    }

    // + cache
    public static UserGroup load(ObjectId groupid) {
        UserGroup u = null;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id",groupid));
            if (iobj != null)
                u = MongoDB.fromDBObject(UserGroup.class, iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    // + cache
    public void addUser(ObjectId uid) {
        members.add(uid);
    }

    public void removeUser(ObjectId uid) {
        members.remove(uid);
    }

    public boolean isMember(ObjectId uid) {
        if (members == null) {
            return false;
        } else {
            return members.contains(uid);
        }
    }

    // vsetky groups v ktorych je typek member
    public static List<UserGroup> listGroupsOfUser(ObjectId uid) {
        List<UserGroup> u = null;
        try {
            DBCursor iobj = dbcol.find();
            u = MongoDB.transform(iobj, MongoDB.getSelf().toUserGroup());
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    // + cache
    public void changeOwner(ObjectId newOwner) {
        owner = newOwner;
        MongoDB.save(this, MongoDB.CUserGroup);
    }

    public static List<UserGroup> loadGroups() {
        List<UserGroup> u = null;
        try {
            DBCursor iobj = dbcol.find();
            u = MongoDB.transform(iobj, MongoDB.getSelf().toUserGroup());
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    public void edit(Map<String, String> params) {
        save();
    }

    // + cache
    public void save() {
        MongoDB.save(this, MongoDB.CUserGroup);
    }

    @Override
    public UserGroup enhance() {
        return this;
    }

}
