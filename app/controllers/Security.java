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

import models.*;

public class Security extends Secure.Security {

    static boolean authentify(String username, String password) {
        User u = User.login(username, password);
        if (u != null) {
            session.put(User.ID,     u.getIdString());
            return true;
        } else {
            return false;
        }
    }

    // toto by mal byt redirect na index
    // TODO redirectovat tam kam chcel ist povodne
    static void onAuthenticated() {
        Application.index();
    }

}
