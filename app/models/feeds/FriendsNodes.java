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
import org.bson.types.ObjectId;
import plugins.MongoDB;

 // TODO - check permissions
public class FriendsNodes extends Feed{

    private static final BasicDBObject dateSort = new BasicDBObject().
            append("date", -1);

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        Integer start = 0;
        Integer count = 30;
        BasicDBObject query = new BasicDBObject("uids", user.getId());
        BasicDBObject sort = dateSort;
        DBCursor iobj = Activity.dbcol.find(query).sort(sort).skip(start).limit(count);
        // TODO tuto tu Activity transformujeme uplne zbytocne, staci _id
        List<ObjectId> nodeIds = Lists.newLinkedList();
        while(iobj.hasNext())
            nodeIds.add(MongoDB.fromDBObject(Activity.class, iobj.next()).getOid());
        renderArgs.put(dataName, NodeContent.load(nodeIds, user));
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
