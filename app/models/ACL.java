package models;

public class ACL {

    public static final int PUBLIC    = 0;
    public static final int PRIVATE   = 1;
    public static final int MODERATED = 2;

    // enum Permissions <- Haiku.java

    public static boolean canView(String userid)
    {
        return true;
    }

    public static boolean canEdit(String userid)
    {
        return true;
    }

    public static void addPermission(String userid, String wat)
    {
        
    }

}
