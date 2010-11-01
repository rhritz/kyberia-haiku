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
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;
import plugins.*;
import play.Logger;
import play.cache.Cache;
import play.mvc.Scope.RenderArgs;

@Entity("Page")
public class Page extends MongoEntity {

    public static DBCollection dbcol = null;
    private static final String key  = "page_";

    private String              name;
    private String              title;
    private String              template;
    private ObjectId            owner;
    private Map<String,String>  blocks;
    // private boolean checkOwner, checkEdit, checkRead..?

    @Transient
    private List<Feed> preparedBlocks;

    public  static final String MAIN     = "main";
    public  static final String NAME     = "name";
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

    public void removeBlock(String blockName) {
        blocks.remove(blockName);
        save(true);
        enhance();
    }

    public static List<Page> loadPages() {
        List<Page> pages = null;
        try {
            DBCursor iobj = dbcol.find();
            pages = MongoDB.transform(iobj, MongoDB.getSelf().toPage());
        } catch (Exception ex) {
            Logger.info("Page list load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return pages;
    }

    public static Page loadByName(String name) {
        Page page = templateStore.get(name);
        if (page == null) {
            try {
                DBObject iobj = dbcol.findOne(new BasicDBObject("name",name));
                Logger.info("Page.loadByName Found:" + iobj);
                if (iobj != null) 
                    page = MongoDB.fromDBObject(Page.class, iobj).enhance();
            } catch (Exception ex) {
                Logger.info("page load fail");
                ex.printStackTrace();
                Logger.info(ex.toString());
                return null;
            }
        }
        return page;
    }

    public static Page load(String pageId) {
        return load(toId(pageId));
    }

    public static Page load(ObjectId oid) {
        Page page = null;
        try {
            DBObject iobj = dbcol.findOne(new BasicDBObject("_id",oid));
            if (iobj != null) 
                page = MongoDB.fromDBObject(Page.class, iobj).enhance();
        } catch (Exception ex) {
            Logger.info("page load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return page;
    }

    public void edit(Map<String, String> params) {
        String blockName   = params.get("blockName");
        String blockValue  = params.get("blockValue");
        boolean doUpdate = false;
        String newTmpl = params.get("template");
        if (newTmpl != null && !newTmpl.equals(template)) {
            template = newTmpl;
            doUpdate = true;
        }
        if (getBlocks() == null)
            blocks = new HashMap<String,String>();
        if (blockName != null && blockValue != null) {
            getBlocks().put(blockName, blockValue);
            doUpdate = true;
        }
        if (doUpdate)
            update(true);
    }

    public static Page create(String name, String template, ObjectId owner) {
        Page p = new Page(name, template, owner);
        if (p != null) {
            p.setId(new ObjectId());
            p.save(true);
        }
        return p;
    }

    private String getName() {
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////

    public static void start() {
        templateStore = new HashMap<String,Page>();
        List<Page> allPages = loadPages();
        if (allPages == null)
            return;
        for (Page page : allPages) 
            templateStore.put(page.getName(), page);
        Logger.info("Pages loaded");
    }

    private void prepareBlocks() {
        preparedBlocks = new LinkedList<Feed>();
        if (getBlocks() == null)
            return;
        for (String blockName : getBlocks().keySet()) {
            Feed f = Feed.getByName(blockName, this);
            if (f != null)
                preparedBlocks.add(f);
        }
    }

    public void process(Map<String, String> params,
                        Request request,
                        Session session,
                        User    user,
                        RenderArgs renderArgs
                        ) {
        for (Feed f: preparedBlocks)
            f.getData(params, request, session, user, renderArgs);
    }

    /**
     * @return the blocks
     */
    public Map<String, String> getBlocks() {
        return blocks;
    }

    @Override
    public Page enhance() {
        prepareBlocks();
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

}
