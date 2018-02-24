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
public class Category extends Container {
    
    public Category(int id, String created, String modified, String name, int amt) {
        super(id, created, modified, name.toUpperCase(), amt);
    }
    
    @Override
    public String toString() {
        return "Category(id: " + this.getID() + ", created: " + this.getCreated() + ", modified: " + this.getModified() + ", name: " +  this.getName() + ", amt: " + this.getAmount() + ")";
    }
}