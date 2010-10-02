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
import org.bson.types.ObjectId;
import java.util.LinkedList;
import java.util.List;

@Entity
public class StatusUpdate extends MongoEntity {

    private ObjectId uid;
    private String   content;
    private Long     date;

    public StatusUpdate() {}

    public StatusUpdate(ObjectId uid, String content, Long date)
    {
        this.uid = uid;
        this.content = content;
        this.date = date;
    }

    public void save()
    {

    }

    public static StatusUpdate load()
    {
        return null;
    }

    public static List<StatusUpdate> getUserStream(ObjectId uid)
    {
        List<StatusUpdate> lu = new LinkedList<StatusUpdate>();
        return lu;
    }

    public static List<StatusUpdate> getFriendStreams(List<ObjectId> friends)
    {
        List<StatusUpdate> lu = new LinkedList<StatusUpdate>();
        return lu;
    }
}
