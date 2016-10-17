package database;

import java.util.LinkedList;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Envelope {
    private final String created;
    private String modified;
    private boolean enabled;
    private final int id;
    private int cid;
    private Category category;
    private String name;
    private double amt;
    private LinkedList<Transaction> transactions;
    private LinkedList<Transaction> transactionsByDate;
    private LinkedList<Transaction> transactionsByQty;
    
    // CONSTRUCTORS
    
    public Envelope(String created, String modified, boolean enabled, int id, int cid, String name, double amt) {
        this.created = created;
        this.modified = modified;
        this.enabled = enabled;
        this.id = id;
        this.cid = cid;
        this.category = null;
        this.name = name;
        this.amt = amt;
        transactionsByQty = new LinkedList();
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
    
    public int getCategoryId() {
        return cid;
    }
    
    public Category getCategory() {
        return category;
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
    
    public void setCategoryId(int cid) {
        this.cid = cid;
    }
    
    public void setCategory(Category cat) {
        category = cat;
    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    public void setAmount(double newAmt) {
        double oldAmt = amt;
        double diff = newAmt - oldAmt;
        if(category!=null) {
            category.updateAmount(diff);
        }
        amt = newAmt;
    }
    
    // Linked List Management
    
    public void setTransactions(LinkedList<Transaction> trans) {
        transactionsByQty = trans;
    }
    
    /**
     * Adds transactions to envelope in chronological order (by date, then id),
     * but does not update the envelope amount (this must be done using the
     * setAmount() method)
     * @param newTran transaction to be added
     */
    public void addTransaction(Transaction newTran) {        
        // add transaction to list in chronological order (by date, then id)
        Transaction curr;
        int size = transactionsByQty.size();
        int i = 1;
        while(true) {
            if(i<=size) {
                // get envelope at current index
                curr = transactionsByQty.get(i);
                // compare current envelope with envelope to be added
                if(curr.getDate().compareTo(newTran.getDate())>0) {
                    // add before current envelope
                    transactionsByQty.add(i, curr);
                    break;
                }
                i++;
            } else {
                // add to the end of list
                transactionsByQty.addLast(newTran);
                break;
            }
        }
    }
    
    /**
     * Removes the specified transaction from the list and updated envelope
     * amount accordingly
     * @param tran transaction to be removed
     */
    public void removeTransaction(Transaction tran) {
        amt -= tran.getAmount();
        transactionsByQty.remove(tran);
    }
    
    public LinkedList<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        String c = "NONE";
        if(category!=null) c = category.getName();
        return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | category: " + c + " | name: " + name + " | amount: " + amt;
    }
}
