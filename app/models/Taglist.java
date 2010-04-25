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

import com.google.code.morphia.AbstractMongoEntity;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoTransient;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import play.cache.Cache;
import play.Logger;
import plugins.MongoDB;


@MongoDocument
public class Taglist extends AbstractMongoEntity {

    public static void tagNode(NodeContent nc, String tag, String uid) {
        nc.addTag(tag);
        nc.update();
        // TagNodeUser.add(tag,nodeid,uid)
        // Tag.add(tag)
        // TODO poznacit si asi aj kto ten tag dal
        // - bud v samostatnej kolekcii, alebo v zozname tagov
        // pre kzdy zoanam aj zoznam userov ktroi ho dali
    }
    /*
     pridavanie tagov,
     zobrazovanie tagov,
     zobrazovanie contentu podla tagov
     */

    public static void add()
    {

    }

    public static void load()
    {
        
    }
}
