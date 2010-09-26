package models.feeds;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import models.Feed;
import models.Message;
import models.MessageThread;
import models.MongoEntity;
import models.Page;
import models.User;
import play.cache.Cache;
import plugins.MongoDB;

// doesn't do anything at all, just an empty example
public class MailThreadMessages extends Feed{

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {

        boolean doUpdate  = true;
        ObjectId uid  = user.getId();
        ObjectId threadId = MongoEntity.toId(params.get("thread"));
        if (threadId == null)
            threadId = Cache.get(uid + "_lastThreadId", ObjectId.class);
        Integer start = 0;
        Integer count = 30;
        BasicDBObject query = new BasicDBObject("thread", threadId);
        BasicDBObject sort  = new BasicDBObject("sent", -1);
        List<Message> ll = null;
        if (start == null) start = 0;
        if (count == null) count = 30;
        DBCursor iobj = MongoDB.getDB()
            .getCollection(MongoDB.CMessage).find(query).sort(sort).skip(start).
            limit(count);
        if (iobj != null) {
            if (doUpdate)
                MessageThread.setAsRead(threadId, uid);
            ll = Lists.transform(iobj.toArray(),
                    MongoDB.getSelf().toMessage());
        }
        renderArgs.put(dataName, ll);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
