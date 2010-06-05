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
        // Page.getByName(Controller.params, Controller.renderArgs, Controller.request, Controller.session

        Page main = Page.getByName(Page.MAIN, params, renderArgs, request, session);
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
        if (node.canRead(new ObjectId(uid))) {
            // NodeContent.getThreadedChildren(oid, start, count);
            // logicky UserVisit save patri sem, lebo tu vieme co ideme zobrazit
            UserLocation.saveVisit(User.load(uid), id);
            renderArgs.put("node", node);
            renderArgs.put("thread",
                    NodeContent.getThreadedChildren(oid, start, count));
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
        renderArgs.put("mailMessages", 
                Message.getMessages(new ObjectId(thread), new ObjectId(uid)));
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
        renderArgs.put("mailMessages",
                Message.getMessages(new ObjectId(session.get("LastThreadId")),
                    new ObjectId(fromId)));
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

    public static void showMe() {
        // linka na current usera
        String myId = session.get(User.ID);
        Logger.info("showMe:: " + myId);
        renderArgs.put("nodes",
                Nodelist.getUserNodes(myId,null));
        User me = User.load(myId);
        /* should this be lazily evaluated? */
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

    // Template processing:
    public static void viewNodeT(NodeTemplate template)
    {
        /*
        for (TemplateDataDef d : template.getWantedData())
        {
            switch (d.getDatasetName()) {
                    case MAIL:
                        break;
                        // renderArgs.put('mail',Mail.getMail(d.prop1, d.prop2, ...);
                    //  ...
                    // tuto treba zohladnit strankovanie, tj niektore veci pojdu z TemplateDataDef,
                    // niektore ine z requestu / session
            }
        }
         *
         */
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
        User me = User.load(new ObjectId(session.get(User.ID)));
        me.changePwd(Controller.params.allSimple());
        showMe();
    }

    // event - zmena property node alebo usera
    public static void event(String id) {
       // vyhryzni event z params, urob akciu, vrat displayNode
    }


}