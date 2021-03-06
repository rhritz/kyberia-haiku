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

import models.actions.Action;
import java.util.Map;
import org.bson.types.ObjectId;
import java.io.File;
import play.mvc.*;
import play.Logger;
import models.*;
import play.cache.Cache;
import play.mvc.Controller;
import static models.MongoEntity.toId;

@With(Secure.class)
public class Application extends Controller {

    @Before
    static void setConnectedUser() {
        renderArgs.put("reqstart", System.currentTimeMillis());
        String uid;
        if(Security.isConnected()) {
            uid = session.get(User.ID);
            renderArgs.put("user",   session.get(User.USERNAME));
            renderArgs.put("userid", uid);
            renderArgs.put("uid", uid);
            renderArgs.put("newMail", MessageThread.getUnreadMailNotif(toId(uid)));
        } else {
            // to este neznamena ze sme uplne mimo, snad
            // mozeme vytvorit nejaku fake session pre neprihlasenych?
            uid = "nobody";
        }
        User user = User.load(uid);
        request.args.put("app-user", user);
        request.args.put("app-view", ViewTemplate.get(params.allSimple(),
                request, session, user, renderArgs));
        // params.flash();
    }

    /* If there is a param "id" in the req, load the corresponding node
     * for later use
     */
    @Before
    static void setNode() {
        String id = params.get("id");
        NodeContent node = NodeContent.load(id,getUser());
        if (node != null) {
            renderArgs.put("node", node);
            request.args.put("app-node", node);
        }
    }

    /* If there is a param "page" in the req, set the corresponding page
     * - do this after loading the user, setting the view and loading the node
     */
    @Before
    static void setPage() {
        String p = params.get("page"); // or from request...?
        if (p == null || p.length() < 1 || Page.loadByName(p) == null) {
            NodeContent node = getNode();
            if (node != null) {
                p = node.getTemplate();
                if (p == null)
                    p = "Node";
            }
            // .. somehow somewhere throw an error
        }
        // TODO apply ViewTemplate too
        Page page = Page.loadByName(p);
        if (page != null) {
            request.args.put("app-page", page);
        }
    }

    @After
    static void measureTime()
    {
        Long start    = renderArgs.get("reqstart", Long.class);
        Long duration = System.currentTimeMillis() - start;
        Logger.info("Request duration " + duration);
    }

    public static void index() {
        viewPage("main");
    }

    // Actions for view (get) -> Page
//     # whatever1:action : edit(showEditNode), new(viewNodeUpdates), '' -> viewNode
// # pars: page, p (pageNum), ..?

    public static void getNode(String id) {
        String ac = params.get("action"); // or rs
        if (ac != null) { // or something else is wrong
            Action.doAction(ac, request, getUser(), params.allSimple());
        } // TODO or make it into a Page..how to distinguish between them?
        displayNode(id);
    }

    // TODO -> doPostAction, doGetAction...?
    public static void postNode(String id) {
        String ac = params.get("action"); // or rs
        checkAuthenticity();
        // NodeContent n = getNode(); // checkNotNull?
        if (ac == null) { // or something else is wrong
           // displayNode(id);
        } else {
            Action.doAction(ac, request, getUser(), params.allSimple());
            // TODO non-node actions -> ac.toLowerCase() + 'mail', ac.toLowerCase() + 'user' ..
            // OR models.actions.user.*?
            // different Action.apply signatures too?
        }
        // delete(), unput() -> index(), ostatne displayNode()?
        
        displayNode(id);
    }
    
    public static void viewNode(String id, Integer pageNum) {
        displayNode(id);
    }

    public static void viewNode(String id, String template, Integer pageNum) {
        displayNode(id);
    }

    // Actions
    public static void friend(String uid)
    {
        checkAuthenticity();
        Logger.info("Add friend :: " + uid);
        getUser().addFriend(toId(uid));
        showUser(uid);
    }

    public static void ignore(String uid)
    {
        checkAuthenticity();
        Logger.info("Add ignore :: " + uid);
        getUser().addIgnore(toId(uid));
        showUser(uid);
    }

    public static void ignoreMail(String uid)
    {
        checkAuthenticity();
        Logger.info("Add ignoreMail :: " + uid);
        getUser().addIgnoreMail(toId(uid));
        showUser(uid);
    }

    public static void book(String id, String type)
    {
        checkAuthenticity();
        Bookmark.add(toId(id), getUser(), type);
        displayNode(id); // could be a user though...
    }

    private static void displayNode(String id)
    {
        renderArgs.put("id", id);
        NodeContent node = getNode();
        Page page = getPage();
        if (node != null) {
            if (node.canRead(getUser().getId())) {
                UserLocation.saveVisit(getUser(), node.getId());
                if (page != null) {
                    if (page.getCheckEdit()) {
                        if (node.canEdit(getUser().getId())) {
                            viewPage(page);
                        } else {
                            viewPage("Node"); // + error message
                        }
                    } else if (page.getCheckOwner()) {
                        if (node.owner.equals(getUser().getId())) {
                            viewPage(page);
                        } else {
                            viewPage("Node"); // + error message
                        }
                    } else {
                        viewPage(page);
                    }
                } else {
                    viewPage("Node");
                }
            } else {
                viewPage("NodeNoAccess");
            }
        } else {
            viewPage("NodeError");
        }
    }

    public static void showNodes() {
        viewPage("UserNodeChildren");
    }

    public static void showBookmarks() {
        viewPage("Bookmarks");
    }

    public static void showMail(String thread) {
        viewPage("Mail");
    }

    public static void sendMail(String to, String content) {
        checkAuthenticity();
        Message.send(getUser().getIdString(), to, content);
        params.put("thread",
                Cache.get(getUser().getIdString() + "_lastThreadId",
                ObjectId.class).toString());
        viewPage("Mail");
    }

    public static void showLastNodes() {
        viewPage("Last");
    }

    public static void showFriendsContent() {
        viewPage("Friends Nodes");
    }

    public static void showFriends(String uid) {
        // TODO - how to switch between viewed user and the user from session?
        // somehow from the Page?
        User u = User.load(uid);
        renderArgs.put("friends", u.listFriends());
        render(ViewTemplate.SHOW_FRIENDS_HTML);
    }

    public static void showMe() {
        // TODO PAGE  renderArgs.put("nodes", Nodelist.getUserNodes(myId,null));
        viewPage("Me");
    }

    public static void showUser(String id) {
        User u = User.load(id);
        if ( u !=null ) {
            renderArgs.put("user", u);
            // TODO PAGE  renderArgs.put("nodes", Nodelist.getUserNodes(id,null));
            render(ViewTemplate.SHOW_USER_HTML);
        }
        // inak co?
    }

    public static void showK() {
        viewPage("K");
    }

    public static void showUsers() {
        viewPage("Users");
    }

    public static void showLive() {
        viewPage("Live");
    }

    // TODO kontrolovat co sa upladuje
    // play.libs.Images.resize(icon, icon, Integer.SIZE, Integer.SIZE);
    // TemplateLoader.load(String key, String templateContent)
    public static void uploadIcon(String uid, File icon) {
        checkAuthenticity();
        String fname = play.Play.applicationPath.getAbsolutePath() +
                "/public/images/upload/" + getUser().getIdString();
        icon.renameTo(new File(fname));
        showMe();
    }

    public static void changePwd(String uid) {
        checkAuthenticity();
        getUser().changePwd(params.allSimple());
        showMe();
    }

    public static void viewNodeUpdates(String id) {
        viewPage("BookmarkUpdates");        
    }

    // Group management
    public static void addGroup() {
        UserGroup g = UserGroup.create(getUser().getIdString(), params.get("name"),
                null);
        renderArgs.put("group", g);
        render(ViewTemplate.SHOW_GROUP_HTML);
    }

    public static void addUserToGroup(String groupId) {
        String uid = params.get("newId");
        UserGroup g = UserGroup.load(groupId);
        ObjectId newUid = toId(uid);
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
    // TODO pimp up SHOW_PAGE_HTML for readonly access to pages
    public static void addPage() {
        Page p = Page.create(params.get("name"), params.get("template"),
                getUser().getId());
        renderArgs.put("page",  p);
        renderArgs.put("classes", MongoEntity.getClasses("models.feeds"));
        render(ViewTemplate.EDIT_PAGE_HTML);
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
        renderArgs.put("classes", Feed.getClasses("models.feeds"));
        render(ViewTemplate.EDIT_PAGE_HTML);
    }

    public static void editPage(String pageId) {
        Page  p = Page.load(pageId);
        p.edit(Controller.params.allSimple());
        renderArgs.put("page",  p);
        renderArgs.put("classes", Feed.getClasses("models.feeds"));
        render(ViewTemplate.EDIT_PAGE_HTML);
    }

    private static void viewPage(String page) {
        viewPage(Page.loadByName(page));
    }

    private static void viewPage(Page page) {
        renderArgs.put("pageBuildStart",  System.currentTimeMillis());
        if (page != null ) {
            page.process(params.allSimple(), request, session, getUser(), renderArgs);
            render(page.getTemplate());
        } else {
            // error
        }
    }

    // Tags
    public static void showTags() {
        viewPage("Tags");
    }

    public static void showNodesByTag(String tag) {
        viewPage("NodesByTag");
    }

    // Views
    public static void addView() {
        ViewTemplate v = ViewTemplate.create(params.get("name"), params.get("inherit"),
                getUser().getId());
        renderArgs.put("view", v);
        // + pages
        render(ViewTemplate.EDIT_VIEW_HTML);
    }

    public static void showAddView() {
        render(ViewTemplate.ADD_VIEW_HTML);
    }

    public static void showViews() {
        renderArgs.put("views",  ViewTemplate.loadViews());
        render(ViewTemplate.SHOW_VIEWS_HTML);
    }

    public static void showView(String viewId) {
        renderArgs.put("view",  ViewTemplate.load(viewId));
        // renderArgs.put("pages", Feed.getClasses("models.feeds"));
        render(ViewTemplate.EDIT_VIEW_HTML);
    }

    public static void editView(String viewId) {
        ViewTemplate v = ViewTemplate.load(viewId);
        v.edit(params.allSimple());
        renderArgs.put("view",  v);
        // renderArgs.put("pages", Feed.getClasses("models.feeds"));
        render(ViewTemplate.EDIT_VIEW_HTML);
    }

    private static void viewView(String viewId) { // lol
        // viewPage(Page.loadByName(page));
    }

    // Helpers
    private static User getUser() {
        return (User) Application.request.args.get("app-user");
    }

    private static NodeContent getNode() {
        return (NodeContent) Application.request.args.get("app-node");
    }

    private static Page getPage() {
        return (Page) Application.request.args.get("app-page");
    }

    private static ViewTemplate getView() {
        return (ViewTemplate) Application.request.args.get("app-view");
    }

}