package models.feeds;

import com.mongodb.ObjectId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import models.Feed;
import models.LinkPair;
import models.MessageThread;
import models.Page;
import models.User;

// doesn't do anything at all, just an empty example
public class MailThreads extends Feed{

    @Override
    public void getData(   Map<String, String> params,
                    Request request,
                    Session session,
                    User    user,
                    RenderArgs renderArgs) {
        List<LinkPair> ll = new LinkedList<LinkPair>();
        ObjectId uid = user.getId();
        List<MessageThread> r = MessageThread.getUserThreads(uid);
        if (r!= null)
            for (MessageThread m : r)
                if (m != null)
                {
                    String lnk = m.getIdString();
                    String txt = null;
                    if (m.getUsers() != null )
                        for (ObjectId oid : m.getUsers())
                            if (! uid.equals(oid)) {
                                txt = User.getNameForId(oid);
                                if (m.getUnreads() != null) {
                                    int un = 0;
                                    for (ObjectId uu : m.getUnreads())
                                        un += uid.equals(uu) ? 1 : 0;
                                    if (un > 0) // TODO toto treba zmenit
                                        txt += "(" + un + ")";
                                }
                            }
                    if (txt != null)
                        ll.add(new LinkPair(txt,lnk));
                }
        renderArgs.put(dataName, ll);
    }

    @Override
    public void init(Page page) {
        dataName = page.getBlocks().get(this.getClass().getCanonicalName());
    }

}
