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

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.List;
import play.Logger;
import plugins.MongoDB;

/*
 * Lists of nodes by various criteria
 */
public class Nodelist {

    private static final String CREATED = "created";
    private static final String OWNER   = "owner";
    private static final String K = "k";
    private static final BasicDBObject kQuery   = new BasicDBObject().
            append(K, new BasicDBObject("$gt", 1));
    private static final BasicDBObject kSort    = new BasicDBObject().
            append(K, -1);
    private static final BasicDBObject lastSort = new BasicDBObject().
            append(CREATED, -1);
    private static final Long t24h = 86400000l;

    /*
     vyber nodes z posledneho dna, zorad podla K
     */
    public static List<NodeContent> getKlist(Integer count)
    {
        List<NodeContent> r = null;
        try {
            // Long t24hAgo = 0l;
            // kQuery.append(CREATED, new BasicDBObject("$gt", t24hAgo)).
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CNode).find(kQuery).sort(kSort).
                        limit(count ==  null ? 100 : count);
            // query.append(fooks, new BasicDBObject("$notin", 1)); ?
            if (iobj !=  null) 
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getKlist");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    /*
     vyber poslednych x nodes
     */
    public static List<NodeContent> getLastNodes(Integer count)
    {
        List<NodeContent> r = null;
        try {
            BasicDBObject query = new BasicDBObject().
                    append(CREATED, new BasicDBObject("$gt", 
                        System.currentTimeMillis() - t24h));
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CNode).find(query).
                        sort(lastSort).limit(count ==  null ? 30 : count);
            if (iobj !=  null) 
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getLastNodes");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    /*
     vyber userovych nodes
     */
    public static List<NodeContent> getUserNodes(String uid, Integer count)
    {
        List<NodeContent> r = null;
        try {
            BasicDBObject query = new BasicDBObject().
                    append(OWNER, uid); // new ObjectId(
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CNode).find(query).
                        sort(lastSort).limit(count ==  null ? 30 : count);
            if (iobj !=  null)
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getUserNodes");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    // +K
    public static void giveK(NodeContent node, String uid)
    {
        // Odpocitaj userovi Kacka, ak ma dost, ak nie bail
        // get node, pozri ci mu uz nedal
        // updatni node
        // TODO errorz mezzagez
        if (node.getK() == null)
        {
            node.setK(1l);
        } else {
            node.setK(node.getK() + 1);
        }
        node.update();
    }

    // -K
    public static void giveMK(NodeContent node, String uid)
    {
        // Odpocitaj userovi Kacka, ak ma dost, ak nie bail
        // get node, pozri ci mu uz nedal
        // updatni node
        // TODO errorz mezzagez
        if (node.getMk() == null)
        {
            node.setMk(1l);
        } else {
            node.setMk(node.getMk() + 1);
        }
        node.update();
    }

}
