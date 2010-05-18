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

import com.google.code.morphia.AbstractMongoEntity;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoValue;
import com.google.code.morphia.annotations.MongoTransient;
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
import plugins.*;
import play.Logger;
import play.cache.Cache;

@MongoDocument
public class Page extends AbstractMongoEntity {

    private String              contentNode;
    private String              name;
    private String              template;
    private String              owner;
    List<String>                contentTags; // -> TemplateDataDef

    public  static final String MAIN     = "main";
    public  static final String NAME     = "name";
    private static Page         mainPage = new Page("main","app/views/Pages/main.html");

    public Page() {}

    public Page(String name, String template) {
        this.name     = name;
        this.template = template;
    }

    public static Page getByName(String name) {
        Page p = null;
        p = Cache.get("page_" + name, Page.class);
        if (p != null)
            return p;
        try {
            BasicDBObject query = new BasicDBObject().append(NAME, name );
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CPage).findOne(query);
            if (iobj != null)
                p = MongoDB.getMorphia().fromDBObject(Page.class, iobj);
        } catch (Exception ex) {
            Logger.info("mongo fail @getIdForName");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        if (p == null )
            p = mainPage; // default fallback
        else
            Cache.add("page_" + p.name , p);
        return p;
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
}
