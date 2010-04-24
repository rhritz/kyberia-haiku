package models;

/*
 Toto by nam mohlo ulahcit posuvanie veci typu nazov-linka z modelov do templaty
 napriklad username-linka na usera, notfi noveho mailu -linka na thread a pod.

 treba to este domysleit ale
 + pripadne pridat veci ako linka na ikonku a tak
 predovsekym urcene na iterovanie
 */
public class LinkPair {

    public String name;
    public String link;

    public LinkPair(String name, String link)
    {
        this.name = name;
        this.link = link;
    }
}
