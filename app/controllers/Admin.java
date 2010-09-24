package controllers;

import play.mvc.*;
import models.User;
import models.ViewTemplate;
import play.mvc.Controller;

@With(Secure.class)
public class Admin  extends Controller {

    public static void showAdminPage() {
        render(ViewTemplate.ADMIN_PAGE_HTML);
    }

    public static void doAdminStuff() {
        User.refreshDailyK(30);
        showAdminPage();
    }

}
