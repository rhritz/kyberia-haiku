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
package controllers;

import com.mongodb.ObjectId;
import java.io.File;
import play.mvc.*;
import play.Logger;
import models.*;
import play.mvc.Controller;

// find . -name "*java" -exec cat '{}' ';' | wc -l
// find . -name "*java" -exec grep 'TODO' '{}' ';' -print
// JDK_HOME=/usr/lib/jvm/java-6-sun-1.6.0.20/ ./idea.sh

@With(Secure.class)
public class Application extends Controller {

    @Before
    static void setConnectedUser() {
        renderArgs.put("reqstart", System.currentTimeMillis());
        if(Security.isConnected()) {
            String uid = session.get(User.ID);
            renderArgs.put("user",   session.get(User.USERNAME));
            renderArgs.put("userid", uid);
            // Logger.info("ahojky " + uid);
            // UserLocation.saveVisit(uid, "some location");
            // - toto enjak doriesit s bookmark visits a tak
            renderArgs.put("newMail", 
                    MessageThread.getUnreadMailNotif(new ObjectId(session.get(User.ID))));
        } else {
            // to este neznamena ze sme uplne mimo, snad
            // moze Secure vytvorit nejaku fake session pre
            // nrephlasenych (?)
        }
        // session.put("view", ViewTemplate.getView(uid,req,..?));
    }

    @After
    static void measureTime()
    {
        Long start    = renderArgs.get("reqstart", Long.class);
        Long duration = System.currentTimeMillis() - start;
        // uhm toto nam nepomoze kedze je to az po uplnom ukonceni
        // renderArgs.put("reqdur", duration.toString());
        Logger.info("Request duration " + duration);
    }

    public static void index() {
        Page main = Page.getByName(Page.MAIN, params, renderArgs, request,
                session);
        renderArgs.put(ViewTemplate.TOP_LEVEL_TEMPLATE, "main.html"); // main.getToplevelTemplate? alebo skor z viewu -> main.view.getToplevelTemplate
        render(main.getTemplate());
    }

    public static void viewNode(String id) {
        displayNode(id);
    }

    
    public static void viewUserGroup(Long id) {
        render(id);
    }

     public static void addNode(String id, String content) {
        Logger.info("about to add node:" + id + "," + content );
        // checkAuthenticity();
        NodeContent parentNode = NodeContent.load(id);
        String newId = NodeContent.addNode(id == null ? null : new ObjectId(id),
                    Controller.params.allSimple(),
                    new ObjectId(session.get(User.ID))
                );
        String nid = null;
        Logger.info("newid::" + newId);
        if (id == null) {
            nid = NodeContent.load(newId).getIdString();
        } else {
            nid = id;
        }
        displayNode(nid);
    }


    //
    // akcie
    //
    public static void friend(String uid)
    {
        checkAuthenticity();
        Logger.info("Add friend :: " + uid);
        User u = User.load(session.get(User.ID));
        u.addFriend(new ObjectId(uid));
        showUser(uid);
    }

    public static void ignore(String uid)
    {
        checkAuthenticity();
        Logger.info("Add ignore :: " + uid);
        User u = User.load(session.get(User.ID));
        u.addIgnore(new ObjectId(uid));
        showUser(uid);
    }

    public static void ignoreMail(String uid)
    {
        checkAuthenticity();
        Logger.info("Add ignoreMail :: " + uid);
        User u = User.load(session.get(User.ID));
        u.addIgnoreMail(new ObjectId(uid));
        showUser(uid);
    }

    public static void editNode(String id)
    {
        checkAuthenticity();
        Logger.info("Show edit node:: " + id);
        NodeContent node = NodeContent.load(id);
        if (node.canEdit(new ObjectId(session.get(User.ID)))) {
            node.edit(Controller.params.allSimple());
        }
        displayNode(id);
    }

    public static void book(String id)
    {
        checkAuthenticity();
        Logger.info("Bookmark action:: " + id);
        Bookmark.add(id, session.get(User.ID));
        displayNode(id);
    }

    public static void unbook(String id)
    {
        checkAuthenticity();
        Logger.info("UnBook:: " + id);
        Bookmark.delete(id, session.get(User.ID));
        displayNode(id); // mh, toto nemusi byt volane z id/...
    }

    public static void fook(String id)
    {
        checkAuthenticity();
        Logger.info("Fook ::" + id);
        NodeContent n = NodeContent.load(id);
        n.fook(new ObjectId(session.get(User.ID)));
        displayNode(id);
    }

    public static void k(String id)
    {
        checkAuthenticity();
        Logger.info("K ::" + id);
        NodeContent nc = NodeContent.load(id);
        if (nc != null)
        {
            Nodelist.giveK(nc, session.get(User.ID));
        }
        displayNode(id);
    }

    public static void mk(String id)
    {
        checkAuthenticity();
        Logger.info("K ::" + id);
        NodeContent nc = NodeContent.load(id);
        if (nc != null)
        {
            Nodelist.giveMK(nc, session.get(User.ID));
        }
        displayNode(id);
    }

    public static void tag(String id, String tag)
    {
        checkAuthenticity();
        Logger.info("Tag id ::" + id + " with tag ::" + tag);
        NodeContent nc = NodeContent.load(id);
        if (nc != null)
        {
            Tag.tagNode(nc,tag,session.get(User.ID));
        }
        displayNode(id);
    }

    // interne, po akcii
    private static void displayNode(String id)
    {
        int start = 0;
        int count = 30;
        ObjectId oid = new ObjectId(id);
        NodeContent node = NodeContent.load(oid);
        String uid = session.get(User.ID);
        renderArgs.put("uid", uid);
        if (node != null && node.canRead(new ObjectId(uid))) {
            // NodeContent.getThreadedChildren(oid, start, count);
            // logicky UserVisit save patri sem, lebo tu vieme co ideme zobrazit
            UserLocation.saveVisit(User.load(uid), id);
            renderArgs.put("node", node);
            renderArgs.put("thread",
                    // NodeContent.getThreadedChildren(oid, start, count));
                    node.getThreadIntern(start, count));
            renderArgs.put("id", id);
        } else {
            renderArgs.put("id", id);
            renderArgs.put("content", "Sorry pal, no see for you");
        }
        render(ViewTemplate.VIEW_NODE_HTML);
        // podl anode nastavit template
        // String template = .getTemplate(node,user,session.viewtemplate)
        // render(template,id);
        // alebo skor render
    }

    public static void showEditNode(String id) {
        String uid = session.get(User.ID);
        NodeContent node = NodeContent.load(id);
        if (node != null) {
            if (node.canEdit(new ObjectId(uid))) {
                renderArgs.put("id", id);
                renderArgs.put("node", node);
                renderArgs.put("users", User.loadUsers(null, 0 , 30, null));
                render(ViewTemplate.EDIT_NODE_HTML);
            } else {
                renderArgs.put("error", "Sorry pal, no edit for you");
                displayNode(id);
            }
        }
    }

    public static void showNodes() {
        renderArgs.put("content", "Neviem co sem napisat");
        render(ViewTemplate.SHOW_NODES_HTML);
    }

    public static void showBookmarks() {
        renderArgs.put("bookmarks",
                Bookmark.getUserBookmarks(session.get(User.ID)));
        render(ViewTemplate.SHOW_BOOKMARKS_HTML);
    }

    public static void showMail(String thread) {
        String uid = session.get(User.ID);
        renderArgs.put("threads",
                MessageThread.viewUserThreads(new ObjectId(uid),session));
        renderArgs.put("users", User.loadUsers(null, 0 , 30, null));
        if (thread == null)
        {
            thread = session.get("LastThreadId");
        }
        if (thread != null) {
            ObjectId threadId = new ObjectId(thread);
            renderArgs.put("mailMessages",
                Message.getMessages(threadId, new ObjectId(uid)));
        }
        render(ViewTemplate.MAIL_HTML);
    }

    public static void sendMail(String to, String content) {
        checkAuthenticity();
        //String toId   = User.getIdForName(to);
        String fromId = session.get(User.ID);
        // TODO filter content
        Message.send(fromId, to, content);
        renderArgs.put("users", User.loadUsers(null, 0 , 30, null));
        renderArgs.put("threads",
                MessageThread.viewUserThreads(new ObjectId(session.get(User.ID)),
                    session));
        String threadStr = session.get("LastThreadId");
        if (threadStr != null) {
            renderArgs.put("mailMessages",
                Message.getMessages(new ObjectId(session.get("LastThreadId")),
                    new ObjectId(fromId)));
        }
        render(ViewTemplate.MAIL_HTML);
    }

    public static void showLastNodes() {
        renderArgs.put("nodes", Nodelist.getLastNodes(30));
        render(ViewTemplate.SHOW_LAST_HTML);
    }

    public static void showFriendsContent() {
        renderArgs.put("nodes",
                Activity.showFriendsContent(session.get(User.ID)));
        render(ViewTemplate.SHOW_LAST_HTML);
    }

    public static void showFriends(String uid) {
        // String myId = session.get(User.ID);
        User u = User.load(uid);
        renderArgs.put("friends", u.listFriends());
        renderArgs.put("uid", uid);
        render(ViewTemplate.SHOW_FRIENDS_HTML);
    }

    public static void showMe() {
        String myId = session.get(User.ID);
        User me = User.load(myId);
        Logger.info("showMe:: " + myId);
        renderArgs.put("nodes", Nodelist.getUserNodes(myId,null));
        renderArgs.put("friends", me.listFriends());
        renderArgs.put("uid", myId);
        render(ViewTemplate.SHOW_ME_HTML);
    }

    public static void showUser(String id) {
        User u = User.load(id);
        if ( u !=null ) {
            renderArgs.put("uid", u.getId());
            // renderArgs.put("user", user);
            renderArgs.put("nodes", Nodelist.getUserNodes(id,null));
            render(ViewTemplate.SHOW_USER_HTML);
        }
        // inak co?
    }

    public static void showK() {
        renderArgs.put("nodes", Nodelist.getKlist(100));
        render(ViewTemplate.SHOW_K_HTML);
    }

    public static void showTag(String tag) {
        renderArgs.put("taglist",  Tag.getTaggedNodes(tag)); // tymto tagom otagovane nody zoradene nejak
        renderArgs.put("tagcloud", Tag.getTagCloud(tag)); // pribuzne tagy
        renderArgs.put("taggers", Tag.getTaggers(tag)); // ti co najcastejsie pozuvaju tento tag
        render(ViewTemplate.SHOW_K_HTML);
    }

    public static void showUsers() {
        // list all users
        // re.append(u.id).append(u.getGid()).append(u.username).append("<br>");
        // renderArgs.put("content", User.listUsers());
        renderArgs.put("users", User.loadUsers(null, 0 , 30, null));
        render(ViewTemplate.SHOW_USERS_HTML);
    }

    public static void showLive() {
        // list all online users and their current/last activities
        renderArgs.put("locations", UserLocation.getAll());
        render(ViewTemplate.SHOW_LIVE_HTML);
    }

    // Set notif. messages
    private static void setMessages()
    {
        // List messages = Cache.get(session.get(User.ID) + "-messages", List.class);
        /*
        if(messages == null) {
            // Cache miss
            messages = Message.findByUser(session.get("user"));
            Cache.set(session.getId() + "-messages", messages, "30mn");
        }
         */
        // Controller.renderArgs.get/put?
        renderArgs.put("messages", Alert.pop(session.get(User.ID)));
    }

    // TODO cesta k obrazkom do konfigu + kontrolovat co sa upladuje
    public static void uploadIcon(String uid, File icon) {
        checkAuthenticity();
        
        String fname = "/code/haiku/public/images/upload/" +
                session.get(User.ID);
        icon.renameTo(new File(fname));
        showMe();
    }

    public static void changePwd(String uid) {
        checkAuthenticity();
        User me = User.load(session.get(User.ID));
        me.changePwd(Controller.params.allSimple());
        showMe();
    }

    // event - zmena property node alebo usera
    public static void event(String id) {
       // vyhryzni event z params, urob akciu, vrat displayNode
    }

    // zobraz nove prispevky pre daneho usera z danej nody
    // - zatial len ako list
    public static void viewNodeUpdates(String id) {
        User u = User.load(session.get(User.ID));
        renderArgs.put("nodes", 
                Bookmark.getUpdatesForBookmark(id, u.getId()));
        // TODo nahradit
        render(ViewTemplate.SHOW_LAST_HTML);
    }

    // Group management
    public static void addGroup() {
        UserGroup g = UserGroup.create(session.get(User.ID), params.get("name"),
                null);
        renderArgs.put("group",  g);
        render(ViewTemplate.SHOW_GROUP_HTML);
    }

    public static void addUserToGroup(String groupId) {
        String uid = params.get("newId");
        UserGroup g = UserGroup.load(groupId);
        ObjectId newUid = new ObjectId(uid);
        if (newUid != null) {
            g.addUser(newUid);
        }
        showGroup(groupId);
    }

    public static void showAddGroup() {
        render(ViewTemplate.ADD_GROUP_HTML);
    }

    public static void showGroups() {
        renderArgs.put("groups",  UserGroup.loadGroups());
        render(ViewTemplate.SHOW_GROUPS_HTML);
    }

    public static void showGroup(String groupId) {
        renderArgs.put("group",  UserGroup.load(groupId));
        render(ViewTemplate.SHOW_GROUP_HTML);
    }

    public static void editGroup(String groupId) {
        UserGroup g = UserGroup.load(groupId);
        g.edit(Controller.params.allSimple());
        renderArgs.put("group",  g);
        render(ViewTemplate.EDIT_GROUP_HTML);
    }


    // Pages
    public static void addPage() {
        Page p = Page.create(params.get("name"), params.get("template"),
                new ObjectId(session.get(User.ID)));
        renderArgs.put("page",  p);
        render(ViewTemplate.SHOW_PAGE_HTML);
    }

    public static void showAddPage() {
        render(ViewTemplate.ADD_PAGE_HTML);
    }

    public static void showPages() {
        renderArgs.put("pages",  Page.loadPages());
        render(ViewTemplate.SHOW_PAGES_HTML);
    }

    public static void showPage(String pageId) {
        renderArgs.put("page",  Page.load(pageId));
        render(ViewTemplate.SHOW_PAGE_HTML);
    }

    public static void editPage(String pageId) {
        Page  p = Page.load(pageId);
        p.edit(Controller.params.allSimple());
        renderArgs.put("page",  p);
        render(ViewTemplate.EDIT_PAGE_HTML);
    }

    public static void viewPage(String page) {
        // meno alebo id... zatial len meno
        // potrebuje Page vediet usera?
        Page  p = Page.loadByName(page);
        if (p != null ) {
            p.emanate(renderArgs);
            render(p.getTemplate());
        }
    }

    // Tags
    public static void showTags() {
        renderArgs.put("tags",  Tag.load());
        render(ViewTemplate.SHOW_TAGS_HTML);
    }

    public static void showNodesByTag(String tag) {
        renderArgs.put("nodes",  Tag.getTaggedNodes(tag));
        render(ViewTemplate.SHOW_LAST_HTML);
    }

}