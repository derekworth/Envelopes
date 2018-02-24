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
public abstract class Record {
    private final int id;
    private final String created;
    private String modified;
    
    static final String EMPTY_NAME = "--";
    
    public Record(int id, String created, String modified) {
        this.id = id;
        this.created = created;
        this.modified = modified;
    }
    
    // SETTERS AND GETTERS
    
    public int getID() {
        return id;
    }
    
    public String getCreated() {
        return created;
    }
    
    public void setModified(String date) {
        modified = date;
    }
    
    public String getModified() {
        return modified;
    }
    
    @Override
    public String toString() {
        return "Record(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ")";
    }
}
