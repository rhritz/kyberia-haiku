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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import play.Logger;
import plugins.*;

public class Haiku {
    
    private GraphDatabaseService graph;
    public static final String LASTID   = "lastid";
    // node property / relation names
    public static final String CONTENT  = "content";
    public static final String CREATED  = "created";
    public static final String TEMPLATE = "template";
    public static final String USERNAME = "username";
    public static final String GROUPNAME = "username";
    public static final String NAME     = "name";
    public static final String TYPE     = "type";
    public static final String PASSWORD = "password";
    public static final String IS_SOURCE   = "is_source";
    public static final String IS_SINK     = "is_sink";
    public static final String ACCESS_TYPE = "access_type";
    public static final String MONGOID  = "mongoid";


    public Haiku()
    {
        graph = Neo.getGraph();
    }

    // ano, treba osetrit chyby
    public static Rel toRel(String name)
    {
        return Rel.valueOf(name);
    }
    
    // TODO dynamicke relations -  DynamicRelationshipType.withName("KNOWS")'
    public static enum Rel implements RelationshipType
    {
        BOOK,
        DFS,
        FOOK,
        FRIEND,
        IGNORE,
        K,
        OWNER,
        REACTION,
        TAG,
        GROUP_OWNER,
        GROUP_MEMBER,
        // perm related:
        ACCESS,
        BAN,
        SILENCE
    }
    // permission rels: MASTER, OP - mozu tiez ukazovta na grupu
    // pri vytvoreni noveho prispevku sa by default vytvori linka na usergrupy
    // parenta
    // potom pri zmene prav 'podriadeneho' prispevku
    // sa len skopiruje obsah grupy a prida/odoberie novy obsah z nej
    // - alebo dedenie grup, ale to je na dlhsie

    public static enum NodeType
    {
        USER,
        CONTENT,
        USERGROUP,
        BOOKMARK,
        SPECIAL
    }

    public static enum AccessType
    {
        PUBLIC,
        MODERATED,
        PRIVATE
    }

    // params su simple, pozor na viacnasobne
    public long addAnyNode( NodeType typ,
                            Map <String,String> params,
                            long parent,
                            long ownergid,
                            String ownerid)
    {
        long id = -1;
        switch(typ) {
            case USER: 
                id = addUser(params);
                break;
            case CONTENT:
                id = addNode(parent, params, ownergid, ownerid);
                break;
            case USERGROUP:
                id = addUsergroup(params);
                break;
        }
        return id;
    }

    public String viewUser(long id)
    {
        String user = "";
        Transaction tx = graph.beginTx();
        try {
            Node usernode = graph.getNodeById(id);
            // este snad aj skontrolovat ci je to naozaj user
            if (usernode.hasProperty(Haiku.USERNAME))
            {
                user = (String) usernode.getProperty(Haiku.USERNAME);
            }
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return user;
    }

    public long addUser(Map<String,String> params)
    {
        Long id = -1l;
        String username = params.get(USERNAME);
        String password = params.get(PASSWORD);
        if (!User.usernameAvailable(username))
        {
            // TODO nejaka kulturna chybova hlaska
            return -1;
        }

        Transaction tx = graph.beginTx();
        try {
            Node user = graph.createNode();
            user.setProperty(TYPE,NodeType.USER.ordinal());
            user.setProperty(USERNAME, username);
            id = user.getId();
            User u = new User(username, password, id);
            u.save();
            tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return id;
    }

    public long addUsergroup(Map<String,String> params)
    {
        long id = -1;
        Transaction tx = graph.beginTx();
        try {
            Node group = graph.createNode();
            group.setProperty(GROUPNAME, params.get(GROUPNAME));
            group.setProperty(TYPE,NodeType.USERGROUP.ordinal());
            Node creator = graph.getNodeById(Long.parseLong(params.get("creator")));
            creator.createRelationshipTo(group, Rel.GROUP_OWNER);
            id = group.getId();
            tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return id;
    }

    // pridaj len ak tam uz nie je
    public void addUserToGroup(long groupid, long userid)
    {
        Transaction tx = graph.beginTx();
        try {
            Node group = graph.getNodeById(groupid);
            Node user  = graph.getNodeById(userid);
            group.createRelationshipTo(user, Rel.GROUP_MEMBER);
            tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
    }

    // hlavne vylistuj zoznam ludi v tejto grupe
    // + pripadne z ktorych nod je linkovana a editacia tychto liniek
    // + add user, remove user
    public String viewUsergroup(long groupid)
    {
        return "";
    }


    // pre rekonstrukciu grafovej databazy, bez notifikacii
    public long addNodeFromMongo(Long ownergid, String mongoid, Long parent)
    {
        long retid = 0;
        Transaction tx = graph.beginTx();

        try {
            List<String> roots = new LinkedList<String>();
            Node newnode = graph.createNode();
            newnode.setProperty(TYPE,NodeType.CONTENT.ordinal());
            if (parent != null) {
                Node sibling = null;
                Relationship dfs;
                Node root = graph.getNodeById(parent);
                if (root != null) {
                    root.createRelationshipTo(newnode, Rel.REACTION);
                    dfs = root.getSingleRelationship(Rel.DFS, Direction.OUTGOING);
                    if (dfs != null ) {
                        sibling = dfs.getEndNode();
                        dfs.delete();
                    }
                    root.createRelationshipTo(newnode, Rel.DFS);
                    if (sibling != null) {
                        newnode.createRelationshipTo(sibling, Rel.DFS);
                    } else {
                        newnode.createRelationshipTo(root, Rel.DFS);
                    }
                    // nizsie su updaty notifikacii
                    Node upnode = root;
                    for (int i = 0; i < 10; i++)
                        // 10 - max hier depth for notif update
                    {
                          if (upnode.hasProperty(MONGOID)) {
                            roots.add((String) upnode.getProperty(MONGOID));
                          }
                          Relationship re = upnode.getSingleRelationship(
                                  Rel.REACTION, Direction.INCOMING);
                          if (re == null) break;
                          upnode = re.getStartNode();
                    }
                }
            }
            if (ownergid > 0) {
                Node owner = graph.getNodeById(ownergid);
                if (owner != null) { // inak by sme mali failnut
                    owner.createRelationshipTo(newnode, Rel.OWNER);
                }
            }
            retid = newnode.getId();
            newnode.setProperty(MONGOID, mongoid);
            tx.success();
        }
        catch(Exception e)
        {
           Logger.info("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return retid;
    }

    public long addNode(Long parent,
            Map<String,String> params,
            long ownergid,
            String ownerid)
    {
        long retid = 0;
        Transaction tx = graph.beginTx();
        
        // TODO validate content & name -> antisamy?
        // TODO podstatna vec co tu potrebujeme je isPut a pripadne permissions
        try {
            List<String> roots = new LinkedList<String>();
            String parOwner = null;
            Node newnode = graph.createNode();
            newnode.setProperty(TYPE,NodeType.CONTENT.ordinal());
            if (parent != null) {
                Node sibling = null;
                Relationship dfs;
                Node root = graph.getNodeById(parent);
                if (root != null) {
                    root.createRelationshipTo(newnode, Rel.REACTION);
                    dfs = root.getSingleRelationship(Rel.DFS, Direction.OUTGOING);
                    if (dfs != null ) {
                        sibling = dfs.getEndNode();
                        dfs.delete();
                    }
                    root.createRelationshipTo(newnode, Rel.DFS);
                    if (sibling != null) {
                        newnode.createRelationshipTo(sibling, Rel.DFS);
                    } else {
                        newnode.createRelationshipTo(root, Rel.DFS);
                    }
                    // nizsie su updaty notifikacii
                    Node upnode = root;
                    for (int i = 0; i < 10; i++) 
                        // 10 - max hier depth for notif update
                    {
                          if (upnode.hasProperty(MONGOID)) {
                            roots.add((String) upnode.getProperty(MONGOID));
                          }
                          Relationship re = upnode.getSingleRelationship(
                                  Rel.REACTION, Direction.INCOMING);
                          if (re == null) break;
                          upnode = re.getStartNode();
                    }
                    Relationship ownage = root.getSingleRelationship(Rel.OWNER,
                            Direction.INCOMING);
                    if (ownage != null) {
                        parOwner = new Long(ownage.getStartNode().
                            getId()).toString();
                    }
                }
            }
            if (ownergid > 0) {
                Node owner = graph.getNodeById(ownergid);
                if (owner != null) { // inak by sme mali failnut
                    owner.createRelationshipTo(newnode, Rel.OWNER);
                }
            }
            NodeContent nc = new NodeContent(newnode,ownerid,params,roots);
            String mongoId = nc.save();
            Logger.info("Mongoloid::" + mongoId);
            if (mongoId == null)
            {
                throw new Exception();
            }
            Activity.newNodeActivity(mongoId, nc, roots, ownerid, parOwner);
            retid = newnode.getId();
            newnode.setProperty(MONGOID, mongoId);
            tx.success();
        }
        catch(Exception e)
        {
           Logger.info("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return retid;
    }

    // TODO variant s id/casom poslednej nody ktoru chceme zobrazit
    public List<NodeContent> getThreadedChildren(
                                    long rootid,
                                    int start,
                                    int count)
    {
        List<NodeContent> thread    = new ArrayList<NodeContent>(count);
        Map<Long,Integer> roots = new HashMap();
        int depth               = 0;
        Relationship dfs, parent;
        Node lastnode, nextnode;
        Transaction tx          = graph.beginTx();
        try
        {
          nextnode = graph.getNodeById(rootid);
          if (nextnode == null) {
              return thread;
          }
          for (int i = 0; i < start + count; i++)
          {
               lastnode = nextnode;
               dfs = lastnode.getSingleRelationship(Rel.DFS,
                       Direction.OUTGOING);
               if (dfs == null) {
                   return thread; // chyba - nema dfs, nie je co zobrazovat
               }
               nextnode = dfs.getEndNode();
               parent = nextnode.getSingleRelationship(Rel.REACTION,
                       Direction.INCOMING);
               if (parent == null) {
                   return thread; // chyba - ma dfs ale nema parenta
               }
               if (parent.getStartNode().equals(lastnode)) {
                   roots.put(lastnode.getId(), depth);
                   depth++;
               } else {
                   long parid = parent.getStartNode().getId();
                   // ak v tomto bode nema parenta v roots,
                   // znamena to ze siahame vyssie ako rootid - koncime
                   if (roots.get(parid) == null) {
                       return thread;
                   }
                   // nasli sme parenta, sme o jedno hlbsie ako on
                   depth = roots.get(parid) + 1;
               }
               if (i >= start) {
                   // tento node chceme zobrazit
                   // tu je vhodne miesto na kontrolu per-node permissions,
                   // ignore a fook zalezitosti
                   // if (! user.ignores(User.getIdByGid(nextnode.owner))) {
                   thread.add( NodeContent.
                            load((String) nextnode.
                                getProperty(Haiku.MONGOID), depth));
                   // } // inak pridaj len ciastocne
               }
          }
          tx.success();
        }
        catch(Exception e)
        {
           Logger.info("Neo failed at id "+ rootid + " " + e.toString() );
           // e.printStackTrace();
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return thread;
    }


    // TODO clearDB + loadDBFromMongo
    public void delAllNodes()
    {
        Transaction tx = graph.beginTx();
        try {
           long refid = graph.getReferenceNode().getId();
           for (Node node: graph.getAllNodes()) {
               if (node.getId() != refid) {
                   for (Relationship rel : node.getRelationships()) {
                       rel.delete();
                   }
                   node.delete();
               }
           }
           tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
    }


    // toto by sa v pdostate nemalo pouzivat okrem debugu
    public String viewNode(long id)
    {
        String ret = "<table>";
        Transaction tx = graph.beginTx();
        try {
           Node node = graph.getNodeById(id);
           // no, vlastne by sme mali vratit nejaky zbastardeny NodeContent a spracovat ho v template
           // ale na debug to bude zatial stacit
           ret += " <tr><td> ID: " + node.getId() + "</td></tr>";
           for ( String key : node.getPropertyKeys() ) {
               ret += " <tr><td> " + key + ": " + node.getProperty(key) + "</td></tr>";
           }
           for ( Relationship rel : node.getRelationships(Direction.OUTGOING)) {
               ret += " <tr><td> " + rel.getType().name() + 
                       " to <a href=\"/id/" +  rel.getEndNode().getId() + "\">"
                       + rel.getEndNode().getId() + "</a></td></tr>";
           }
           for ( Relationship rel : node.getRelationships(Direction.INCOMING)) {
               ret += " <tr><td> " + rel.getType().name() + 
                       " from <a href=\"/id/" + rel.getStartNode().getId() +
                       "\">" + rel.getStartNode().getId() + "</a></td></tr>";
           }
           tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed at id "+ id + " " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        ret += "</table>";
        return ret;
    }


    public void moveNode(long id, long new_parent)
    {
        // removeNode(id) + addNode(id,parent)
        // akurat ze z remove pouzijeme len detach relationships
    }

    public void removeNode(NodeContent nc)
    {
        Transaction tx = graph.beginTx();
        try {
           // Node node = indexService.getSingleNode( ID,id);
           Node node = graph.getNodeById(nc.gid);
           // ma bud obe alebo ani jedno alebo je nieco zle
           Relationship dfs_in = node.getSingleRelationship(Rel.DFS,
                   Direction.INCOMING);
           Relationship dfs_out = node.getSingleRelationship(Rel.DFS,
                   Direction.OUTGOING);
           if (dfs_in != null && dfs_out != null) {
               if (dfs_in == null) {
                   dfs_out.delete();
               } else if (dfs_out == null) {
                   dfs_in.delete();
               } else {
                   Node prev = dfs_in.getStartNode();
                   Node next = dfs_out.getEndNode();
                   // dfs_in.delete();
                   // dfs_out.delete();
                   // if (! (prev.getId() == next.getId()) ) {
                   //     prev.createRelationshipTo(next, Rel.DFS);
                   // }
                   //
                   Node dfs_prev = dfs_in.getStartNode();

                   Node dfs_after = null;
                   // TODO - kazdy REACTION child tohto node bude mat nastaveny
                   // parent na null aj v mongodb
                   // + ideme 'dole' po dfs a ked narazime na node 'nad nami' alebo 'za nami' (tj nie v nasom pdostrome) (tak ako v getThreadedChildren),
                   // tak ten spojime s nasim parentom
                   // - v nasledujucom myslime pod 'parentom' nde ktory ideme mazat
                   // - pozor na koncove nodes, ci su spracovane korektne
                   /////////////////////////////////////////////////////////////////////
                    Map<Long,Integer> roots = new HashMap();
                    Relationship dfs, parent = null;
                    Node lastnode, nextnode;
                    nextnode = node;
                    for (int i = 0;; i++)
                    {
                       lastnode = nextnode;
                       dfs = lastnode.getSingleRelationship(Rel.DFS,
                               Direction.OUTGOING);
                       if (dfs == null) {
                           break;
                       }
                       nextnode = dfs.getEndNode();
                       parent = nextnode.getSingleRelationship(Rel.REACTION,
                               Direction.INCOMING);
                       if (parent == null) {
                           break; // chyba - ma dfs ale nema parenta
                       }
                       if (parent.getStartNode().equals(lastnode)) {
                           roots.put(lastnode.getId(), 1);
                       } else {
                           long parid = parent.getStartNode().getId();
                           // ak v tomto bode nema parenta v roots,
                           // znamena to ze siahame vyssie ako rootid
                           // - mame co sme chceli - Node dfs_parent = parent.getStartNode();
                           if (roots.get(parid) == null) {
                               dfs_after = parent.getStartNode();
                               break;

                           }
                           // nasli sme parenta, sme o jedno hlbsie ako on
                       }
                       // nextnode

                    }
                    // toto nam vyriesi vonkajsiu cast stromu
                    if (dfs_after != null) {
                        dfs_in.delete();
                        parent.delete();
                        dfs_prev.createRelationshipTo(dfs_after, Rel.DFS);
                    }
                    // este musime upravit nas podstrom, aby nam nezostali odtrhnute dfs
                    // na zaciatku a konci
                    // zaciatok sme my a koniec je teraz uloceny v nextnode
                    if (nextnode != null && !nextnode.equals(node)) {
                        // uhm lenze nam sa to teraz moze rozpadnut na X podstromov
                        // takze musime prejst vsetky REACTION (ako vyssie si mozeme
                        // pozriet vsetkych ktorym sme parentom prave my)
                        // a urobit z nich samostatne stromy
                    }
                   /////////////////////////////////////////////////////////////////////
                }
           }
           for ( Relationship rel : node.getRelationships(Direction.BOTH)) {
               rel.delete();
           }
           node.delete();
           nc.delete(); // TODO - samozrejme podla typu, len ak je to content
           tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed at id "+ nc.gid + " " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
    }

    // TODO exclusive == true - neprida, ak takto relation uz existuje medzi nimi
    public void addRelation(long from,
                            long to,
                            boolean exclusive,
                            String relation) 
    {
        Transaction tx = graph.beginTx();
        try {
           Node node_from = graph.getNodeById(from);
           Node node_to   = graph.getNodeById(to);
           node_from.createRelationshipTo(node_to, toRel(relation));
           tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed at id "+ from + " " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
    }


    
    public void removeRelation(long from, long to, String relation)
    {
        Transaction tx = graph.beginTx();
        try {
           Node node_from = graph.getNodeById(from);
           for (Relationship rel :
               node_from.getRelationships(toRel(relation), Direction.OUTGOING))
           {
               if (rel.getEndNode().getId() == to)
               {
                   rel.delete();
                   break;
               }
           }
           tx.success();
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed at id "+ from + " " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
    }


    // currently checks for read perms only
    // should return perm type in the future
    public boolean checkPermissions(long nodeid, long userid)
    {
        Transaction tx = graph.beginTx();
        try {
            // TODO check node is a content-node
            // TODO masters, ops etc at a later stage
            Node node = graph.getNodeById(nodeid);
            Node user = graph.getNodeById(userid);
            // TODO check if he is the owner - return true;
            Integer acc_type = (Integer) node.getProperty(ACCESS_TYPE);
            AccessType accessType = AccessType.values()[acc_type]; //au
            switch (accessType) {
                case PUBLIC:
                    // look for bans & silences only
                    return true;
                case MODERATED:
                    // bans & silence & access
                    return true;
                case PRIVATE:
                    // access only
                    Relationship rel = node.getSingleRelationship(Rel.ACCESS,
                            Direction.OUTGOING);
                    if (rel == null) {
                        return false;
                    }
                    Node accGroup = rel.getEndNode();
                    // toto je WTFuckup...
                    for (Relationship r :
                        accGroup.getRelationships(Rel.GROUP_MEMBER,
                        Direction.OUTGOING))
                    {
                        Node n = r.getEndNode();
                        if (n.getId() == userid )
                        {
                            return true;
                        }
                    }
                    return false;
            }
        }
        catch(Exception e)
        {
           Logger.trace("Neo failed at id "+ nodeid + " " + e.toString() );
           tx.failure();
        }
        finally
        {
           tx.finish();
        }
        return false;
    }

}

