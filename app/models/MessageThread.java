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
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoValue;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import plugins.*;
import play.Logger;
import play.cache.Cache;

@MongoDocument
public class MessageThread extends AbstractMongoEntity {

    // { _id: '...', users: [user1, user2...]}
    // @Indexed
    private List<String> users;
    private List<String> unreads;
    private List<String> deleted;

    public static final String USERS   = "users";
    public static final String UNREAD  = "unreads";
    public static final String LAST    = "last";

    public MessageThread() {}

    public MessageThread(String from, 
                         String to)
    {
        users = new LinkedList<String>();
        users.add(from);
        users.add(to);
    }

    // create a new mailthread if it doesn't already exist
    public static MessageThread create(String from, String to)
    {
        String c = getThreadId(from, to);
        if (c != null)
        {
            return null;
        }
        try {
            Logger.info("creating new thread");
            MessageThread m = new MessageThread(from,to);
            MongoDB.save(m, MongoDB.CMessageThread);
            // TODO teraz chceme vratit jeho id - snad sa to da aj krajsie,
            // bohuzial java driver nepozna last_insert_id, zatial teda takto
            return getThread(from, to, false);
        } catch (Exception ex) {
            Logger.info("create failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return null;
    }

    // loadne vsetky thready usera, zoradi podla poctu neprecitanych sprav
    public static List<MessageThread> getUserThreads(String uid)
    {
        // TODO limitneme to na 30, treba potom pridat moznost zobrazit vsetko
        List<MessageThread> r = null;
        try {
            BasicDBObject query = new BasicDBObject().append(USERS, uid);
            BasicDBObject sort = new BasicDBObject().append(LAST, 1);
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CMessageThread).
                    find(query).sort(sort).limit(30);
            if (iobj ==  null) {
                r = new ArrayList<MessageThread>();
            } else {
                Logger.info("user threads found");
                r = Lists.transform(iobj.toArray(),
                        MongoDB.getSelf().toMessageThread());
            }
        } catch (Exception ex) {
            Logger.info("getUserThreads");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return r;
    }

    public static String viewUserThreads(String uid,
            play.mvc.Scope.Session session)
    {
        List<MessageThread> r = getUserThreads(uid);
        StringBuilder ret = new StringBuilder();
        // LAME - prerobit!
        boolean i = true;
        if (r!= null) {
            for (MessageThread m : r)
            {
                if (m != null)
                {
                    if (i) {session.put("lastThreadId", m.id);i=false;}
                    ret.append("<a href=/mail/").append(m.id).append(">");
                    if (m.users != null )
                    {
                        for (String oid : m.users) {
                            if (! uid.equals(oid)) {
                                ret.append(User.getNameForId(oid) );
                                if (m.unreads != null) {
                                    int un = 0;
                                    for (String uu : m.unreads) {
                                        // toto je pocet nami neprecitanych
                                        // v tomto threade
                                        un += uid.equals(uu) ? 1 : 0;
                                    }
                                    if (un > 0)
                                        ret.append("(" + un + ")");
                                }
                            }
                        }
                    }
                    ret.append("</a>").append("<br>");
                }
            }
        }
        return ret.toString();
    }

    // vratime thread 
    // doRead - oznacime neprecitane posty v nom ako precitane
    // pre aktualneho usera
    public static MessageThread getThread(String forUser, 
            String otherUser,
            boolean doRead)
    {
        Logger.info("Trying to find thread for " + forUser + " & " + otherUser);
        try {
            BasicDBObject query = new BasicDBObject().append(USERS,
                    new BasicDBObject("$all",new String[]{forUser,otherUser}) );
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CMessageThread).findOne(query);
            if (iobj !=  null) {
                MessageThread m = MongoDB.getMorphia().
                        fromDBObject(MessageThread.class, iobj);
                Logger.info("thread found: " + m.getId());
                if (doRead) {
                    // oznacime posty za precitane pre forUser
                    LinkedList<String> lr = new LinkedList<String>();
                    for (String s : m.unreads) {
                        if (s.equals(forUser)) {
                            lr.add(s);
                        }
                    }
                    for (String s : lr) {
                            m.unreads.remove(s);
                    }
                    MongoDB.update(m, MongoDB.CMessageThread);
                }
                return m;
            }
        } catch (Exception ex) {
            Logger.info("getThread failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return null;
    }

    // tu potrebujeme len thread id, takze mozeme veselo cachovat
    public static String getThreadId(String from, String to)
    {
        String mt = Cache.get("message_thread-"+from+"-"+to, String.class);
        if (mt == null) {
            MessageThread m = getThread(from, to, false);
            if (m != null) {
                Cache.add("message_thread-"+from+"-"+to, m.getId());
                return m.getId();
            }
        }
        return null;
    }

    // notifikuj thread o pridani noveho postu
    public void notify(String from, String to)
    {
        unreads.add(to);
        MongoDB.save(this, MongoDB.CMessageThread);
    }

    // oznac thread ako precitany danym userom
    public static void setAsRead(String threadId, String forUser) {
        try {
            BasicDBObject iobj = (BasicDBObject) MongoDB.getDB().
                    getCollection(MongoDB.CMessageThread).
                    findOne(new BasicDBObject().
                    append("_id", new ObjectId(threadId)));
            if (iobj !=  null) {
                MessageThread m = MongoDB.getMorphia().
                        fromDBObject(MessageThread.class, iobj);
                LinkedList<String> lr = new LinkedList<String>();
                for (String s : m.unreads) {
                    if (s.equals(forUser)) {
                        lr.add(s);
                    }
                }
                for (String s : lr) {
                        m.unreads.remove(s);
                }
                MongoDB.update(m, MongoDB.CMessageThread);
            }
        } catch (Exception ex) {
            Logger.info("setRead failed:");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
    }

    // utility pre meno druheho usera v threade
    private static String getOtherUser(List<String> users, String uid )
    {
        for (String u : users)
            if (! uid.equals(u))
                return User.getNameForId(u);
        // nieco je zle
        return "";
    }

    public static List<String> checkUnreadMail(String uid)
    {
        List<String> ll = new LinkedList<String>();
        try {
            // uniq alebo nam toto nebude davat viacnasobne viackrat?
            BasicDBObject query = new BasicDBObject().append(UNREAD, uid) ;
            DBCursor iobj = MongoDB.getDB().
                    getCollection(MongoDB.CMessageThread).find(query);
            if (iobj !=  null) {
                while(iobj.hasNext())
                {
                    MessageThread mt = MongoDB.getMorphia().
                           fromDBObject(MessageThread.class,
                           (BasicDBObject) iobj.next());
                    ll.add("New mail from " + getOtherUser(mt.users,uid));
        // TODO - pre linkovanie daneho threadu zo zahlavia
        // toto si pyta reverznu route na Application.showMail(threadId),
        // takze treba class MailNotif{String from, String threadid} a je to
                }
            }
        } catch (Exception ex) {
            Logger.info("checkUnreadMail");
            ex.printStackTrace();
            Logger.info(ex.toString());
        }
        return ll;
    }

    // provizorne lesenie
    public static String getUnreadMailNotif(String uid) {
        StringBuilder ret = new StringBuilder();
        for (String s : checkUnreadMail(uid))
        {
            ret.append(s).append("<br>");
        }
        return ret.toString();
    }

    // TODO nezobrazovat tym ktori si spravu deletli
    // TODO asi tento flag by mal byt v Message a nie tu
    public void delete(String uid)
    {
        deleted.add(uid);
        MongoDB.save(this, MongoDB.CMessageThread);
    }

}
