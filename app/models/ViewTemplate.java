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
import com.mongodb.ObjectId;
import java.util.Map;
import play.mvc.Scope.RenderArgs;

// - zavisi od usera a sposobu pristupu
@Entity("ViewTemplate")
public class ViewTemplate extends MongoEntity{

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
        public static final String SHOW_K_HTML         = "app/views/Application/showK.html";
        public static final String SHOW_USERS_HTML     = "app/views/Application/showUsers.html";
        public static final String SHOW_USER_HTML      = "app/views/Application/viewUser.html";
        public static final String SHOW_LIVE_HTML      = "app/views/Application/showLive.html";
        public static final String SHOW_TAGS_HTML      = "app/views/Application/showTags.html";
        public static final String ADD_USER_HTML       = "app/views/Application/addUser.html";
        public static final String ADD_PAGE_HTML       = "app/views/Application/addPage.html";
        public static final String EDIT_PAGE_HTML      = "app/views/Application/editPage.html";
        public static final String SHOW_PAGE_HTML      = "app/views/Application/showPage.html";
        public static final String SHOW_PAGES_HTML     = "app/views/Application/showPages.html";

        public static final String TOP_LEVEL_TEMPLATE = "topLevelTemplate";

	public boolean isDefault; // true - this is the root/default view
	// public String superViewId;  // view inheritance
	public ObjectId superView; // view inheritance
	public String defaultFrame; // '/main.html'
	public String defaultMenu;  // '/menu/html'
	public String defaultCss;
	// ... and more properties
	public HashMap<String,Page> templates; // eg 'mail'=> mailTemplateInstance

	// default View singleton
	private static ViewTemplate defaultView; // new View(....)

	public static ViewTemplate getDefaultView()
	{
		return defaultView;
	}

	public ViewTemplate(boolean isDefault, ViewTemplate superView)
	{
		this.isDefault = isDefault;
                // this.superView = superView;
	}

	public void registerTemplate(String templateId, Page t)
	{
		templates.put(templateId, t);
	}

        // eventualne get a getTemplate budu jedno a to iste
        public static String getHtml(String wat)
        {
         //   
            return null;
        }

	public static void renderPage(
                Map<String, String> params,
                HashMap r,
                HashMap s,
                NodeContent n, // tu to uz budeme vediet? ale ano, ak loadujeme node
                User u,
                RenderArgs renderArgs
                ) 
	{
		ViewTemplate v = null;
		if (s.containsKey("view")) {
			v = (ViewTemplate) s.get("view");
		} else if(r.containsKey("view")) {
			v = (ViewTemplate) r.get("view");
		} else {
			v = ViewTemplate.getDefaultView();
		}
                // toto by sme asi mali oddelit, v/t

		// Location bude nastavena v sessionm alebo kde
                // - tyka sa hlavne veci ako mail a td. ktore nie su Node
		Page t = null;
		if (s.containsKey("Location")) {
			t = Page.loadByName((String) s.get("Location"));
		} else {
			// hierarchia template: 1. request (override), 2. View<->Node
			if(r.containsKey("template")) {
				t = Page.loadByName((String) r.get("template"));
			}
			if (t == null) {
                            // else + priapd ze dana tmpl neexistuje
                            // tu samozrejme predpokladame (ale aj ninde) ze tempalte urcene v Node urcite
                            // existuju, co nemusi byt pravda
				t = Page.loadByName(n.getTemplate().toString());
			}
		}
                t.process(params, r, s , u, renderArgs);
	}

        public static ViewTemplate load(String id)
        {
            return null;
        }

}
