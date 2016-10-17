package database;

import java.util.LinkedList;

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
    private final LinkedList<Envelope> envelopes;
    
    // CONSTRUCTORS
    
    public Category(String created, String modified, boolean enabled, int id, String name) {
        this.created = created;
        this.modified = modified;
        this.enabled = enabled;
        this.id = id;
        this.name = name;
        this.amt = 0;
        envelopes = new LinkedList();
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
    
    // Linked List Management
    
    /**
     * Adds envelope to category in alphabetical order (by envelope name).
     * @param newEnv envelope to be added
     */
    public void addEnvelope(Envelope newEnv) {
        // increment category amount by envelope amount
        amt += newEnv.getAmount();
        
        // add envelope to list in alphabetical order (by envelope name)
        Envelope curr;
        int size = envelopes.size();
        int i = 1;
        while(true) {
            if(i<=size) {
                // get envelope at current index
                curr = envelopes.get(i);
                // compare current envelope with envelope to be added
                if(curr.getName().compareTo(newEnv.getName())>0) {
                    // add before current envelope
                    envelopes.add(i, curr);
                    break;
                }
                i++;
            } else {
                // add to the end of list
                envelopes.addLast(newEnv);
                break;
            }
        }
    }
    
    /**
     * Removes the specified envelope from the list
     * @param env envelope to be removed
     */
    public void removeEnvelope(Envelope env) {
        amt -= env.getAmount();
        envelopes.remove(env);
    }
    
    /**
     * Sums the amount in all envelopes and updates the category amount
     */
    public void updateAmount() {
        amt = 0;
        for(Envelope env : envelopes) {
            amt += env.getAmount();
        }
    }
    
    /**
     * Adds specified difference to current amount
     * @param diff amount to change category amount by
     */
    public void updateAmount(double diff) {
        amt += diff;
    }

    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | name: " + name;
    }
}
