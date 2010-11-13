package models.actions;

import java.util.Map;
import models.Bookmark;
import models.User;
import play.mvc.Http.Request;

public class Unbook extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        Bookmark.delete(params.get("id"), user.getIdString());
        return Boolean.TRUE;
    }

}
