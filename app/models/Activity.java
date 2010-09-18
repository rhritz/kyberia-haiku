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
import com.mongodb.ObjectId;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import plugins.*;
import play.Logger;

// new nodes etc create new activity instances to notify bookmarks etc
// about these new events

@Entity("Activity")
public class Activity extends MongoEntity {
    
    private ObjectId         oid;  // o ktorom objekte je tato notifikacia
    private Integer        type; // enum? new, delete...? zatial neviem
    private Long           date;
    private String         name; // nazov prispevku, meno usera atd
                                 // pripadne popis aktivity?
    private List<ObjectId>   vector;
    // vecna otazka: String alebo oids?
    private List<ObjectId>   ids;  // idcka nodov ktore notifikujeme
    private List<ObjectId>   uids; // userov ktorych notifikujeme, skrze friend/follow
    private ObjectId         parid; // user submission children - len ak parowner != owner
    private ObjectId         owner;
    
    public Activity () {}

    public Activity (ObjectId oid,
                     Long date,
                     List<ObjectId> ids,
                     List<ObjectId> uids,
                     List<ObjectId> vector,
                     ObjectId parid,
                     ObjectId owner)
    {
        this.oid    = oid;
        this.date   = date;
        this.ids    = ids;
        this.uids   = uids;
        this.vector = vector;
        this.parid  = parid;
        this.owner  = owner;
    }
    
    public static void newNodeActivity(
            ObjectId id,
            NodeContent node,
            List<ObjectId> parents,
            ObjectId ownerid,
            ObjectId parOwnerId
            )
    {
        try {
            Logger.info("@newNodeActivity");
            List<ObjectId> friends = User.load(ownerid).getFriends();
            User parent = User.load(parOwnerId);
            if (parent != null) {
                parOwnerId = parent.getId();
            }
            // parents==vektor sucasne aj (ak mame vsetkych)
            Activity notif = new Activity(id, System.currentTimeMillis(),
                    parents, friends, parents, parOwnerId,node.getOwner());
            notif.save();

            // lame - prerobit!
            SortedSet<UserLocation> uso = UserLocation.getAll();
            if (uso == null)
                return;
            HashMap<String,Integer> usersOnline = new HashMap<String,Integer>();
            for (UserLocation u : uso)
                usersOnline.put(u.getUserid(), 1);
            // update bookmarks for users online
            for (ObjectId par : parents)
                for (Bookmark b : Bookmark.getByDest(par)) 
                    if (usersOnline.containsKey(b.getUid().toString())) 
                        Bookmark.updateVisit(b.getUid(),par.toString());
        } catch (Exception ex) {
            Logger.info("newNodeActivity failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    public void save()
    {
        try {
            MongoDB.save(this, MongoDB.CActivity);
        } catch (Exception ex) {
            Logger.info("activity save failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    /**
     * @return the oid
     */
    public ObjectId getOid() {
        return oid;
    }

}

