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

// definuje celkovo pohlad na stranku

import java.util.HashMap;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

// - zavisi od usera a sposobu pristupu
@Entity("ViewTemplate")
public class ViewTemplate extends MongoEntity{

    public static DBCollection dbcol = null;

        // hlavne templaty - nazvy
        public static final String VIEW_USER = "view_user";
        
        // default templaty
        public static final String VIEW_NODE_HTML      = "app/views/Application/viewNode.html";
        public static final String EDIT_NODE_HTML      = "app/views/Application/editNode.html";
        public static final String SHOW_NODES_HTML     = "app/views/Application/showNodes.html";
        public static final String SHOW_BOOKMARKS_HTML = "app/views/Application/showBookmarks.html";
        public static final String MAIL_HTML           = "app/views/Application/mail.html";
        public static final String SHOW_LAST_HTML      = "app/views/Application/showLast.html";
        public static final String SHOW_GROUPS_HTML    = "app/views/Application/showGroups.html";
        public static final String SHOW_GROUP_HTML     = "app/views/Application/showGroup.html";
        public static final String EDIT_GROUP_HTML     = "app/views/Application/editGroup.html";
        public static final String ADD_GROUP_HTML      = "app/views/Application/editGroup.html";
        public static final String SHOW_ME_HTML        =  "app/views/Application/showMe.html";
        public static final String SHOW_FRIENDS_HTML   =  "app/views/Application/showFriends.html";
        public static final String SHOW_USER_HTML      = "app/views/Application/viewUser.html";
        public static final String ADD_USER_HTML       = "app/views/Application/addUser.html";
        public static final String ADD_PAGE_HTML       = "app/views/Application/addPage.html";
        public static final String EDIT_PAGE_HTML      = "app/views/Application/editPage.html";
        public static final String SHOW_PAGE_HTML      = "app/views/Application/showPage.html";
        public static final String SHOW_PAGES_HTML     = "app/views/Application/showPages.html";

        public static final String ADMIN_PAGE_HTML     = "app/views/Admin/admin.html";

        public static final String TOP_LEVEL_TEMPLATE = "topLevelTemplate";

	public boolean isDefault; // true - this is the root/default view
	// public String superViewId;  // view inheritance
	public ObjectId superView; // view inheritance
	public String defaultMenu;  // '/menu/html'
	public String defaultCss;
        public String template;
        
	private static HashMap<String,ViewTemplate> views;

        public static void start() {
            ViewTemplate def = new ViewTemplate(true,null,"main.html");
            views = new HashMap<String,ViewTemplate>();
            views.put("default", def);
        }

	public ViewTemplate(boolean isDefault, 
                            ViewTemplate superView,
                            String template)
	{
            this.isDefault = isDefault;
            this.template  = template;
            // this.superView = superView;
	}

        public static ViewTemplate loadByName(String name) {
            return views.get(name);
        }

        public static ViewTemplate get(
                Map<String, String> params,
                Request r,
                Session s,
                User u,
                RenderArgs renderArgs
                )
        {
            ViewTemplate view = null;
            if (s.contains("view")) {
                view = loadByName(s.get("view"));
            } else if(params.containsKey("view")) {
                view = loadByName(params.get("view"));
                //... and maybe some other mechanisms, from User etc
            } else {
                view = views.get("default");
            }
            renderArgs.put(ViewTemplate.TOP_LEVEL_TEMPLATE, view.template);
            return view;
        }

	private void renderPage(
                Map<String, String> params,
                Request r,
                Session s,
                NodeContent n,
                User u,
                RenderArgs renderArgs
                ) 
	{
		Page t = null;
		if (s.contains("Location")) {
			t = Page.loadByName((String) s.get("Location"));
		} else {
			// hierarchia template: 1. request (override), 2. View<->Node
			if(params.containsKey("template")) {
				t = Page.loadByName(params.get("template"));
			}
			if (t == null) {
                            // else + priapd ze dana tmpl neexistuje
                            // tu samozrejme predpokladame (ale aj inde) ze tempalte urcene v Node urcite
                            // existuju, co nemusi byt pravda
				t = Page.loadByName(n.getTemplate().toString());
			}
		}
                t.process(params, r, s , u, renderArgs);
	}

}
