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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import play.Logger;
import play.Play;

public class Neo {

    private static GraphDatabaseService graph;
    private static String DB_PATH = "data";

    static {
        if (Play.configuration.containsKey("play.neo.path"))
            DB_PATH = Play.configuration.getProperty("play.neo.path");
        else
            DB_PATH = Play.applicationPath.getAbsolutePath()+"/data/";
        Logger.trace("Neo db is in " + DB_PATH);
    }

    public static void start()
    {
        Logger.info("Neo starting");
        graph = new EmbeddedGraphDatabase( DB_PATH );
        // start indexService here too
        registerShutdownHook();
    }

    public static void shutdown()
    {
        Logger.info("Neo shutting down");
        graph.shutdown();
        // indexService.shutdown();
    }

    public static GraphDatabaseService getGraph()
    {
        return graph;
    }

    private static void registerShutdownHook()
    {
        // Registers a shutdown hook for the Neo4j and index service instances
        // so that it shuts down nicely when the VM exits (even if you
        // "Ctrl-C" the running example before it's completed)
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                shutdown();
            }
        } );
    }
      
}