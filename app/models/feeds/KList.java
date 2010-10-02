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
import java.util.List;
import java.util.Map;
import play.Logger;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import models.Feed;
import models.NodeContent;
import models.Page;
import models.User;
import plugins.MongoDB;

public class KList extends Feed{

    private static final String K = "k";
    private static final BasicDBObject kQuery   = new BasicDBObject().
            append(K, new BasicDBObject("$gt", 1));
    private static final BasicDBObject kSort    = new BasicDBObject().
            append(K, -1);

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        Integer start = 0;
        Integer count = 30;
        List<NodeContent> r = null;
        try {
            // kQuery.append(NodeContent.CREATED, new BasicDBObject("$gt", t24hAgo)).
            DBCursor iobj = NodeContent.dbcol.find(kQuery).sort(kSort).
                        limit(count ==  null ? 100 : count);
            // query.append(fooks, new BasicDBObject("$notin", 1)); ?
            if (iobj !=  null)
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getKlist");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        renderArgs.put("nodes", r);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }
    
}

