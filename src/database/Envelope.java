package database;

import misc.Utilities;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Envelope extends Container {
    private int cid;

    public Envelope(String created, String modified, int id, boolean enabled, String name, double amt, int cid) {
        super(created, modified, id, enabled, name, amt);
        this.cid = cid;
    }
    
    public int getCategoryId() {
        return cid;
    }
    
    public void setCategoryId(int cid) {
        this.cid = cid;
    }

    @Override
    public String toString() {
        return "created: " + this.getCreated() 
                + " | modified: " + this.getModified() 
                + " | enabled: " + this.isEnabled() 
                + " | id: " + this.getId() 
                + " | name: " + this.getName() 
                + " | amount: " + Utilities.roundAmount(this.getAmount())
                + " | cid: " + cid;
    }
    
}
