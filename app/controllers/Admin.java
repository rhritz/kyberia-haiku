package controllers;

import play.mvc.*;
import models.User;
import models.ViewTemplate;
import play.Logger;
import play.mvc.Controller;

@With(Secure.class)
public class Admin  extends Controller {

    public static void showAdminPage() {
        renderArgs.put("reqstart", System.currentTimeMillis());
        render(ViewTemplate.ADMIN_PAGE_HTML);
    }

    @After
    static void measureTime()
    {
        Long start    = renderArgs.get("reqstart", Long.class);
        Long duration = System.currentTimeMillis() - start;
        Logger.info("Request duration " + duration);
    }

    public static void doAdminStuff() {
        renderArgs.put("reqstart", System.currentTimeMillis());
        User.refreshDailyK(30);
        showAdminPage();
    }

}
