
package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.Logger;
import play.mvc.Http.Request;


public class Add extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        // TODO nenastavujeme dobreho parenta zeby?
        String id = params.get("id");
        String content = params.get("content");
        Logger.info("about to add node:" + id + "," + content );
        NodeContent parentNode = (NodeContent) request.args.get("app-node");
        String newId = NodeContent.addNode(
                    parentNode == null ? null : parentNode.getId(),
                    params,
                    user.getId()
                );
        String nid = null;
        Logger.info("newid::" + newId);
        if (id == null) {
            NodeContent newNode = NodeContent.load(newId,user);
            nid = newNode.getIdString();
            // renderArgs.put("node", newNode);
            request.args.put("app-node", newNode);
        } else {
            // nid = id;
        }
        // displayNode(nid);
        return Boolean.TRUE;
    }

}
