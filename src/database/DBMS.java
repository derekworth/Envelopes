package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import misc.Utilities;

/**
 * Created on Aug 17, 2013
 * @author Derek Worth
 */
public class DBMS {
    
    private static final String DATABASE = "database.db";
    private static final String DRIVER_NAME = "org.sqlite.JDBC";
    private static final String URL = "jdbc:sqlite:" + DATABASE;
    private static final int TIMEOUT = 30;
    
    private LinkedList<Account> accounts;
    private LinkedList<Envelope> envelopes;
    private LinkedList<Category> categories;
    private LinkedList<User> users;
    private LinkedList<Email> email;
    private LinkedList<Transaction> transactions;
    
    // CONSTRUCTOR
    
    public DBMS() {
        // setup database if not already done so
        initializeDB();
        // pull data from database
        initializeModel();
    }
    
    //==========================================================================
    // PUBLIC METHODS
    //==========================================================================
    
    public LinkedList<Account> getAccounts() {
        return accounts;
    }
    
    public LinkedList<Envelope> getEnvelopes() {
        return envelopes;
    }
    
    public LinkedList<Category> getCategories() {
        return categories;
    }
    
    public LinkedList<User> getUsers() {
        return users;
    }
    
    public LinkedList<Email> getEmail() {
        return email;
    }
    
    public LinkedList<Transaction> getTransactions() {
        return transactions;
    }
    
    //==========================================================================
    // PRIVATE METHODS
    //==========================================================================
    
    private void initializeModel() {
        accounts     = getAccountsFromDB();
        envelopes    = getEnvelopesFromDB();
        categories   = getCategoriesFromDB(); // must set category amounts
        users        = getUsersFromDB();
        email        = getEmailFromDB();
        transactions = getTransactionsFromDB(25, 0, null, null, false);
        
        setCategoryAmounts(); // sets amounts for each category
    }
    
    // GETTERS
    
    private Account getAccount(int aid) {
        if(aid==-1) return null;
        for(Account a : accounts) {
            if(a.getId()==aid) return a;
        }
        return null;
    }
    
    private Envelope getEnvelope(int eid) {
        if(eid==-1) return null;
        for(Envelope e : envelopes) {
            if(e.getId()==eid) return e;
        }
        return null;
    }
    
    private Category getCategory(int cid) {
        if(cid==-1) return null;
        for(Category c : categories) {
            if(c.getId()==cid) return c;
        }
        return null;
    }
    
    private User getUser(int uid) {
        if(uid==-1) return null;
        for(User u : users) {
            if(u.getId()==uid) return u;
        }
        return null;
    }
    
    // SETTERS
    
    private void setCategoryAmounts() {
        for(Envelope e : envelopes) {
            if(e.getCategoryId()!=-1) {
                Category c = getCategory(e.getCategoryId());
                c.setAmount(c.getAmount() + e.getAmount());
            }
        }
    }
    
    //==========================================================================
    // PRIVATE STATIC METHODS
    //==========================================================================
    
    // DATABASE SETUP
    
    /**
     * Creates tables in database and two users (gmail and admin)
     * @return true if tables and users created, false otherwise
     */
    private static boolean initializeDB() {
        String ts = Utilities.getTimestamp();
        // initialize tables
        String [] queries = {
            /*INITIALIZE DATABASE*/
            "CREATE TABLE accts (created TEXT NOT NULL, modified TEXT NOT NULL, enabled INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL UNIQUE)",
            "CREATE TABLE cats  (created TEXT NOT NULL, modified TEXT NOT NULL, enabled INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL UNIQUE)",
            "CREATE TABLE envs  (created TEXT NOT NULL, modified TEXT NOT NULL, enabled INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY, catid INTEGER NOT NULL, name TEXT NOT NULL UNIQUE, FOREIGN KEY (catid) REFERENCES cats (id))",
            "CREATE TABLE trans (created TEXT NOT NULL, modified TEXT NOT NULL, id INTEGER NOT NULL PRIMARY KEY, acctid INTEGER NOT NULL, envid INTEGER NOT NULL, userid INTEGER NOT NULL, date TEXT NOT NULL, desc TEXT NOT NULL, amt REAL NOT NULL, tx INTEGER DEFAULT -1, FOREIGN KEY (acctid) REFERENCES accts (id), FOREIGN KEY (envid) REFERENCES envs (id), FOREIGN KEY (userid) REFERENCES users (id))",
            "CREATE TABLE users (created TEXT NOT NULL, modified TEXT NOT NULL, enabled INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY, type INTEGER NOT NULL, un TEXT NOT NULL UNIQUE, pw TEXT NOT NULL)",
            "CREATE TABLE email (created TEXT NOT NULL, modified TEXT NOT NULL, attempt INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY, userid INTEGER NOT NULL, addr TEXT NOT NULL UNIQUE, FOREIGN KEY (userid) REFERENCES users (id))",
            
            /*INITIALIZE GMAIL AND ADMIN ACCOUNTS*/
            "INSERT INTO users (created, modified, enabled, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 1, 'gmail_username', 'gmail_password')",
            "INSERT INTO users (created, modified, enabled, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 2, 'admin', '" + Utilities.getHash("password") + "')"};
        return executeQueries(queries);
    }
    
    /**
     * Resets database with empty tables and inserts admin and gmail user
     * accounts
     */
    private static void resetDB() {
        String [] queries = {
            /*DROP TABLES*/
            "DROP TABLE IF EXISTS accts",
            "DROP TABLE IF EXISTS cats",
            "DROP TABLE IF EXISTS envs",
            "DROP TABLE IF EXISTS trans",
            "DROP TABLE IF EXISTS users",
            "DROP TABLE IF EXISTS email"};
        executeQueries(queries);
        initializeDB();
    }
    
    /**
     * Updates the database with the given query
     * @param query SQL query to update database (ex. "UPDATE email SET
     * modified='2013-08-17 15:50:44', attempt=5 WHERE id=3")
     * @return true if successful, false otherwise
     */
    private static boolean executeQuery(String query) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // create tables
                stmt.executeUpdate(query);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
        return true;
    }
    
    /**
     * Updates the database with the given queries
     * @param queries String array of SQL queries to update database
     * @return true if successful, false otherwise
     */
    private static boolean executeQueries(String[] queries) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // create tables
                for(int i = 0; i < queries.length; i++) {
                    stmt.executeUpdate(queries[i]);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
        return true;
    }
    
    // GETTERS
    
    /**
     * Pulls all categories from the database and inserts them into a linked list.
     * The amount in each category is NOT updated prior to returning the linked
     * list of categories and must be set after the linked list is generated.
     * @return Linked list of categories
     */
    private static LinkedList<Category> getCategoriesFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM cats ORDER BY name");
                LinkedList<Category> cats = new LinkedList();
                while(rs.next()) {
                    String ts = Utilities.getTimestamp();
                    //String created, String modified, boolean enabled, int id, String name, double amt
                    cats.add(new Category(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("enabled")==1,
                            rs.getInt("id"),
                            rs.getString("name")));
                }
                return cats;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Pulls all envelopes from the database and inserts them into a linked list.
     * The amount in each envelope is updated prior to returning the linked list
     * of envelopes. However, envelope categories are not set and must be
     * updated after linked list is generated.
     * @return Linked list of all envelopes
     */
    private static LinkedList<Envelope> getEnvelopesFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM envs ORDER BY name");
                
                LinkedList<Envelope> envs = new LinkedList();
                while(rs.next()) {
                    Envelope e = new Envelope(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("enabled")==1,
                            rs.getInt("id"),
                            rs.getInt("catid"),
                            rs.getString("name"),
                            0);
                    e.setAmount(getEnvelopeAmountFromDB(e));
                    envs.add(e);
                }
                return envs;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Pulls all accounts from the database and inserts them into a linked list.
     * The amount in each account is updated prior to returning the linked list
     * of accounts.
     * @return Linked list of all accounts
     */
    private static LinkedList<Account> getAccountsFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM accts ORDER BY name");
                LinkedList<Account> accts = new LinkedList();
                while(rs.next()) {
                    Account a = new Account(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("enabled")==1,
                            rs.getInt("id"),
                            rs.getString("name"),
                            0);
                    a.setAmount(getAccountAmountFromDB(a));
                    accts.add(a);
                }
                return accts;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Pulls all email addresses from the database and inserts them into a
     * linked list. The associated User object for each email is NOT updated
     * prior to returning the linked list of email addresses (only the user id)
     * and must be set after the linked list is generated.
     * @return Linked list of email addresses
     */
    private static LinkedList<Email> getEmailFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM email ORDER BY userid, addr");
                LinkedList<Email> email = new LinkedList();
                while(rs.next()) {
                    email.add(new Email(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("attempt"),
                            rs.getInt("id"),
                            rs.getInt("userid"),
                            rs.getString("addr")
                    ));
                }
                return email;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Pulls all users from the database and inserts them into a linked list.
     * The list includes all users with the following types:
     *   0 = Unprivileged user
     *   1 = Gmail
     *   2 = Admin
     * @return Linked list of all users
     */
    private static LinkedList<User> getUsersFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE ORDER BY un");
                LinkedList<User> users = new LinkedList();
                while(rs.next()) {
                    users.add(new User(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("enabled")==1,
                            rs.getInt("id"),
                            rs.getInt("type"),
                            rs.getString("un"),
                            rs.getString("pw")
                    ));
                }
                return users;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves transactions from the database based on specified filters
     * and date range.
     * @param from beginning date (yyyy-mm-dd format) of range
     * @param to ending date (yyyy-mm-dd format) of range
     * @param acct account in which to filter transactions by; pass a 'null' for all accounts
     * @param env envelope in which to filter transactions by; pass a 'null' for all envelopes
     * @param hideTx Transfer transactions are pairs of transactions that move funds
     * from one envelope to another OR one account to another. Specify true if you
     * wish to hide these transfer pairs or false otherwise.
     * @return linked list of transactions based on given filter inputs
     */
    private static LinkedList<Transaction> getTransactionsFromDB(String from, String to, Account acct, Envelope env, boolean hideTx) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to)) { // checks for valid dates
            return null;
        }
        int a, e;
        String query;
        String whereContainer = "";
        
        if (from.compareToIgnoreCase(to)>0) { // makes 'from' the earlier date
            String tmp = from;
            from = to;
            to = tmp;
        }
        String whereDate = " date>='" + from + "' AND date<='" + to + "'";
        
        if(acct==null) { // check for account
            a = -1;
        } else {
            a = acct.getId();
        }
        
        if(env==null) { // check for envelope
            e = -1;
        } else {
            e = env.getId();
        }

        // set search criteria as necessary according to XX (AE) where:
        // 0X = account not specified   1X = account specified
        // X0 = envelope not specified  X1 = envelope specified
        if(a==-1 && e!=-1) {        // 01
            whereContainer = " envid="  + e + " AND";
        } else if(a!=-1 && e==-1) { // 10
            whereContainer = " acctid=" + a + " AND";
        } else if(a!=-1 && e!=-1) { // 11
            whereContainer = " acctid=" + a + " AND envid="  + e + " AND";
        } else {                    // 00
            // do nothing (no WHERE search criteria)
        }
        
        // builds query
        if(hideTx) { // hide transfer transactions
            query = "SELECT * FROM trans WHERE (acctid!=-1 AND envid!=-1) AND" + whereContainer + whereDate + " ORDER BY date DESC, id DESC";
        } else {
            query = "SELECT * FROM trans WHERE" + whereContainer + whereDate + " ORDER BY date DESC, id DESC";
        }
        
        // execute query
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("id"),
                            rs.getInt("acctid"),
                            rs.getInt("envid"),
                            rs.getInt("userid"),
                            rs.getString("date"),
                            rs.getString("desc"),
                            rs.getDouble("amt"),
                            rs.getInt("tx")
                            ));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Retrieves transactions from the database based on specified filters and
     * desired transaction quantity.
     * @param qty number of transactions to be retrieved
     * @param offset number of transactions to skip from the beginning of all transactions
     * @param acct account in which to filter transactions by; pass a 'null' for all accounts
     * @param env envelope in which to filter transactions by; pass a 'null' for all envelopes
     * @param hideTx Transfer transactions are pairs of transactions that move funds
     * from one envelope to another OR one account to another. Specify true if you
     * wish to hide these transfer pairs or false otherwise.
     * @return linked list of transactions based on given filter inputs
     */
    private static LinkedList<Transaction> getTransactionsFromDB(int qty, int offset, Account acct, Envelope env, boolean hideTx) {
        int a, e;
        String query;
        String where = "", hideTxCriteria = "";
        
        if(acct==null) {
            a = -1;
        } else {
            a = acct.getId();
        }
        
        if(env==null) {
            e = -1;
        } else {
            e = env.getId();
        }
        
        // set search criteria as necessary according to XX (AE) where:
        // 0X = account not specified   1X = account specified
        // X0 = envelope not specified  X1 = envelope specified
        if(hideTx) {
            hideTxCriteria = " (acctid!=-1 AND envid!=-1) AND";
        }
        if(a==-1 && e!=-1) {        // 01
            where = "WHERE" + hideTxCriteria + " envid="  + e;
        } else if(a!=-1 && e==-1) { // 10
            where = "WHERE" + hideTxCriteria + " acctid=" + a;
        } else if(a!=-1 && e!=-1) { // 11
            where = "WHERE" + hideTxCriteria + " acctid=" + a + " AND envid="  + e;
        } else {                    // 00
            if(hideTx) {
                where = "WHERE (acctid!=-1 AND envid!=-1)";
            }
        }
        query = "SELECT * FROM trans " + where + " ORDER BY date DESC, id DESC LIMIT " + (qty+offset);
        
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                LinkedList<Transaction> trans = new LinkedList();
                int count = 1;
                while(rs.next()) {
                    if(count <= offset) { // skip to first transaction after offset
                        count++;
                    } else {
                        trans.add(new Transaction(
                                rs.getString("created"),
                                rs.getString("modified"),
                                rs.getInt("id"),
                                rs.getInt("acctid"),
                                rs.getInt("envid"),
                                rs.getInt("userid"),
                                rs.getString("date"),
                                rs.getString("desc"),
                                rs.getDouble("amt"),
                                rs.getInt("tx")
                        ));
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    // SETTERS
    
    private static boolean updateEnvelopeInDB(Envelope env, boolean en, String name, int cid) {
        boolean sameName = env.getName().equalsIgnoreCase(name);
        boolean sameEn   = env.isEnabled()==en;
        boolean sameCat  = env.getCategoryId()==cid;
        
        // prevents updates if specified attributes are already set or name is invalid
        if(!Utilities.isValidContainerName(name) || // checks name is valid
          (sameName && sameEn && sameCat)) {        // checks for changes
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // get enabled
                int enabled = 0;
                if(en) enabled = 1;
                // update database
                stmt.executeUpdate("UPDATE envs SET modified='" + ts + "', enabled="+ enabled +", name='" + name + "', catid=" + cid + " WHERE id=" + env.getId());
                // update object
                env.setModified(ts);
                env.setEnabled(en);
                env.setName(name);
                env.setCategoryId(cid);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static boolean updateAccountInDB(Account acct, boolean en, String name) {
        boolean sameName = acct.getName().equalsIgnoreCase(name);
        boolean sameEn   = acct.isEnabled()==en;
        
        // prevents updates if specified attributes are already set or name is invalid
        if(!Utilities.isValidContainerName(name) || // checks name is valid
          (sameName && sameEn)) {                   // checks for changes
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // get enabled
                int enabled = 0;
                if(en) enabled = 1;
                // update database
                stmt.executeUpdate("UPDATE accts SET modified='" + ts + "', enabled="+ enabled +", name='" + name + "' WHERE id=" + acct.getId());
                // update object
                acct.setModified(ts);
                acct.setEnabled(en);
                acct.setName(name);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static boolean updateEmailInDB(Email em, int attempt, int uid) {
        boolean sameAttempt = em.getAttempt()==attempt;
        boolean sameUser    = em.getUserId()==uid;
        
        // prevents updates if specified attributes are already set
        if((sameAttempt && sameUser)) { // checks for changes
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // update database
                stmt.executeUpdate("UPDATE email SET modified='" + ts + "', attempt=" + attempt + ", userid=" + uid + " WHERE id=" + em.getId());
                // update object
                em.setModified(ts);
                em.setAttempt(attempt);
                em.setUserId(uid);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static boolean updateUserInDB(User usr, boolean en, String un, String pw) {
        un = un.toLowerCase();
        boolean sameEn = usr.isEnabled()==en;
        boolean sameUn = usr.getUsername().equals(un);
        boolean samePw = usr.getPassword().equals(pw);
        // prevents updates if specified attributes are already set
        if(sameEn && sameUn && samePw) {
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // update database
                int enabled = 0;
                if(en) enabled = 1;
                stmt.executeUpdate("UPDATE users SET modified='" + ts + "', enabled=" + enabled + ", un='" + un + "', pw='" + pw + "' WHERE id=" + usr.getId());
                // update object
                usr.setEnabled(en);
                usr.setModified(ts);
                usr.setUsername(un);
                usr.setPassword(pw);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static boolean updateCategoryInDB(Category cat, boolean en, String name) {
        boolean sameName = cat.getName().equalsIgnoreCase(name);
        boolean sameEn   = cat.isEnabled()==en;
        
        // prevents updates if specified attributes are already set or name is invalid
        if(!Utilities.isValidContainerName(name) || // checks name is valid
          (sameName && sameEn)) {                   // checks for changes
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // get enabled
                int enabled = 0;
                if(en) enabled = 1;
                // update database
                stmt.executeUpdate("UPDATE cats SET modified='" + ts + "', enabled="+ enabled +", name='" + name + "' WHERE id=" + cat.getId());
                // update object
                cat.setModified(ts);
                cat.setEnabled(en);
                cat.setName(name);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static boolean updateTransactionInDB(Transaction tran, int aid, int eid, String date, String desc, double amt, int tid) {
        boolean sameAcct = tran.getAccountId()==aid;
        boolean sameEnv  = tran.getEnvelopeId()==eid;
        boolean sameDate = tran.getDate().equals(date);
        boolean sameDesc = tran.getDescription().equals(desc);
        boolean sameAmt  = tran.getAmount()==amt;
        boolean sameTx   = tran.getTxId()==tid;
        
        if(!Utilities.isDate(date) || (sameDate && sameDesc && sameAmt && sameAcct && sameEnv && sameTx)) {
            return false;
        }
        
        // something has changed, so let's modify the database record
        String ts = Utilities.getTimestamp(); // modified timestamp
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // update database
                stmt.executeUpdate("UPDATE trans SET modified='" + ts + "', acctid=" + aid +", envid=" + eid + ", tx=" + tid + ", date='" + date + "', desc='" + desc + "', amt=" + amt + " WHERE id=" + tran.getId());
                // update object
                tran.setModified(ts);
                tran.setAccountId(aid);
                tran.setEnvelopeId(eid);
                tran.setTxId(tid);
                tran.setDate(date);
                tran.setDescription(desc);
                tran.setAmount(amt);
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    private static void mergeEnvelopesInDB(Envelope from, Envelope to) {
//        // move transactions
//        Transaction transaction, partner;
//        while(getTransactionCount(from)>0) {
//            transaction = getTransactions(from, 1).getFirst();
//            partner = new Transaction(transaction.getTxTransaction());
//            
//            if(transaction.getTxTransaction()!=-1 && partner.getEnvelope().getName().equalsIgnoreCase(to.getName())) {
//                // delete same-envelope transfer transactions (they cancel each other out)
//                executeQuery("DELETE FROM trans WHERE id=" + transaction.getId());
//                executeQuery("DELETE FROM trans WHERE id=" + partner.getId());
//            } else {
//                // set new envelope
//                transaction.setEnvelope(to);
//            }
//        }
//        // remove (disable) 'from' envelope
//        executeQuery("UPDATE envs SET enabled=0 WHERE id=" + from.getId());
    }
        
    // ADD TO DATABASE
    
    private static Envelope addEnvelopeToDB(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new envelope
            if(executeQuery("INSERT INTO envs (created, modified, enabled, catid, name) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + name + "')")) {
                // get envelope id
                try {
                    // register the driver
                    Class.forName(DRIVER_NAME);
                    // connect to database
                    try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                        stmt.setQueryTimeout(TIMEOUT);
                        // execute query
                        ResultSet rs = stmt.executeQuery("SELECT id FROM envs WHERE name='" + name + "'");
                        // Envelope(String created, String modified, boolean enabled, int id, String name, double amt)
                        return new Envelope(ts, ts, true, rs.getInt("id"), -1, name, 0);
                    }
                } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
            }
        }
        return null;
    }
    
    private static Account addAccountToDB(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new account
            if(executeQuery("INSERT INTO accts (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')")) {
                // get account id
                try {
                    // register the driver
                    Class.forName(DRIVER_NAME);
                    // connect to database
                    try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                        stmt.setQueryTimeout(TIMEOUT);
                        // execute query
                        ResultSet rs = stmt.executeQuery("SELECT id FROM accts WHERE name='" + name + "'");
                        // Account(String created, String modified, boolean enabled, int id, String name, double amt)
                        return new Account(ts, ts, true, rs.getInt("id"), name, 0);
                    }
                } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
            }
        }
        return null;
    }
    
    private static Email addEmailToDB(String addr, User usr) {
        // formats input
        addr = addr.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents checks that user exists
        int uid = -1;
        int attempt = 1;
        if(usr!=null) {
            uid = usr.getId();
            if(!usr.isGmail()) {
                attempt = 0;
            }
        }
        executeQuery("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', " + attempt + ", " + uid + ", '" + addr + "')");
        // get email id
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM email ORDER BY id DESC LIMIT 1");
                return new Email(ts, ts, attempt, rs.getInt("id"), uid, addr);
            }
        } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
        return null;
    }
    
    private static User addUserToDB(String un, String pw) {
        // format input
        un = un.toLowerCase();
        pw = Utilities.getHash(pw);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        {
            // creates new account in database where types: 0 = User
            //                                              1 = Gmail
            //                                              2 = Admin
            executeQuery("INSERT INTO users (created, modified, enabled, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 0, '" + un + "', '" + pw + "')");
            // sets variables accordingly
            try {
                // register the driver
                Class.forName("org.sqlite.JDBC");
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id DESC LIMIT 1");
                    // User(String created, String modified, boolean enabled, int id, int type, String un, String pw)
                    return new User(ts, ts, true, rs.getInt("id"), 0, un, pw);
                }
            } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
        }
        return null;
    }
    
    private static Category addCategoryToDB(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new category
            if(executeQuery("INSERT INTO cats (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')")) {
                // get category id
                try {
                    // register the driver
                    Class.forName(DRIVER_NAME);
                    // connect to database
                    try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                        stmt.setQueryTimeout(TIMEOUT);
                        // execute query
                        ResultSet rs = stmt.executeQuery("SELECT id FROM cats WHERE name='" + name + "'");
                        // create obj with appropriate attributes
                        return new Category(ts, ts, true, rs.getInt("id"), name);
                    }
                } catch (ClassNotFoundException | SQLException e) { }
            }
        }
        return null;
    }
    
    private static Transaction addTransactionToDB(Account acct, Envelope env, User usr, String date, String desc, double amt) {
        desc = Utilities.trimInvalidCharacters(desc);
        desc = Utilities.removeDoubleApostrophes(desc);
        desc = Utilities.doubleApostrophes(desc);
        if (usr!=null && usr.isEnabled() && !usr.isGmail()) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // sets current date if date format is invalid
            if (!Utilities.isDate(date)) {
                date = ts.substring(0, 10);
            }
            // gets account and envelope IDs
            int aid = -1;
            if(acct!=null) aid = acct.getId();
            int eid = -1;
            if(env!=null) eid = env.getId();
            String query = "INSERT INTO trans (created, modified, acctid, envid, userid, date, desc, amt) VALUES ('" + ts + "', '" + ts + "', " + aid + ", " + eid + ", " + usr.getId() + ", '" + date + "', '" + desc + "', " + amt + ")";
            if(executeQuery(query)) {
                // sets variables accordingly
                try {
                    // register the driver
                    Class.forName(DRIVER_NAME);
                    // connect to database
                    try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                        stmt.setQueryTimeout(TIMEOUT);
                        // execute query
                        ResultSet rs = stmt.executeQuery("SELECT * FROM trans ORDER BY id DESC LIMIT 1");
                        // sets variables accordingly
                        //Transaction(String created, String modified, int id, int aid, int eid, int uid, String date, String desc, double amt, int tid)
                        Transaction t = new Transaction(ts, ts, rs.getInt("id"), aid, eid, usr.getId(), date, desc, 0, -1);
                        return t;
                    }
                } catch (ClassNotFoundException | SQLException e) {}
            }
        }
        return null;
    }
    
    // REMOVE FROM DATABASE
    
    private static boolean removeTransactionInDB(Transaction tran) {
        return executeQuery("DELETE FROM trans WHERE id=" + tran.getId());
    }
        
    private static boolean removeZeroAmtTransactionsInDB() {
        return executeQuery("DELETE FROM trans WHERE amt=0");
    }
    
    // HELPER METHODS
    
    private static double getAccountAmountFromDB(Account acct) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query to find amount
                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid=" + acct.getId());
                // sets variables accordingly
                return rs.getDouble(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return 0;
        }
    }
    
    /**
     * Provides the amount in an account by summing all transactions on and before
     * the given "as of" date.
     * @param acct Account of the amount requested; specify null if requesting the
     * amount for ALL accounts
     * @param asOfDate Date ("yyyy-mm-dd" format) at which to calculate the amount
     * @return the amount of money in the specified account as of the specified date
     * or -999999999 if error occurs
     */
    private static double getAccountAsOfAmountFromDB(Account acct, String asOfDate) {
        if(!Utilities.isDate(asOfDate)) {
            return -999999999;
        }
        if(acct==null) { // all accounts
            try {
                // register the driver
                Class.forName(DRIVER_NAME);
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid!=-1 AND date<'" + asOfDate + "'");
                    return rs.getDouble(1);
                }
            } catch (ClassNotFoundException | SQLException e) {
                return -999999999;
            }
        } else { // a specific account
            try {
                // register the driver
                Class.forName(DRIVER_NAME);
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid=" + acct.getId() + " AND date<'" + asOfDate + "'");
                    return rs.getDouble(1);
                }
            } catch (ClassNotFoundException | SQLException e) {
                return -999999999;
            }
        }
    }
    
    private static double getEnvelopeAmountFromDB(Envelope env) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query to find amount
                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE envid=" + env.getId());
                // sets variables accordingly
                return rs.getDouble(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return 0;
        }
    }
        
    /**
     * Provides the amount in an envelope by summing all transactions on and before
     * the given "as of" date.
     * @param env Envelope of the amount requested; specify null if requesting the
     * amount for ALL envelopes
     * @param asOfDate Date ("yyyy-mm-dd" format) at which to calculate the amount
     * @return the amount of money in the specified envelope as of the specified date
     * or -999999999 if error occurs
     */
    private static double getEnvelopeAsOfAmountFromDB(Envelope env, String asOfDate) {
        if(!Utilities.isDate(asOfDate)) {
            return -999999999;
        }
        if(env==null) { // all envelopes
            try {
                // register the driver
                Class.forName(DRIVER_NAME);
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE envid!=-1 AND date<'" + asOfDate + "'");
                    return rs.getDouble(1);
                }
            } catch (ClassNotFoundException | SQLException e) {
                return -999999999;
            }
        } else {
            try {
                // register the driver
                Class.forName(DRIVER_NAME);
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE envid=" + env.getId() + " AND date<'" + asOfDate + "'");
                    return rs.getDouble(1);
                }
            } catch (ClassNotFoundException | SQLException e) {
                return -999999999;
            }
        }
    }
    
    private static int getTransactionCountFromDB() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT count() FROM trans");
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -1;
        }
    }
    
    private static int getTransactionCountFromDB(Account acct) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT count() FROM trans WHERE acctid=" + acct.getId());
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -1;
        }
    }
    
    private static int getTransactionCountFromDB(Envelope env) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT count() FROM trans WHERE envid=" + env.getId());
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -1;
        }
    }
}
