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
public class Envelope extends Container {
    private int catid;
    
    public Envelope(int id, String created, String modified, String name, int amt, int catid) {
        super(id, created, modified, name, amt);
        this.catid = catid;
    }
    
    public void setCategoryID(int catid) {
        this.catid = catid;
    }
    
    public int getCategoryID() {
        return catid;
    }
    
    @Override
    public String toString() {
        return "Envelope(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", name: " +  this.getName() + ", amt: " + this.getAmount() + ", catid: " + this.getCategoryID() + ")";
    }
}
