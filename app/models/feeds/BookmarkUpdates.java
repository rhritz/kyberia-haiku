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
import com.mongodb.ObjectId;
import java.util.List;
import java.util.Map;
import models.Activity;
import models.Bookmark;
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
public class BookmarkUpdates extends Feed{

    private static final BasicDBObject sort = new BasicDBObject().append("name", 1);

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        ObjectId nodeId = MongoEntity.toId(params.get("id"));
        ObjectId uid = user.getId();

        // 1. get bookmark
        List<NodeContent> newNodes = null;
        Bookmark b = Bookmark.getByUserAndDest(uid, nodeId);
        Long lastVisit = b.getLastVisit();
        Bookmark.updateVisit(uid, nodeId.toString());
        // 2. load notifications
        try {
            BasicDBObject query = new BasicDBObject().
                    append(b.getTyp() == null ? "ids" : b.getTyp(), nodeId).
                    append("date", new BasicDBObject("$gt",lastVisit));
            // Logger.info("getUpdatesForBookmark::"  + query.toString());
            // BasicDBObject sort = new BasicDBObject().append("date", -1);
            // vlastne chceme natural sort a iba idcka nodes ktore mame zobrazit
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CActivity).find(query).
                    sort(sort);
            if (iobj !=  null) {
                // Logger.info("getUpdatesForBookmark found");
                List<Activity> lll = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toActivity());
                // 3. load nodes we want to show
                List<ObjectId> nodeIds = Lists.newLinkedList();
                for (Activity ac : lll)
                    nodeIds.add(ac.getOid());
                newNodes = NodeContent.load(nodeIds);
            }
        } catch (Exception ex) {
            Logger.info("getUpdatesForBookmark");
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
