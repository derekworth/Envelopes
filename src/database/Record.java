package database;

/**
 * Created on 10/29/2016
 * @author Derek
 */
public class Record {
    private final String created;
    private String modified;
    private final int id;
    
    public Record(String created, String modified, int id) {
        this.created = created;
        this.modified = modified;
        this.id = id;
    }
    
    // GETTERS
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public int getId() {
        return id;
    }
    
    // SETTERS
    
    public void setModified(String mod) {
        modified = mod;
    }
    
}
