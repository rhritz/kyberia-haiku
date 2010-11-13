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
package models;

import java.io.Serializable;
import com.google.code.morphia.annotations.Id;
import com.mongodb.DBCollection;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.bson.types.ObjectId;
import play.Logger;
import play.cache.Cache;
import plugins.MongoDB;

public abstract class MongoEntity implements Serializable {
    @Id protected ObjectId id;

    public MongoEntity() {}

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getIdString() {
        return id.toString();
    }

    public void setIdString(String id) {
        this.id = new ObjectId(id);
    }

    public abstract <T extends MongoEntity> T enhance();
    public abstract DBCollection getCollection();
    public abstract String key();

    // TODO error handling
    public static ObjectId toId(String x) {
        ObjectId bubu = null;
        try { bubu = new ObjectId(x);} catch (Exception e ) {};
        return bubu;
    }

    public void save() {
        MongoDB.save(this);
    }

    public void save(boolean doCache, String cacheTime) {
        MongoDB.save(this);
        if (doCache) 
            Cache.replace(key() + getIdString(), this, cacheTime);
    }

    public void save(boolean doCache) {
        MongoDB.save(this);
        if (doCache)
            Cache.replace(key() + getIdString(), this);
    }

    public void update() {
        MongoDB.update(this);
    }

    public void update(boolean doCache, String cacheTime) {
        MongoDB.update(this);
        if (doCache)
            Cache.replace(key() + getIdString(), this, cacheTime);
    }

    public void update(boolean doCache) {
        MongoDB.update(this);
        if (doCache)
            Cache.replace(key() + getIdString(), this);
    }


    /*
    Dirty hacks ftw!
    list Java files (not classes, since they are not going to be there)
    inside a given package directory, and use them as a list of available feeds
    adapted from Jon Peck http://jonpeck.com
    adapted from http://www.javaworld.com/javaworld/javatips/jw-javatip113.html
    */
    public static List<Class> getClasses(String pckgname) {
        List<Class> classes=new LinkedList<Class>();
        try {
          File directory=new File(Thread.currentThread().getContextClassLoader()
                  .getResource('/'+pckgname.replace('.', '/')).getFile());
            if(directory.exists()) {
              String[] files=directory.list();
              for( int i=0 ; i < files.length; i++)
                if(files[i].endsWith(".java"))
                  classes.add(Class.forName(pckgname+'.'+
                    files[i].substring(0, files[i].length()-5)));
            } else
              Logger.info(pckgname + " does not appear to be a valid package");
        } catch(Exception x) {
            Logger.info(pckgname + " does not appear to be a valid package");
            x.printStackTrace();
            Logger.info(x.toString());
        }
        return classes;
      }

}
