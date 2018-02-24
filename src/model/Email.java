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
public class Email extends Record {
    private int attempt; // 0 = authenticated, 1-5 = failed attempt, +6 = locked out
    private int userid; // -1 = no user assigned
    private String un;
    private final String addr;
    
    public static final Comparator<Email> UN_ADDR_COMPARATOR = new Comparator<Email>() {
        @Override
        public int compare(Email e1, Email e2) {
            if(e1.getUsername().compareTo(e2.getUsername())<0) {
                return -1;
            } else if(e1.getUsername().compareTo(e2.getUsername())==0) {
                return e1.getAddress().compareTo(e2.getAddress());
            } else {
                return 1;
            }
        }
    };
    
    public static final Comparator<Email> ADDR_COMPARATOR = new Comparator<Email>() {
        @Override
        public int compare(Email e1, Email e2) {
            return e1.getAddress().compareTo(e2.getAddress());
        }
    };
    
    public Email(int id, String created, String modified, int attempt, int userid, String addr) {
        super(id, created, modified);
        this.attempt = attempt;
        this.userid = userid;
        this.un = Record.EMPTY_NAME; // must set later
        this.addr = addr;
    }
    
    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }
    
    public int getAttempt() {
        return attempt;
    }
    
    public void setUserID(int userid) {
        this.userid = userid;
    }
    
    public int getUserID() {
        return userid;
    }
    
    public void setUsername(String un) {
        this.un = un;
    }
    
    public String getUsername() {
        return un;
    }
    
    public String getAddress() {
        return addr;
    }
    
    @Override
    public String toString() {
        return "Email(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", attempt: " +  this.getAttempt() + ", userid: " + this.getUserID() + ", addr: " + this.getAddress() + ")";
    }
}
