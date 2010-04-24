/*
    Kyberia Haiku - advanced community web application
    Copyright (C) 2010 Robert Hritz

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package controllers;

import models.Haiku;
import play.mvc.*;

// TODO zmenit Secure login stranku - pridat linku sem
public class Registration extends Controller {

    public static void addUser(String username, String password) {
        Haiku h = new Haiku();
        long userid = h.addAnyNode(
                Haiku.NodeType.USER,
                Controller.params.allSimple(),
                0,0,null);
        // TODO ak bola registracia uspesna - zobraz profil
        // ak nebola - zobraz preco
        renderArgs.put("user", h.viewUser(userid));
        renderArgs.put("content", "zatial nic");
        render("app/views/Application/viewUser.html");
    }

    // TODO tu by sme mali kontrolovat ci nie je prihlaseny 
    public static void showAddUser() {
        render("app/views/Application/addUser.html");
    }

}
