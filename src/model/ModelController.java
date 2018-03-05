package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import misc.Utilities;

/**
 * @Created Jan 14, 2018
 * @Modified Jan 15, 2018
 * @author Derek Worth
 */
public final class ModelController {
    private HashMap<Integer, Account>    allAccts;
    private HashMap<Integer, Envelope>   allEnvs;
    private HashMap<Integer, Category>   allCats;
    private HashMap<Integer, Credential> allUsers;
    
    private Credential gmail;
    
    // These feed the Table Models directly (what the user sees)
    private LinkedList<Container>   listOfAccts;
    private LinkedList<Container>   listOfEnvsU;
    private LinkedList<Container>   listOfEnvsC;
    private LinkedList<Transaction> listOfTrans;
    private LinkedList<Email>       listOfEmail;
    private LinkedList<Credential>  listOfUsers;
    
    private boolean byDateRange, hideTx, categorized;
    private int fromIndex, toIndex;
    private String acctName, envName, fromDate, toDate;
    
    private static final String UNCAT = "uncategorized";
    public static final int MAX_ATTEMPT = 5;
    
    public ModelController() {
        initComponents();
    }
    
    public void initComponents() {
        allAccts = new HashMap<>();
        allEnvs  = new HashMap<>();
        allCats  = new HashMap<>();
        allUsers = new HashMap<>();
        
        listOfAccts = new LinkedList<>();
        listOfEnvsU = new LinkedList<>();
        listOfEnvsC = new LinkedList<>();
        listOfUsers = new LinkedList<>();
        
        byDateRange = false;
        categorized = true;
        // Populate accounts
        LinkedList<Account> acctsTmp = DBMS.getAccounts();
        if(acctsTmp==null) { // list will be null if database hasn't been initialized
            DBMS.initializeDatabase();     // create new database
            acctsTmp = DBMS.getAccounts(); // pull accounts
        }
        acctsTmp.sort(Container.NAME_COMPARATOR);
        int tot = 0;
        for(Account acct : acctsTmp) {
            allAccts.put(acct.getID(), acct); // populate hashmap
            if(acct.isEnabled()) {
                listOfAccts.add(acct);        // populate linked list
                tot += acct.getAmount();      // calculate total
            }
        }
        listOfAccts.add(new Total(tot));      // populate total
        
        // Populate envelopes
        LinkedList<Envelope> envsTmp = DBMS.getEnvelopes();
        envsTmp.sort(Container.NAME_COMPARATOR);
        tot = 0;
        for(Envelope env : envsTmp) {
            allEnvs.put(env.getID(), env); // populate hashmap
            listOfEnvsU.add(env);          // populate linked list
            tot += env.getAmount();        // calculate total
        }
        Total envsTotal = new Total(tot);
        listOfEnvsU.add(envsTotal);    // populate total
        
        // Populate categories
        LinkedList<Category> catsTmp = DBMS.getCategories(); 
        catsTmp.sort(Container.NAME_COMPARATOR);
        for(Category cat : catsTmp) {
            allCats.put(cat.getID(), cat); // populate hashmap
            listOfEnvsC.add(cat);          // populate linked list w/ categories
            for(int i = 0; i < envsTmp.size(); i++) {
                Envelope env = envsTmp.get(i);
                if(env.getCategoryID()==cat.getID()) {
                    listOfEnvsC.add(env);  // populate linked list w/ envelopes
                    envsTmp.remove(env);
                    i--;
                }
            }
        }
        if(!envsTmp.isEmpty()) {
            Category uncat = new Category(-1,Record.EMPTY_NAME,Record.EMPTY_NAME, UNCAT, 0);
            listOfEnvsC.add(uncat);           // populate linked list w/ uncategorized category
            allCats.put(-1, uncat);
            int uncatTot = 0;
            for(Envelope env : envsTmp) {
                listOfEnvsC.add(env);        // populate linked list w/ uncategorized envelopes
                uncatTot += env.getAmount(); // calculate uncategorized total
            }
            uncat.setAmount(uncatTot);       // populate uncategorized total
        }
        listOfEnvsC.add(envsTotal);          // populate total
        
        // Populate credentials
        LinkedList<Credential> listOfUsersTmp = DBMS.getCredentials();
        for(Credential cred : listOfUsersTmp) {
            if(cred.getType()==Credential.TYPE_GMAIL) {
                gmail = cred;                     // populate gmail
            } else {
                allUsers.put(cred.getID(), cred); // populate hashmap
                if(cred.isEnabled()) {
                    listOfUsers.add(cred);        // populate linked list
                }
            }
        }
        listOfUsers.sort(Credential.USERNAME_COMPARATOR);
        
        // Populate transactions
        showTransactionsByIndexRange("-ALL-", "-ALL-", 1, 250, true);
        refreshTransactionsFromDatabase();
        
        // Populate email
        listOfEmail = DBMS.getEmail();
        for(Email em : listOfEmail) {
            if(em.getUserID()!=-1) {
                em.setUsername(allUsers.get(em.getUserID()).getUsername()); // update usernames for each authenticated email
            }
        }
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
    }
    
    public void resetDatabase() {
        DBMS.initializeDatabase();
        initComponents();
    }
    
    //---------------
    // Helpers
    //---------------
    
    /**
     * Provides the account, category, or envelope of the specified name
     * @param name name of container
     * @return the container with the specified name, null if no such container exists
     */
    private Container getContainer(String name) {
        Iterator<Account> accts = allAccts.values().iterator();
        while(accts.hasNext()) {
            Account a = accts.next();
            if(a.getName().equalsIgnoreCase(name)) {
                return a;
            }
        }
        Iterator<Category> cats = allCats.values().iterator();
        while(cats.hasNext()) {
            Category c = cats.next();
            if(c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        Iterator<Envelope> envs = allEnvs.values().iterator();
        while(envs.hasNext()) {
            Envelope e = envs.next();
            if(e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }
    
    private Account getAccount(String name) {
        Container c = this.getContainer(name);
        if(c!=null && c instanceof Account) {
            return (Account) c;
        }
        return null;
    }
    
    private Envelope getEnvelope(String name) {
        Container c = this.getContainer(name);
        if(c!=null && c instanceof Envelope) {
            return (Envelope) c;
        }
        return null;
    }
    
    private Category getCategory(String name) {
        Container c = this.getContainer(name);
        if(c!=null && c instanceof Category) {
            return (Category) c;
        }
        return null;
    }
    
    private Email getEmail(String addr) {
        for(int i = 0; i < listOfEmail.size(); i++) {
            Email e = listOfEmail.get(i);
            if(e.getAddress().equalsIgnoreCase(addr)) {
                return e;
            }
        }
        return null;
    }
    
    private Credential getUser(String username) {
        for (int id : allUsers.keySet()) {
            Credential user = allUsers.get(id);
            if(user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }
    
    private Transaction getTransaction(int id) {
        // get from model if possible
        for(Transaction t : listOfTrans) {
            if(t.getID()==id) return t;
        }
        // get from databse if not in model
        Transaction t = DBMS.getTransaction(id);
        // update account name as applicable
        int acctid = t.getAccountID();
        if(acctid!=-1) {
            Account acct = allAccts.get(acctid);
            if(acct!=null) {
                t.setAccountName(acct.getName());
            }
        }
        // update envelopename as applicable
        int envid  = t.getEnvelopeID();
        if(envid!=-1) {
            Envelope env = allEnvs.get(envid);
            if(env!=null) {
                t.setEnvelopeName(env.getName());
            }
        }
        return t;
    }
 
    //---------------
    // Account getters/setters
    //---------------
    
    /**
     * Provides the name of the specified account
     * @param index corresponding item number in the accounts table model
     * @return name of specified account/total
     */
    public String getAccountName(int index) {
        return listOfAccts.get(index).getName();
    }
    
    /**
     * Provides a list of names for all enabled accounts sorted in alphabetical order
     * @return String array of enabled account names
     */
    public String[] getAccountNames() {
        String[] names = new String[listOfAccts.size()-1];
        for(int i = 0; i < names.length; i++) {
            names[i] = listOfAccts.get(i).getName();
        }
        return names;
    }
    
    public boolean isAccount(String name) {
        return getAccount(name)!=null;
    }
    
    /**
     * Provides the amount of the specified account in comma/decimal format
     * (e.g. "X,XXX.XX")
     * @param index corresponding item number in the accounts table model
     * @return amount of the specified account/total
     */
    public String getAccountAmount(int index) {
        return listOfAccts.get(index).getAmountString();
    }
    
    public String getAccountAmount(String name) {
        return getAccountAmount(name, "ALL");
    }
    
    public String getAccountAmount(String name, String asOfDate) {
        Account a = getAccount(name);
        if(a==null) { // total for all accounts
            return Utilities.amountToString(DBMS.getAccountAmount(-1, asOfDate));
        } else {      // total for specific account
            return Utilities.amountToString(DBMS.getAccountAmount(a.getID(), asOfDate));
        }
    }
     
    /**
     * Tests if container at the specified index within the accounts list is an
     * account (list contains a total as well)
     * @param index index of container
     * @return true if account, false if total
     */
    public boolean isAccount(int index) {
        return listOfAccts.get(index) instanceof Account;
    }
    
    private boolean addToAccountTotal(int amt) {
        Container c = listOfAccts.getLast();
        if(c instanceof Total) {
            c.addToAmount(amt);
            return true;
        }
        return false;
    }
    
    /**
     * Provides the number of entries in the Accounts table model
     * @return count of account entries including the overall Total entry
     */
    public int getAccountCount() {
        return listOfAccts.size();
    }
    
    /**
     * Adds a new account to the database & model if not already existing, 
     * otherwise enables any corresponding disabled account.
     * @param name Account name
     * @return true if account successfully added/enabled, false otherwise;
     * NOTE: names must be unique between containers (i.e. accounts, categories,
     * & envelopes, thus attempting to add an account with a name that is
     * already in use will fail and return false.
     */
    public boolean addAccount(String name) {
        name = Utilities.cleanContainerName(name);
        // ensure name isn't already in use
        Container existingContainer = this.getContainer(name);
        if(existingContainer!=null) { // acct, cat, or env already has that name
            if(existingContainer instanceof Account) {
                if(!((Account) existingContainer).isEnabled()) { // disabled acct has name so let's enable it and be done
                    // enable in database
                    DBMS.modifyAccount(existingContainer.getID(), existingContainer.getName(), true);
                    ((Account) existingContainer).setEnabled(true);
                    // add to model
                    for(int i = 0; i < listOfAccts.size(); i++) {
                        Container acct = listOfAccts.get(i);
                        if(acct instanceof Total || existingContainer.getName().compareTo(acct.getName())<0) {
                            listOfAccts.add(i, existingContainer);
                            break;
                        }
                    }
                    return true;
                }
            }
            return false; // cat, env, or enabled acct
        }
        // add to DB
        Account addedAcct = DBMS.addAccount(name);
        if(addedAcct==null) {
            return false;
        } else {
            // add to model
            for(int i = 0; i < listOfAccts.size(); i++) {
                Container acct = listOfAccts.get(i);
                if(acct instanceof Total || addedAcct.getName().compareTo(acct.getName())<0) {
                    listOfAccts.add(i, addedAcct);
                    allAccts.put(addedAcct.getID(), addedAcct);
                    break;
                }
            }
        }
        return true;
    }
    
    /**
     * Sets account to disabled in the database and the model is updated accordingly
     * @param name the name of the account
     * @return true if account is successfully removed, false otherwise; NOTE:
     * accounts can only be disabled if they have a zero balance
     */
    public boolean disableAccount(String name) {
        Container c = getContainer(name);
        if(c==null) {
            return false;
        }
        if(c instanceof Account && c.getAmount()==0 && ((Account) c).isEnabled()) {
            DBMS.modifyAccount(c.getID(), c.getName(), false);
            ((Account) c).setEnabled(false);
            listOfAccts.remove(c);
            return true;
        }
        return false;
    }
    
    /**
     * Changes name (database and model) and sorts list by name in table model
     * @param oldName name before update
     * @param newName name after update
     * @return true if successful, false otherwise
     */
    public boolean renameAccount(String oldName, String newName) {
        newName = Utilities.cleanContainerName(newName);
        // get account
        Container renamedAcct = this.getContainer(oldName);
        if(renamedAcct==null 
                || !(renamedAcct instanceof Account) 
                || !((Account) renamedAcct).isEnabled() 
                || getContainer(newName)!=null
                || oldName.equalsIgnoreCase(newName)) {
            return false;
        }
        
        // update name in database
        DBMS.modifyAccount(renamedAcct.getID(), newName, true);
        // update name in model
        renamedAcct.setName(newName);
        // update envelope name associated with corresponsing transactions
        for(Transaction t : listOfTrans) {
            if(t.getAccountID() == renamedAcct.getID()) {
                t.setAccountName(renamedAcct.getName());
            }
        }
        
        // remove from model (so we can return to list in alphabetical order)
        listOfAccts.remove(renamedAcct);
        
        // returns to model in alphabetical order
        for(int i = 0; i < listOfAccts.size(); i++) {
            Container acct = listOfAccts.get(i);
            if(acct instanceof Total || renamedAcct.getName().compareTo(acct.getName())<0) {
                listOfAccts.add(i, renamedAcct);
                break;
            }
        }
        return true;
    }
    
    //---------------
    // Envelope getters/setters
    //---------------
    
    /**
     * Provides name of envelope at specified index in uncategorized list
     * @param index corresponds to number in linked list (starts index = 0)
     * @return name of envelope
     */
    public String getEnvelopeName(int index) {
        if(categorized) {
            return listOfEnvsC.get(index).getName();
        } else {
            return listOfEnvsU.get(index).getName();
        }
    }
    
    public String getEnvelopeUName(int index) {
        return listOfEnvsU.get(index).getName();
    }
    
    public String getEnvelopeCName(int index) {
        return listOfEnvsC.get(index).getName();
    }
    
    /**
     * Provides an array of strings representing all envelope names
     * @return String array of envelope names
     */
    public String[] getEnvelopeNames() {
        String[] names = new String[listOfEnvsU.size()-1];
        for(int i = 0; i < names.length; i++) {
            names[i] = listOfEnvsU.get(i).getName();
        }
        return names;
    }
    
    /**
     * Provides the name and amount of all envelopes in the specified category
     * @param catName name of category
     * @return null if category doesn't exist, otherwise returns a 2-D array
     * where array[0][x] = the envelope names and array[1][x] = envelope amounts
     */
    public String[][] getEnvelopes(String catName) {
        int envCount = 0;
        Container c = getContainer(catName);
        if(c==null || !(c instanceof Category)) return null;
        
        // get count on first pass
        for(Container item : listOfEnvsC) {
            if(item instanceof Envelope && ((Envelope) item).getCategoryID()==c.getID()) {
                envCount++;
            }
        }
        // get names/amounts on second pass
        String[][] envs = new String[2][envCount];
        int i = 0;
        for(Container item : listOfEnvsC) {
            if(item instanceof Envelope && ((Envelope) item).getCategoryID()==c.getID()) {
                envs[0][i]   = item.getName();
                envs[1][i++] = Utilities.amountToStringSimple(item.getAmount());
            }
        }
        return envs;
    }
    
    /**
     * Tests if envelope exists with specified name
     * @param name name of envelope
     * @return true if envelope exists, false otherwise
     */
    public boolean isEnvelope(String name) {
        return getEnvelope(name)!=null;
    }
    
    /**
     * Provides amount of envelope at specified index in uncategorized list
     * @param index corresponds to number in linked list (starts index = 0)
     * @return amount of envelope
     */
    public String getEnvelopeAmount(int index) {
        if(categorized) {
            return listOfEnvsC.get(index).getAmountString();
        } else {
            return listOfEnvsU.get(index).getAmountString();
        }
    }
    
    public String getEnvelopeUAmount(int index) {
        return listOfEnvsU.get(index).getAmountString();
    }
    
    public String getEnvelopeCAmount(int index) {
        return listOfEnvsC.get(index).getAmountString();
    }
    
    public String getEnvelopeAmount(String name) {
        return getEnvelopeAmount(name, "ALL");
    }
    
    public String getEnvelopeAmount(String name, String asOfDate) {
        Envelope e = getEnvelope(name);
        if(e==null) { // for all envelopes
            return Utilities.amountToString(DBMS.getEnvelopeAmount(-1, asOfDate));
        } else {      // for specific envelope
            return Utilities.amountToString(DBMS.getEnvelopeAmount(e.getID(), asOfDate));
        }
    }
    
    /**
     * Tests if container at the specified index within the envelopes list is an
     * envelope (list contains categories and totals as well)
     * @param index index of container
     * @return true if envelope, false if category or total
     */
    public boolean isEnvelope(int index) {
        if(categorized) {
            return listOfEnvsC.get(index) instanceof Envelope;
        } else {
            return listOfEnvsU.get(index) instanceof Envelope;
        }
    }
    
    /**
     * Provides number of containers in envelopes linked list
     * @return qty of containers in envelope list
     */
    public int getEnvelopeCount() {
        if(categorized) {
            return listOfEnvsC.size();
        } else  {
            return listOfEnvsU.size();
        }
    }
    
    /**
     * Provides number of containers in uncategorized envelopes linked list
     * @return qty of containers in uncategorized envelopes list
     */
    public int getEnvelopeUCount() {
        return listOfEnvsU.size();
    }
    
    /**
     * Provides number of containers in categorized envelopes linked list
     * @return qty of containers in categorized envelope list
     */
    public int getEnvelopeCCount() {
        return listOfEnvsC.size();
    }
    
    /**
     * Adds new envelope to database/model with given name; NOTE: name must
     * be unique to all containers (i.e. no other envelope, category, or account
     * can share this name)
     * @param name name of new envelope to be added
     * @return true if successful, false otherwise
     */
    public boolean addEnvelope(String name) {
        name = Utilities.cleanContainerName(name);
        // ensure name isn't already in use
        if(getContainer(name)!=null) { // acct, cat, or env already has that name
            return false;
        }
        // add to DB
        Envelope addedEnv = DBMS.addEnvelope(name);
        if(addedEnv==null) {
            return false;
        } else {
            // ADD TO MODEL:
            
            // add to ID lookup table
            allEnvs.put(addedEnv.getID(), addedEnv);
            
            // add to uncategorized envelopes list
            for(int i = 0; i < listOfEnvsU.size(); i++) {
                Container curr = listOfEnvsU.get(i);
                if(curr instanceof Total || addedEnv.getName().compareTo(curr.getName())<0) {
                    listOfEnvsU.add(i, addedEnv);
                    break;
                }
            }
            
            // add to categorized envelopes list
            boolean catMatchFound = false;
            for(int i = 0; i < listOfEnvsC.size(); i++) {
                Container curr = listOfEnvsC.get(i);
                if(curr instanceof Category && !catMatchFound && ((Category) curr).getID()==addedEnv.getCategoryID()) { // Category match found
                    catMatchFound = true;
                } else if(catMatchFound) { // insert envelope in the correct order in current category
                    if((curr instanceof Envelope)) {
                        if(addedEnv.getName().compareTo(curr.getName())<0) {
                            listOfEnvsC.add(i, addedEnv);
                            break;
                        }
                    } else {
                        listOfEnvsC.add(i, addedEnv);
                        break;
                    }
                } else if(curr instanceof Total) { // TOTAL found
                    // add envelope before TOTAL in impossible event of no category match found
                    listOfEnvsC.add(i, addedEnv);
                }
            }
        }
        return true;
    }
    
    /**
     * Remove envelope from model and database; NOTE: only removes envelope if
     * there are no corresponding transactions in specified envelope
     * @param name Name of envelope to be removed
     * @return true if successful, false otherwise
     */
    public boolean removeEnvelope(String name) {
        Envelope curr = getEnvelope(name);
        if(curr==null) return false;
        if(DBMS.getTransactionCount(curr.getID())==0) {
            DBMS.deleteEnvelope(curr.getID());
            listOfEnvsU.remove(curr);
            listOfEnvsC.remove(curr);
            allEnvs.remove(curr.getID());
            return true;
        }
        return false;
    }
    
    /**
     * Update name of specified envelope
     * @param oldName Name before update
     * @param newName Name after update
     * @return true if successful, false otherwise
     */
    public boolean renameEnvelope(String oldName, String newName) {
        newName = Utilities.cleanContainerName(newName);
        // get envelope
        Container modEnv = this.getContainer(oldName);
        if(modEnv==null 
                || !(modEnv instanceof Envelope) 
                || getContainer(newName)!=null
                || oldName.equalsIgnoreCase(newName)) {
            return false;
        }
        
        // update name in database
        DBMS.modifyEnvelope(modEnv.getID(), newName, ((Envelope) modEnv).getCategoryID());
        // update name in model
        modEnv.setName(newName);
        // update envelope name associated with corresponsing transactions
        for(Transaction t : listOfTrans) {
            if(t.getEnvelopeID() == modEnv.getID()) {
                t.setEnvelopeName(modEnv.getName());
            }
        }
        
        // remove from model (so we can return it in alphabetical order)
        listOfEnvsU.remove(modEnv);
        listOfEnvsC.remove(modEnv);
        
        // RETURN TO MODEL IN ALPHABETICAL ORDER:

        // add to uncategorized envelopes list
        for(int i = 0; i < listOfEnvsU.size(); i++) {
            Container curr = listOfEnvsU.get(i);
            if(curr instanceof Total || modEnv.getName().compareTo(curr.getName())<0) {
                listOfEnvsU.add(i, modEnv);
                break;
            }
        }

        // add to categorized envelopes list
        boolean catMatchFound = false;
        for(int i = 0; i < listOfEnvsC.size(); i++) {
            Container curr = listOfEnvsC.get(i);
            if(curr instanceof Category 
                    && !catMatchFound 
                    && ((Category) curr).getID()==((Envelope) modEnv).getCategoryID()) { // Category match found
                catMatchFound = true;
            } else if(catMatchFound) { // insert envelope in the correct order in current category
                if((curr instanceof Envelope)) {
                    if(modEnv.getName().compareTo(curr.getName())<0) {
                        listOfEnvsC.add(i, modEnv);
                        break;
                    }
                } else {
                    listOfEnvsC.add(i, modEnv);
                    break;
                }
            } else if(curr instanceof Total) { // TOTAL found
                // add envelope before TOTAL in impossible event of no category match found
                listOfEnvsC.add(i, modEnv);
            }
        }
        return true;
    }
    
    /**
     * Update category of specified envelope
     * @param envName Envelope name
     * @param catName Category name
     * @return true if successful update, false otherwise
     */
    public boolean setEnvelopeCategory(String envName, String catName) {
        // get envelope
        Container modEnv = this.getContainer(envName); // modified envelope
        Container newCat = this.getContainer(catName); // set to category
        
        if(modEnv==null 
                || !(modEnv instanceof Envelope) 
                || newCat==null 
                || !(newCat instanceof Category) 
                || ((Envelope) modEnv).getCategoryID()==newCat.getID()
                || allCats.get(((Envelope) modEnv).getCategoryID()).getID()==newCat.getID()) {
            return false;
        }
        Container oldCat = allCats.get(((Envelope) modEnv).getCategoryID());
        
        // update category in database
        DBMS.modifyEnvelope(modEnv.getID(), modEnv.getName(), newCat.getID());
        // update category in model
        ((Envelope) modEnv).setCategoryID(newCat.getID());
        // update amounts
        newCat.addToAmount(modEnv.getAmount());
        oldCat.addToAmount(-modEnv.getAmount());
        
        // remove from model (so we can return it to the correct category)
        listOfEnvsC.remove(modEnv);

        // return to model, add to correct category
        boolean catMatchFound = false;
        for(int i = 0; i < listOfEnvsC.size(); i++) {
            Container curr = listOfEnvsC.get(i);
            if(curr instanceof Category 
                    && !catMatchFound 
                    && ((Category) curr).getID()==((Envelope) modEnv).getCategoryID()) { // Category match found
                catMatchFound = true;
            } else if(catMatchFound) { // insert envelope in the correct order in current category
                if((curr instanceof Envelope)) {
                    if(modEnv.getName().compareTo(curr.getName())<0) {
                        listOfEnvsC.add(i, modEnv);
                        break;
                    }
                } else {
                    listOfEnvsC.add(i, modEnv);
                    break;
                }
            } else if(curr instanceof Total) { // TOTAL found
                // add envelope before TOTAL in impossible event of no category match found
                listOfEnvsC.add(i, modEnv);
            }
        }
        return true;
    }
    
    /**
     * Moves all transactions from one envelope to another, then removes the
     * empty envelope
     * @param fromName name of envelope that you want to get rid of
     * @param toName name of destination envelope
     * @return true if successful, false otherwise
     */
    public boolean mergeEnvelopes(String fromName, String toName) {
        // clean names
        fromName = Utilities.cleanContainerName(fromName);
        toName = Utilities.cleanContainerName(toName);
        
        // get from envelope (env1) and to envelope (env2)
        Container env1 = this.getContainer(fromName);
        Container env2 = this.getContainer(toName);
        
        // check to see if merge is possible (both containers are envelopes)
        if(env1==null
                || env2==null
                || !(env1 instanceof Envelope)
                || !(env2 instanceof Envelope)
                || env1.getID()==env2.getID()
                ) {
            return false;
        }
        
        // get corresponding categories
        Category cat1 = this.allCats.get(((Envelope) env1).getCategoryID());
        Category cat2 = this.allCats.get(((Envelope) env2).getCategoryID());
        
        // in database, move transactions from env1 to env2, then remove env1
        DBMS.mergeEnvelopes(env1.getID(), env2.getID());
        
        // in model, move amt from env1 to env2 and cat1 to cat2
        int amt = env1.getAmount();
        env2.addToAmount(amt);
        cat1.addToAmount(-amt);
        cat2.addToAmount(amt);
        
        // in model, remove env1
        this.allEnvs.remove(env1.getID());
        this.listOfEnvsC.remove(env1);
        this.listOfEnvsU.remove(env1);
        
        return true;
    }
    
    private boolean addToEnvelopeTotal(int amt) {
        Container c = listOfEnvsU.getLast();
        if(c instanceof Total) {
            c.addToAmount(amt);
            return true;
        }
        return false;
    }
    
    //---------------
    // Category getters/setters
    //---------------
    
    public String[] getCategoryNames() {
        String[] names = new String[allCats.size()-1];
        int i = 0;
        for(Container c : listOfEnvsC) {
            if(c instanceof Category && c.getID()!=-1) {
                names[i++] = c.getName().toLowerCase();
            }
        }
        return names;
    }
    
    /**
     * Tells whether or not the container at specified index in categorized
     * envelope list is a category
     * @param index of container in categorized envelope list
     * @return true if specified container is a category, false otherwise
     */
    public boolean isCategory(int index) {
        return listOfEnvsC.get(index) instanceof Category;
    }
    
    public boolean addCategory(String name) {
        name = Utilities.cleanContainerName(name);
        // ensure name isn't already in use
        if(getContainer(name)!=null) { // acct, cat, or env already has that name
            return false;
        }
        // add to DB
        Category addedCat = DBMS.addCategory(name);
        if(addedCat==null) {
            return false;
        } else {
            // ADD TO MODEL:
            
            // add to ID lookup table
            allCats.put(addedCat.getID(), addedCat);
            
            // add to categorized envelopes list
            for(int i = 0; i < listOfEnvsC.size(); i++) {
                Container curr = listOfEnvsC.get(i);
                if(curr instanceof Total                                                                   // insert before total
                        || (curr instanceof Category && curr.getID()==-1)                                  // insert before uncategorized category
                        || (curr instanceof Category && addedCat.getName().compareTo(curr.getName())<0)) { // insert before other category (alphabetically)
                    listOfEnvsC.add(i, addedCat);
                    break;
                }
            }
        }
        return true;
    }
    
    public boolean removeCategory(String name) {
        Container remCat = this.getContainer(name);
        if(remCat==null 
                || !(remCat instanceof Category)
                || name.equalsIgnoreCase(UNCAT)) {
            return false;
        }
        
        // find category
        int index = 0;
        for(int i = 0; i < this.listOfEnvsC.size(); i++) {
            Container cat = listOfEnvsC.get(i);
            if(cat instanceof Category && cat.getID()==remCat.getID()) {
                index = i;
                break;
            }
        }
        
        // retrieve corresponding envelopes
        LinkedList<Container> tmp = new LinkedList();
        for(int i = index+1; i < this.listOfEnvsC.size(); i++) {
            Container env = listOfEnvsC.get(i);
            if(env instanceof Envelope) {
                tmp.add(env);
            } else {
                break;
            }
        }
        // update category amount
        Container uncat = this.getContainer(UNCAT);
        uncat.addToAmount(remCat.getAmount());
        
        for(Container env : tmp) {
            // remove corresponding envelopes
            listOfEnvsC.remove(env);
            // set envelope category to uncategorized in model
            ((Envelope) env).setCategoryID(-1);
            // set envelope category to uncategorized in database
            DBMS.modifyEnvelope(env.getID(), env.getName(), -1);
        }
        
        // add corresponding envelopes to uncategorized category
        boolean uncatMatchFound = false;
        for(int i = index; i < listOfEnvsC.size(); i++) {
            if(listOfEnvsC.get(i) instanceof Category
                    && !uncatMatchFound
                    && ((Category) listOfEnvsC.get(i)).getID()==-1) { // uncategorized category found
                uncatMatchFound = true;
            } else if(uncatMatchFound) { // insert envelope in the correct order in current category
                for(int j = 0; j < tmp.size(); j++) {
                    while(i < listOfEnvsC.size()) {
                        if(listOfEnvsC.get(i) instanceof Envelope) {
                            if(tmp.get(j).getName().compareTo(listOfEnvsC.get(i).getName())<0) {
                                listOfEnvsC.add(i++, tmp.get(j));
                                break;
                            }
                            i++;
                        } else {
                            // dump remainder of envelopes to end just before the TOTAL
                            while(j < tmp.size()) {
                                listOfEnvsC.add(i++, tmp.get(j++));
                            }
                            break;
                        }
                    }
                }
                break;
            }
        }
        
        // remove category from database
        DBMS.deleteCategory(remCat.getID());
        
        // remove category from model
        listOfEnvsC.remove(remCat);
        allCats.remove(remCat.getID());
        
        return true;
    }
    
    public boolean renameCategory(String oldName, String newName) {
        newName = Utilities.cleanContainerName(newName);
        newName = newName.toUpperCase();
        Container renamedCat = this.getContainer(oldName);
        if(renamedCat==null
                || !(renamedCat instanceof Category)
                || getContainer(newName)!=null
                || oldName.equalsIgnoreCase(newName)
                || renamedCat.getID()==-1) {
            return false;
        }
        
        // find category
        int index = 0;
        for(int i = 0; i < this.listOfEnvsC.size(); i++) {
            Container cat = listOfEnvsC.get(i);
            if(cat instanceof Category && cat.getID()==renamedCat.getID()) {
                index = i;
                break;
            }
        }
        
        // retrieve corresponding envelopes
        LinkedList<Container> tmp = new LinkedList();
        for(int i = index+1; i < this.listOfEnvsC.size(); i++) {
            Container env = listOfEnvsC.get(i);
            if(env instanceof Envelope) {
                tmp.add(env);
            } else {
                break;
            }
        }
        
        // remove corresponding envelopes
        for(Container env : tmp) {
            listOfEnvsC.remove(env);
        }
        
        // remove category
        listOfEnvsC.remove(renamedCat);
        
        // update category
        renamedCat.setName(newName);
        DBMS.modifyCategory(renamedCat.getID(), renamedCat.getName());
        
        // return category with corresponding envelopes in alphabetical order
        for(int i = 0; i < listOfEnvsC.size(); i++) {
            Container curr = listOfEnvsC.get(i);
            if(curr instanceof Total                                                                     // insert before total
                    || (curr instanceof Category && curr.getID()==-1)                                    // insert before uncategorized category
                    || (curr instanceof Category && renamedCat.getName().compareTo(curr.getName())<0)) { // insert before other category (alphabetically)
                listOfEnvsC.add(i, renamedCat);
                for(Container env : tmp) {
                    listOfEnvsC.add(++i, env);
                }
                break;
            }
        }
        
        return true;
    }
    
    public void setCategorized(boolean isCategorized) {
        categorized = isCategorized;
    }
    
    public boolean isCategorized() {
        return categorized;
    }
    
    //---------------
    // Email getters/setters
    //---------------
    
    public int getEmailIndex(String addr) {
        for(int i = 0; i < listOfEmail.size(); i++) {
            if(addr.equalsIgnoreCase(listOfEmail.get(i).getAddress())) {
                return i;
            }
        }
        return -1;
    }
    
    public String getEmailCreated(int index) {
        return this.listOfEmail.get(index).getCreated();
    }
    
    public String getEmailModified(int index) {
        return this.listOfEmail.get(index).getModified();
    }
    
    public int getEmailAttempt(int index) {
        return this.listOfEmail.get(index).getAttempt();
    }
    
    public String getEmailUsername(int index) {
        return this.listOfEmail.get(index).getUsername();
    }
    
    public String getEmailAddress(int index) {
        return this.listOfEmail.get(index).getAddress();
    }
    
    public String[] getEmailAddresses() {
        listOfEmail.sort(Email.ADDR_COMPARATOR);
        String[] addresses = new String[listOfEmail.size()];
        for(int i = 0; i < addresses.length; i++) {
            addresses[i] = listOfEmail.get(i).getAddress();
        }
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
        return addresses;
    }
    
    public int getEmailCount() {
        return listOfEmail.size();
    }
    
    public boolean addEmail(String addr) {
        addr = addr.toLowerCase();
        addr = Utilities.stripHeaderFromAddress(addr);
        if(!Utilities.isValidEmailAddress(addr) || isEmailAlreadyAdded(addr)) {
            return false;
        }
        // add email to database
        Email em = DBMS.addEmail(addr);
        // check for email addition success
        if(em==null) {
            return false;
        }
        // add to model
        this.listOfEmail.add(em);
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
        return true;
    }
    
    public boolean isEmailAlreadyAdded(String addr) {
        return this.getEmail(addr) != null;
    }
    
    public boolean isEmailAuthenticated(String addr) {
        Email em = this.getEmail(addr);
        return em!=null && em.getAttempt()==0 && em.getUserID()!=-1;
    }
    
    public boolean setEmailUser(String addr, String username) {
        // get email and user
        Email em = getEmail(addr);
        Credential user = this.getUser(username);
        if(   em==null                        // email doesn't exist
           || user==null                      // user doesn't exist
           || em.getUserID()==user.getID()) { // email already assigned to user
            return false;
        }
        // update email in database
        DBMS.modifyEmail(em.getID(), 0, user.getID());
        // update email in model
        em.setAttempt(0);
        em.setUserID(user.getID());
        em.setUsername(user.getUsername());
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
        return true;
    }
    
    public boolean blockEmail(String addr) {
        // get email
        Email em = getEmail(addr);
        if(em==null || em.getAttempt()>5) { // email doesn't exist or already blocked
            return false;
        }
        // update email in database
        DBMS.modifyEmail(em.getID(), MAX_ATTEMPT, -1);
        // update email in model
        em.setAttempt(MAX_ATTEMPT);
        em.setUserID(-1);
        em.setUsername(Record.EMPTY_NAME);
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
        return true;
    }
    
    public boolean incrementEmailAttempt(String addr) {
        // get email
        Email em = getEmail(addr);
        if(em==null) { // email doesn't exist
            return false;
        }
        // update email in database
        int newAttempt = em.getAttempt()+1;
        DBMS.modifyEmail(em.getID(), newAttempt, -1);
        // update email in model
        em.setAttempt(newAttempt);
        em.setUserID(-1);
        em.setUsername(Record.EMPTY_NAME);
        listOfEmail.sort(Email.UN_ADDR_COMPARATOR);
        return true;
    }
    
    //---------------
    // User getters/setters
    //---------------
    
    public String[] getUsernames() {
        String[] usernames = new String[listOfUsers.size()];
        for(int i = 0; i < usernames.length; i++) {
            usernames[i] = listOfUsers.get(i).getUsername();
        }
        return usernames;
    }
    
    public String getPassword(String username) {
        return getUser(username).getPassword();
    }
    
    public boolean isUserAdmin(String username) {
        return getUser(username).getType()==Credential.TYPE_ADMIN;
    }
    
    public String getGmailPassword() {
        return gmail.getPassword();
    }
    
    public String getGmailUsername() {
        return gmail.getUsername();
    }
    
    public void setGmailPassword(String pw) {
        // update in model
        gmail.setPassword(pw);
        // update in database
        DBMS.modifyCredential(gmail.getID(), gmail.getUsername(), pw, gmail.isEnabled());
    }
    
    public void setGmailUsername(String un) {
        // update in model
        gmail.setUsername(un);
        // update in database
        DBMS.modifyCredential(gmail.getID(), un, gmail.getPassword(), gmail.isEnabled());
    }
    
    public boolean addUser(String username, String password) {
        if(username.equalsIgnoreCase(gmail.getUsername())) {
            return false;
        }
        Credential user = getUser(username);
        if(user!=null) {
            if(user.isEnabled()) {
                return false;
            } else {
                // enable disabled account and set password accordingly
                user.setEnabled(true);
                DBMS.modifyCredential(user.getID(), username, Utilities.getHash(password), true);
                listOfUsers.add(user);
                listOfUsers.sort(Credential.USERNAME_COMPARATOR);
                return true;
            }
        }
        user = DBMS.addCredential(username, password);
        if(user==null) {
            return false;
        } else {
            listOfUsers.add(user);
            listOfUsers.sort(Credential.USERNAME_COMPARATOR);
            allUsers.put(user.getID(), user);
            return true;
        }
    }
    
    public boolean disableUser(String username) {
        // get user
        Credential user = getUser(username);
        if(user==null || user.getType()==Credential.TYPE_ADMIN) {
            return false;
        }
        // get email addresses assigned to user
        for(Email em : listOfEmail) {
            if(em.getUserID()==user.getID()) {
                // block those email addresses
                em.setAttempt(5);
                DBMS.modifyEmail(em.getID(), 5, em.getUserID());
            }
        }
        // disable user in database
        DBMS.modifyCredential(user.getID(), user.getUsername(), user.getPassword(), false);
        // disable user in model
        user.setEnabled(false);
        listOfUsers.remove(user);
        return true;
    }
    
    public boolean isUserEnabled(String username) {
        Credential user = this.getUser(username);
        return user!=null && user.isEnabled();
    }
    
    /**
     * Sets password for specified user
     * @param username username of specified user
     * @param password new password; NOTE: this method stores the hash of the
     * password, not the actual password itself
     * @return true if successfully set, false otherwise
     */
    public boolean setUserPassword(String username, String password) {
        // get user
        Credential user = this.getUser(username);
        if(user==null) {
            return false;
        }
        // update user in database
        String pwHash = Utilities.getHash(password);
        DBMS.modifyCredential(user.getID(), user.getUsername(), pwHash, user.isEnabled());
        // update user in model
        user.setPassword(pwHash);
        return true;
    }
    
    public boolean renameUser(String oldUN, String newUN) {
        // get user
        Credential user = this.getUser(oldUN);
        if(newUN.equalsIgnoreCase(gmail.getUsername())
                || user==null
                || this.getUser(newUN)!=null
                || !Utilities.isValidUsername(newUN)) {
            return false;
        }
        // rename in database
        DBMS.modifyCredential(user.getID(), newUN, user.getPassword(), user.isEnabled());
        // rename in model
        user.setUsername(newUN);
        // update username associated with corresponsing email
        for(Email em : listOfEmail) {
            if(em.getUserID() == user.getID()) {
                em.setUsername(user.getUsername());
            }
        }
        // update username associated with corresponsing transaction
        for(Transaction t : listOfTrans) {
            if(t.getUserID() == user.getID()) {
                t.setUserName(user.getUsername());
            }
        }
        listOfUsers.sort(Credential.USERNAME_COMPARATOR);
        return true;
    }
    
    public boolean isUserAuthenticated(String username, String password) {
        String hashPW = Utilities.getHash(password);
        Credential user = this.getUser(username);
        return user!=null && user.getPassword().equals(hashPW) && user.isEnabled();
    }
    
    //---------------
    // Transaction getters/setters
    //---------------
    
    public void showTransactionsByDateRange(String acctName, String envName, String from, String to, boolean hideTx) {
        this.acctName    = acctName;
        this.envName     = envName;
        if(from.compareTo(to)>0) {
            this.fromDate = to;
            this.toDate   = from;
        } else {
            this.fromDate = from;
            this.toDate   = to;
        }
        this.hideTx      = hideTx;
        this.byDateRange = true;
        refreshTransactionsFromDatabase();
    }
    
    public void showTransactionsByIndexRange(String acctName, String envName, int from, int to, boolean hideTx) {
        this.acctName    = acctName;
        this.envName     = envName;
        this.fromIndex   = from;
        this.toIndex     = to;
        this.hideTx      = hideTx;
        this.byDateRange = false;
        refreshTransactionsFromDatabase();
    }
    
    private void refreshTransactionsFromDatabase() {
        DBMS.removeZeroAmtTransactions();
        Account  a = getAccount(acctName);
        Envelope e = getEnvelope(envName);
        if(byDateRange) {
            if(a!=null && e!=null) {        // both specified
                listOfTrans = DBMS.getTransactions(a.getID(), e.getID(), fromDate, toDate, hideTx);
            } else if(a==null && e!=null) { // envelope specified
                listOfTrans = DBMS.getTransactions(-1       , e.getID(), fromDate, toDate, hideTx);
            } else if(a!=null && e==null) { // account specified
                listOfTrans = DBMS.getTransactions(a.getID(), -1       , fromDate, toDate, hideTx);
            } else {                        // neither specified
                listOfTrans = DBMS.getTransactions(-1       , -1       , fromDate, toDate, hideTx);
            }
        } else {
            if(a!=null && e!=null) {        // both specified
                listOfTrans = DBMS.getTransactions(a.getID(), e.getID(), fromIndex, toIndex, hideTx);
            } else if(a==null && e!=null) { // envelope specified
                listOfTrans = DBMS.getTransactions(-1       , e.getID(), fromIndex, toIndex, hideTx);
            } else if(a!=null && e==null) { // account specified
                listOfTrans = DBMS.getTransactions(a.getID(), -1       , fromIndex, toIndex, hideTx);
            } else {                        // neither specified
                listOfTrans = DBMS.getTransactions(-1       , -1       , fromIndex, toIndex, hideTx);
            }
        }
        // update all account/envelope names
        for(Transaction t : listOfTrans) {
            // update account name as applicable
            int acctid = t.getAccountID();
            if(acctid!=-1) {
                Account acct = allAccts.get(acctid);
                if(acct!=null) {
                    t.setAccountName(acct.getName());
                }
            }
            // update envelopename as applicable
            int envid  = t.getEnvelopeID();
            if(envid!=-1) {
                Envelope env = allEnvs.get(envid);
                if(env!=null) {
                    t.setEnvelopeName(env.getName());
                }
            }
        }
    }
    
    public String getTransactionDate(int index) {
        return listOfTrans.get(index).getDate();
    }
    
    public String getTransactionDesc(int index) {
        return listOfTrans.get(index).getDesc();
    }
    
    public String getTransactionAmountString(int index) {
        return listOfTrans.get(index).getAmountString();
    }
    
    public int getTransactionAmountInteger(int index) {
        return listOfTrans.get(index).getAmount();
    }
    
    public String getTransactionRunTotal(int index) {
        return listOfTrans.get(index).getRunningTotal();
    }
    
    public String getTransactionAccount(int index) {
        return listOfTrans.get(index).getAccountName();
    }
    
    public String getTransactionUser(int index) {
        return allUsers.get(listOfTrans.get(index).getUserID()).getUsername();
    }
    
    public String getTransactionEnvelope(int index) {
        return listOfTrans.get(index).getEnvelopeName();
    }
    
    public String getTransactionCategory(int index) {
        Envelope e = allEnvs.get(listOfTrans.get(index).getEnvelopeID());
        if(e==null) {
            return "--";
        }
        Category c = allCats.get(e.getCategoryID());
        if(c==null) {
            return "--";
        }
        return c.getName().toLowerCase();
    }
    
    /**
     * Provides the number of transactions currently loaded in the model
     * @return transaction count loaded in the model
     */
    public int getTransactionCount() {
        return listOfTrans.size();
    }
    
    /**
     * Provides a String array of all transactions
     * @param reportProgressBar
     * @return String array of all transactions; first string in array is the
     * header string
     */
    public String[] getAllTransactions(javax.swing.JProgressBar reportProgressBar) {
        LinkedList<Transaction> trans = DBMS.getAllTransactions(reportProgressBar);
        int curr = 0;
        int max = trans.size()+1;
        int runTotal = 0;
        String[] transArray = new String[max];
        transArray[0] = "ID,TxID,Created,Modified,Date,Description,Amount,Run Total,Account,Envelope,User";
        for(Transaction t : trans) {
            runTotal += t.getAmount();
            transArray[curr] = "\n"
                    + t.getID() + ","
                    + t.getTransferID() + ","
                    + t.getCreated() + ","
                    + t.getModified() + ","
                    + t.getDate() + ","
                    + t.getDesc() + ","
                    + Utilities.amountToStringSimple(t.getAmount()) + ","
                    + Utilities.amountToStringSimple(runTotal) + ","
                    + t.getAccountName() + ","
                    + t.getEnvelopeName() + ","
                    + t.getUserName();
            reportProgressBar.setValue((++curr)*100/max);
        }
        return transArray;
    }
    
    public boolean setTransactionDate(int index, String date) {
        // get transaction
        Transaction t1 = listOfTrans.get(index);
        if(t1==null || !Utilities.isDate(date) || t1.getDate().equalsIgnoreCase(date)) return false;
        // get transfer transaction if exists
        Transaction t2 = DBMS.getTransaction(t1.getTransferID());
        // update t2
        if(t2!=null) {
            // update date in database
            DBMS.modifyTransaction(t2.getID(), date, t2.getDesc(), t2.getAmount(), t2.getAccountID(), t2.getEnvelopeID());
        }
        // update t1
        DBMS.modifyTransaction(t1.getID(), date, t1.getDesc(), t1.getAmount(), t1.getAccountID(), t1.getEnvelopeID());
        // refresh transactions in model
        refreshTransactionsFromDatabase();
        return false;
    }
    
    public boolean setTransactionDesc(int index, String desc) {
        if(desc.length()==1) {
            desc = desc.toUpperCase();
        } else {
            desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
        }
        Transaction t1 = listOfTrans.get(index);
        if(t1==null) return false;
        desc = Utilities.cleanTransactionDesc(desc);
        desc = Utilities.getShortDesc(desc);
        // update transfer transaction if exists
        Transaction t2 = getTransaction(t1.getTransferID());
        if(t2!=null) {
            // update account name as applicable
            int acctid = t2.getAccountID();
            if(acctid!=-1) {
                t2.setAccountName(allAccts.get(acctid).getName());
            }
            // update envelopename as applicable
            int envid = t2.getEnvelopeID();
            if(envid!=-1) {
                t2.setEnvelopeName(allEnvs.get(envid).getName());
            }
            
            if(t1.getAccountID()==-1) { // this is an envelope transfer
                // append transfer header to desc
                if(t1.getAmount()>0) {
                    desc = "(" + t2.getEnvelopeName() + " > " + t1.getEnvelopeName() + ") " + desc;
                } else {
                    desc = "(" + t1.getEnvelopeName() + " > " + t2.getEnvelopeName() + ") " + desc;
                }
            } else {                    // this is an account transfer
                // append transfer header to desc
                if(t1.getAmount()>0) {
                    desc = "*(" + t2.getAccountName() + " > " + t1.getAccountName() + ") " + desc;
                } else {
                    desc = "*(" + t1.getAccountName() + " > " + t2.getAccountName() + ") " + desc;
                }
                
            }
            // update in model
            t2.setDesc(desc);
            // update in database
            DBMS.modifyTransaction(t2.getID(), t2.getDate(), desc, t2.getAmount(), t2.getAccountID(), t2.getEnvelopeID());
        }
        // update in model
        t1.setDesc(desc);
        // update in database
        DBMS.modifyTransaction(t1.getID(), t1.getDate(), desc, t1.getAmount(), t1.getAccountID(), t1.getEnvelopeID());
        return true;
    }
    
    public boolean setTransactionAmount(int index, int amt) {
        Transaction t1 = listOfTrans.get(index);
        if(t1==null) return false;
        int diff = amt - t1.getAmount();
        // update in database
        DBMS.modifyTransaction(t1.getID(), t1.getDate(), t1.getDesc(), amt, t1.getAccountID(), t1.getEnvelopeID());
        // update in model
        t1.setAmount(amt);
        // Update corresponding envelope, category, and account amount for transaction
        Envelope e1 = allEnvs.get(t1.getEnvelopeID());
        if(e1!=null) {
            e1.addToAmount(diff);     // update envelopes amount in model
            Category c1 = allCats.get(e1.getCategoryID());    
            if(c1!=null) {
                c1.addToAmount(diff); // update category amount in model
            }
        }
        Account a1 = allAccts.get(t1.getAccountID());
        if(a1!=null) {
            a1.addToAmount(diff);     // update account amount in model
        }
        // update transfer transaction if applicable
        Transaction t2 = getTransaction(t1.getTransferID());
        if(t2!=null) {
            // update in database
            DBMS.modifyTransaction(t2.getID(), t2.getDate(), t2.getDesc(), -amt, t2.getAccountID(), t2.getEnvelopeID());
            // update in model
            t2.setAmount(-amt);
            // Update corresponding envelope, category, and account amount for transfer transaction
            Envelope e2 = allEnvs.get(t2.getEnvelopeID());
            if(e2!=null) {
                e2.addToAmount(-diff);     // update envelopes amount in model
                Category c2 = allCats.get(e2.getCategoryID());    
                if(c2!=null) {
                    c2.addToAmount(-diff); // update category amount in model
                }
            }
            Account a2 = allAccts.get(t2.getAccountID());
            if(a2!=null) {
                a2.addToAmount(-diff);     // update account amount in model
            }
            if(amt==0) {
                listOfTrans.remove(t2);
            }
        } else { // update totals since this is not a transfer
            addToEnvelopeTotal(diff);
            addToAccountTotal(diff);
        }
        if(amt==0) {
            listOfTrans.remove(t1);
        }
        DBMS.removeZeroAmtTransactions();
        return true;
    }
    
    public boolean setTransactionAccount(int index, String acctName) {
        Transaction t1 = listOfTrans.get(index);
        Account a2 = getAccount(acctName);
        if(t1==null || a2==null) return false;
        Account a1 = allAccts.get(t1.getAccountID());
        if(a1==null) return false;
        t1.setAccountID(a2.getID());
        t1.setAccountName(a2.getName());
        // get description with transfer header removed
        String desc = Utilities.getShortDesc(t1.getDesc());
        // update description of transfer transaction if exists
        Transaction t2 = getTransaction(t1.getTransferID());
        if(t2!=null) {
            // append transfer header to desc
            if(t1.getAmount()>0) {
                desc = "(" + t2.getAccountName() + " > " + t1.getAccountName() + ") " + desc;
            } else {
                desc = "(" + t1.getAccountName() + " > " + t2.getAccountName() + ") " + desc;
            }
            // append account transfer indicator to desc
            if(t1.getAccountID()!=-1) {
                desc = "*" + desc;
            }
            // update in model
            t2.setDesc(desc);
            // update desc in database
            DBMS.modifyTransaction(t2.getID(), t2.getDate(), desc, t2.getAmount(), t2.getAccountID(), t2.getEnvelopeID());
        }
        // update desc in model
        t1.setDesc(desc);
        // update account amounts in model
        a1.addToAmount(-t1.getAmount());
        a2.addToAmount( t1.getAmount());
        // update desc and acctid in database
        DBMS.modifyTransaction(t1.getID(), t1.getDate(), desc, t1.getAmount(), t1.getAccountID(), t1.getEnvelopeID());
        return true;
    }
    
    public boolean setTransactionEnvelope(int index, String envName) {
        Transaction t1 = listOfTrans.get(index);
        Envelope e2 = getEnvelope(envName);
        if(t1==null || e2==null) return false;
        Envelope e1 = allEnvs.get(t1.getEnvelopeID());
        if(e1==null) return false;
        Category c1 = allCats.get(e1.getCategoryID());
        Category c2 = allCats.get(e2.getCategoryID());
        if(c1==null || c2==null) return false;
        t1.setEnvelopeID(e2.getID());
        t1.setEnvelopeName(e2.getName());
        // get description with transfer header removed
        String desc = Utilities.getShortDesc(t1.getDesc());
        // update description of transfer transaction if exists
        Transaction t2 = getTransaction(t1.getTransferID());
        if(t2!=null) {
            // append transfer header to desc
            if(t1.getAmount()>0) {
                desc = "(" + t2.getEnvelopeName() + " > " + t1.getEnvelopeName() + ") " + desc;
            } else {
                desc = "(" + t1.getEnvelopeName() + " > " + t2.getEnvelopeName() + ") " + desc;
            }
            // update in model
            t2.setDesc(desc);
            // update desc in database
            DBMS.modifyTransaction(t2.getID(), t2.getDate(), desc, t2.getAmount(), t2.getAccountID(), t2.getEnvelopeID());
        }
        // update desc in model
        t1.setDesc(desc);
        // update envelope & category amounts in model
        e1.addToAmount(-t1.getAmount());
        c1.addToAmount(-t1.getAmount());
        e2.addToAmount( t1.getAmount());
        c2.addToAmount( t1.getAmount());
        // update desc and acctid in database
        DBMS.modifyTransaction(t1.getID(), t1.getDate(), desc, t1.getAmount(), t1.getAccountID(), t1.getEnvelopeID());
        return true;
    }
    
    public boolean addTransaction(String date, String desc, String amt, String acct, String user, String env) {
        desc = Utilities.capitalizeFirstCharacter(desc);
        Account    a = getAccount(acct);
        Envelope   e = getEnvelope(env);
        Credential u = getUser(user);
        if(a==null || e==null || u==null) return false;
        // update database
        Transaction t = DBMS.addTransaction(date, desc, Utilities.amountToInteger(amt), a.getID(), u.getID(), e.getID());
        // update model
        a.addToAmount(t.getAmount());
        e.addToAmount(t.getAmount());
        addToAccountTotal(t.getAmount());
        addToEnvelopeTotal(t.getAmount());
        Category c = allCats.get(e.getCategoryID());
        if(c!=null) {
            c.addToAmount(t.getAmount());
        }
        // refresh transactions in model
        refreshTransactionsFromDatabase();
        return false;
    }
    
    /**
     * Transfer funds between 2 accounts or 2 envelopes by adding 2 transactions
     * (1 that deducts the specified amount from the 'from' container and 1 that
     * adds the specified amount to the 'to' container), then linking those
     * transactions together so any future updates to 1 transaction will
     * automatically reflect in the other
     * @param date date transfer took place, format: yyyy-mm-dd
     * @param desc description of transaction
     * @param amt amount of transfer; must not equal 0
     * @param from envelope or account to transfer funds from
     * @param to envelope or account to transfer funds to; must match container
     * type of 'from' (e.g. if 'from' is an Envelope, 'to' must also be an
     * Envelope)
     * @param username username of individual initiating the transfer
     * @return message indicating results of transfer
     */
    public String addTransfer(String date, String desc, String amt, String from, String to, String username) {
        // Initialize result string
        String result = "ERROR: unable to transfer funds";
        // clean description
        desc = Utilities.cleanTransactionDesc(desc);
        // switch from and to if amt is negative (transfer amount should be positive)
        int amtInt = Utilities.amountToInteger(amt);
        if(amtInt<0) {
            String tmp = from;
            from = to;
            to = tmp;
            amtInt*=-1;
        }
        // get accounts/envelopes corresponding to the specified from/to
        Container c1 = this.getContainer(from);
        Container c2 = this.getContainer(to);
        // get user
        Credential usr = this.getUser(username);
        if(amtInt==0 || c1==null || c2==null || usr==null || c1.getID()==c2.getID()) return result;
        int oldFromAmt = c1.getAmount();
        int oldToAmt   = c2.getAmount();
        // append transfer header to description
        desc = "(" + c1.getName() + " > " + c2.getName() + ") " + desc;
        // get corresponding account/envelope IDs
        int acctid1 = -1, acctid2 = -1, envid1 = -1, envid2 = -1, userid = usr.getID();
        boolean acctTransfer = false;
        if(c1 instanceof Envelope && c2 instanceof Envelope) {      // Envelope transfer
            envid1 = c1.getID();
            envid2 = c2.getID();
            // reflect category transfer in model
            Category cat1 = this.allCats.get(((Envelope) c1).getCategoryID());
            Category cat2 = this.allCats.get(((Envelope) c2).getCategoryID());
            if(cat1.getID()!=cat2.getID()) {
                cat1.addToAmount(-amtInt);
                cat2.addToAmount( amtInt);
            }
        } else if(c1 instanceof Account && c2 instanceof Account) { // Account transfer
            acctid1 = c1.getID();
            acctid2 = c2.getID();
            desc = "*" + desc;
            acctTransfer = true;
        } else {
            return result;
        }
        // reflect account/envelope transfer in model
        c1.addToAmount(-amtInt);
        c2.addToAmount( amtInt);
        // add to database
        Transaction t1 = DBMS.addTransaction(date, desc, -amtInt, acctid1, userid, envid1);
        Transaction t2 = DBMS.addTransaction(date, desc,  amtInt, acctid2, userid, envid2);
        // reflect transfer in database
        DBMS.setTransactionTransfer(t1.getID(), t2.getID());
        // update transactions table
        refreshTransactionsFromDatabase();
        if(acctTransfer) {
            result = "ACCOUNT TRANSFER:";
        } else {
            result = "ENVELOPE TRANSFER:";
        }
        result += "\n"
                + " amt: " + Utilities.amountToString(amtInt) + "\n"
                + "FROM: '" + from + "'\n"
                + " " + Utilities.amountToString(oldFromAmt) + " >> " + Utilities.amountToString(c1.getAmount()) + "\n"
                + "TO: '" + to + "'\n"
                + " " + Utilities.amountToString(oldToAmt) + " >> " + Utilities.amountToString(c2.getAmount());
        return result;
    }
    
}
