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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import play.Logger;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.cache.Cache;
import plugins.MongoDB;
import plugins.Validator;

@Entity("Node")
public class NodeContent extends MongoEntity {

    private String        content     = "";
    public  ObjectId      owner       ; // mongoid ownera
    private Long          created     = 0l;
    private String        cr_date     = "";
    public  String        name        = "";
    public  ObjectId      par             ;
    public  ObjectId      dfs;

    private String        template;
    private ObjectId      putId;

    private Integer       accessType  = 0;
    private Integer       contentType = 0; // zatial nic

    private Long          k           = 0l;
    private Long          mk          = 0l;
    private Long          numVisits   = 0l;
    private Boolean       kAllowed    = true;

    private List<String>    tags;
    private List<ObjectId>  kgivers;  // +k
    private List<ObjectId>  mkgivers; // -k

    private List<ObjectId>  bans;
    private List<ObjectId>  access;
    private List<ObjectId>  silence;
    private List<ObjectId>  masters;

    private Map<ObjectId,Boolean>  fook;

    @Transient
    private List<ObjectId>  vector;

    @Transient
    private ImmutableMap<ObjectId,ACL> acl;

    @Transient
    public Integer        depth       = 1;
    @Transient
    public String           parName;
    @Transient
    public String           ownerName;

    public static final String CONTENT  = "content";
    public static final String CREATED  = "created";
    public static final String NAME     = "name";
    public static final String OWNER   = "owner";

    public static final int PUBLIC    = 0;
    public static final int PRIVATE   = 1;
    public static final int MODERATED = 2;

    public NodeContent() {}

    public NodeContent (ObjectId ownerid,
                        Map<String,String> params)
    {
        content = Validator.validate(params.get(CONTENT));
        created = System.currentTimeMillis();
        cr_date = DateFormat.getDateTimeInstance(DateFormat.LONG,
                    DateFormat.LONG).format(new Date(getCreated()));
        // template =  NodeTemplate.BASIC_NODE;
        name    = params.containsKey(NAME) ?
                    params.get(NAME) : cr_date; // zatial
        owner   = ownerid;

        Logger.info("Adding node with content:: " + content);
    }

    public void edit(Map<String,String> params)
    {
        String accType = params.get("access_type");
        if (accType != null) {
            accessType = Integer.parseInt(accType);
        }
        String addAccess = params.get("access");
        if (addAccess != null ) {
            ObjectId accId = toId(addAccess);
            if (getAccess() == null)
                access = new LinkedList<ObjectId>();
            if (accId != null && ! access.contains(accId))
                getAccess().add(accId);
        }
        String addSilence = params.get("silence");
        if (addSilence != null ) {
            ObjectId silId = toId(addSilence);
            if (getSilence() == null)
                silence = new LinkedList<ObjectId>();
            if (silId != null && ! silence.contains(silId))
                getSilence().add(silId);
        }
        String addMaster = params.get("master");
        if (addMaster != null ) {
            ObjectId masterId = toId(addMaster);
            if (getMasters() == null)
                masters = new LinkedList<ObjectId>();
            if (masterId != null && !masters.contains(masterId) )
                getMasters().add(masterId);
        }
        String ban = params.get("ban");
        if (ban != null ) {
            ObjectId banId = toId(ban);
            if (getBans() == null)
                bans = new LinkedList<ObjectId>();
            if (banId != null && ! bans.contains(banId))
                getBans().add(banId);
        }
        String chOwner = params.get("change_owner");
        if (chOwner != null && User.load(chOwner) != null) {
            owner = new ObjectId(chOwner);
        }
        String chParent = params.get("parent");
        if (chParent != null) {
            NodeContent np = load(chParent);
            if (np != null)
                moveNode(np.getId());
        }
        String chName = params.get(NAME);
        if (chName != null) {
            name = Validator.validateTextonly(chName);
        }
        String chTemplate = params.get("template");
        if (chTemplate != null ) {
            template = Validator.validateTextonly(chTemplate);
        }
        String chContent = params.get(CONTENT);
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
        if (tags.contains(tag))
            return;
        else
            tags.add(tag);
    }

    // save to mongodb
    public NodeContent save()
    {
        try {
            Logger.info("creating new node");
            MongoDB.save(this, MongoDB.CNode);
            // TODO generovat ID pred insertom
            NodeContent koko = MongoDB.getMorphia().fromDBObject(NodeContent.class,
               (BasicDBObject) MongoDB.getDB().getCollection(MongoDB.CNode).findOne(
               (BasicDBObject) MongoDB.getMorphia().toDBObject(this)));
            Cache.set("node_" + koko.getId(), koko);
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

    private void delete()
    {
        try {
            Logger.info("deleting node");
            Cache.delete("node_" + this.getId());
            MongoDB.delete(this, MongoDB.CNode);
        } catch (Exception ex) {
            Logger.info("delete failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    // load from mongodb
    public static NodeContent load(String id)
    {
        ObjectId oid = toId(id);
        if (oid == null)
           return null;
        return load(oid);
    }

    // TODO parametrize parent loading
    public static NodeContent load(ObjectId id)
    {
        // Logger.info("About to load node " + id);
        NodeContent n = Cache.get("node_" + id, NodeContent.class);
        if (n != null )
            return n;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    findOne(new BasicDBObject("_id",id));
            if (iobj !=  null) {
                n = MongoDB.getMorphia().fromDBObject(NodeContent.class,
                           (BasicDBObject) iobj);
                n.ownerName = User.getNameForId(n.owner);
                if (n.par != null )
                    n.parName  = load(n.par).name;
                else
                    n.parName  = "";
                n.loadRights();
                Cache.add("node_" + id, n);
            }
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    private static NodeContent loadByDfs(ObjectId id)
    {
        NodeContent n = null;
        try {
            DBObject iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    findOne(new BasicDBObject("dfs",id));
            if (iobj !=  null)
                n = MongoDB.getMorphia().fromDBObject(NodeContent.class,
                           (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    // some form of smart caching?
    public static List<NodeContent> load(List<ObjectId> nodeIds) {
        List<NodeContent> nodes = null;
        try {
            DBObject query = new BasicDBObject("_id", 
                    new BasicDBObject("$in",
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

    static Map<ObjectId,NodeContent> loadByPar(ObjectId parId) {
        Map<ObjectId,NodeContent> nodes = Maps.newHashMap();
        try {
            DBCursor iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    find(new BasicDBObject("par", parId));
            if (iobj !=  null)
                for (NodeContent node : Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent()))
                    nodes.put(node.getId(), node);
        } catch (Exception ex) {
            Logger.info("load nodes::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return nodes;
    }

    public boolean canRead(ObjectId uid)
    {
        Logger.info("canRead:: uid " + uid.toString() + " acc type " + accessType);
        if (accessType == null) // ale to by sa nemalo stat -> load
            return Boolean.TRUE;
        ACL ur = acl.get(uid);
        if (ACL.BAN == ur)
            return Boolean.FALSE;
        switch (accessType) {
            case MODERATED:
            case PUBLIC:
                            return Boolean.TRUE;
            case PRIVATE:
                            if (ur == null || ACL.SILENCE == ur )
                                return Boolean.FALSE;
                            return Boolean.TRUE;
        }
        return Boolean.TRUE;
    }

    // moze pridavat reakcie, tagy etc?
    public boolean canWrite(ObjectId uid)
    {
        Logger.info("canWrite:: uid " + uid.toString() + " acc type " + accessType);
        if (accessType == null) // TODO remove this
            return Boolean.FALSE;
        ACL ur = acl.get(uid);
        if (ACL.BAN ==  ur || ACL.SILENCE == ur)
            return Boolean.FALSE;
        switch (accessType) {
            case PUBLIC:
                            return Boolean.TRUE;
            case MODERATED:
            case PRIVATE:
                            if (ur == null)
                                return Boolean.FALSE;
                            return Boolean.TRUE;
        }
        return Boolean.TRUE;
    }

    // moze editovat properties 
    public boolean canEdit(ObjectId uid)
    {
        return owner.equals(uid) || ACL.MASTER == acl.get(uid);
    }

    // moze presuvat objekty? tj menit parenta ersp. childy
    public boolean canMove(ObjectId uid)
    {
        ACL ur = acl.get(uid);
        return owner.equals(uid) || ACL.MASTER == ur || ACL.HMASTER == ur;
    }

    public void fook(ObjectId uid)
    {
        if (getFook() == null) {
            fook = new HashMap<ObjectId,Boolean>();
        } else if (getFook().containsKey(uid)) {
            return;
        }
        getFook().put(uid,Boolean.TRUE);
        update();
    }

    public void unfook(ObjectId uid)
    {
        if (getFook() == null) {
            return;
        } else if (getFook().containsKey(uid)) {
            getFook().remove(uid);
        }
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
    public String getTemplate() {
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

    // TODO - make sure no node is a child of a put node,
    // that would cause serious trouble
    // + update threading functions to maek them aware of putnodes
    public ObjectId putNode(ObjectId parentId)
    {
        if (parentId == null || load(parentId) == null)
            // put into null makes no sense
            return null;
        NodeContent parent = load(parentId);
        // TODO check & set permissions
        ObjectId pid = null;
        NodeContent putNode = new NodeContent();
        putNode.owner   = owner;
        putNode.name    = name;
        putNode.created = created;
        putNode.putId   = id;
        putNode.par     = parent.id;
        putNode = putNode.save();
        pid = putNode.id;
        if (parent.dfs == null) {
            parent.dfs = pid;
            putNode.dfs = parent.getId();
        } else {
            putNode.dfs = parent.dfs;
        }
        putNode.update();
        parent.update();
        // Notifications
        List<ObjectId> roots = new LinkedList<ObjectId>();
        NodeContent upnode = parent;
        for (int i = 0; i < 10; i++)
            // 10 - max hier depth for notif update
        {
              roots.add(upnode.getId());
              ObjectId re = upnode.getParent();
              if (re == null) break;
              upnode = NodeContent.load(re);
        }
        Activity.newNodeActivity(pid, putNode, roots, owner, parent.owner);
        
        return pid;
    }

    // TODO - should be simple reverse of the above, if caled on the putNode
    public void unputNode() {
        
    }

    public static String addNode(ObjectId parent,
            Map<String,String> params,
            ObjectId ownerid)
    {
        // TODO check & set permissions
        List<ObjectId> roots = new LinkedList<ObjectId>();
        ObjectId parOwner = null;
        NodeContent newnode = new NodeContent(ownerid,params).save();
        ObjectId mongoId = newnode.getId();
        if (mongoId == null)
        {
            // validation errors...
            return null;
        }
        Logger.info("parent :: " + parent);
        if (parent != null) {
            NodeContent root = NodeContent.load(parent);
            ObjectId dfs;
            if (root != null) {
                newnode.par = parent;
                Logger.info("parent loaded :: " + root.dfs);
                dfs = root.dfs;
                root.dfs = mongoId;
                root.update();
                Logger.info("parent saved:: " + root.dfs);
                if (dfs != null) {
                    newnode.dfs = dfs;
                } else {
                    newnode.dfs = root.getId();
                }
                Logger.info("newnode dfs:: " + newnode.dfs );
                // TODO change this - load vector etc
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

    // unles it's a putNode...
    public void deleteNode() {
        NodeContent dfsNode = null;
        NodeContent dfsSource = null;
        if (dfs != null) {
            dfsNode = load(dfs);
            // if we have dfs-out, we have a dfs-in too -
            dfsSource = loadByDfs(id);
        }
        if (dfsNode != null) {
            // dfsSource.dfs = dfs; // !!! unless this is our child
            // we have to unlink all direct children, making them orphans
            Map<ObjectId,NodeContent> children = loadByPar(id);
            if (children == null || children.isEmpty()) {
                dfsSource.dfs = dfs;
                dfsSource.update();
            } else {
                for (NodeContent child : children.values()) {
                    List<NodeContent> childrenSubtree = child.getTree(children);
                    // the last one of this list links either to the next child
                    // or somewhere outside the subtree - we don't know exactly
                    if (childrenSubtree != null && !childrenSubtree.isEmpty()) {
                        NodeContent lastOne = childrenSubtree.
                                               get(childrenSubtree.size() - 1);
                        if (! children.containsKey(lastOne.dfs)) {
                            // this one is out of the tree
                            dfsSource.dfs = lastOne.dfs;
                            dfsSource.update();
                        }
                        // create a subtree
                        lastOne.dfs = child.getId();
                        lastOne.update();
                    }
                    if (children.containsKey(child.dfs)) {
                        child.dfs = null;
                    }  else {
                        // same as above
                        dfsSource.dfs = child.dfs;
                        dfsSource.update();
                        child.dfs = null; // sure?
                    }
                    child.par = null;
                    child.update();
                }
            }
        }

        // + rm bookmarks, perhaps activities too
        delete();
    }

    // + add activity to new place, remove from old one
    // + updates in one place
    public void moveNode(ObjectId to) {
        NodeContent toNode = load(to);
        if (toNode == null) // + permissions
            return;
        // fix old dfs, if dfs goes out of the subtree;
        // set new dfs, -||-
        NodeContent dfsNode = null;
        NodeContent dfsSource = null;
        NodeContent lastOne = null;
        if (dfs != null) {
            dfsNode = load(dfs);
            dfsSource = loadByDfs(id);

            // 1. - remove from the old place
            if (dfsNode != null) {
                List<NodeContent> childrenSubtree = getTree(null);
                if (childrenSubtree != null) {
                    lastOne = childrenSubtree.get(childrenSubtree.size() - 1);
                    if (lastOne.dfs.equals(id)) {
                        // this one is out of the tree
                        dfsSource.dfs = lastOne.dfs;
                        dfsSource.update();
                    }
                } else { // no children
                    dfsSource.dfs = dfs;
                    dfsSource.update();
                }
            }
        }
        // 2. - move to the new one - added to the top, not sorted by time!
        if (lastOne == null) 
            dfs = toNode.dfs;
        else {
            lastOne.dfs = toNode.dfs;
            lastOne.update();
        }
        toNode.dfs = id;
        toNode.update();
        par = to;
        update();
    }

    private static List<NodeContent> getThreadedChildren(ObjectId id,
            Integer start, Integer count)
    {
        List<NodeContent> thread = null;
        try {
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

    public List<NodeContent> getThreadIntern(Integer start, Integer count)
    {
        List<NodeContent> thread = new LinkedList<NodeContent>();
        HashMap<ObjectId,Integer> roots = new HashMap<ObjectId,Integer>();
        int local_depth = 0;
        NodeContent nextnode = this;
        NodeContent lastnode;
        ObjectId parent;
        for (int i = 0; i < start + count; i++) {
            lastnode = nextnode;
            if (lastnode.dfs == null) 
              return thread;
            nextnode = NodeContent.load(lastnode.dfs);
            if (nextnode == null || nextnode.par == null)
              return thread;
            parent = nextnode.par;
            if (parent.equals(lastnode.id)) {
                roots.put(parent,local_depth);
                local_depth++;
            } else {
                // ak v tomto bode nema parenta v roots,
                // znamena to ze siahame vyssie ako root - koncime
                if (roots.get(parent) == null) 
                    return thread;
                // nasli sme parenta, sme o jedno hlbsie ako on
                local_depth = roots.get(parent) + 1;
            }
            if ( i>= start) {
                // tento node chceme zobrazit
                // tu je vhodne miesto na kontrolu per-node permissions,
                // ignore a fook zalezitosti
                nextnode.depth = local_depth;
                thread.add(nextnode);
            }
        }
        return thread;
    }

    // returns the whole subtree of a node, optionally stop on stopNodes.
    // with stopNodes being usually either the vector of a node, or its siblings
    private List<NodeContent> getTree(Map<ObjectId,NodeContent> stopNodes)
    {
        List<NodeContent> thread = Lists.newLinkedList();
        HashMap<ObjectId,Integer> roots = Maps.newHashMap();
        int localDepth = 0;
        NodeContent nextnode = this;
        NodeContent lastnode;
        ObjectId parent;
        for (int i = 0; i < 1000000; i++) {
            lastnode = nextnode;
            if (lastnode.dfs == null)
              break;
            if (stopNodes != null && stopNodes.containsKey(lastnode.dfs))
              break;
            nextnode = NodeContent.load(lastnode.dfs);
            if (nextnode == null || nextnode.par == null)
              break;
            parent = nextnode.par;
            if (parent.equals(lastnode.id)) {
                roots.put(parent,localDepth);
                localDepth++;
            } else {
                if (roots.get(parent) == null)
                    break;
                localDepth = roots.get(parent) + 1;
            }
            nextnode.depth = localDepth;
            thread.add(nextnode);
        }
        return thread;
    }

    protected List<NodeContent> loadVector() {
        List<NodeContent> nodes = null; 
        // TODO este predtym by to chcelo vybrat zacachovane a vratit ich tak
        try {
            DBObject query = new BasicDBObject("_id", new BasicDBObject("$in",
                    vector.toArray(new ObjectId[vector.size()])));
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

    // +K
    public void giveK(User user)
    {
        //TODO error messages
        if (user.getDailyK() < 1)
            return;
        if (kgivers == null)
            kgivers = new LinkedList<ObjectId>();
        else if (kgivers.contains(user.getId()))
            return;
        else
            kgivers.add(user.getId());
        if (getK() == null)
            setK(1l);
        else 
            setK(getK() + 1);
        user.setDailyK(user.getDailyK() - 1);
        user.update();
        update();
    }

    // -K
    public void giveMK(User user)
    {
        //TODO error messages
        if (user.getDailyK() < 1)
            return;
        if (kgivers == null)
            kgivers = new LinkedList<ObjectId>();
        else if (kgivers.contains(user.getId()))
            return;
        else
            kgivers.add(user.getId());
        if (getMk() == null)
            setMk(1l);
        else
            setMk(getMk() + 1);
        user.setDailyK(user.getDailyK() - 1);
        user.update();
        update();
    }

    /**
     * @return the bans
     */
    protected List<ObjectId> getBans() {
        return bans;
    }

    /**
     * @return the access
     */
    protected List<ObjectId> getAccess() {
        return access;
    }

    /**
     * @return the silence
     */
    protected List<ObjectId> getSilence() {
        return silence;
    }

    /**
     * @return the masters
     */
    protected List<ObjectId> getMasters() {
        return masters;
    }

    /**
     * @return the fook
     */
    protected Map<ObjectId, Boolean> getFook() {
        return fook;
    }

    // Node o 1 vyssie nad nami uz urcite ma vsetko poriesene
    public void loadRights()
    {
        // ImmutableMap.Builder allows duplicate keys but cannot handle them
        // stupid cocks
        Map<ObjectId,ACL> bu = Maps.newHashMap();
        if (access != null)
            for (ObjectId acc : access)
                bu.put(acc, ACL.ACCESS);
        if (silence != null)
            for (ObjectId sil : silence)
                bu.put(sil, ACL.SILENCE);        
        if (masters != null)
            for (ObjectId master : masters)
                bu.put(master, ACL.MASTER);
        if (bans != null)
            for (ObjectId ban : bans)
                bu.put(ban, ACL.BAN);
        if (par != null) {
            NodeContent parent = load(par);
            if (parent.fook != null)
                fook.putAll(parent.fook);
            if (parent.acl != null) {
                for (Map.Entry<ObjectId,ACL> e : parent.acl.entrySet()) {
                    switch (e.getValue()) {
                        case ACCESS:  bu.put(e.getKey(), ACL.ACCESS);  break;
                        case SILENCE: bu.put(e.getKey(), ACL.SILENCE); break;
                        case BAN:     bu.put(e.getKey(), ACL.BAN);     break;
                        case MASTER:  bu.put(e.getKey(), ACL.HMASTER); break;
                        case OWNER:   bu.put(e.getKey(), ACL.HMASTER); break;
                    }
                }
            }
        }
        bu.put(owner, ACL.OWNER);
        acl = ImmutableMap.copyOf(bu);
    }
}
