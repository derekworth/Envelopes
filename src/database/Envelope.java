package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Envelope {
    private final String created;
    private String modified;
    private boolean enabled;
    private final int id;
    private int cid;
    private String name;
    private double amt;
    
    // CONSTRUCTORS
    
    public Envelope(String created, String modified, boolean enabled, int id, int cid, String name, double amt) {
        this.created = created;
        this.modified = modified;
        this.enabled = enabled;
        this.id = id;
        this.cid = cid;
        this.name = name;
        this.amt = amt;
    }
    
    // GETTERS
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getId() {
        return id;
    }
    
    public int getCategoryId() {
        return cid;
    }
    
    public String getName() {
        return name;
    }
    
    public double getAmount() {
        return amt;
    }
    
    // SETTERS
        
    public void setEnabled(boolean en) {
        enabled = en;
    }
    
    public void setModified(String newModified) {
        modified = newModified;
    }
    
    public void setCategoryId(int cid) {
        this.cid = cid;
    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    public void setAmount(double newAmt) {
        amt = newAmt;
    }

    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | cid: " + cid + " | name: " + name + " | amount: " + amt;
    }
}
