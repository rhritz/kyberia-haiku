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
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoSchema {

     private static void init()
     {
        Mongo m = MongoDB.getMongo();
        DB db = MongoDB.getDB();
        String [] cols = new String[] {"Message",  "User", "Friend", "Fook",
        "Ignore", "MessageThread", "Bookmark", "Activity", "Node", "Tag",
        "TagNodeUser", "UserLocation"};

        for (String col : cols) {
            db.createCollection(col, null);
        }
        DBCollection dbc = db.getCollection("Node");
        // dbc.ensureIndex(null, null, true); ...
        /* - pre vsetky indexy a specialitky ::
         Na NodeContent - created, K, owner, parent, name
         User - username db.User.ensureIndex({username: 1}, {unique: true});
         Message - threadid,created
         Bookmark - db.Bookmark.ensureIndex({destination: 1, uid:1}, {unique: true});
         Activity - ?
         EnsureIndex('users') on MessageThread
     */
     }
    // find().sort({$natural:-1}) <-- sortovanie an natural colls, mozno aj idne funguje takto?
    // http://www.mongodb.org/display/DOCS/Capped+Collections

}
