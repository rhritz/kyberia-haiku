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
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import play.Logger;
import plugins.MongoDB;
import plugins.Validator;


// TODO caching
@Entity("Tag")
public class Tag extends MongoEntity {

    public static DBCollection dbcol = null;

    String  tag;
    Integer count;

    public Tag() {}

    public Tag(String name) { tag = name; count = 0; }
    
    public static void tagNode(NodeContent nc, String tag, String uid) {
        Tag tagDoc = Tag.load(tag);
        if (tagDoc == null) 
            tagDoc = add(tag);
        if (tagDoc == null)
            return;
        nc.addTag(tag);
        nc.update();
        tagDoc.inc();
        Logger.info(" tag " + tag + " count " + tagDoc.count);
        // TODO poznacit si asi aj kto ten tag dal
        // - bud v samostatnej kolekcii, alebo v zozname tagov
        // nieco ako TagNodeUser.add(tag,nodeid,uid)
    }

    public static Tag add(String name)
    {
        if (name == null || name.isEmpty() || name.length() > 50) 
            return null;
        name = Validator.validateTextonly(name);
        if (name == null)
            return null;
        Tag t = new Tag(name);
        t.save();
        return t;
    }

    private void inc()
    {
        try {
            dbcol.update(new BasicDBObject("tag",tag), new BasicDBObject("$inc",
                    new BasicDBObject("count",1)),false, false);
        } catch (Exception ex) {
            Logger.info("tag inc fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public static Tag load(String name)
    {
        Tag tag = null;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("tag",name));
            if (iobj != null)
                tag = MongoDB.fromDBObject(Tag.class, iobj);
        } catch (Exception ex) {
            Logger.info("tag load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return tag;
    }

    // TODO + cache
    public void save()
    {
        MongoDB.save(this, MongoDB.CTag);
    }

    // TODO + cache
    public void update()
    {
        MongoDB.update(this, MongoDB.CTag);
    }

    public static List<NodeContent> getTaggedNodes(String tag)
    {
        List<NodeContent> nodes = null;
        try {
            DBCursor iobj = NodeContent.dbcol.find(new BasicDBObject("tags", tag));
            if (iobj !=  null)
                nodes = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getTaggedNodes::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return nodes;
    }

    public static List<Tag> getTagCloud(String tag)
    {
        List<Tag> r = null;
        try {
            BasicDBObject query = new BasicDBObject("tag", tag);
            BasicDBObject sort = new BasicDBObject("tag", 1);
            DBCursor iobj = dbcol.find(query).sort(sort).limit(30);
            if (iobj ==  null) {
                r = new ArrayList<Tag>();
            } else {
                Logger.info("tags found");
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toTag());
            }
        } catch (Exception ex) {
            Logger.info("getTagCloud");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    public static List<User> getTaggers(String tag)
    {
        List<User> r = null;
        return r;
    }
}
