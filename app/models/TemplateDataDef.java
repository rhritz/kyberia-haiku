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

// Template data set definition class
// this can be defined in the db, so that we don't have to code it by hand

public class TemplateDataDef {

    String name; // dataset name , like 'node ' (all node data) or 'mail' (user mail)
    String type; // collection or a String; do we need this?
    int count;
    int start;
    int order;

    // mozno by to mohlo byt dynamickejsie
    public static enum SET
    {
        MAIL,
        THREAD,
        NODE,
        USER
    }


    public static void create()
    {

    }

    // alebo vraciame List ?
    public static TemplateDataDef load(String id)
    {
        return null;
    }

    public static void save()
    {

    }

    public TemplateDataDef.SET getDatasetName()
    {
        return SET.MAIL;
    }
}
