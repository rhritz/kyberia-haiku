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
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import play.Logger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.cache.Cache;
import plugins.MongoDB;
import plugins.Validator;

@Entity("Node")
public class NodeContent extends MongoEntity {

    private String        content     = "";
    private ObjectId      owner       ; // mongoid ownera
    private String        ownerGid; // grafove id ownera - pouziva sa niekde?
    private Long          created     = 0l;
    private String        cr_date     = "";
    private String        name        = "";
    private ObjectId      par             ;

    private Integer       template    = 1;
    private String        putId       = null; // ak != null tak toto je hardlink

    private Integer       accessType  = 0;
    private Integer       contentType = 0; // zatial nic

    private Long          k           = 0l;
    private Long          mk          = 0l;
    private Long          numVisits   = 0l; // TODO
    private Boolean       kAllowed    = true;

    // tu by asi mali byt idcka tagov a nie tagy samotne
    private List<String>  tags;
    private List<ObjectId>  kgivers;  // +k
    private List<ObjectId>  mkgivers; // -k

    private List<ObjectId>  bans;
    private List<ObjectId>  access;
    private List<ObjectId>  silence;
    private List<ObjectId>  masters;

    private List<ObjectId>  fooks;

    @Transient
    private List<ObjectId>  vector;

    @Transient
    public Integer        depth       = 1;

    private ObjectId      dfs;

    public NodeContent() {}

    public NodeContent (ObjectId ownerid,
                        Map<String,String> params)
    {
        content = Validator.validate(params.get(Haiku.CONTENT));
        created = System.currentTimeMillis();
        cr_date = DateFormat.getDateTimeInstance(DateFormat.LONG,
                    DateFormat.LONG).format(new Date(getCreated()));
        template =  NodeTemplate.BASIC_NODE;
        name    = params.containsKey(Haiku.NAME) ?
                    params.get(Haiku.NAME) : cr_date; // zatial
        owner   = ownerid;

        Logger.info("Adding node with content:: " + content);
    }

    public void edit(Map<String,String> params)
    {
        String accType = params.get("access_type");
        if (accType != null) {
            this.accessType = Integer.parseInt(accType);
        }
        String access = params.get("access");
        if (access != null ) {
            ObjectId accId = toId(access);
            if (accId != null && ! this.access.contains(accId))
                this.access.add(accId);
        }
        String silence = params.get("silence");
        if (silence != null ) {
            ObjectId silId = toId(silence);
            if (silId != null && ! this.silence.contains(silId))
                this.silence.add(silId);
        }
        String master = params.get("master");
        if (master != null ) {
            ObjectId masterId = toId(master);
            if (masterId != null && ! this.masters.contains(masterId) )
                this.masters.add(masterId);
        }
        String ban = params.get("ban");
        if (ban != null ) {
            ObjectId banId = toId(ban);
            if (banId != null && ! this.bans.contains(banId))
                this.bans.add(banId);
        }
        String chOwner = params.get("change_owner");
        if (chOwner != null && User.load(chOwner) != null) {
            owner = new ObjectId(chOwner);
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
        this.update(); // TODO udpate cache
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
    // TODO invalidate cache
    public NodeContent save()
    {
        try {
            Logger.info("creating new node");
            MongoDB.save(this, MongoDB.CNode);
            // TODO toto je snad docasne.. ked opravia driver tak aby vracal
            // last insert id
            NodeContent koko = MongoDB.getMorphia().fromDBObject(NodeContent.class,
               (BasicDBObject) MongoDB.getDB().getCollection(MongoDB.CNode).findOne(
               (BasicDBObject) MongoDB.getMorphia().toDBObject(this)));
            Logger.info("new NodeContent now has ID::" + koko.getId());
            return koko;
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
            Cache.set("node_" + this.getId(), this);
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
            Cache.delete("node_" + this.getId());
            MongoDB.delete(this, MongoDB.CNode);
            
            // + ostatne veci co treba deltnut: Activity, Bookmarks, ..?
            // + graf
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
        if (id == null || id.length() < 10) return null;
        return load(new ObjectId(id));
    }

    // load from mongodb
    public static NodeContent load(ObjectId id)
    {
        // Logger.info("About to load node " + id);
        NodeContent n = Cache.get("node_" + id, NodeContent.class);
        if (n != null )
            return n;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    findOne(new BasicDBObject().append("_id",id));
            if (iobj !=  null) {
                n = MongoDB.getMorphia().fromDBObject(NodeContent.class,
                           (BasicDBObject) iobj);
                Cache.add("node_" + id, n);
            }
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    // some form of smart caching?
    static List<NodeContent> load(List<ObjectId> nodeIds) {
        List<NodeContent> nodes = null;
        try {
            DBObject query = new BasicDBObject("_id", 
                    new BasicDBObject().append("$in",
                    nodeIds.toArray(new ObjectId[nodeIds.size()])));
            DBCursor iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    find(query);
            if (iobj !=  null)
                nodes = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("load nodes::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return nodes;
    }

    // bud to bude priamo v Node ALEBO v grupach ALEBO zdedene cez grupy
    public boolean canRead(ObjectId uid)
    {
        Logger.info("canRead:: uid " + uid.toString() + " acc type " + accessType);
        if (owner.equals(uid))
            return true;
        if (accessType == null) 
            return true;
        switch (accessType) {
            case ACL.PUBLIC:
                            if (bans != null && bans.contains(uid))
                                return false;
                            break;
            case ACL.PRIVATE:
                            if (access != null && !access.contains(uid))
                                return false;
                            break;
            case ACL.MODERATED:
                            if (bans != null  && bans.contains(uid))
                                return false;
                            break;                
        }

        return true;
    }

    // moze pridavat reakcie, tagy etc?
    public boolean canWrite(ObjectId uid)
    {
        Logger.info("canWrite:: uid " + uid.toString() + " acc type " + accessType);

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
    public boolean canEdit(ObjectId uid)
    {
        // Logger.info("canEdit:: " + User.getNameForId(owner) + " acc type " + accessType + " owner:" + User.getNameForId(owner));
        return owner.equals(uid) || (masters != null && masters.contains(uid));
    }

    // if null inherit everything
    public void inheritPermissions(String from, String type)
    {
        
    }

    public void fook(ObjectId uid)
    {
        if (fooks == null) {
            fooks = new LinkedList<ObjectId>();
        } else if (fooks.contains(uid)) {
            return;
        }
        fooks.add(uid);
        update();
    }
    
    /**
     * @return the owner
     */
    public ObjectId getOwner() {
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
    public ObjectId getParent() {
        return par;
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

    // docasne
    public static String addNode(ObjectId parent,
            Map<String,String> params,
            ObjectId ownerid)
    {
        // TODO validate content & name -> antisamy?
        // TODO podstatna vec co tu potrebujeme je isPut a pripadne permissions
        List<ObjectId> roots = new LinkedList<ObjectId>();
        ObjectId parOwner = null;
        NodeContent newnode = new NodeContent(ownerid,params).save();
        ObjectId mongoId = newnode.getId();
        if (mongoId == null)
        {
            // throw new Exception();
            return null;
        }
        Logger.info("parent :: " + parent);
        if (parent != null) {
            NodeContent root = NodeContent.load(parent);
            NodeContent sibling = null;
            ObjectId dfs;
            if (root != null) {
                newnode.par = parent;
                Logger.info("parent loaded :: " + root.dfs);
                dfs = root.dfs;
                if (dfs != null ) {
                    sibling = NodeContent.load(dfs);
                }
                root.dfs = mongoId;
                root.update();
                Logger.info("parent saved:: " + root.dfs);
                if (sibling != null) {
                    newnode.dfs = sibling.getId();
                } else {
                    newnode.dfs = root.getId();
                }
                Logger.info("newnode dfs:: " + newnode.dfs );
                // nizsie su updaty notifikacii
                NodeContent upnode = root;
                for (int i = 0; i < 10; i++)
                    // 10 - max hier depth for notif update
                {
                      roots.add(upnode.getId());
                      ObjectId re = upnode.getParent();
                      if (re == null) break;
                      upnode = NodeContent.load(re);
                }
                User parentOwner = User.load(root.owner);
                if (parentOwner != null) {
                    parOwner = parentOwner.getId();
                }
            }
        }
        newnode.update();
        Activity.newNodeActivity(mongoId, newnode, roots, ownerid, parOwner);
        return mongoId.toString(); // neskor len id
    }


    public static List<NodeContent> getThreadedChildren(ObjectId id,
            Integer start, Integer count)
    {
        List<NodeContent> thread = null;
        try {
            // ach jaj
            String evalQuery = "return getThreadedChildren(ObjectId(\"" +
                    id.toString() + "\"), " + start + "," + count + ");";
            DBObject iobj = MongoDB.getDB().doEval(evalQuery, "");
            if (iobj !=  null) {
                // Logger.info("getThreadedChildren:: " + iobj.toString());
                BasicDBList oo = (BasicDBList) iobj.get("retval");
                // Logger.info(oo.getClass().getCanonicalName());
                if (oo != null) {
                    thread = new LinkedList<NodeContent>();
                    for (Object ooo : oo ) {
                        BasicDBList nodef = (BasicDBList) ooo;
                        ObjectId nodeId = (ObjectId) nodef.get(0);
                        Double depth = (Double)  nodef.get(1);
                        NodeContent nc = NodeContent.load(nodeId);
                        nc.depth = depth.intValue();
                        thread.add(nc);
                        // Logger.info("bla:: " + ooo.toString() + " " + ooo.getClass().getCanonicalName() + "0:" + nodeId + "1:" + depth);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.info("getThreadedChildren");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return thread;
    }

    // blabla
    public static ObjectId toId(String x) {
        ObjectId bubu = null;
        try { bubu = new ObjectId(x);} catch (Exception e ) {};
        return bubu;
    }

}
