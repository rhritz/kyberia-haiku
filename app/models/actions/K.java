package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.mvc.Http.Request;

public class K extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        NodeContent node = (NodeContent) request.args.get("app-node");
        node.giveK(user);
        return Boolean.TRUE;
    }

}
