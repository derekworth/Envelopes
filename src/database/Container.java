package database;

import misc.Utilities;

/**
 * Created on 10/29/2016
 * @author Derek
 */
public class Container extends Record {
    private boolean enabled;
    private String name;
    private double amt;
    
    public Container(String created, String modified, int id, boolean enabled, String name, double amt) {
        super(created, modified, id);
        this.enabled = enabled;
        this.name = name;
        this.amt = amt;
    }
    
    // GETTERS
    
    public boolean isEnabled() {
        return enabled;
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
    
    public void setName(String newName) {
        name = newName;
    }
    
    public void setAmount(double amt) {
        this.amt = amt;
    }
    
    public void addToAmount(double diff) {
        amt += diff;
    }

    @Override
    public String toString() {
        return "created: " + this.getCreated() 
                + " | modified: " + this.getModified() 
                + " | enabled: " + this.isEnabled() 
                + " | id: " + this.getId() 
                + " | name: " + this.getName() 
                + " | amount: " + Utilities.roundAmount(this.getAmount());
    }
    
}
