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
import java.util.HashMap;
import java.util.LinkedList;
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

// doesn't do anything at all, just an empty example
public class Thread extends Feed{

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {

        Integer start = 0;
        Integer count = 30;
        int pageNum = 0;
        try{ pageNum = Integer.parseInt(params.get("pageNum"));
        } catch(Exception e) {}
        start = count * pageNum;

        List<NodeContent> thread = new LinkedList<NodeContent>();
        HashMap<ObjectId,Integer> roots = new HashMap<ObjectId,Integer>();
        int local_depth = 0;
        NodeContent nextnode = (NodeContent) request.args.get("app-node");
        NodeContent lastnode;
        ObjectId parent;
        for (int i = 0; i < start + count; i++) {
            lastnode = nextnode;
            if (lastnode.dfs == null)
              break;
            nextnode = NodeContent.load(lastnode.dfs);
            if (nextnode == null || nextnode.par == null)
              break;
            parent = nextnode.par;
            if (parent.equals(lastnode.getId())) {
                roots.put(parent,local_depth);
                local_depth++;
            } else {
                // ak v tomto bode nema parenta v roots,
                // znamena to ze siahame vyssie ako root - koncime
                if (roots.get(parent) == null)
                    break;
                // nasli sme parenta, sme o jedno hlbsie ako on
                local_depth = roots.get(parent) + 1;
            }
            if ( i>= start) {
                // tento node chceme zobrazit
                // tu je vhodne miesto na kontrolu per-node permissions,
                // ignore a fook zalezitosti
                nextnode.depth = local_depth;
                thread.add(nextnode);
            }
        }
        renderArgs.put(dataName, thread);
        renderArgs.put("currentPage",pageNum); // ?
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
