/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author Derek
 */
public class Account extends Container {
    
    private boolean enabled;
    
    public Account(int id, String created, String modified, String name, int amt, boolean enabled) {
        super(id, created, modified, name, amt);
        this.enabled = enabled;
    }
    
    public void setEnabled(boolean en) {
        enabled = en;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String toString() {
        return "Account(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", name: " +  this.getName() + ", amt: " + this.getAmount() + ", enabled: " + this.isEnabled() + ")";
    }
}