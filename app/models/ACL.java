package models;

public class ACL {

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
