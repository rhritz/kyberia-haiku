package models.actions;

import java.util.Map;
import models.Bookmark;
import models.User;
import play.mvc.Http.Request;

public class Book extends Action {

    @Override
    public Boolean apply(Request request, User user, Map<String, String> params) {
        Bookmark.add(User.toId(params.get("id")), user, "ids");
        return Boolean.TRUE;
    }

}
