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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import play.Logger;
import org.joda.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.joda.time.format.DateTimeFormat;
import play.cache.Cache;
import plugins.MongoDB;
import plugins.Validator;

@Entity("Node")
public class NodeContent extends MongoEntity {

    public static DBCollection dbcol = null;
    private static final String key  = "node_";
    
    private String        content     = "";
    public  ObjectId      owner       ; // mongoid ownera
    private Long          created     = 0l;
    private String        cr_date     = ""; // transient?
    public  String        name        = "";
    public  ObjectId      par             ;
    public  ObjectId      dfs;

    private String        template;
    private ObjectId      putId;

    private Integer       accessType  = 0;
    private Integer       typ         = 0;

    private Long          k           = 0l;
    private Long          mk          = 0l;
    private Long          numVisits   = 0l; // TODO move this elsewhere
    private Boolean       kAllowed    = true;

    private List<String>    tags;
    private List<ObjectId>  kgivers;  // +k
    private List<ObjectId>  mkgivers; // -k

    private List<ObjectId>  bans;
    private List<ObjectId>  access;
    private List<ObjectId>  silence;
    private List<ObjectId>  masters;

    private Map<String,Boolean>  fook;

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
    public static final String OWNER    = "owner";

    public static final int PUBLIC    = 0;
    public static final int PRIVATE   = 1;
    public static final int MODERATED = 2;

    // -> plugin?
    private static DateTimeFormatter dateFormatter =
            DateTimeFormat.forPattern("dd.MM.YYYY - HH:mm:ss");

    public NodeContent() {}

    public NodeContent (ObjectId ownerid,
                        Map<String,String> params) {
        content  = Validator.validate(params.get(CONTENT));
        created  = System.currentTimeMillis();
        cr_date  = dateFormatter.print(created);
        typ      = Type.NODE.ordinal();
        template = "Node";
        String pname = Validator.validateTextonly(params.get(NAME));
        if (pname == null || pname.length() < 1) {
            name = cr_date;
        } else {
            name = pname;
        }
        owner   = ownerid;

        Logger.info("Adding node with content:: " + content);
    }

    public void edit(Map<String,String> params, User user) {
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
        ObjectId chOid = toId(chOwner);
        if (chOid != null && User.load(chOid) != null) {
            owner = chOid;
        }
        String chParent = params.get("parent");
        if (chParent != null) {
            NodeContent np = load(chParent);
            if (np != null && ! np.getId().equals(par) )
                moveNode(np.getId(), user);
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
        update(true);
    }

    public String getContent() {
        return content;
    }

    /**
     * @return the k
     */
    public Long getK() {
        return k;
    }

    public void addTag(String tag) {
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
    public NodeContent insert() {
        try {
            setId(new ObjectId());
            save();
            enhance();
            Cache.set("node_" + getId(), this);
            return this;
        } catch (Exception ex) {
            Logger.info("create failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return null;
    }

    private void delete() {
        try {
            Logger.info("deleting node");
            Cache.delete("node_" + getId());
            MongoDB.delete(this);
        } catch (Exception ex) {
            Logger.info("delete failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public static NodeContent load(String id, User user) {
        return load(toId(id),user);
    }
    
    private static NodeContent load(String id) {
        ObjectId oid = toId(id);
        if (oid == null)
           return null;
        return load(oid);
    }

    public static NodeContent load(ObjectId id, User user) {
        NodeContent node = load(id);
        if ( node != null && node.canRead(user.getId()))
            return node;
        else
            return null;
    }

    // TODO parametrize parent loading
    // TODO permissions @Thread
    public static NodeContent load(ObjectId id) {
        // Logger.info("About to load node " + id);
        NodeContent n = Cache.get("node_" + id, NodeContent.class);
        if (n != null )
            return n;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id",id));
            if (iobj !=  null) {
                n = MongoDB.fromDBObject(NodeContent.class, iobj);
                n.enhance();
                Cache.add("node_" + id, n);
            }
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    private static NodeContent loadByDfs(ObjectId id) {
        NodeContent n = null;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("dfs",id));
            if (iobj !=  null)
                n = MongoDB.fromDBObject(NodeContent.class, iobj);
        } catch (Exception ex) {
            Logger.info("load node");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return n;
    }

    // apply permissions
    public static List<NodeContent> load(List<ObjectId> nodeIds, User user) {
        // TODO smart caching?
        List<NodeContent> res2 = Lists.newLinkedList();
        /*
        for (ObjectId nodeId : checkNotNull(nodeIds)) {
            NodeContent cachedNode = loadCachedOnly(nodeId);
        }
        */
        List<NodeContent> res  = MongoDB.loadIds(nodeIds, MongoDB.CNode, MongoDB.getSelf().toNodeContent());
        ObjectId uid = user.getId();
        for (NodeContent node : res)
            if (node.canRead(uid))
                res2.add(node);
        return res2;
    }

    private static List<NodeContent> load(List<ObjectId> nodeIds) {
        return MongoDB.loadIds(nodeIds, MongoDB.CNode, MongoDB.getSelf().toNodeContent());
    }

    private static List<NodeContent> loadByPut(ObjectId putId) {
        return MongoDB.transform(dbcol.find(new BasicDBObject("putId", putId)), MongoDB.getSelf().toNodeContent());
    }

    private static Map<ObjectId,NodeContent> loadByPar(ObjectId parId) {
        Map<ObjectId,NodeContent> nodes = Maps.newHashMap();
        try {
            DBCursor iobj = dbcol.find(new BasicDBObject("par", parId));
            for (NodeContent node : MongoDB.transform(iobj,
                        MongoDB.getSelf().toNodeContent()))
                    nodes.put(node.getId(), node);
        } catch (Exception ex) {
            Logger.info("load nodes::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return nodes;
    }

    public boolean canRead(ObjectId uid) {
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
    public boolean canWrite(ObjectId uid) {
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
    public boolean canEdit(ObjectId uid) {
        return owner.equals(uid) || ACL.MASTER == acl.get(uid);
    }

    public boolean canEdit(String uid) {
        return canEdit(toId(uid));
    }

    // moze presuvat objekty? tj menit parenta ersp. childy
    // TODO is this right?
    public boolean canMove(ObjectId uid) {
        ACL ur = acl.get(uid);
        return owner.equals(uid) || ACL.MASTER == ur || ACL.HMASTER == ur;
    }

    public void fook(ObjectId uid) {
        if (getFook() == null) {
            fook = new HashMap<String,Boolean>();
        } else if (fook.containsKey(uid.toString())) {
            return;
        }
        fook.put(uid.toString(),Boolean.TRUE);
        update();
    }

    public void unfook(ObjectId uid) {
        if (fook == null) {
            return;
        } else if (fook.containsKey(uid.toString())) {
            fook.remove(uid.toString());
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
        putNode = putNode.insert();
        pid = putNode.id;
        if (parent.dfs == null) {
            parent.dfs = pid;
            putNode.dfs = parent.getId();
        } else {
            putNode.dfs = parent.dfs;
        }
        putNode.update(true);
        parent.update(true);
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

    public void unputNode() {
        if (putId != null) {
            // a putNode has no children - just remove from dfs
            if (dfs != null) {
                NodeContent dfsNode = load(dfs);
                // if we have dfs-out, we have a dfs-in too -
                NodeContent dfsSource = loadByDfs(id);
                if (dfsNode != null && dfsSource != null) {
                    dfsSource.dfs = dfs;
                    dfsSource.update(true);
                }
            }
            delete();
        }
    }

    public static String addNode(ObjectId parent,
            Map<String,String> params,
            ObjectId ownerid)
    {
        // TODO check & set permissions
        NodeContent root = null;
        if (parent != null) {
            root = NodeContent.load(parent);
            if (root == null || root.isPut()) { // &&  check root type too
                return null;
            } else {
                if (!root.canWrite(ownerid)) {
                    // no permissions to add Node here
                    return null;
                }
            }
        }
        List<ObjectId> roots = new LinkedList<ObjectId>();
        ObjectId parOwner = null;
        NodeContent newnode = new NodeContent(ownerid,params).insert();
        ObjectId mongoId = newnode.getId();
        if (mongoId == null)
        {
            // validation errors...
            return null;
        }
        if (root != null) {
            ObjectId dfs;
            newnode.par        = parent;
            // TODO load accType from params if set, override the inherited one
            newnode.accessType = root.accessType;
            Logger.info("parent loaded :: " + root.dfs);
            dfs = root.dfs;
            root.dfs = mongoId;
            root.update();
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
        } else {
            newnode.accessType = NodeContent.PUBLIC;
            // newnode.typ = NodeContent.CONTENT;
        }
        
        newnode.update(true);
        Activity.newNodeActivity(mongoId, newnode, roots, ownerid, parOwner);
        return mongoId.toString();
    }

    public void deleteNode() {
        NodeContent dfsNode = null;
        NodeContent dfsSource = null;
        if (isPut())
            return;
        if (dfs != null) {
            dfsNode = load(dfs);
            // if we have dfs-out, we have a dfs-in too -
            dfsSource = loadByDfs(id);
        }
        for (NodeContent putNode : checkNotNull(loadByPut(id)))
                putNode.unputNode();
        if (dfsNode != null) {
            // dfsSource.dfs = dfs; // !!! unless this is our child
            // we have to unlink all direct children, making them orphans
            Map<ObjectId,NodeContent> children = loadByPar(id);
            if (children == null || children.isEmpty()) {
                dfsSource.dfs = dfs;
                dfsSource.update(true);
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
                            dfsSource.update(true);
                        }
                        // create a subtree
                        lastOne.dfs = child.getId();
                        lastOne.update(true);
                    }
                    if (children.containsKey(child.dfs)) {
                        child.dfs = null;
                    }  else {
                        // same as above
                        dfsSource.dfs = child.dfs;
                        dfsSource.update(true);
                        child.dfs = null; // sure?
                    }
                    child.par = null;
                    child.update(true);
                }
            }
        }

        // + rm bookmarks, perhaps activities too
        delete();
    }

    // + add activity to new place, remove from old one
    public void moveNode(ObjectId to, User user) {
        NodeContent toNode = load(to);
        if (toNode == null || !toNode.canMove(user.getId()) || !toNode.isPut())
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
                        dfsSource.update(true);
                    }
                } else { // no children
                    dfsSource.dfs = dfs;
                    dfsSource.update(true);
                }
            }
        }
        // 2. - move to the new one - added to the top, not sorted by time!
        if (lastOne == null) 
            dfs = toNode.dfs;
        else {
            lastOne.dfs = toNode.dfs;
            lastOne.update(true);
        }
        toNode.dfs = id;
        toNode.update(true);
        par = to;
        update(true);
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
            DBCursor iobj = dbcol.find(query);
            nodes = MongoDB.transform(iobj, MongoDB.getSelf().toNodeContent());
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
        if (user.getDailyK() < 1)
            return;
        if (kgivers == null)
            kgivers = new LinkedList<ObjectId>();
        else if (kgivers.contains(user.getId()))
            return;
        kgivers.add(user.getId());
        if (getK() == null)
            setK(1l);
        else 
            setK(getK() + 1);
        user.setDailyK(user.getDailyK() - 1);
        user.update(true);
        update(true);
    }

    // -K
    public void giveMK(User user)
    {
        if (user.getDailyK() < 1)
            return;
        if (kgivers == null)
            kgivers = new LinkedList<ObjectId>();
        else if (kgivers.contains(user.getId()))
            return;
        kgivers.add(user.getId());
        if (getMk() == null)
            setMk(1l);
        else
            setMk(getMk() + 1);
        user.setDailyK(user.getDailyK() - 1);
        user.update(true);
        update(true);
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
    protected Map<String, Boolean> getFook() {
        return fook;
    }

    public boolean isFook(User user) {
        return fook != null && fook.containsKey(user.getIdString()) ? Boolean.TRUE : Boolean.FALSE;
    }

    // Node o 1 vyssie nad nami uz urcite ma vsetko poriesene
    public void loadRights()
    {
        // ImmutableMap.Builder allows duplicate keys but cannot handle them
        // stupid cocks
        Map<ObjectId,ACL> bu = Maps.newHashMap();
        if (accessType == null)
            accessType = 0;
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
            NodeContent parent = checkNotNull(load(par));
            if (parent.fook != null) {
                if (fook == null) {
                    fook = new HashMap<String,Boolean>();
                }
                fook.putAll(parent.fook);
            }
            if (parent.acl == null) {
                parent.loadRights();
            } 
            for (Map.Entry<ObjectId,ACL> e : checkNotNull(parent.acl).entrySet()) {
                switch (e.getValue()) {
                    case ACCESS:  bu.put(e.getKey(), ACL.ACCESS);  break;
                    case SILENCE: bu.put(e.getKey(), ACL.SILENCE); break;
                    case BAN:     bu.put(e.getKey(), ACL.BAN);     break;
                    case MASTER:  bu.put(e.getKey(), ACL.HMASTER); break;
                    case OWNER:   bu.put(e.getKey(), ACL.HMASTER); break;
                }
            }
        }
        bu.put(owner, ACL.OWNER);
        acl = ImmutableMap.copyOf(bu);
    }

    public boolean isPut() {
        return putId == null ? false : true;
    }

    // unsafe, should check permissions
    public NodeContent getPutNode() {
        return load(checkNotNull(putId));
    }

    @Override
    public NodeContent enhance() {
        ownerName = User.getNameForId(owner);
        if (par != null )
            parName  = load(par).name; // checkNotNull?
        else
            parName  = "";
        loadRights();
        return this;
    }

    @Override
    public DBCollection getCollection() {
        return dbcol;
    }

    @Override
    public String key() {
        return key;
    }

    /**
     * @param template the template to set
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    public enum Type {
        NODE,
        FORUM,
        CATEGORY,
        USER,
        FRIEND // ?
    };

}
