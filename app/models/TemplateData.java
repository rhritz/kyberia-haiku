package models;

import java.util.List;

public interface TemplateData<T> {

    public T       getOne();
    public List<T> getList();

    /*
     * public class Communicate  {
          public <T extends Speaks> void speak(T speaker) {
            speaker.speak();
          }
        }
     */

}
