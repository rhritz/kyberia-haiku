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
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoTransient;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import org.neo4j.graphdb.Node;
import play.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import plugins.MongoDB;
import plugins.Validator;

// zobrazitelne polozky z Node
@MongoDocument
public class NodeContent extends AbstractMongoEntity {

    private String        content     = "";
    private String        owner       = ""; // mongoid ownera
    private String        ownerGid; // grafove id ownera
    private Long          created     = 0l;
    private String        cr_date     = "";
    private String        name        = "";
    private String        parent      = "";

    private Integer       template    = 1;
    public Long           gid         = 1l;
    private String        putId       = null; // ak != null tak toto je hardlink

    private Integer       accessType  = 0; // AccessType.PUBLIC = 0, PRIVATE, MODERATED, ?INHERIT?
    private Integer       contentType = 0; // zatial nic

    private Long          k           = 0l;
    private Long          mk          = 0l;
    private Long          numVisits   = 0l;

    // tu by asi mali byt idcka tagov a nie tagy samotne
    private List<String>  tags;
    private List<String>  kgivers;  // +k
    private List<String>  mkgivers; // -k

    private List<String>  bans;
    private List<String>  access;
    private List<String>  silence;
    private List<String>  masters;

    private List<String>  fooks;

    // transient ?
    private List<String>  vector;

    @MongoTransient
    public Integer        depth       = 1;

    public NodeContent() {}


    public NodeContent (Node n,
                        String ownerid,
                        Map<String,String> params,
                        List<String> roots)
    {
        content = Validator.validate(params.get(Haiku.CONTENT));
        created = System.currentTimeMillis();
        gid     = n.getId();
        cr_date = DateFormat.getDateTimeInstance(DateFormat.LONG,
                    DateFormat.LONG).format(new Date(getCreated()));
        template =  NodeTemplate.BASIC_NODE;
        name    = params.containsKey(Haiku.NAME) ?
                    params.get(Haiku.NAME) : gid.toString(); // zatial
        owner   = ownerid;
        collectionName = "Node";
        if (roots != null && roots.size() > 0)
            parent = roots.get(0);
        vector     = roots;

        Logger.info("Adding node with c " + content);
    }

    public void edit(Map<String,String> params)
    {
        String accType = params.get("access_type");
        if (accType != null) {
            this.accessType = Integer.parseInt(accType);
        }
        String access = params.get("access");
        if (access != null && ! this.access.contains(access)) {
            this.access.add(access);
        }
        String silence = params.get("silence");
        if (silence != null && ! this.silence.contains(silence)) {
            this.silence.add(silence);
        }
        String master = params.get("master");
        if (master != null && ! this.masters.contains(master) ) {
            this.masters.add(master);
        }
        String ban = params.get("ban");
        if (ban != null && ! this.bans.contains(ban)) {
            this.bans.add(ban);
        }
        String chOwner = params.get("change_owner");
        if (chOwner != null && User.load(chOwner) != null) {
            owner = chOwner;
        }
        String chParent = params.get("parent");
        if (chParent != null && NodeContent.load(chParent) != null) {
            // TODO Haiku.reparent this.owner = chOwner;
        }
        String chName = params.get("name");
        if (chName != null) {
            name = Validator.validateTextonly(chName);
        }
        String chTemplate = params.get("template");
        if (chTemplate != null ) {
            template = Integer.parseInt(chTemplate);
        }
        String chContent = params.get("content");
        if (chContent != null) {
            content = Validator.validate(chContent);
        }
        this.update();
    }

    // hlavicka prispevku - TODO zmenit na tag
    public String getHead()
    {
        StringBuilder head = new StringBuilder();
        head.append("<a href=\"/id/").append(id).append("\">").append(gid);
        head.append("</a>(").append(depth).append(") ").append(getName());
        head.append(" ").append("<a href=\"/user/").append(getOwner()).append("\">");
        // docasne
        if (! owner.equals("ubik") ) head.append(User.getNameForId(getOwner()));
        head.append("</a>").append(" ");
        head.append(getCr_date());
        // User.getLink(owner);
        return head.toString() ;
    }

    public String getContent()
    {
        return content;
    }

    /**
     * @return the k
     */
    public Long getK() {
        return k;
    }

    public void addTag(String tag)
    {
        if (tag == null)
            return;
        if (tags == null)
            tags = new LinkedList<String>();
        else if (tags.contains(tag))
            return;
        else
            tags.add(tag);
    }

    // save to mongodb
    public String save()
    {
        try {
            Logger.info("creating new node");
            MongoDB.save(this, MongoDB.CNode);
            Logger.info("NodeContent now has ID::" + this.getId());
            // TODO toto je snad docasne.. ked opravia driver tak aby vracal
            // last insert id
            NodeContent withId = loadByGid(this.gid);
            Logger.info("NodeContent now has ID::" + withId.getId());
            return withId.getId();
        } catch (Exception ex) {
            Logger.info("create failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return null;
    }

    public void update()
    {
        try {
            Logger.info("updating node");
            MongoDB.update(this, MongoDB.CNode);
        } catch (Exception ex) {
            Logger.info("update failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public void delete()
    {
        try {
            Logger.info("deleting node");
            MongoDB.delete(this, MongoDB.CNode);
            // + ostatne veci co treba deltnut: Activity, Bookmarks, ..?
        } catch (Exception ex) {
            Logger.info("delete failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public static NodeContent load(String id, int depth)
    {
        NodeContent nc = load(id);
        nc.depth = depth;
        return nc;
    }

    // load from mongodb
    public static NodeContent load(String id)
    {
        NodeContent n = null;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    findOne(new BasicDBObject().append("_id",
                    new ObjectId(id)));
            if (iobj !=  null) {
                Logger.info("node found");
                n = MongoDB.getMorphia().fromDBObject(NodeContent.class,
                           (BasicDBObject) iobj);
            }
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    // TODO Cache!
    public static NodeContent loadByGid(Long gid)
    {
        Logger.info("about to look for " + gid);
        NodeContent n = null;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    findOne(new BasicDBObject().append("gid",gid));
            if (iobj !=  null) {
                Logger.info("node found");
                n = MongoDB.getMorphia().fromDBObject(NodeContent.class,
                           (BasicDBObject) iobj);
            }
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        Logger.info("node found::" + n);
        return n;
    }

    // bud to bude priamo v Node ALEBO v grupach ALEBO zdedene cez grupy
    public boolean canRead(String uid)
    {
        Logger.info("canRead:: uid " + uid + " acc type " + accessType);
        if (owner.equals(uid))
            return true;
        if (accessType == null) 
            return true;
        switch (accessType) {
            case ACL.PUBLIC:
                            if (bans.contains(uid))
                                return false;
                            break;
            case ACL.PRIVATE:
                            if (! access.contains(uid))
                                return false;
                            break;
            case ACL.MODERATED:
                            if (bans.contains(uid))
                                return false;
                            break;                
        }

        return true;
    }

    // moze pridavat reakcie, tagy etc?
    public boolean canWrite(String uid)
    {
        Logger.info("canWrite:: uid " + uid + " acc type " + accessType);

        if (owner.equals(uid))
            return true;
        if (accessType == null) 
            return false;
        switch (accessType) {
            case ACL.PUBLIC:
                            if (bans.contains(uid) || silence.contains(uid))
                                return false;
                            break;
            case ACL.PRIVATE:
                            if (! access.contains(uid))
                                return false;
                            break;
            case ACL.MODERATED:
                            if (bans.contains(uid))
                                return false;
                            break;
        }
        return true;
    }

    // moze editovat properties?
    public boolean canEdit(String uid)
    {
        // Logger.info("canEdit:: " + User.getNameForId(owner) + " acc type " + accessType + " owner:" + User.getNameForId(owner));
        return owner.equals(uid) || (masters != null && masters.contains(uid));
    }

    // if null inherit everything
    public void inheritPermissions(String from, String type)
    {
        
    }
    
    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the created
     */
    public Long getCreated() {
        return created;
    }

    /**
     * @return the cr_date
     */
    public String getCr_date() {
        return cr_date;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * @return the template
     */
    public Integer getTemplate() {
        return template;
    }

    /**
     * @param k the k to set
     */
    public void setK(Long k) {
        this.k = k;
    }

    /**
     * @return the mk
     */
    public Long getMk() {
        return mk;
    }

    /**
     * @param mk the mk to set
     */
    public void setMk(Long mk) {
        this.mk = mk;
    }

}
