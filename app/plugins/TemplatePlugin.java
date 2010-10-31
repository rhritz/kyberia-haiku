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

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.io.BufferedReader;
import java.io.FileReader;
import models.Page;
import models.ViewTemplate;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import static com.mongodb.util.JSON.parse;

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

        importFile("/mongojs/Page.js", Page.dbcol);
        importFile("/mongojs/ViewTemplate.js", ViewTemplate.dbcol);

        try {
            Page.start();
            ViewTemplate.start();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        }
    }

    @Override
    public void onApplicationStop() {
        
    }

    private void importFile(String fname, DBCollection col)
            throws UnexpectedException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader(
                    Play.applicationPath.getAbsolutePath() + fname ));
            String strLine;

            while ((strLine = reader.readLine()) != null)   {
                DBObject obj = (DBObject) parse(strLine);
                if (obj != null)
                    col.insert(obj);
            }
            reader.close();
        } catch (Exception e) {
            throw new UnexpectedException (e);
        } 
    }
    
}