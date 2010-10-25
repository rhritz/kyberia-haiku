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

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.Map;
import models.Activity;
import play.Logger;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import models.Feed;
import models.MongoEntity;
import models.NodeContent;
import models.Page;
import models.User;
import plugins.MongoDB;

// idealne by ma to hodilo na prvych 30 neprecitanych a aj zodpvoedajuco
// updatlo lastVisit
public class NodeUpdates extends Feed{

    // TODO natural sort
    private static final BasicDBObject sort = new BasicDBObject("date", 1);

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        Integer start = 0;
        Integer count = 30;
        ObjectId nodeId = MongoEntity.toId(params.get("id"));
        ObjectId uid = user.getId();

        List<NodeContent> newNodes = null;
        try {
            BasicDBObject query = new BasicDBObject( "oid", nodeId);
            DBCursor iobj = Activity.dbcol.find(query).sort(sort).
                    skip(start).limit(count);
            List<Activity> lll = MongoDB.transform(iobj,
                        MongoDB.getSelf().toActivity());
            if (! lll.isEmpty()) {
                List<ObjectId> nodeIds = Lists.newLinkedList();
                for (Activity ac : lll)
                    nodeIds.add(ac.getOid());
                newNodes = NodeContent.load(nodeIds);
            }
        } catch (Exception ex) {
            Logger.info("NodeUpdates");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        renderArgs.put(dataName, newNodes);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
