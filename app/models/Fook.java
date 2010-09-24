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

import com.google.common.base.Functions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.ObjectId;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.cache.Cache;
import plugins.MongoDB;

public class Fook {

    private ObjectId uid; // new ObjectId(id)
    private ObjectId nodeid;
    private Long     date;

    private static int MAX_VECTOR_LENGTH = 1000; // vsetko ma svoje hranice

    public Fook () {}

    // zisti ci nieco z daneho vektoru nie je fooknute tymto userom
    // TODO userove fooky si mame ulozene v cache.. alebo nie
    // TODO to znamena, ze k-list bude musiet obsahovat aj vektory nodov
    public static void loadHierarchicalRights(ObjectId oid) // List<ObjectId> nodes,
    {
        ObjectId nid = oid;
        // iterujeme smerom hore, ale fooky/prip. permissions chceme kumulovat
        // smerom dole
        // cize budeme chicet, aby nam .load*(oid) vratil uz "loaded" node
        // ale pokial mozno bez rekurzie...
        // tj loadneme cely vektor a potom smerom dole to pozbierame
        List<NodeContent> vector = new LinkedList<NodeContent>();
        for (int i = 0; i < MAX_VECTOR_LENGTH; i++) {
            NodeContent nc = NodeContent.load(nid);
            vector.add(nc);
            if (nc.getParent() == null) {
                break;
            }
            nid = nc.getParent();
            // tu este pozerajme ci uz sme nespravili cast prace predtym:
            // ? uu = Cache.get("vector_" + nid, ?.class);
            // if (uu != null) {
            //    vector.add(uu); break; }
        }
        // v tomto bode mame kompletny vektor; ideme od konca a vyhodnocujeme
        // kazdy vysledok ukladame do cache ak by niekto potreboval medzikroky
        // - permissions
        // - fooks
        HashMap<ObjectId,Boolean>  bans   = new HashMap<ObjectId,Boolean>();
        HashMap<ObjectId,Boolean>  access = new HashMap<ObjectId,Boolean>();
        HashMap<ObjectId,Boolean>  silence = new HashMap<ObjectId,Boolean>();
        // mastri by nemali editovat nody pod sebou, iba ich presuvat...
        HashMap<ObjectId,Boolean>  masters = new HashMap<ObjectId,Boolean>();
        HashMap<ObjectId,Boolean>  fooks = new HashMap<ObjectId,Boolean>();
        // navyse si musime sledovat accType a doriesit co ak sa zmeni
        // + resolvovat konflikty - co ked je niekto aj owner a vyssie ma ban
        // atd
        for (NodeContent nc: Iterables.reverse(vector)) {
            // poznacit si ci prichadzame uplne zhora - ak nie, load tohto z cache ako pociatocny stav
            for (ObjectId ban : nc.getBans())
                bans.put(ban, true);
            for (ObjectId acc : nc.getAccess())
                access.put(acc, true);
            for (ObjectId sil : nc.getSilence())
                silence.put(sil, true);
            for (ObjectId master : nc.getMasters())
                masters.put(master, true);
            masters.put(nc.owner, Boolean.TRUE); //  a podobne
            fooks.putAll(nc.getFook());

            Cache.set("bans_" + nc.getIdString(), bans.clone());
            Cache.set("access_" + nc.getIdString(), access.clone());
            Cache.set("silence_" + nc.getIdString(), silence.clone());
            Cache.set("master_" + nc.getIdString(), masters.clone());
            Cache.set("fook_" + nc.getIdString(), fooks.clone());
            // otazka je ci toto nerozbit uplne, tj "fook_" + nodeId + "_" + userId
            // co nam vyrobi asi milion klucov + ked tam kluc nie je, nevieme ci chyba alebo tam nema byt
            // ale pristup by zase bol asi rychlejsi
        }

        return;

        // na konci tohto mame co sme chceli a mozeme to pouzivat <- Cache.get
        // len netreba zabudnut na updaty/invlaidate pri zmenach
    }

    public boolean isFook(NodeContent node, ObjectId userId)
    {
        List<NodeContent> vector = node.loadVector();
        return false;
    }
    
}
