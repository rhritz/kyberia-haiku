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

import org.owasp.validator.html.*;
import play.Play;
import play.Logger;

// User-generated content validation with AntiSamy
public class Validator {

    private static String POLICY_FILE_LOCATION = "conf/antisamy-1.3.xml";
    private static AntiSamy as;
    // viac policies?
    private static Policy policy;

    static {
        if (Play.configuration.containsKey("validator.antisamy.path"))
            POLICY_FILE_LOCATION =
                    Play.configuration.getProperty("validator.antisamy.path");
        else
            POLICY_FILE_LOCATION =
                    Play.applicationPath.getAbsolutePath()+"/conf/antisamy-1.3.xml";
        Logger.info("Validator policy file is " + POLICY_FILE_LOCATION);
    }

    public static void start()
    {
        try {
            policy = Policy.getInstance(POLICY_FILE_LOCATION);
            as = new AntiSamy();
        } catch (PolicyException ex) {
            Logger.info(Validator.class.getName());
        }
    }

    // scanTxt pre nazvy atd / scanContent pre ostatne
    public static CleanResults scan(String input)
    {
        try {
            CleanResults cr = as.scan(input, policy);
            return cr;
        } catch (Exception ex) {
            Logger.info(Validator.class.getName());
        } 
        // vracat cr.getCleanHTML() ?
        return null; // + nastav error
    }
    /*
    The CleanResults object provides a lot of useful stuff.
getErrorMessages() - a list of String error messages
getCleanHTML() - the clean, safe HTML output
getCleanXMLDocumentFragment() - the clean, safe XMLDocumentFragment which is reflected in getCleanHTML()
getScanTime() - returns the scan time in seconds
*/
    public static void shutdown()
    {
     
    }
}
