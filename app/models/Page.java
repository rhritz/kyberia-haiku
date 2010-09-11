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
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
import plugins.*;
import play.Logger;
import play.cache.Cache;
import play.mvc.Scope.RenderArgs;

@Entity("Page")
public class Page extends MongoEntity {

    private String              contentNode;
    private String              name;
    private String              template;
    private ObjectId            owner;
    private List<ObjectId>      feeds; // -> tag getFeed/displayFeed ?

    @Transient
    // list alebo hash?
    private HashMap<String,Feed> content;

    @Transient
    private List<Feed> preparedFeeds;

    public  static final String MAIN     = "main";
    public  static final String NAME     = "name";
    private static Page         mainPage = new Page("main","app/views/Pages/main.html");
    private static HashMap<String,Page> templateStore;

    public Page() {}

    public Page(String name, String template) {
        this.name     = name;
        this.template = template;
    }

    public Page(String name, String template, ObjectId owner) {
        this.name     = name;
        this.template = template;
        this.owner    = owner;
    }

    public static Page get(   String name,
                                    Params params,
                                    RenderArgs renderArgs,
                                    Request request,
                                    Session session) {
        Page p = null;
        User u = User.load(new ObjectId(session.get(User.ID)));
        ViewTemplate vt = ViewTemplate.load(u.getView());
        return p;
    }

    public static Page getByName(   String name,
                                    Params params,
                                    RenderArgs renderArgs,
                                    Request request,
                                    Session session) {
        Page p = null;
        p = Cache.get("page_" + name, Page.class);
        if (p != null)
            return p;
        try {
            BasicDBObject query = new BasicDBObject().append(NAME, name );
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CPage).findOne(query);
            if (iobj != null) {
                p = MongoDB.getMorphia().fromDBObject(Page.class, iobj);
                Cache.set("page_" + p.name, p);
            }
        } catch (Exception ex) {
            Logger.info("mongo fail @getIdForName");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        if (p == null )
            p = mainPage; // default fallback
        return p;
        /*
         ViewTemplate v = null;
		if (s.containsKey("view")) {
			v = (ViewTemplate) s.get("view");
		} else if(r.containsKey("view")) {
			v = (ViewTemplate) r.get("view");
		} else {
			v = ViewTemplate.getDefaultView();
		}

		// Location bude nastavena v session - tyka sa hlavne veci ako mail a td. ktore nie su Node
		NodeTemplate t = null;
		if (s.containsKey("Location")) {
			t = v.getTemplate((String) s.get("Location"));
		} else {
			// hierarchia template: 1. request (override), 2. View<->Node
			if(r.containsKey("template")) {
				t = v.getTemplate((String) r.get("template"));
			}
			if (t == null) { // else + priapd ze dana tmpl neexistuje
                            // tu samozrejme predpokladame (ale aj ninde) ze tempalte urcene v Node urcite
                            // existuju, co nemusi byt pravda
				t = v.getTemplate(n.getTemplate().toString());
			}
		}
		// bail here if t is null

		// v podstate to co ma tato funkcis spravit je nastavit mena inkludovanych suborov
		// a premennych/tagov
		// ktore sa potom spracuju v .html subore
		// v.render(...);
		// t.render(...);
         */
    }

    /**
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @param template the template to set
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    // page p = Page.getByName("dgfsdfj");
    // p.emanate(renderArgs)
    public void emanate(RenderArgs renderArgs) {
     //   List<Stuff> ddd // member
    // ddd.add(new TemplateDataDef("last10") {@Override public List<NodeContent> getData(int start, int count, int order, ...) { return NodeContent.load(blablabla)}})
       //  for (Stuff stuff : ddd) {
       //    renderArgs.put(stuff.getName(),stuff.getData(uid,id,...))
       //  }
    }

    public void loadFeeds() {
        if (feeds == null)
            return;
        for (ObjectId feedId : feeds) {
           Feed feed = Feed.loadContent(feedId);
           content.put(feed.getName(), feed);
        }
    }

    public void addFeed(ObjectId feedId) {
        if (feeds == null)
            feeds = new LinkedList<ObjectId>();
        feeds.add(feedId);
        save();
        // + pripadne vybuit z cache
    }

    public void removeFeed(ObjectId feedId) {
        if (feeds != null) {
            feeds.remove(feedId); // toto asi treba na zaklade hodnoty?
            save();
        }
    }

    public static List<Page> loadPages() {
        List<Page> pages = null;
        try {
            DBCursor iobj = (DBCursor) MongoDB.getDB().
                    getCollection(MongoDB.CPage).
                    find();
            if (iobj != null) 
                pages = Lists.transform(iobj.toArray(),
                            MongoDB.getSelf().toPage());
        } catch (Exception ex) {
            Logger.info("Page list load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return pages;
    }

    public static Page loadByName(String name) {
        Page page = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CPage).
                    findOne(new BasicDBObject().
                    append("name",name));
            Logger.info("Page.loadByName Found:" + iobj);
            if (iobj != null)
                page = (Page) MongoDB.getMorphia().
                        fromDBObject(Page.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return page;
    }

    public static Page load(String pageId) {
        Page page = null;
        ObjectId oid = new ObjectId(pageId);
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CPage).
                    findOne(new BasicDBObject().
                    append("_id",oid));
            if (iobj != null)
                page = (Page) MongoDB.getMorphia().
                        fromDBObject(Page.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return page;
    }

    public void edit(Map<String, String> params) {
        save();
    }

    // + cache
    public void save() {
        MongoDB.save(this, MongoDB.CPage);
    }

    public static Page create(String name, String template, ObjectId owner) {
        Page p = new Page(name, template, owner);
        if (p != null)
            p.save();
        return p;
    }

    private String getName() {
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////

    // pri starte aplikacie
    public static void start() {
        templateStore = new HashMap<String,Page>();
        List<Page> allPages = loadPages();
        if (allPages == null)
            return;
        for (Page page : allPages) {
            page.prepareFeeds();
            templateStore.put(page.getName(), page);
        }
    }

    // pri loade templaty
    private void prepareFeeds() {
        preparedFeeds = new LinkedList<Feed>();
        if (feeds == null)
            return;
        for (ObjectId feedId : feeds) {
            Feed f = Feed.loadSubclass(feedId, this);
            if (f != null)
                preparedFeeds.add(f);
        }
    }

    // pri spracovani Page
    public void process(Map<String, String> params,
                        HashMap request,
                        HashMap session,
                        User    user,
                        RenderArgs renderArgs
                        ) {
        for (Feed f: preparedFeeds) 
            f.getData(params, request, session, user, renderArgs);
    }

}
