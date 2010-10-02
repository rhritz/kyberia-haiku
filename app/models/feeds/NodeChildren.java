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

// ergo 'show flat'
public class NodeChildren extends Feed{

    private static final BasicDBObject sort = new BasicDBObject("created", -1);

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        ObjectId id   = MongoEntity.toId(params.get("id"));
        Integer start = 0;
        Integer count = 30;

        List<NodeContent> l = null;
        try {
            DBCursor iobj = NodeContent.dbcol.find(new BasicDBObject("par", id)).
                    sort(sort).skip(start).limit(count);
            if (iobj !=  null)
                l = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("NodeChildren::");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        renderArgs.put(dataName, l);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}

