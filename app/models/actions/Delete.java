
package models.actions;

import java.util.Map;
import models.NodeContent;
import models.User;
import play.mvc.Http.Request;

public class Delete extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        NodeContent node = (NodeContent) request.args.get("app-node");
        if (node != null && node.owner.equals(user.getId())) {
            node.deleteNode();
        } else {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
