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

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.Morphia;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@Entity("Vote")
public class Vote extends MongoEntity {

    private List<Option> options; // TODO v pohode embedded?
    private List<String> votes;
    private String name;
    private String description;
    private String owner; // nie nutne
    // private int type;
    @Transient
    private Integer totalVotes;

    public Vote() {}

    public Vote(String name,
                String owner,
                String description,
                List<String> optNames) {
        this.name = name;
        this.owner = owner;
        this.description = description;
        options = new ArrayList<Option>(optNames.size());
        for (String opt : optNames)
            options.add(new Option(opt));
    }

    public static Vote create(  String name,
                                String owner,
                                String description,
                                List<String> optNames) {
        Vote v = new Vote(owner, name, description, optNames);
        v.save();
        return v;
    }
    
    // zatial asi vsetky?
    public static List<Vote> listVotes() {
        return null;
    }

    public void addOption(String name) {
        // stub
    }

    public void removeOption(String name) {
        // ? stub
    }

    public static Vote load(String id) {
        Vote v = null;
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CVote).
                    findOne(new BasicDBObject().append("_id",new ObjectId(id)));
            if (iobj != null)
                v = (Vote) MongoDB.getMorphia().
                        fromDBObject(Vote.class, (BasicDBObject) iobj);
        } catch (Exception ex) {
            Logger.info("user load fail");
            ex.printStackTrace();
            Logger.info(ex.toString());
            return null;
        }
        return v;
    }

    public void save()
    {
        try {
             MongoDB.save(this,MongoDB.CVote);
        } catch (Exception ex) {
            Logger.info(ex.toString());
        }
    }

    public void update()
    {
        try {
             MongoDB.update(this,MongoDB.CVote);
        } catch (Exception ex) {
            Logger.info(ex.toString());
        }
    }

    // true.. ok, false.. already voted
    public boolean vote(User u, Integer optid) {
        boolean suc = false;
        if (hasVoted(u))
            return false;
        votes.add(u.getIdString());
        Option o = options.get(optid);
        if (o != null ) {
            suc = o.vote();
        }
        if (suc) {
            totalVotes++;
            update();
         }
        return suc;
    }

    // pripadne votes ako hash?
    public boolean hasVoted(User u) {
        for (String s: votes)
            if (s.equals(u.getIdString()))
                return true;
        return false;
    }

    public Vote showResults() {
        // napln transient fields... alebo co.
        return this;
    }

    public class Option {
        private String name;
        
        private Integer numVotes;
        @Transient
        private List<String> results;
        @Transient
        private Integer percentage;

        public Option() {}

        public Option(String name) {
            this.name = name;
        }

        private boolean vote() {
            boolean suc = false;
            numVotes++;
            return suc;
        }
    }
}
