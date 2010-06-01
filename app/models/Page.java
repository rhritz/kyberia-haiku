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

import java.util.List;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.Morphia;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import com.mongodb.QueryBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import play.mvc.Http.Request;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;
import plugins.*;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Scope.RenderArgs;

@Entity("Page")
public class Page extends MongoEntity {

    private String              contentNode;
    private String              name;
    private String              template;
    private String              owner;
    List<String>                contentTags; // -> TemplateDataDef
    // List<TemplateDataDef> ddd
    // ddd.add(new TemplateDataDef("last10") {@Override public List<NodeContent> getData(int start, int count, int order, ...) { return NodeContent.load(blablabla)}})
    // for (TemplateDataDef tdd : ddd) {
    //   renderArgs.put(tdd.getName(),tdd.getData())
    // }
    // pri starte aplikacie pozbierat a vytvorit vsetky taketo triedy?

    public  static final String MAIN     = "main";
    public  static final String NAME     = "name";
    private static Page         mainPage = new Page("main","app/views/Pages/main.html");

    public Page() {}

    public Page(String name, String template) {
        this.name     = name;
        this.template = template;
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

    public void emit() {
        List<TemplateData> dd = new LinkedList<TemplateData>();
        // teraz ako to vytvorit na zkalade TemplateDataDef?

        // cize dva podinterfacy, jedno pre listy a druhy pre single
        TemplateData<NodeContent> cc = new TemplateData<NodeContent>() {
            public List<NodeContent> getList() {
                return new LinkedList<NodeContent>();
            }

            public NodeContent getOne() {
                return new NodeContent();
            }
        };
        dd.add(cc);
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
}

/*
 public interface TemplateData<T> {

    public T       getOne();
    public List<T> getList();
 */