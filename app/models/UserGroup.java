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
import com.google.code.morphia.Morphia;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@Entity("UserGroup")
public class UserGroup extends MongoEntity {
    String name;
    ObjectId owner;
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

    public static UserGroup create(ObjectId uid,
            String name,
            List<ObjectId> members)
    {
            UserGroup u = new UserGroup(uid, name, members);
            MongoDB.save(u, MongoDB.CUserGroup);
            return u;
    }

    public static UserGroup load(ObjectId groupid) {
        UserGroup u = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUserGroup).
                    findOne(new BasicDBObject().
                    append("_id",groupid));
            if (iobj != null)
                u = (UserGroup) MongoDB.getMorphia().
                        fromDBObject(UserGroup.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return u;
    }

    public static void addUser(ObjectId uid, ObjectId groupid) {

    }

    public void changeOwner() {

    }

    public static void removeUser() {

    }

    public static List<UserGroup> listGroups() {
        return null;
    }

}
