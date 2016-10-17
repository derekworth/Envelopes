package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public final class Category {
    private final String created;
    private String modified;
    private boolean enabled;
    private final int id;
    private String name;
    private double amt;
    
    // CONSTRUCTORS
    
    public Category(String created, String modified, boolean enabled, int id, String name) {
        this.created = created;
        this.modified = modified;
        this.enabled = enabled;
        this.id = id;
        this.name = name;
        this.amt = 0;
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
    
    public void setName(String newName) {
        name = newName;
    }
    
    public void setAmount(double newAmt) {
        amt = newAmt;
    }

    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | name: " + name;
    }
}
