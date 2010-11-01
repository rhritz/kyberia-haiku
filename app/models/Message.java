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
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import java.util.List;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import plugins.*;
import play.Logger;

@Entity
public class Message extends MongoEntity {

    public static DBCollection dbcol = null;
    private static final String key  = "message_";
    
    private String content;
    private Long sent;
    public ObjectId from;
    public ObjectId to;
    private ObjectId thread;
    private List <ObjectId> deleted;

    @Transient
    public String fromUser;
    @Transient
    public String toUser;
    @Transient
    public String datetime;

    // -> plugin?
    private static DateTimeFormatter dateFormatter =
            DateTimeFormat.forPattern("dd.MM.YYYY - HH:mm:ss");

    public Message() {}

    public Message( String content,
                    ObjectId from,
                    ObjectId to,
                    ObjectId thread)
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
            m = MongoDB.load(id, MongoDB.CMessage, Message.class);
            if ( m != null)
                m.enhance();
        } catch (Exception e) {
            Logger.info(e.toString());
        }
        return m;
    }

    // TODO nezobrazovat spravu tym ktori si spravu deletli
    public void delete(String uid)
    {
        deleted.add(toId(uid));
        save();
    }

    public static void send(
            String fromIdStr,
            String toIdStr,
            String content
            )
    {
        ObjectId fromId = toId(fromIdStr);
        ObjectId toId = toId(toIdStr);
        MessageThread mt = MessageThread.getThread(fromId, toId, false);
        if (mt == null)
            mt = MessageThread.create(fromId, toId);
        if (mt == null) {
            Logger.error("Unable to find/create message thread");
            return;
        }
        Message m = new Message(Validator.validateTextonly(content),
                fromId, toId, mt.getId());
        MongoDB.save(m);
        mt.notify(fromId,toId);
    }

    @Override
    public Message enhance() {
        fromUser = User.getNameForId(from);
        toUser   = User.getNameForId(to);
        datetime = dateFormatter.print(sent);
        return this;
    }

    @Override
    public DBCollection getCollection() {
        return dbcol;
    }

    @Override
    public String key() {
        return key;
    }

}