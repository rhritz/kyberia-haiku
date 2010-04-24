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
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import plugins.*;
import play.Logger;

@MongoDocument
public class Message extends AbstractMongoEntity {
    private String content;
    private Long sent;
    private String from;
    private String to;
    private String thread;
    private List <String> deleted;
    // TODO String<>ObjectID?

    public Message() {}

    public Message( String content,
                    String from,
                    String to,
                    String thread)
    {
        this.content = content;
        this.from    = from;
        this.to      = to;
        this.sent    = System.currentTimeMillis();
        this.thread  = thread;
    }


    public static Message load(String id)
    {
        Message m = null;
        try {
            m = (Message) MongoDB.load(id, MongoDB.CMessage);
        } catch (Exception e) {
            Logger.info(e.toString());
        }
        return m;
    }

    // TODO nezobrazovat spravu tym ktori si spravu deletli
    public void delete(String uid)
    {
        deleted.add(uid);
        MongoDB.save(this, MongoDB.CMessage);
    }

    public static void send(
            String fromId,
            String toId,
            String content
            )
    {
        MessageThread mt = MessageThread.getThread(fromId, toId, false);
        if (mt == null)
        {
            mt = MessageThread.create(fromId, toId);
        }
        if (mt == null)
        {
            Logger.info("Unable to find/create thread, bailing");
            return;
        }
        Message m = new Message(content, fromId, toId, mt.getId());
        MongoDB.save(m, MongoDB.CMessage);
        mt.notify(fromId,toId);
    }

    // list of last messages from a mailthread
    // TODO errorchecking
    public static List<Message> getLastMessages (
            String threadId,
            boolean doUpdate,
            String forUser,
            Integer start,
            Integer count)
    {
        BasicDBObject query = new BasicDBObject().append("thread", threadId);
        BasicDBObject sort  = new BasicDBObject().append("sent", -1);
        if (start == null) start = 0;
        if (count == null) count = 30;
        DBCursor iobj = MongoDB.getDB()
            .getCollection(MongoDB.CMessage).find(query).sort(sort).skip(start).
            limit(count);
        if (doUpdate)
            MessageThread.setAsRead(threadId, forUser);
        // Hilarity ensues :)
        return Lists.transform(iobj.toArray(), MongoDB.getSelf().toMessage());
    }

    public static List<Message> getMessages(String threadId,
            String forUser)
    {
        return getLastMessages(threadId, true, forUser, 0, 30);
    }

}