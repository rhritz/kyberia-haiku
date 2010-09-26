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

import com.mongodb.DBObject;
import java.io.BufferedReader;
import java.io.FileReader;
import models.Page;
import models.ViewTemplate;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

/*
 load Pages & Views
 if Pages are missing, populate the collection with the contents of a init file.
 such file can be produced with the following command:
 ./mongoexport -d local -c Page
 where 'local' is the db name
*/

public class TemplatePlugin extends PlayPlugin {
    @Override
    public void onApplicationStart() {
        try {
            Page.start();
            ViewTemplate.start();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }

        try {
            BufferedReader reader = new BufferedReader( new
                    FileReader(Play.applicationPath.getAbsolutePath()
                    + "/mongojs/Page.js"));
            String strLine;
        
            while ((strLine = reader.readLine()) != null)   {
                DBObject page = (DBObject) com.mongodb.util.JSON.parse(strLine);
                if (page != null)
                    MongoDB.getDB().getCollection(MongoDB.CPage).insert(page);
            }
            reader.close();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }

    @Override
    public void onApplicationStop() {
        
    }
    
}