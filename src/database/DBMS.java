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
    
    public static final String DATABASE = "database.db";
    public static final String DRIVER_NAME = "org.sqlite.JDBC";
    public static final String URL = "jdbc:sqlite:" + DATABASE;
    public static final int TIMEOUT = 30;
    
    // DATABASE SETUP
    
    /**
     * Creates tables in database and two users (gmail and admin)
     */
    public static void initializeDatabase() {
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
        updateDatabase(queries);
    }
    
    /**
     * Resets database with empty tables and inserts admin and gmail user
     * accounts
     */
    public static void resetDatabase() {
        String [] queries = {
            /*DROP TABLES*/
            "DROP TABLE IF EXISTS accts",
            "DROP TABLE IF EXISTS cats",
            "DROP TABLE IF EXISTS envs",
            "DROP TABLE IF EXISTS trans",
            "DROP TABLE IF EXISTS users",
            "DROP TABLE IF EXISTS email"};
        updateDatabase(queries);
        initializeDatabase();
    }
        
    public static void removeZeroAmtTransactions() {
        // execute query
        executeQuery("DELETE FROM trans WHERE amt=0");
    }
    
    // GETTERS
    
    public static LinkedList<Category> getCategories() {
        return null;
    }
    
    public static LinkedList<Envelope> getEnvelopes() {
        return null;
    }
    
    public static LinkedList<Account> getAccounts() {
        return null;
    }
    
    public static LinkedList<Email> getEmails() {
        return null;
    }
    
    public static LinkedList<User> getUsers() {
        return null;
    }
    
    public static LinkedList<Transaction> getTransactions(String from, String to, Account acct, Envelope env) {
        return null;
    }
    
    public static LinkedList<Transaction> getTransactions(int qty, Account acct, Envelope env) {
        return null;
    }
    
    public static LinkedList<Transaction> getTransactions(int startIndex, int stopIndex, Account acct, Envelope env) {
        return null;
    }
    
    // SETTERS
    
    public static boolean updateEnvelope(Envelope env, boolean en, String name, Category cat) {
        boolean sameName = env.getName().equalsIgnoreCase(name);
        boolean sameEn   = env.isEnabled()==en;
        boolean sameCat  = false;
        if(env.getCat()==null || cat==null) {
            if(env.getCat()==null && cat==null) {
                sameCat = true;
            }
        } else if(env.getCat().getId()==cat.getId()) {
            sameCat = true;
        }
        
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
                // get new category id
                int catid = -1;
                if(cat!=null) {
                    catid = cat.getId();
                }
                // execute query
                stmt.executeUpdate("UPDATE envs SET modified='" + ts + "', enabled="+ enabled +", name='" + name + "', catid=" + catid + " WHERE id=" + env.getId());
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    public static boolean updateAccount(Account acct, boolean en, String name) {
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
                // execute query
                stmt.executeUpdate("UPDATE accts SET modified='" + ts + "', enabled="+ enabled +", name='" + name + "' WHERE id=" + acct.getId());
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    public static boolean updateEmail(Email em, int attempt, User usr) {
        boolean sameAttempt = em.getAttempt()==attempt;
        boolean sameUser    = false;
        if(em.getUser()==null || usr==null) {
            if(em.getUser()==null && usr==null) {
                sameUser = true;
            }
        } else if(em.getUser().getId()==usr.getId()) {
            sameUser = true;
        }
        
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
                // get new category id
                int userid = -1;
                if(usr!=null) {
                    userid = usr.getId();
                }
                // execute query
                stmt.executeUpdate("UPDATE email SET modified='" + ts + "', attempt=" + attempt + ", userid=" + userid + " WHERE id=" + em.getId());
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
    }
    
    public static boolean updateUser(User usr) {
        return false;
    }
    
    public static boolean updateCategory(Category cat) {
        return false;
    }
    
    public static boolean updateTransaction(Transaction tran) {
        return false;
    }
        
    // ADD TO DATABASE
    
    public static Envelope addEnvelope(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new envelope
            if(DBMS.executeQuery("INSERT INTO envs (created, modified, enabled, catid, name) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + name + "')")) {
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
                        return new Envelope(ts, ts, true, rs.getInt("id"), name, 0);
                    }
                } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
            }
        }
        return null;
    }
    
    public static Account addAccount(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new account
            if(DBMS.executeQuery("INSERT INTO accts (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')")) {
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
    
    public static Email addEmail(String addr, User usr) {
        // formats input
        addr = addr.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents checks that user exists
        boolean isUserValid = usr!=null && !usr.isGmail();
        if (isUserValid) {
            // creates new address in database (with user = enabled)
            DBMS.executeQuery("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 0, " + usr.getId() + ", '" + addr + "')");
        } else {
            // creates new address in database (without user = disabled)
            DBMS.executeQuery("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + addr + "')");
        }
        // get email id
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM email ORDER BY id DESC LIMIT 1");
                // Email(String created, String modified, int attempt, int id, User user, String addr)
                if(isUserValid) {
                    return new Email(ts, ts, 0, rs.getInt("id"), usr, addr);
                } else {
                    return new Email(ts, ts, 1, rs.getInt("id"), null, addr);
                }
            }
        } catch (ClassNotFoundException | SQLException e) { /* do nothing */ }
        return null;
    }
    
    public static User addUser(String un, String pw) {
        // format input
        un = un.toLowerCase();
        pw = Utilities.getHash(pw);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        {
            // creates new account in database
            DBMS.executeQuery("INSERT INTO users (created, modified, enabled, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 0, '" + un + "', '" + pw + "')");
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
    
    public static Category addCategory(String name) {
        // format input
        name = name.toLowerCase();
        if(Utilities.isValidContainerName(name)) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // add new category
            if(DBMS.executeQuery("INSERT INTO cats (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')")) {
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
                        return new Category(ts, ts, true, rs.getInt("id"), name, 0);
                    }
                } catch (ClassNotFoundException | SQLException e) { }
            }
        }
        return null;
    }
    
    public static Transaction addTransaction(Account acct, Envelope env, User usr, String date, String desc, double amt) {
        desc = Utilities.trimInvalidCharacters(desc);
        desc = Utilities.removeDoubleApostrophes(desc);
        desc = Utilities.doubleApostrophes(desc);
        if (acct!=null && env!=null && usr!=null && usr.isEnabled() && !usr.isGmail() && (acct.isEnabled() || env.isEnabled()) ) {
            // sets created/modified date/time
            String ts = Utilities.getTimestamp();
            // sets current date if date format is invalid
            if (!Utilities.isDate(date)) {
                date = ts.substring(0, 10);
            }
            String query = "INSERT INTO trans (created, modified, acctid, envid, userid, date, desc, amt) VALUES ('" + ts + "', '" + ts + "', " + acct.getId() + ", " + env.getId() + ", " + usr.getId() + ", '" + date + "', '" + desc + "', " + amt + ")";
            if(DBMS.executeQuery(query)) {
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
                        return new Transaction(ts, ts, rs.getInt("id"), acct, env, usr, date, desc, 0, null);
                    }
                } catch (ClassNotFoundException | SQLException e) {}
            }
        }
        return null;
    }
    
    // REMOVE FROM DATABASE
    
    public static boolean removeTransaction(Transaction tran) {
        return executeQuery("DELETE FROM trans WHERE id=" + tran.getId());
    }
    
    // HELPER METHODS
    
    public static double getAccountAmount(Account acct) {
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
    
    public static double getEnvelopeAmount(Envelope env) {
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
    
    // =============================================== REPLACE AND DELETE EVERYTHING BELOW THIS LINE ===============================================
    
//    /**
//     * Adds a new account to database. Accounts track the amount of money
//     * available in its different forms.
//     * @param name Name of new account to be added (ex. 'cash' or 'checking')
//     * @return Account if successfully added, null otherwise
//     */
//    public static Account addAccount(String name) {
//        if(name==null || name.length()>20) {
//            return null;
//        }
//        name = name.toLowerCase();
//        if(DBMS.isContainer(name, false)) { // name is in use and is enabled or disabled
//            if(DBMS.isAccount(name, false)) { // name is an account and is enabled or disabled
//                Account a = DBMS.getAccount(name, false);
//                if(!a.isEnabled()) { // account is enabled
//                    a.setEnabled(true);
//                    return a;
//                }
//            }
//        } else if(name.length()>0 && name.charAt(0)>96 && name.charAt(0)<123) { // first character is a letter
//            // format input
//            name = name.toLowerCase();
//            // sets created/modified date/time
//            String ts = Utilities.getTimestamp();
//            // creates new account in database
//            if(DBMS.executeQuery("INSERT INTO accts (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')")) {
//                return DBMS.getAccount(name, true);
//            } else {
//                return null;
//            }
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new category to database. Envelopes can be placed in categories
//     * for organization.
//     * @param name Name of new category to be added (ex. 'food' or 'fun')
//     * @return Category if successfully added, null otherwise
//     */
//    public static Category addCategory(String name) {
//        if(name==null || name.length()>20) {
//            return null;
//        }
//        name = name.toLowerCase();
//        if(DBMS.isContainer(name, false)) {
//            if(DBMS.isCategory(name, false)) {
//                Category c = DBMS.getCategory(name, false);
//                if(!c.isEnabled()) {
//                    c.setEnabled(true);
//                    return c;
//                }
//            }
//        } else if(name.length()>0 && name.charAt(0)>96 && name.charAt(0)<123) { // first character is a letter
//            return new Category(name);
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new envelope to database. Envelopes track the logical allocation
//     * of money, regardless of its form.
//     * @param name Name of new envelope to be added (ex. 'groceries' or
//     * 'housing')
//     * @return Envelope if successfully added, null otherwise
//     */
//    public static Envelope addEnvelope(String name) {
//        if(name==null || name.length()>20) {
//            return null;
//        }
//        name = name.toLowerCase();
//        if(DBMS.isContainer(name, false)) {
//            if(DBMS.isEnvelope(name, false)) {
//                Envelope e = DBMS.getEnvelope(name, false);
//                if(!e.isEnabled()) {
//                    e.setEnabled(true);
//                    return e;
//                }
//            }
//        } else if(name.length()>0 && name.charAt(0)>96 && name.charAt(0)<123) { // first character is a letter
//            return new Envelope(name);
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new envelope to database. Envelopes track the logical allocation
//     * of money, regardless of its form. Envelopes can be assigned a category
//     * for organization.
//     * @param name Name of new envelope to be added (ex. 'groceries' or
//     * 'housing')
//     * @param category Name of envelope's assigned category
//     * @return Envelope if successfully added, null otherwise
//     */
//    public static Envelope newEnvelope(String name, String category) {
//        if(name==null || name.length()>20) {
//            return null;
//        }
//        name = name.toLowerCase();
//        if(DBMS.isContainer(name, false)) {
//            if(DBMS.isEnvelope(name, false)) {
//                Envelope e = DBMS.getEnvelope(name, false);
//                if(!e.isEnabled()) {
//                    e.setEnabled(true);
//                    e.setCategory(getCategory(category, true));
//                    return e;
//                }
//            }
//        } else if(name.length()>0 && name.charAt(0)>96 && name.charAt(0)<123) { // first character is a letter
//            return new Envelope(name, getCategory(category, true));
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new transaction to database. Transactions describe exactly how
//     * money flows, either for a transfer, income, or outgo. Transfers happen
//     * between envelopes or accounts, but never both.
//     * @param account Name of the account tied to this transaction (if
//     * applicable)
//     * @param envelope Name of the envelope tied to this transaction (if
//     * applicable)
//     * @param user Username of the individual making this transaction
//     * @param date Date of transaction in format: YYYY-MM-DD (ex. 2013-08-17)
//     * @param description Brief description of the transaction
//     * @param amount The amount (+/-) of the transaction
//     * @param runTot Running total in relation to either a specific account or envelope
//     * @return Transaction if successfully added, null otherwise
//     */
//    public static Transaction addTransaction(String account, String envelope, String user, String date, String description, double amount, String runTot) {
//        // uppercase the first letter of the description
//        if(description.length()>0) { // 1 or more characters in desc
//            if(description.charAt(0)>=97 && description.charAt(0)<=122) { // first character in desc is lowercase letter
//                if(description.length()==1) { // only one character in desc
//                    description = description.toUpperCase();
//                } else { // more than one character in desc
//                    description = (char)(description.charAt(0)-32) + description.substring(1);
//                }
//            }
//        }
//        
//        // set user
//        User usr = getUser(user, true);
//        // set account
//        Account acct;
//        if(account==null || account.length()==0) {
//            acct = new Account(-1);
//        } else {
//            acct = getAccount(account, true);
//        }
//        // set envelope
//        Envelope env;
//        if(envelope==null || envelope.length()==0) {
//            env  = new Envelope(-1);
//        } else {
//            env  = getEnvelope(envelope, true);
//        }
//        // return new transaction
//        if (acct != null && env != null && usr != null) {
//            return new Transaction(acct, env, usr, date, description, amount, runTot);
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new user to database. Users are used for authentication, ensuring
//     * only valid users can access and update system. All enabled users are
//     * considered valid users
//     * @param username Username of user (ex. 'derekw')
//     * @param password Password of for the user (ex. 'My$e(r3tP@$$W0rd')
//     * @return User if successfully added, null otherwise
//     */
//    public static User addUser(String username, String password) {
//        if(username==null || username.length()>20) {
//            return null;
//        }
//        username = username.toLowerCase();
//        if(DBMS.isUser(username, false)) {
//            User u = DBMS.getUser(username, false);
//            if(!u.isEnabled()) {
//                u.setEnabled(true);
//                u.setPassword(password);
//                return u;
//            }
//        } else if(username.length()>0 && username.charAt(0)>96 && username.charAt(0)<123) { // first character is a letter
//            return new User(username, password);
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new email address to database. Email addresses not tied to valid
//     * users will not be able to send commands for processing. Attempts will be
//     * tracked and only 5 maximum attempts are allowed before permanent lockout.
//     * @param address Email address of the command originator
//     * @return Email if successfully added, null otherwise
//     */
//    public static Email addEmail(String address) {
//        Email email = new Email(address);
//        if (email.isInDatabase()) {
//            return email;
//        }
//        return null;
//    }
//    
//    /**
//     * Adds a new email address to database. Email addresses not tied to valid
//     * users will not be able to send commands for processing. Attempts will be
//     * tracked and only 5 maximum attempts are allowed before permanent lockout.
//     * @param address Email address of the command originator
//     * @param username Username of the validated user tied to this email address
//     * @return Email if successfully added, null otherwise
//     */
//    public static Email addEmail(String address, String username) {
//        User usr = DBMS.getUser(username, true);
//        if (usr!=null && !usr.isGmail()) {
//            Email email = new Email(address, usr.getId());
//            if (email.isInDatabase()) {
//                return email;
//            }    
//        } else {
//            Email email = new Email(address);
//            if (email.isInDatabase()) {
//                return email;
//            }
//        }
//        return null;
//    }
//        
//    // GET FROM DATABASE
//    
//    /**
//     * Retrieves a specific account by name
//     * @param name Name of account to retrieve
//     * @param onlyEnabled if set to true, only returns an account with the given
//     * name if such account is enabled
//     * @return Account specified, null otherwise
//     */
//    public static Account getAccount(String name, boolean onlyEnabled) {
//        if(name==null) {
//            return null;
//        }
//        name = name.toLowerCase();
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT * FROM accts WHERE enabled=1 AND name='" + name + "'");
//                } else {
//                    rs = stmt.executeQuery("SELECT * FROM accts WHERE name='" + name + "'");
//                }
//                // create account obj with appropriate attributes
//                Account acct = new Account(rs.getString("created"),
//                                   rs.getString("modified"),
//                                   rs.getInt("enabled")==1,
//                                   rs.getInt("id"),
//                                   rs.getString("name"),
//                                   0);
//                // execute query to find amount
//                rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid=" + acct.getId());
//                // sets variables accordingly
//                acct.setAmt(rs.getDouble(1));
//                
//                return acct;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    public static Account getAccount(int id) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query to get account
//                ResultSet rs = stmt.executeQuery("SELECT * FROM accts WHERE id = " + id);
//                // create account obj with appropriate attributes
//                Account acct = new Account(rs.getString("created"),
//                                   rs.getString("modified"),
//                                   rs.getInt("enabled")==1,
//                                   rs.getInt("id"),
//                                   rs.getString("name"),
//                                   0);
//                // execute query to find amount
//                rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid=" + id);
//                // sets variables accordingly
//                acct.setAmt(rs.getDouble(1));
//                return acct;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    public static double getAccountsTotal() {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid!=-1");
//                return rs.getDouble(1);
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return -9999999.99;
//        }
//    }
//    
//    public static double getEnvelopesTotal() {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE envid!=-1");
//                return rs.getDouble(1);
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return -9999999.99;
//        }
//    }
//    
//    public static double getUncategorizedTotal() {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans JOIN envs ON trans.envid=envs.id WHERE catid=-1");
//                return rs.getDouble(1);
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return -9999999.99;
//        }
//    }
//    
//    /**
//     * Retrieves category of the specified category name
//     * @param name Name of category to retrieve
//     * @return The category specified. If no such category exists, returns null.
//     */
//    public static Category getCategory(String name, boolean onlyEnabled) {
//        if(name==null) {
//            return null;
//        }
//        name = name.toLowerCase();
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM cats WHERE enabled=1 AND name='" + name + "'");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM cats WHERE name='" + name + "'");
//                }
//                return new Category(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves the Envelope of the specified envelope name
//     * @param name Name of envelope to retrieve
//     * @return The envelope specified. If no such envelope exists, returns null.
//     */
//    public static Envelope getEnvelope(String name, boolean onlyEnabled) {
//        if(name==null) {
//            return null;
//        }
//        name = name.toLowerCase();
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE enabled=1 AND name='" + name + "'");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE name='" + name + "'");
//                }
//                return new Envelope(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves the user associated with the specified username including the
//     * admin user (but not the Gmail user)
//     * @param username Name of user to retrieve ID for
//     * @return User with specified username if exists, null otherwise
//     */
//    public static User getUser(String username, boolean onlyEnabled) {
//        if(username==null) {
//            return null;
//        }
//        username = username.toLowerCase();
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM users WHERE enabled=1 AND un='" + username + "' and type!=1");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM users WHERE un='" + username + "' and type!=1");
//                }
//                return new User(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Gmail username and password are used by the Gmail server to authenticate
//     * to the Gmail account associated with this Gmail server.
//     * @return Gmail user
//     */
//    public static User getGmail() {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE type = 1");
//                // sets variables accordingly
//                return new User(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Admin is the root user who cannot be disabled or removed. Admin also has
//     * the rights to update all user passwords and usernames
//     * @return the root/admin user
//     */
//    public static User getAdmin() {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE type = 2");
//                // sets variables accordingly
//                return new User(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves the Email object by email address
//     * @param address Address of email to retrieve
//     * @return Email object if exists, null otherwise
//     */
//    public static Email getEmail(String address) {
//        if(address==null || address.length()==0) {
//            return null;
//        }
//        address = address.toLowerCase();
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT id FROM email WHERE addr='" + address + "'");
//                return new Email(rs.getInt("id"));
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
    
    public static double getAccountAmount(Account acct, String asOfDate) {
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
    
    public static double getEnvelopeAmount(Envelope env, String asOfDate) {
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
//    
//    // UPDATE DATABASE
//    public static String updateAccountName(int id, String newName) {
//        // check if account exists in database
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT * FROM accts WHERE id = " + id);
//                // sets variables accordingly
//                String created = rs.getString("created");
//                this.modified = rs.getString("modified");
//                this.enabled = rs.getInt("enabled");
//                this.id = rs.getInt("id");
//                this.name = rs.getString("name");
//                updateAmt();
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            this.id = -1;
//        }
//
//        if (!isInDatabase()) {
//            return "Error: account does not exist in database";
//        } else if (!isEnabled()) {
//            return "Error: disabled accounts cannot be updated";
//        } else if (newName.equalsIgnoreCase(this.name)) {
//            return "Account is already named '" + newName + "'";
//        }
//        String oldName = this.name;
//        String ts = Utilities.getTimestamp();
//        // set new name and updates modified getTimestamp
//        String query = "UPDATE accts SET modified='" + ts + "', name='" + newName + "' WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            this.modified = ts;
//            this.name = newName;
//            return "Account (" + oldName + ") successfully renamed to '" + newName + "'";
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to rename account (" + oldName + ") to '" + newName + "'";
//        }
//    }
//
//    // GET LISTS FROM DATABASE
//    
//    /**
//     * Retrieves all accounts, categories, and envelopes (i.e. containers) whose
//     * name begins with the given string
//     * @param beginsWith string representing the first letters of the container
//     * names for which to return.
//     * @return Linked list of all containers whose name begins with the given 
//     * string
//     */
//    public static LinkedList<Object> getContainers(String beginsWith, boolean onlyEnabled) {
//        LinkedList<Account> accts = getAccounts(beginsWith, onlyEnabled);
//        LinkedList<Category> cats = getCategories(beginsWith, onlyEnabled);
//        LinkedList<Envelope> envs = getEnvelopes(beginsWith, onlyEnabled);
//        LinkedList<Object> containers = new LinkedList();
//        
//        while(!accts.isEmpty()) {
//            containers.add(accts.poll());
//        }
//        while(!cats.isEmpty()) {
//            containers.add(cats.poll());
//        }
//        while(!envs.isEmpty()) {
//            containers.add(envs.poll());
//        }
//        
//        return containers;
//    }
//    
//    /**
//     * Retrieves all accounts from database and stores them in a linked list
//     * @return Linked list of all accounts
//     */
//    public static LinkedList<Account> getAccounts(boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM accts WHERE enabled=1 ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM accts ORDER BY name");
//                }
//                LinkedList<Account> accts = new LinkedList();
//                while(rs.next()) {
//                    accts.add(new Account(rs.getInt("id")));
//                }
//                return accts;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all accounts from database and stores them in a linked list
//     * @param beginsWith string representing the first letters of the account
//     * names for which to return.
//     * @return Linked list of all accounts whose name begins with the given 
//     * string
//     */
//    public static LinkedList<Account> getAccounts(String beginsWith, boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM accts WHERE enabled=1 AND name LIKE '" + beginsWith + "%' ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM accts WHERE name LIKE '" + beginsWith + "%' ORDER BY name");
//                }
//                
//                LinkedList<Account> accts = new LinkedList();
//                while(rs.next()) {
//                    accts.add(new Account(rs.getInt("id")));
//                }
//                return accts;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//
//    /**
//     * Retrieves all categories from database and stores them in a linked list
//     * @return Linked list of all categories
//     */
//    public static LinkedList<Category> getCategories(boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM cats WHERE enabled=1 ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM cats ORDER BY name");
//                }
//                
//                LinkedList<Category> cats = new LinkedList();
//                while(rs.next()) {
//                    cats.add(new Category(rs.getInt("id")));
//                }
//                return cats;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all categories from database and stores them in a linked list
//     * @param beginsWith string representing the first letters of the category
//     * names for which to return.
//     * @return Linked list of all categories whose name begins with the given 
//     * string
//     */
//    public static LinkedList<Category> getCategories(String beginsWith, boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM cats WHERE enabled=1 AND name LIKE '" + beginsWith + "%' ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM cats WHERE name LIKE '" + beginsWith + "%' ORDER BY name");
//                }
//                
//                LinkedList<Category> cats = new LinkedList();
//                while(rs.next()) {
//                    cats.add(new Category(rs.getInt("id")));
//                }
//                return cats;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all envelopes from database and stores them in a linked list
//     * @return Linked list of all envelopes
//     */
//    public static LinkedList<Envelope> getEnvelopes(boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE enabled=1 ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM envs ORDER BY name");
//                }
//                
//                LinkedList<Envelope> envs = new LinkedList();
//                while(rs.next()) {
//                    envs.add(new Envelope(rs.getInt("id")));
//                }
//                return envs;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all envelopes from database and stores them in a linked list
//     * @param beginsWith string representing the first letters of the envelope
//     * names for which to return.
//     * @return Linked list of all envelopes whose name begins with the given 
//     * string
//     */
//    public static LinkedList<Envelope> getEnvelopes(String beginsWith, boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE enabled=1 AND name LIKE '" + beginsWith + "%' ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE name LIKE '" + beginsWith + "%' ORDER BY name");
//                }
//                
//                LinkedList<Envelope> envs = new LinkedList();
//                while(rs.next()) {
//                    envs.add(new Envelope(rs.getInt("id")));
//                }
//                return envs;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all envelopes in the given category from database and stores 
//     * them in a linked list
//     * @param category Envelope category
//     * @return Linked list of all envelopes from the given category
//     */
//    public static LinkedList<Envelope> getEnvelopes(Category category, boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE enabled=1 AND catid=" + category.getId() + " ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE catid=" + category.getId() + " ORDER BY name");
//                }
//                
//                LinkedList<Envelope> envs = new LinkedList();
//                while(rs.next()) {
//                    envs.add(new Envelope(rs.getInt("id")));
//                }
//                return envs;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
//    
//    /**
//     * Retrieves all envelopes in the given category from database and stores 
//     * them in a linked list
//     * @param category Envelope category
//     * @return Linked list of all envelopes from the given category
//     */
//    public static LinkedList<Envelope> getUncategorizedEnvelopes(boolean onlyEnabled) {
//        try {
//            // register the driver
//            Class.forName(DRIVER_NAME);
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs;
//                if(onlyEnabled) {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE enabled=1 AND catid=-1 ORDER BY name");
//                } else {
//                    rs = stmt.executeQuery("SELECT id FROM envs WHERE catid=-1 ORDER BY name");
//                }
//                
//                LinkedList<Envelope> envs = new LinkedList();
//                while(rs.next()) {
//                    envs.add(new Envelope(rs.getInt("id")));
//                }
//                return envs;
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            return null;
//        }
//    }
    
    /**
     * Retrieves all transactions in the given date range from database and 
     * stores them in a linked list
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @param acct refines history search to only transactions that a tied to given account
     * @param env refines history search to only transactions that a tied to given envelope
     * @param hideTx does not return transactions that are considered transfers (account and/or envelope is null)
     * @return Linked list of all transactions in given date range or null if
     * from and/or to dates are not in the format YYYY-MM-DD. The order of the
     * from and to dates do not affect the outcome (i.e. 'to' > 'from' is the same
     * as 'from' > 'to')
     */
    public static LinkedList<Transaction> getTransactions(String from, String to, Account acct, Envelope env, boolean hideTx) {
        
        if (!Utilities.isDate(from) || !Utilities.isDate(to)) { // checks for valid dates
            return null;
        }
        int a, e;
        String criteria, query;
        String whereContainer = "";
        
        if (from.compareToIgnoreCase(to)>0) { // makes 'from' the earlier date
            String tmp = from;
            from = to;
            to = tmp;
        }
        String whereDate = " date>='" + from + "' AND date<='" + to + "'";
        
        if(acct==null) { // checks for account
            a = -1;
        } else {
            a = acct.getId();
        }
        
        if(env==null) { // checks for envelope
            e = -1;
        } else {
            e = env.getId();
        }

        // set search criteria as necessary according to XX (AE) where:
        // 0X = account not specified   1X = account specified
        // X0 = envelope not specified  X1 = envelope specified
        if(a==-1 && e!=-1) {        // 01
            criteria = "01";
            whereContainer = " envid="  + e + " AND";
        } else if(a!=-1 && e==-1) { // 10
            criteria = "10";
            whereContainer = " acctid=" + a + " AND";
        } else if(a!=-1 && e!=-1) { // 11
            criteria = "11";
            whereContainer = " acctid=" + a + " AND envid="  + e + " AND";
        } else {                             // 00
            criteria = "00";
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
                    //String created, String modified, int id, Account acct, Envelope env, User usr, String date, String desc, double amt, Transaction tx
                    trans.add(new Transaction(
                            rs.getString("created"),
                            rs.getString("modified"),
                            rs.getInt("id"),
                            acct,
                            env,
                            
                            ));
                }
                // only show running total from most recent transaction and when:
                // 00 (no criteria specified), 10 (only acct specified), or 01 (only env specified)
                if(trans.size()>0) {
                    String lastestTransactionDate = DBMS.getTransactions(1).getFirst().getDate();
                    if(lastestTransactionDate.compareTo(to)<=0) { // compute running totals only if query captures most recent transaction
                        if(!criteria.equalsIgnoreCase("11")) { // only if acct, env, or none is specified
                            double runTot, diff = 0;
                            if(acct!=null && criteria.equalsIgnoreCase("10")) {        // 10 (running total for only the specified account)
                                runTot = DBMS.getAccountAmount(acct.getName(), Utilities.getNewDate(lastestTransactionDate, 1));
                            } else if(env!=null  && criteria.equalsIgnoreCase("01")) { // 01 (running total for only the specified envelope)
                                runTot = DBMS.getEnvelopeAmount(env.getName(), Utilities.getNewDate(lastestTransactionDate, 1));
                            } else {                                                   // 00 (running total for all transactions)
                                runTot = DBMS.getAccountAmount("-all-", Utilities.getNewDate(lastestTransactionDate, 1));
                            }
                            // sets running total for each transaction
                            for (Transaction t : trans) {
                                runTot -= diff;
                                t.setRunningTotal(Utilities.addCommasToAmount(runTot));
                                diff = t.getAmt();
                            }
                        }
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Retrieves the most recent specified quantity of transactions from 
     * database and stores them in a linked list
     * @param quantity Number of transactions to retrieve
     * @param acct refines history search to only transactions that a tied to given account
     * @param env refines history search to only transactions that a tied to given envelope
     * @param hideTx does not return transactions that are considered transfers (account and/or envelope is null)
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(int quantity, Account acct, Envelope env, boolean hideTx) {
        int a, e;
        String criteria, query;
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
        if(a==-1 && e!=-1) {        // X01
            criteria = "01";
            where = "WHERE" + hideTxCriteria + " envid="  + e;
        } else if(a!=-1 && e==-1) { // X10
            criteria = "10";
            where = "WHERE" + hideTxCriteria + " acctid=" + a;
        } else if(a!=-1 && e!=-1) { // X11
            criteria = "11";
            where = "WHERE" + hideTxCriteria + " acctid=" + a + " AND envid="  + e;
        } else {                    // X00
            criteria = "00";
            if(hideTx) {
                where = "WHERE (acctid!=-1 AND envid!=-1)";
            }
        }
        query = "SELECT id FROM trans " + where + " ORDER BY date DESC, id DESC LIMIT " + quantity;
        
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
                    trans.add(new Transaction(rs.getInt("id")));
                }
                if(quantity>0 && trans.size()>0) {
                    // only show running total from most recent transaction and when:
                    // 00 (no criteria specified), 10 (only acct specified), or 01 (only env specified)
                    String lastestTransactionDate = DBMS.getTransactions(1).getFirst().getDate();
                    if(!criteria.equalsIgnoreCase("11")) { // only if acct, env, or none is specified
                        double runTot, diff = 0;
                        if(acct!=null && criteria.equalsIgnoreCase("10")) {        // 10 (running total for only the specified account)
                            runTot = DBMS.getAccountAmount(acct.getName(), Utilities.getNewDate(lastestTransactionDate, 1));
                        } else if(env!=null  && criteria.equalsIgnoreCase("01")) { // 01 (running total for only the specified envelope)
                            runTot = DBMS.getEnvelopeAmount(env.getName(), Utilities.getNewDate(lastestTransactionDate, 1));
                        } else {                                                   // 00 (running total for all transactions)
                            runTot = DBMS.getAccountAmount("-all-", Utilities.getNewDate(lastestTransactionDate, 1));
                        }
                        // sets running total for each transaction
                        for (Transaction t : trans) {
                            runTot -= diff;
                            t.setRunningTotal(Utilities.addCommasToAmount(runTot));
                            diff = t.getAmt();
                        }
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Retrieves the next 25 additional transactions from database and stores
     * them in a linked list
     * @param quantity Number of transactions previously retrieved
     * @param acct refines history search to only transactions that a tied to given account
     * @param env refines history search to only transactions that a tied to given envelope
     * @param hideTx does not return transactions that are considered transfers (account and/or envelope is null)
     * @param prev last transaction from previous set
     * @return Linked list of next transactions
     */
    public static LinkedList<Transaction> getMoreTransactions(int quantity, Account acct, Envelope env, boolean hideTx, Transaction prev) {
        int a, e;
        String criteria, query;
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
        if(a==-1 && e!=-1) {        // X01
            criteria = "01";
            where = "WHERE" + hideTxCriteria + " envid="  + e;
        } else if(a!=-1 && e==-1) { // X10
            criteria = "10";
            where = "WHERE" + hideTxCriteria + " acctid=" + a;
        } else if(a!=-1 && e!=-1) { // X11
            criteria = "11";
            where = "WHERE" + hideTxCriteria + " acctid=" + a + " AND envid="  + e;
        } else {                    // X00
            criteria = "00";
            if(hideTx) {
                where = "WHERE (acctid!=-1 AND envid!=-1)";
            }
        }
        query = "SELECT id FROM trans " + where + " ORDER BY date DESC, id DESC LIMIT " + (quantity+25);
        
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
                    if(count <= quantity) {
                        count++;
                    } else {
                        trans.add(new Transaction(rs.getInt("id")));
                    }
                }
                if(quantity>0 && trans.size()>0) {
                    // only show running total from most recent transaction and when:
                    // 00 (no criteria specified), 10 (only acct specified), or 01 (only env specified)
                    String lastestTransactionDate = DBMS.getTransactions(1).getFirst().getDate();
                    if(!criteria.equalsIgnoreCase("11")) { // only if acct, env, or none is specified
                        String runTotStr = Utilities.removeCommas(prev.getRunTot());
                        double runTot = Double.parseDouble(runTotStr);
                        double diff = prev.getAmt();
                        // sets running total for each transaction
                        for (Transaction t : trans) {
                            runTot -= diff;
                            t.setRunningTotal(Utilities.addCommasToAmount(runTot));
                            diff = t.getAmt();
                        }
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions in the given date range from database and 
     * stores them in a linked list
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @return Linked list of all transactions in given date range or null if
     * from and/or to dates are not in the format YYYY-MM-DD. The order of the
     * from and to dates do not affect the outcome (i.e. 'to' > 'from' is the same
     * as 'from' > 'to')
     */
    public static LinkedList<Transaction> getTransactions(String from, String to) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to)) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if (from.compareToIgnoreCase(to)>0) {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE date<='" + from + "' and date>='" + to + "' ORDER BY date, modified");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE date>='" + from + "' and date<='" + to + "' ORDER BY date, modified");
                }
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions in the given date range for the specified
     * account from database and stores them in a linked list
     * @param account Account for which transactions are tied to
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @return Linked list of all transactions in given date range for the given
     * account
     */
    public static LinkedList<Transaction> getTransactions(Account account, String from, String to) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to) || !account.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if (from.compareToIgnoreCase(to)>1) {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE acctid=" + account.getId() + " and date<='" + from + "' and date>='" + to + "' ORDER BY date, modified");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE acctid=" + account.getId() + " and date>='" + from + "' and date<='" + to + "' ORDER BY date, modified");
                }
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions in the given date range for the specified
     * category from database and stores them in a linked list
     * @param category Category for which transactions are tied to
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @return Linked list of all transactions in given date range for the given
     * category
     */
    public static LinkedList<Transaction> getTransactions(Category category, String from, String to) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to) || !category.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if (from.compareToIgnoreCase(to)>1) {
                    rs = stmt.executeQuery("SELECT trans.id FROM trans JOIN envs ON trans.envid=envs.id WHERE envs.catid=" + category.getId() + " and date<='" + from + "' and date>='" + to + "' ORDER BY date, trans.modified");
                } else {
                    rs = stmt.executeQuery("SELECT trans.id FROM trans JOIN envs ON trans.envid=envs.id WHERE envs.catid=" + category.getId() + " and date>='" + from + "' and date<='" + to + "' ORDER BY date, trans.modified");
                }
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions in the given date range for the specified
     * envelope from database and stores them in a linked list
     * @param envelope Envelope for which transactions are tied to
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @return Linked list of all transactions in given date range for the given
     * envelope
     */
    public static LinkedList<Transaction> getTransactions(Envelope envelope, String from, String to) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to) || !envelope.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if (from.compareToIgnoreCase(to)>1) {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE envid=" + envelope.getId() + " and date<='" + from + "' and date>='" + to + "' ORDER BY date, modified");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE envid=" + envelope.getId() + " and date>='" + from + "' and date<='" + to + "' ORDER BY date, modified");
                }
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions in the given date range for the specified
     * user from database and stores them in a linked list
     * @param user User for which transactions are tied to
     * @param from Start date of the date range in format: YYYY-MM-DD
     * @param to End date of the date range in format: YYYY-MM-DD
     * @return Linked list of all transactions in given date range for the given
     * user
     */
    public static LinkedList<Transaction> getTransactions(User user, String from, String to) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to) || !user.isInDatabase() || user.isGmail()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if (from.compareToIgnoreCase(to)>1) {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE userid=" + user.getId() + " and date<='" + from + "' and date>='" + to + "' ORDER BY date, modified");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM trans WHERE userid=" + user.getId() + " and date>='" + from + "' and date<='" + to + "' ORDER BY date, modified");
                }
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.add(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves the most recent specified quantity of transactions from 
     * database and stores them in a linked list
     * @param quantity Number of transactions to retrieve
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(int quantity) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM trans ORDER BY date DESC, modified DESC LIMIT " + quantity);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves the most recent specified quantity of transactions tied to the 
     * given account from database and stores them in a linked list
     * @param account Account in which transactions are tied to
     * @param quantity Number of transactions to retrieve
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(Account account, int quantity) {
        if (!account.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM trans WHERE acctid =" + account.getId() + " ORDER BY date DESC, modified DESC LIMIT " + quantity);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves the most recent specified quantity of transactions tied to the 
     * given category from database and stores them in a linked list
     * @param category Category in which transactions are tied to
     * @param quantity Number of transactions to retrieve
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(Category category, int quantity) {
        if (!category.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT trans.id FROM trans JOIN envs ON trans.envid=envs.id WHERE envs.catid=" + category.getId() + " ORDER BY date DESC, trans.modified DESC LIMIT " + quantity);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves the most recent specified quantity of transactions tied to the 
     * given envelope from database and stores them in a linked list
     * @param envelope Envelope in which transactions are tied to
     * @param quantity Number of transactions to retrieve
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(Envelope envelope, int quantity) {
        if (!envelope.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                String query = "SELECT id FROM trans WHERE envid=" + envelope.getId() + " ORDER BY date DESC, modified DESC LIMIT " + quantity;
                ResultSet rs = stmt.executeQuery(query);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all transactions tied to the given envelope from
     * database and stores them in a linked list
     * @param envelope Envelope in which transactions are tied to
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(Envelope envelope) {
        if (!envelope.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM trans WHERE envid =" + envelope.getId() + " ORDER BY date DESC, id DESC");
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    /**
     * Retrieves the most recent specified quantity of transactions tied to the 
     * given user from database and stores them in a linked list
     * @param user User in which transactions are tied to
     * @param quantity Number of transactions to retrieve
     * @return Linked list of transactions
     */
    public static LinkedList<Transaction> getTransactions(User user, int quantity) {
        if (!user.isInDatabase() || user.isGmail()) {
            return null;
        }
        if (!user.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM trans WHERE userid =" + user.getId() + " ORDER BY date DESC, modified DESC LIMIT " + quantity);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    trans.addFirst(new Transaction(rs.getInt("id")));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    public static int getTransactionCount() {
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
    
    public static int getTransactionCount(Account acct) {
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
    
    public static int getTransactionCount(Envelope env) {
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
    
    /**
     * Retrieves all users from database and stores them in a linked list
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return Linked list of all users
     */
    public static LinkedList<User> getUsers(boolean onlyEnabled) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT id FROM users WHERE enabled=1 AND type!=1 ORDER BY un");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM users WHERE type!=1 ORDER BY un");
                }
                
                LinkedList<User> users = new LinkedList();
                while(rs.next()) {
                    users.add(new User(rs.getInt("id")));
                }
                return users;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all users from database and stores them in a linked list
     * @param beginsWith only need to specify the first few letters of the user
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return Linked list of all users
     */
    public static LinkedList<User> getUsers(String beginsWith, boolean onlyEnabled) {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT id FROM users WHERE enabled=1 AND type!=1 AND un LIKE '" + beginsWith + "%' ORDER BY un");
                } else {
                    rs = stmt.executeQuery("SELECT id FROM users WHERE type!=1 AND un LIKE '" + beginsWith + "%' ORDER BY un");
                }
                
                LinkedList<User> users = new LinkedList();
                while(rs.next()) {
                    users.add(new User(rs.getInt("id")));
                }
                return users;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all email from database and stores them in a linked list
     * @return Linked list of all email
     */
    public static LinkedList<Email> getEmail() {
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM email ORDER BY userid, addr");
                LinkedList<Email> email = new LinkedList();
                while(rs.next()) {
                    email.add(new Email(rs.getInt("id")));
                }
                return email;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves all email from database and stores them in a linked list
     * @param user User for which you wish to retrieve email addresses for
     * @return Linked list of all email for the given user
     */
    public static LinkedList<Email> getEmail(User user) {
        if (!user.isInDatabase()) {
            return null;
        }
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT id FROM email WHERE userid=" + user.getId() + " ORDER BY userid, addr");
                LinkedList<Email> email = new LinkedList();
                while(rs.next()) {
                    email.add(new Email(rs.getInt("id")));
                }
                return email;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    // MISC.
    
    /**
     * Check if name is already in use as an account, category, or envelope.
     * @param name Name to check for duplication
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return true if name is already in use, false otherwise
     */
    public static boolean isContainer(String name, boolean onlyEnabled) {
        if(name==null) {
            return false;
        }
        name = name.toLowerCase();
        return DBMS.isAccount(name, onlyEnabled) || DBMS.isCategory(name, onlyEnabled) || DBMS.isEnvelope(name, onlyEnabled);
    }
    
    /**
     * Updates the database with the given query
     * @param query SQL query to update database (ex. "UPDATE email SET
     * modified='2013-08-17 15:50:44', attempt=5 WHERE id=3")
     * @return true if successful, false otherwise
     */
    public static boolean executeQuery(String query) {
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
    public static boolean updateDatabase(String[] queries) {
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
    
    /**
     * Determines whether account exists with specified name
     * @param name potential account name
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return true if account exists by specified name, false otherwise
     */
    public static boolean isAccount(String name, boolean onlyEnabled) {
        if(name==null) {
            return false;
        }
        name = name.toLowerCase();
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute queries to determine if name already exists
                ResultSet  rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT count(*) FROM accts WHERE enabled=1 AND name='" + name + "'");
                } else {
                    rs = stmt.executeQuery("SELECT count(*) FROM accts WHERE name='" + name + "'");
                }
                return rs.getInt(1)>0;
            }
        } catch (ClassNotFoundException | SQLException e) {
        }
        return false;
    }
    
    /**
     * Determines whether category exists with specified name
     * @param name potential category name
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return true if category exists by specified name, false otherwise
     */
    public static boolean isCategory(String name, boolean onlyEnabled) {
        if(name==null) {
            return false;
        }
        name = name.toLowerCase();
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute queries to determine if name already exists
                ResultSet  rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT count(*) FROM cats WHERE enabled=1 AND name='" + name + "'");
                } else {
                    rs = stmt.executeQuery("SELECT count(*) FROM cats WHERE name='" + name + "'");
                }
                return rs.getInt(1)>0;
            }
        } catch (ClassNotFoundException | SQLException e) {
        }
        return false;
    }
    
    /**
     * Determines whether envelope exists with specified name
     * @param name potential envelope name
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return true if envelope exists by specified name, false otherwise
     */
    public static boolean isEnvelope(String name, boolean onlyEnabled) {
        if(name==null) {
            return false;
        }
        name = name.toLowerCase();
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute queries to determine if name already exists
                ResultSet  rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT count(*) FROM envs WHERE enabled=1 AND name='" + name + "'");
                } else {
                    rs = stmt.executeQuery("SELECT count(*) FROM envs WHERE name='" + name + "'");
                }
                return rs.getInt(1)>0;
            }
        } catch (ClassNotFoundException | SQLException e) {
        }
        return false;
    }
    
    /**
     * Determines whether user exists with specified username
     * @param username potential username
     * @param onlyEnabled if false, returns disabled as well as enabled
     * @return true if user exists by specified username, false otherwise
     */
    public static boolean isUser(String username, boolean onlyEnabled) {
        if(username==null) {
            return false;
        }
        username = username.toLowerCase();
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute queries to determine if name already exists
                ResultSet  rs;
                if(onlyEnabled) {
                    rs = stmt.executeQuery("SELECT count(*) FROM users WHERE enabled=1 AND un='" + username + "' and type!=1");
                } else {
                    rs = stmt.executeQuery("SELECT count(*) FROM users WHERE un='" + username + "' and type!=1");
                }
                return rs.getInt(1)>0;
            }
        } catch (ClassNotFoundException | SQLException e) {
        }
        return false;
    }
    
    /**
     * Determines whether email exists with specified address
     * @param addr potential email address
     * @return true if email exists by specified address, false otherwise
     */
    public static boolean isEmail(String addr) {
        if(addr==null) {
            return false;
        }
        addr = addr.toLowerCase();
        try {
            // register the driver
            Class.forName(DRIVER_NAME);
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute queries to determine if addr already exists
                ResultSet  rs;
                rs = stmt.executeQuery("SELECT count(*) FROM email WHERE addr='" + addr + "'");
                if (rs.getInt(1)>0) {
                    return true;
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
        }
        return false;
    }
    
    public static String validateTrendInput(String input) {
        if(input.length()>0) {
            input = input.toLowerCase();
            // remove invalid charaters
            String tmp = "";
            for(int i = 0 ; i < input.length(); i++) {
                if((input.charAt(i)>='a' && input.charAt(i)<='z') || (input.charAt(i)=='-' && i>0) || input.charAt(i)==',') {
                    tmp += input.charAt(i);
                }
            }
            String[]names = tmp.split(",");
            tmp = "";
            LinkedList<String> validatedNames = new LinkedList();
            for (String s : names) {
                if(!validatedNames.contains(s)) { // removes duplicate entries
                    validatedNames.add(s);
                    if(isAccount(s, true) || isEnvelope(s, true)) {
                        tmp += s + ",";
                    } else {
                        tmp += "[" + s + "],";
                    }
                }
            }
            // removes last comma
            tmp = tmp.substring(0, tmp.length()-1);
            
            return tmp;
        } else {
            return "";
        }
    }
    
    public static boolean setTransferRelationship(Transaction t1, Transaction t2) {
        if(t1.isInDatabase()                 // t1 is a valid transaction
                && t2.isInDatabase()         // t2 is a valid transaction
                && t1.getAmt()==-t2.getAmt() // amounts match
                && (t1.getAcct().getId()==-1 && t2.getAcct().getId()==-1    // accounts null...
                 || t1.getEnv().getId()==-1  && t2.getEnv().getId()==-1)) { // or envelopes null (but not both)
            t1.setTransferTransaction(t2.getId());
            t2.setTransferTransaction(t1.getId());
            return true;
        }
        return false;
    }
    
    public static void mergeEnvelopes(Envelope from, Envelope to) {
        // move transactions
        Transaction transaction, partner;
        while(DBMS.getTransactionCount(from)>0) {
            transaction = DBMS.getTransactions(from, 1).getFirst();
            partner = new Transaction(transaction.getTx());
            if(transaction.getTx()!=-1 && partner.getEnv().getName().equalsIgnoreCase(to.getName())) {
                // delete same-envelope transfer transactions (they cancel each other out)
                executeQuery("DELETE FROM trans WHERE id=" + transaction.getId());
                executeQuery("DELETE FROM trans WHERE id=" + partner.getId());
            } else {
                // set new envelope
                transaction.setEnvelope(to.getName());
            }
        }
        // remove (disable) 'from' envelope
        executeQuery("UPDATE envs SET enabled=0 WHERE id=" + from.getId());
    }
}
