package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Email {
    private final String created;
    private String modified;
    private int attempt;
    private final int id;
    private int uid;
    private final String addr;
    
    // CONSTRUCTOR
    
    public Email(String created, String modified, int attempt, int id, int uid, String addr) {
        this.created = created;
        this.modified = modified;
        this.attempt = attempt;
        this.id = id;
        this.uid = uid;
        this.addr = addr;
    }
    
    // GETTERS
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public int getAttempt() {
        return attempt;
    }
    
    public int getId() {
        return id;
    }
    
    public int getUserId() {
        return uid;
    }
    
    public String getAddress() {
        return addr;
    }
    
    // SETTERS
    
    public void setModified(String mod) {
        modified = mod;
    }
        
    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }
    
    public void setUserId(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | attempt: " + attempt + " | id: " + id + " | uid: " + uid + " | address: " + addr;
    }
}
