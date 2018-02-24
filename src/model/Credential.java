/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.Comparator;

/**
 *
 * @author Derek
 */
public class Credential extends Record {
    private String username;
    private String password; // hash of password stored for users
    private int type; // 0 = user, 1 = admin, 2 = gmail
    private boolean enabled;
    
    public static final int TYPE_USER  = 0;
    public static final int TYPE_ADMIN = 1;
    public static final int TYPE_GMAIL = 2;
    
    public static final Comparator<Credential> USERNAME_COMPARATOR = new Comparator<Credential>() {
        @Override
        public int compare(Credential o1, Credential o2) {
            return o1.getUsername().compareTo(o2.getUsername());
        }
    };
    
    public Credential(int id, String created, String modified, String username, String password, int type, boolean enabled) {
        super(id, created, modified);
        this.username = username;
        this.password = password;
        this.type = type;
        this.enabled = enabled;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String un) {
        this.username = un;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String pw) {
        this.password = pw;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean en) {
        enabled = en;
    }
    
    @Override
    public String toString() {
        return "Credential(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", un: " +  this.getUsername() + ", pw: " + this.getPassword() + ", type: " + this.getType() + ", en: " + this.isEnabled() + ")";
    }
}
