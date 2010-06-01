package models;

import java.io.Serializable;
import com.google.code.morphia.annotations.Id;
import com.mongodb.ObjectId;

public abstract class MongoEntity implements Serializable {
    @Id protected ObjectId id;

    public MongoEntity() {}

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getIdString() {
        return id.toString();
    }

    public void setIdString(String id) {
        this.id = new ObjectId(id);
    }
}
