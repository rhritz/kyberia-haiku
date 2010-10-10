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

import com.mongodb.BasicDBObject;
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

public class LastNodes extends Feed {

    private static final BasicDBObject lastSort = new BasicDBObject(NodeContent.CREATED, -1);
    private static final Long t24h = 86400000l;

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        Integer start = 0;
        Integer count = 30;
        List<NodeContent> r = null;
        renderArgs.put("reqstart2", System.currentTimeMillis());
        try {
            BasicDBObject query = new BasicDBObject(NodeContent.CREATED,
                    new BasicDBObject("$gt",System.currentTimeMillis() - t24h));
            r = MongoDB.transform( NodeContent.dbcol.find(query).
                    sort(lastSort).skip(start).limit(count),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.error(ex, ex.toString());
        }
        renderArgs.put("reqstart3", System.currentTimeMillis());
        renderArgs.put(dataName, r);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }
}
