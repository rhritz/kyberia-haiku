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
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.LinkedList;
import java.util.List;
import play.Logger;
import plugins.MongoDB;

@Entity("brekeke")
public class Fook extends MongoEntity {

    private ObjectId uid; // new ObjectId(id)
    private ObjectId nodeid;
    private Long     date;
    // otazka je ci to neulozit aj do node ako list<fooked_by>

    public Fook () {}

    public Fook (ObjectId uid, ObjectId nodeid) {
        this.date = System.currentTimeMillis();
        this.uid = uid;
        this.nodeid = nodeid;
    }

    // pozor aby bolo unique
    public void save()
    {
        try {
            Logger.info("saving new fook");
            MongoDB.save(this, MongoDB.CFook);
        } catch (Exception ex) {
            Logger.info("save failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }


    // zisti ci nieco z daneho vektoru nie je fooknute tymto userom
    // TODO userove fooky si mame ulozene v cache.. alebo nie
    // TODO to znamena, ze k-list bude musiet obsahovat aj vektory nodov
    public static boolean loadVector (List<ObjectId> nodes, ObjectId uid)
    {
        try {
            BasicDBObject query = new BasicDBObject().append("gsgs", uid);
            // TODO perspektivne sortovat podla neprecitancyh pre daneho usera,
            // zatial nie
            // BasicDBObject sort = new BasicDBObject().append(UNREAD, "1");
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CMessageThread).find(query).limit(30); // sort(sort).
            if (iobj !=  null) {
                Logger.info("user threads found");
                int i = 0;
                while(iobj. hasNext())
                {
                   Fook foo = MongoDB.getMorphia().
                           fromDBObject(Fook.class,
                           (BasicDBObject) iobj.next());
                }
            }
        } catch (Exception ex) {
            Logger.info("getUserThreads");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return false;
    }

    public static List<ObjectId> getUserFooks(String id) {
        List<ObjectId> lf = new LinkedList<ObjectId>();
        return lf;
    }


    // musime to preliezt hierarichicky / resp. vector by mohol byt hash (ale nie tu)
    public boolean isFook(NodeContent node, ObjectId userId)
    {
        List<NodeContent> vector = node.loadVector();
        return false;
    }
    
}
