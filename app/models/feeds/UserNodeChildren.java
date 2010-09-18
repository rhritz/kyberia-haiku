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
package models.feeds;

import com.google.code.morphia.Morphia;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import models.Activity;
import play.Logger;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import models.Feed;
import models.NodeContent;
import models.Page;
import models.User;
import plugins.MongoDB;

public class UserNodeChildren extends Feed {

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        Integer start = 0;
        Integer count = 30;
        List<NodeContent> ll = new LinkedList<NodeContent>();
        start = start * count;
        try {
            BasicDBObject query = new BasicDBObject().append("parid", 
                    user.getId());
            BasicDBObject sort = new BasicDBObject().append("date", -1);
            DBCursor iobj = MongoDB.getDB()
                .getCollection(MongoDB.CActivity).find(query).
                sort(sort).skip(start).limit(count);
            Morphia morphia = MongoDB.getMorphia();
            while(iobj.hasNext())
               ll.add(NodeContent.load((morphia.fromDBObject(Activity.class,
                       (BasicDBObject) iobj.next())).getOid()));
        } catch (Exception ex) {
            Logger.info("load nodes::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        renderArgs.put("nodes",ll);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
