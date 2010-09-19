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

public class NodesByTag extends Feed{

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        String tag = params.get("tag");

        List<NodeContent> l = null;
        try {
            DBCursor iobj = MongoDB.getDB().getCollection(MongoDB.CNode).
                    find(new BasicDBObject("tags", tag));
            if (iobj !=  null)
                l = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toNodeContent());
        } catch (Exception ex) {
            Logger.info("getTaggedNodes::");
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
