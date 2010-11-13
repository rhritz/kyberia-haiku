package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.mvc.Http.Request;

public class Unput extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        NodeContent node = (NodeContent) request.args.get("app-node");
        if (node != null && node.canWrite(user.getId())) {
            node.unputNode();
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

}
