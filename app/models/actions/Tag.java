package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.mvc.Http.Request;

public class Tag extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        String tag = params.get("tag");
        NodeContent node = (NodeContent) request.args.get("app-node");
        if (node != null) {
            models.Tag.tagNode(node,tag,user.getIdString());
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

}
