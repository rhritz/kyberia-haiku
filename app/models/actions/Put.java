package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.mvc.Http.Request;

public class Put extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        NodeContent node = (NodeContent) request.args.get("app-node");
        NodeContent toNode = NodeContent.load(params.get("to"), user);
        if (node != null && toNode != null && node.canWrite(user.getId())) {
            node.putNode(toNode.getId());
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

}
