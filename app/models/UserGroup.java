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

@MongoDocument
public class UserGroup extends AbstractMongoEntity {
    String name;
    String owner;
    List<String> masters;
    List<String> members;
    List<String> extend; // usergroup inheritance - just a thought for now
    String oid;  // ak sa vztahuje k nejakemu contentu, tak tu - just a thought for now

    public UserGroup () {}

    public UserGroup (String uid,
            String name,
            List<String> members)
    {
        this.owner   = uid;
        this.name    = name;
        this.members = members;
    }

    public static UserGroup create(String uid, 
            String name,
            List<String> members)
    {
            UserGroup u = new UserGroup(uid, name, members);
            MongoDB.save(u, MongoDB.CUserGroup);
            return u;
    }

    public static UserGroup load(String groupid) {
        UserGroup u = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CUserGroup).
                    findOne(new BasicDBObject().
                    append("_id",new ObjectId(groupid)));
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

    public static void addUser(String uid, String groupid) {

    }

    public void changeOwner() {

    }

    public static void removeUser() {

    }

    public static List<UserGroup> listGroups() {
        return null;
    }

}
