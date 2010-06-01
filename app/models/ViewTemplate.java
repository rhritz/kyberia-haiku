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

// - zavisi od usera a sposobu pristupu
@Entity("ViewTemplate")
public class ViewTemplate extends MongoEntity{

        // hlavne templaty - nazvy
        public static final String VIEW_USER = "view_user";
        
        // default templaty
        public static final String VIEW_NODE_HTML = "app/views/Application/viewNode.html";
        public static final String EDIT_NODE_HTML = "app/views/Application/editNode.html";
        public static final String SHOW_NODES_HTML = "app/views/Application/showNodes.html";
        public static final String SHOW_BOOKMARKS_HTML = "app/views/Application/showBookmarks.html";
        public static final String MAIL_HTML = "app/views/Application/mail.html";
        public static final String SHOW_LAST_HTML = "app/views/Application/showLast.html";
        public static final String SHOW_ME_HTML =  "app/views/Application/showMe.html";
        public static final String SHOW_K_HTML = "app/views/Application/showK.html";
        public static final String SHOW_USERS_HTML = "app/views/Application/showUsers.html";
        public static final String SHOW_USER_HTML = "app/views/Application/viewUser.html";
        public static final String SHOW_LIVE_HTML = "app/views/Application/showLive.html";
        public static final String SHOW_TAGS_HTML = "app/views/Application/showTags.html";
        public static final String ADD_USER_HTML = "app/views/Application/addUser.html";

        public static final String TOP_LEVEL_TEMPLATE = "topLevelTemplate";

	public boolean isDefault; // true - this is the root/default view
	// public String superViewId;  // view inheritance
	public ViewTemplate superView; // view inheritance
	public String defaultFrame; // '/main.html'
	public String defaultMenu;  // '/menu/html'
	public String defaultCss;
	// ... and more properties
	public HashMap<String,NodeTemplate> templates; // eg 'mail'=> mailTemplateInstance

	// default View singleton
	private static ViewTemplate defaultView; // new View(....)

	public static ViewTemplate getDefaultView()
	{
		return defaultView;
	}

	public ViewTemplate(boolean isDefault, ViewTemplate superView)
	{
		this.isDefault = isDefault;
                this.superView = superView;
	}

	public void registerTemplate(String templateId, NodeTemplate t)
	{
		templates.put(templateId, t);
	}

        // eventualne get a getTemplate budu jedno a to iste
        public static String getHtml(String wat)
        {
         //   
            return null;
        }

	public NodeTemplate getTemplate(String templateId)
	{
		if (templates.containsKey(templateId))
		{
			return templates.get(templateId);
		}
		else
		{
			if (isDefault)
			{
				// we don't know how to render this
				// throw new TemplateInstantiationException();
				return null;
			}
			return superView.getTemplate(templateId);
		}
	}

	public static void renderPage(HashMap r, HashMap s, NodeContent n, User u) // RequestParams r, Session s, Node n, User u)
	{
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
	}

	public static void render()
	{
		// put names of files etc into renderArgs
	}

        // load all views, templates and datadefs and store them either locally
        // in a structure or in the cache (but preferably locally)
        // - which goes also for the html files (?)
        public static void loadViews()
        {
            // first create the default view
        }

        /*
         ViewTemplate.getTemplate(Templates.MAIL) or so?
         render(ViewTemplate.getTmpl(Templates.MAIL), stuff); looks ok
         but still we need to feed him session, request and view somehow
         */

        public static String getHtmlTemplate(String view, String what)
        {
            // zatial tmpl podla hashu #what asi len
            // + zadefinovat staticke identifikatory tmpl
            return null;
        }

        public static ViewTemplate load(String id)
        {
            return null;
        }

}
