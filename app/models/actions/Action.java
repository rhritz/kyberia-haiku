package models.actions;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Collections2;
import java.util.Map;
import models.MongoEntity;
import models.User;
import play.Logger;
import play.mvc.Http.Request;

public abstract class Action {

    private static Map<String,Action> actions = Maps.newHashMap();
    private String name;

    public abstract Boolean apply(
            Request request,
            User    user,
            Map<String, String> params
            );

    public static Boolean doAction(String acName,
            Request request,
            User    user,
            Map<String, String> params) {
        Action action = actions.get(acName.toLowerCase());
        if (action == null) {
            Logger.info("No Action found for action string:" + acName);
            return Boolean.FALSE;
        } else {
            Logger.info("Action found for action string:" + acName);
            return action.apply(request, user, params);
        }
    }

    // instantiate all Actions
    public static void start() {
        for (Class c : 
                Collections2.filter(MongoEntity.getClasses("models.actions"),
                    new Predicate<Class>() {
                    public boolean apply(Class t) {
                        return ! "models.actions.Action".equals(t.getName());
                    }
                })
            )
            actions.put(c.getSimpleName().toLowerCase(), getByName(c.getName()));
    }
    
    private static <T extends Action> T getByName(String name) {
        Logger.info("Action.getByName::" + name);
        T le = null;
        try {
            Class<T> fu = (Class<T>) Class.forName(name);
            le = fu.newInstance();
        } catch (Exception ex) {
            Logger.info("Action.getByName::" + name + ex.toString());
        }
        return le;
    }

    protected String getName() {
        return name;
    }

    private void setName(String newName) {
        name = newName;
    }

}
