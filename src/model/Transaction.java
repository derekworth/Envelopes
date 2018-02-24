/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.Comparator;
import misc.Utilities;

/**
 *
 * @author Derek
 */
public class Transaction extends Record {
    private String date;
    private String desc;
    private int amt;
    private String amtStr, run;
    private int acctid, userid, envid;
    private String acctName, userName, envName;
    private int txid;
    
    public static final Comparator<Transaction> DATE_ID_COMPARATOR = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            int dateCompare = o1.getDate().compareTo(o2.getDate());
            if(dateCompare == 0) {
                return o1.getID() - o2.getID();
            } else {
                return dateCompare;
            }
        }
    };
    
    public Transaction(int id, String created, String modified, String date, String desc, int amt, int acctid, int userid, int envid, int txid) {
        super(id, created, modified);
        this.date     = date;
        this.desc     = desc;
        this.amt      = amt;
        this.amtStr   = Utilities.amountToString(amt);
        this.run      = Record.EMPTY_NAME;
        this.acctid   = acctid;
        this.userid   = userid;
        this.envid    = envid;
        this.acctName = Record.EMPTY_NAME; // must set later
        this.userName = Record.EMPTY_NAME; // must set later
        this.envName  = Record.EMPTY_NAME; // must set later
        this.txid     = txid;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDesc(String d) {
        desc = d;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public void setAmount(int amt) {
        this.amt = amt;
        this.amtStr = Utilities.amountToString(amt);
    }
    
    public int getAmount() {
        return amt;
    }
    
    public String getAmountString() {
        return amtStr;
    }
    
    public void setRunningTotal(String run) {
        this.run = run;
    }
    
    public String getRunningTotal() {
        return run;
    }
    
    public void setAccountID(int id) {
        this.acctid = id;
    }
    
    public int getAccountID() {
        return acctid;
    }
    
    public void setUserID(int id) {
        this.userid = id;
    }
    
    public int getUserID() {
        return userid;
    }
    
    public void setEnvelopeID(int id) {
        this.envid = id;
    }
    
    public int getEnvelopeID() {
        return envid;
    }
    
    public void setAccountName(String name) {
        this.acctName = name;
    }
    
    public String getAccountName() {
        return this.acctName;
    }
    
    public void setUserName(String name) {
        this.userName = name;
    }
    
    public String getUserName() {
        return this.userName;
    }
    
    public void setEnvelopeName(String name) {
        this.envName = name;
    }
    
    public String getEnvelopeName() {
        return this.envName;
    }
    
    public void setTransferID(int id) {
        this.txid = id;
    }
    
    public int getTransferID() {
        return txid;
    }
    
    @Override
    public String toString() {
        return "Transaction(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", date: " +  this.getDate() + ", desc: " + this.getDesc() + ", amt: " + this.getAmount() + ", run: " + this.getRunningTotal() + ", acct: " + this.getAccountID() + ", user: " + this.getUserID() + ", env: " + this.getEnvelopeID() + ", tx: " + this.getTransferID() + ")";
    }
}
