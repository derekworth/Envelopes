package database;

import java.util.LinkedList;
import misc.Utilities;

/**
 * Created on Aug 17, 2013
 * @author Derek Worth
 */
public class Model {
    
    public final String TOTAL = "TOTAL";
    public final String UNCAT = "UNCATEGORIZED";
    
    private LinkedList<Object> accounts;
    private LinkedList<Object> envelopesUncat;
    private LinkedList<Object> envelopesCat;
    private LinkedList<User> users;
    private LinkedList<Email> emails;
    private LinkedList<Transaction> transactionsByQty;
    private LinkedList<Transaction> transactionsByDate;
    
    private Total acctTotal;
    private Total envTotal;
    private Total uncatTotal;
    private int uncatCount = 0;
    
    private boolean categorized;
    
    // CONSTRUCTOR
    
    public Model() {
        // setup database if not already done so
        DBMS.initializeDB();
        // pull data from database
        initializeModel();
    }
    
    //==========================================================================
    // PRIVATE METHODS
    //==========================================================================
    
    private void initializeModel() {
        accounts           = new LinkedList();
        envelopesUncat     = new LinkedList();
        envelopesCat       = new LinkedList();
        users              = new LinkedList();
        emails             = new LinkedList();
        transactionsByQty  = new LinkedList();
        transactionsByDate = new LinkedList();
        
        acctTotal     = new Total(TOTAL, 0); // total amt of all accounts
        envTotal      = new Total(TOTAL, 0); // total amt of all envelopes
        uncatTotal    = new Total(UNCAT, 0); // total amt of all uncategorized envelopes
        uncatCount = 0; // number of uncategorized envelopes
        
        // add totals
        accounts.add(acctTotal);
        envelopesUncat.add(envTotal);
        envelopesCat.add(envTotal);
        
        categorized = true;
        
        // initialize accounts
        LinkedList<Account> accts = DBMS.getAccountsFromDB();
        for(Account a : accts) {
            if(a.isEnabled()) {
                addAccountToList(a);
            }
        }
        
        // initialize categories
        LinkedList<Category> cats = DBMS.getCategoriesFromDB();
        for(Category c : cats) {
            if(c.isEnabled()) {
                addCategoryToList(c);
            }
        }
        
        // initialize envelopes
        LinkedList<Envelope> envs = DBMS.getEnvelopesFromDB();
        for(Envelope e : envs) {
            if(e.isEnabled()) {
                addEnvelopeToList(e);
            }
        }
        
        // intialize users
        users = DBMS.getUsersFromDB();
        
        // initialize emails
        emails = DBMS.getEmailsFromDB();
        
        // initialize transactions
        transactionsByQty = DBMS.getTransactionsFromDB(25, 0, null, null, false);
    }
    
    public void removeAccount(Account acct) {
        // update total (last element will always be the Total)
        ((Total) accounts.getLast()).addToAmount(-acct.getAmount());
        // remove from list
        accounts.remove(acct);
    }
    
    public void removeEnvelope(Envelope env) {
        // update totals
        envTotal.addToAmount(-env.getAmount());
        // remove from lists
        envelopesUncat.remove(env);
        envelopesCat.remove(env);
        // remove uncategorized Total if necessary
        if(env.getCategoryId()==-1) {
            // update amt
            uncatTotal.addToAmount(-env.getAmount());
            // check for zero uncategorized envelopes
            uncatCount--;
            if(uncatCount==0) {
                envelopesCat.remove(uncatTotal);
            }
        } else {
            // update amt
            getCategoryById(env.getCategoryId()).addToAmount(-env.getAmount());
        }
    }
    
    public void removeCategory(Category cat) {
        
    }
    
    public void addAccount(Account acct) {
        // update total
        acctTotal.addToAmount(acct.getAmount());
        
        for(Object o : accounts) {
            if(o instanceof Total || Utilities.isOrdered(acct.getName(), ((Account) o).getName())) {
                accounts.add(accounts.indexOf(o), acct);
                break;
            }
        }
    }
    
    public void addEnvelope(Envelope env) {
        // update totals
        envTotal.addToAmount(env.getAmount());
        
        // update category amount
        boolean isUncat = env.getCategoryId()==-1;
        if(isUncat) { // uncategorized
            uncatTotal.addToAmount(env.getAmount());
        } else {      // categorized
            getCategoryById(env.getCategoryId()).addToAmount(env.getAmount());
        }
        
        // add envelope to uncategorized list
        for(Object o : envelopesUncat) {
            if(o instanceof Total || Utilities.isOrdered(env.getName(), ((Envelope) o).getName())) {
                envelopesUncat.add(envelopesUncat.indexOf(o), env);
                break;
            }
        }
        
        // add envelope to categorized list
        boolean catFound = false;
        for(Object o : envelopesCat) {
            if(isUncat) {
                if(uncatCount==0) {
                    uncatCount++;
                    envelopesCat.add(envelopesCat.size(), uncatTotal);
                    envelopesCat.add(envelopesCat.size(), env);
                    return;
                } else if(o instanceof Total && ((Total) o).getName().equalsIgnoreCase(UNCAT)) {
                    catFound = true;
                } else if(catFound && (o instanceof Total || Utilities.isOrdered(env.getName(), ((Envelope) o).getName()))) { // adds subsequent uncategorized entry
                    uncatCount++;
                    envelopesCat.add(envelopesCat.indexOf(o), env);
                    return;
                }
            } else {
                if(o instanceof Category && ((Category) o).getId()==env.getCategoryId()) {
                    catFound = true;
                } else if(catFound && (o instanceof Total || o instanceof Category || Utilities.isOrdered(env.getName(), ((Envelope) o).getName()))) {
                    envelopesCat.add(envelopesCat.indexOf(o), env);
                    break;
                }
            }
        }
    }
    
    public void addCategory(Category cat) {
        // update total
        acctTotal.addToAmount(cat.getAmount());
        
        for(Object o : accounts) {
            if(o instanceof Total || Utilities.isOrdered(cat.getName(), ((Account) o).getName())) {
                accounts.add(accounts.indexOf(o), cat);
                break;
            }
        }
    }
    
    // GETTERS
    
    /**
     * Returns the name of the account or total (last object in list) at the
     * specified index
     * @param i index of account or total
     * @return String representing the name of the account or total
     */
    public String getAccountName(int i) {
        Object obj = accounts.get(i);
        if(obj instanceof Account) {
            return ((Account) obj).getName();
        } else if(obj instanceof Total) {
            return ((Total) obj).getName();
        } else {
            return null;
        }
    }
    
    public String getEnvelopeName(int i) {
        Object obj;
        if(categorized) {
            obj = envelopesCat.get(i);
        } else {
            obj = envelopesUncat.get(i);
        }
        if(obj instanceof Envelope) {
            return ((Envelope) obj).getName();
        } else if(obj instanceof Category) {
            return ((Category) obj).getName();
        } else if(obj instanceof Total) {
            return ((Total) obj).getName();
        } else {
            return null;
        }
    }
    
    private Account getAccountById(int aid) {
        if(aid==-1) return null;
        for(Object o : accounts) {
            if(o instanceof Account && ((Account) o).getId()==aid) return (Account) o;
        }
        return null;
    }
    
    private Envelope getEnvelopeById(int eid) {
        if(eid==-1) return null;
        for(Object o : envelopesUncat) {
            if(o instanceof Envelope && ((Envelope) o).getId()==eid) return (Envelope) o;
        }
        return null;
    }
    
    private Category getCategoryById(int cid) {
        if(cid==-1) return null;
        for(Object o : envelopesCat) {
            if(o instanceof Category && ((Category) o).getId()==cid) return (Category) o;
        }
        return null;
    }
    
    private User getUserById(int uid) {
        if(uid==-1) return null;
        for(User u : users) {
            if(u.getId()==uid) return u;
        }
        return null;
    }
    
    private Email getEmailById(int emid) {
        if(emid==-1) return null;
        for(Email e : emails) {
            if(e.getId()==emid) return e;
        }
        return null;
    }
    
    //==========================================================================
    // PRIVATE STATIC METHODS
    //==========================================================================
}
