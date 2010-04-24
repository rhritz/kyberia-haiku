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
package plugins;

import play.PlayPlugin;
import play.exceptions.UnexpectedException;

public class ValidatorPlugin extends PlayPlugin {
    
    @Override
    public void onApplicationStart() {
        try {
            Validator.start();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }

    @Override
    public void onApplicationStop() {
        try {
            Validator.shutdown();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }

}