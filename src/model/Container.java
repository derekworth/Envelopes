package model;

import java.util.Comparator;
import misc.Utilities;

/**
 *
 * @author Derek
 */
public abstract class Container extends Record {
    private String name;
    private int    amt;
    private String amtStr;
    
    public static final Comparator<Container> NAME_COMPARATOR = new Comparator<Container>() {
        @Override
        public int compare(Container c1, Container c2) {
            return c1.getName().compareTo(c2.getName());
        }
    };
    
    public Container(int id, String created, String modified, String name, int amt) {
        super(id, created, modified);
        this.name = name;
        this.amt = amt;
        this.amtStr = Utilities.amountToString(amt);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAmount() {
        return amt;
    }
    
    public String getAmountString() {
        return amtStr;
    }
    
    public void setAmount(int amt) {
        this.amt = amt;
        this.amtStr = Utilities.amountToString(this.amt);
    }
    
    public void addToAmount(int amt) {
        this.amt += amt;
        this.amtStr = Utilities.amountToString(this.amt);
    }
    
    @Override
    public String toString() {
        return "Container(id: " + this.getID() + ", created: " + this.getCreated() + " modified: " + this.getModified() + ", name: " +  this.getName() + ", amt: " + this.getAmountString() + ")";
    }
}
