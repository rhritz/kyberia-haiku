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

import com.mongodb.ObjectId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import play.Logger;
import plugins.*;

public class Haiku {
    
    public static final String LASTID   = "lastid";
    // node property / relation names
    public static final String CONTENT  = "content";
    public static final String CREATED  = "created";
    public static final String TEMPLATE = "template";
    public static final String USERNAME = "username";
    public static final String GROUPNAME = "username";
    public static final String NAME     = "name";
    public static final String TYPE     = "type";
    public static final String PASSWORD = "password";
    public static final String IS_SOURCE   = "is_source";
    public static final String IS_SINK     = "is_sink";
    public static final String ACCESS_TYPE = "access_type";
    public static final String MONGOID  = "mongoid";

    
    // TODO dynamicke relations -  DynamicRelationshipType.withName("KNOWS")'
    public static enum Rel
    {
        BOOK,
        DFS,
        FOOK,
        FRIEND,
        IGNORE,
        K,
        OWNER,
        REACTION,
        TAG,
        GROUP_OWNER,
        GROUP_MEMBER,
        // perm related:
        ACCESS,
        BAN,
        SILENCE
    }
    // permission rels: MASTER, OP - mozu tiez ukazovta na grupu
    // pri vytvoreni noveho prispevku sa by default vytvori linka na usergrupy
    // parenta
    // potom pri zmene prav 'podriadeneho' prispevku
    // sa len skopiruje obsah grupy a prida/odoberie novy obsah z nej
    // - alebo dedenie grup, ale to je na dlhsie

    public static enum NodeType
    {
        USER,
        CONTENT,
        USERGROUP,
        BOOKMARK,
        SPECIAL
    }

    public static enum AccessType
    {
        PUBLIC,
        MODERATED,
        PRIVATE
    }
}