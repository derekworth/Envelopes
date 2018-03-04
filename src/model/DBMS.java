package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import misc.Utilities;

/**
 * @Created Aug 17, 2013
 * @Modified Jan 12, 2018
 * @author Derek Worth
 */
class DBMS {

    static final String DATABASE = "database.db";
    static final String DRIVER_NAME = "org.sqlite.JDBC";
    static final String URL = "jdbc:sqlite:" + DATABASE;
    static final int TIMEOUT = 30;
    
    // DATABASE SETUP
    /**
     * Resets database with empty tables, and then inserts admin and gmail user
     * accounts along with sample envelopes/accounts
     */
    static void initializeDatabase() {
        String ts = Utilities.getTimestamp();
        // initialize tables
        String[] queries = {
            /*DROP TABLES*/
            "DROP TABLE IF EXISTS accts",
            "DROP TABLE IF EXISTS cats",
            "DROP TABLE IF EXISTS envs",
            "DROP TABLE IF EXISTS trans",
            "DROP TABLE IF EXISTS creds",
            "DROP TABLE IF EXISTS email",
            "DROP TABLE IF EXISTS ver",
            /*INITIALIZE DATABASE*/
            "CREATE TABLE accts (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, name TEXT NOT NULL UNIQUE, enabled INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE cats  (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, name TEXT NOT NULL UNIQUE)",
            "CREATE TABLE envs  (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, name TEXT NOT NULL UNIQUE, catid INTEGER NOT NULL DEFAULT -1, FOREIGN KEY (catid) REFERENCES cats (id))",
            "CREATE TABLE trans (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, acctid INTEGER NOT NULL, envid INTEGER NOT NULL, userid INTEGER NOT NULL, date TEXT NOT NULL, desc TEXT NOT NULL, amt INTEGER NOT NULL, txid INTEGER DEFAULT -1, FOREIGN KEY (acctid) REFERENCES accts (id), FOREIGN KEY (envid) REFERENCES envs (id), FOREIGN KEY (userid) REFERENCES creds (id))",
            "CREATE TABLE creds (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, type INTEGER NOT NULL, un TEXT NOT NULL UNIQUE, pw TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE email (id INTEGER NOT NULL PRIMARY KEY, created TEXT NOT NULL, modified TEXT NOT NULL, attempt INTEGER NOT NULL, userid INTEGER NOT NULL, addr TEXT NOT NULL UNIQUE, FOREIGN KEY (userid) REFERENCES creds (id))",
            "CREATE TABLE ver   (id INTEGER NOT NULL PRIMARY KEY, date TEXT NOT NULL UNIQUE)",
            /*INITIALIZE ADMIN AND GMAIL CREDENTIALS*/
            "INSERT INTO creds (created, modified, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 'admin', '" + Utilities.getHash("password") + "')",
            "INSERT INTO creds (created, modified, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 2, 'gmail_username', 'gmail_password')",
            /*INITIALIZE SAMPLE ACCOUNTS*/
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'cash')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'checking')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'savings')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'ira-roth')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'ira-traditional')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'visa')",
            "INSERT INTO accts (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'mastercard')",
            /*INITIALIZE SAMPLE CATEGORIES*/
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'giving', 1)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'save', 2)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'housing', 3)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'utilities', 4)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'food', 5)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'clothing', 6)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'transportation', 7)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'medical-health', 8)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'insurance', 9)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'personal', 10)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'recreation', 11)",
            "INSERT INTO cats (created, modified, name, id) VALUES ('" + ts + "', '" + ts + "', 'debts', 12)",
            /*INITIALIZE SAMPLE ENVELOPES*/
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'tithes', 1)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'charity', 1)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'emergency', 2)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'retirement', 2)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'college', 2)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'mortgage1_rent', 3)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'mortgage2', 3)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'real-estate-taxes', 3)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'repairs-maint', 3)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'association-dues', 3)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'electricity', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'gas', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'water', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'trash', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'phone-mobile', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'internet', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'cable', 4)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'groceries', 5)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'restaurants', 5)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'adults', 6)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'children', 6)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'cleaning-laundry', 6)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'gas-oil', 7)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'repairs-tires', 7)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'license-registration', 7)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'car-replacement', 7)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'medications', 8)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'doctor-bills', 8)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'dentist', 8)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'optometrist', 8)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'vdentistitamins', 8)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'life-insurance', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'health-insurance', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'homeowner-Renter', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'auto-insurance', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'disability-insurance', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'identity-theft', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'long-term-care', 9)", 
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'child-care', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'toiletries', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'cosmetics-hair-care', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'education-tuition', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'books-supplies', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'child-support', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'alimony', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'subscriptions', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'organization-dues', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'gifts', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'replace-furniture', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'pocket-money-his', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'pocket-money-hers', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'baby-supplies', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'pet-supplies', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'music-technology', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'miscellaneous', 10)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'entertainment', 11)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'vacation', 11)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'car-payment-1', 12)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'car-payment-2', 12)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'student-loan-1', 12)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'student-loan-2', 12)",
            "INSERT INTO envs (created, modified, name, catid) VALUES ('" + ts + "', '" + ts + "', 'hospital', 12)",
            "INSERT INTO envs (created, modified, name) VALUES ('" + ts + "', '" + ts + "', 'income')"
        };
        updateDatabase(queries);
    }

    /**
     * Clears out any transaction in the database with an amt equal to zero
     */
    static void removeZeroAmtTransactions() {
        // execute query
        updateDatabase("DELETE FROM trans WHERE amt=0");
    }

    /**
     * Updates the database with the given query
     *
     * @param query SQL query to update database (ex. "UPDATE email SET
     * modified='2013-08-17 15:50:44', attempt=5 WHERE id=3")
     * @return true if successful, false otherwise
     */
    static boolean updateDatabase(String query) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
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
     *
     * @param queries String array of SQL queries to update database
     * @return true if successful, false otherwise
     */
    static boolean updateDatabase(String[] queries) {
        String curr = "";
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // create tables
                for (String query : queries) {
                    curr = query;
                    stmt.executeUpdate(query);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    // ADD TO DATABASE
    /**
     * Adds a new account to database.
     *
     * @param name Name of new account to be added (ex. 'cash' or 'checking');
     * must be 20 or less characters in length
     * @return Account if successfully added, null otherwise
     */
    static Account addAccount(String name) {
        return (Account) addContainer(name, "accts");
    }

    /**
     * Adds a new category to database. Envelopes can be placed in categories
     * for organization.
     *
     * @param name Name of new category to be added (ex. 'food' or 'fun'); must
     * be 20 or less characters in length
     * @return Category if successfully added, null otherwise
     */
    static Category addCategory(String name) {
        return (Category) addContainer(name, "cats");
    }

    /**
     * Adds a new envelope to database. Envelopes track the logical allocation
     * of money, regardless of its form. Envelopes are created without a
     * category and may be later categorized.
     *
     * @param name Name of new envelope to be added (ex. 'groceries' or
     * 'housing'); must be 20 or less characters in length
     * @return Envelope if successfully added, null otherwise
     */
    static Envelope addEnvelope(String name) {
        return (Envelope) addContainer(name, "envs");
    }

    /**
     * Helper method for addAccount/Category/Envelope
     *
     * @param name Name of container
     * @param table Name of table (i.e. accts, envs, cats)
     * @return Container added (i.e. Account, Envelope, or Category)
     */
    private static Container addContainer(String name, String table) {
        // lowercases all letters and removes invalid characters
        name = Utilities.cleanContainerName(name);
        // names are limited to 20 characters
        if (name.length()>20 || name.length()==0) {
            return null;
        }
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // creates new record in database
        String query = "INSERT INTO " + table + " (created, modified, name) VALUES ('" + ts + "', '" + ts + "', '" + name + "')";
        if (!DBMS.updateDatabase(query)) {
            return null;
        }
        // sets variables accordingly
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query (retrieves newest record)
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY id DESC LIMIT 1");
                // sets variables accordingly
                int i = rs.getInt("id");
                String n = rs.getString("name");
                if (table.equalsIgnoreCase("accts")) {
                    return new Account(i, ts, ts, n, 0, true);
                } else if (table.equalsIgnoreCase("envs")) {
                    return new Envelope(i, ts, ts, n, 0, -1);
                } else if (table.equalsIgnoreCase("cats")) {
                    return new Category(i, ts, ts, n, 0);
                } else {
                    return null;
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    /**
     * Adds a new transaction to database. Transactions describe exactly how
     * money flows, either for a transfer, income, or outgo. Transfers happen
     * between envelopes or accounts, but never both. This method does not set
     * the account, envelope, or user of the transaction (the controller must
     * set these after the transaction has been created)
     *
     * @param date Date of transaction in format: YYYY-MM-DD (ex. 2013-08-17)
     * @param desc Brief description of the transaction
     * @param amt The amount (+/-) of the transaction
     * @param acctid id of corresponding Account record
     * @param userid id of corresponding User record
     * @param envid id of corresponding Envelope record
     * @return Transaction if successfully added, null otherwise
     */
    static Transaction addTransaction(String date, String desc, int amt, int acctid, int userid, int envid) {
        // format description
        desc = Utilities.cleanTransactionDesc(desc);
        // sets current date if date format is invalid
        if (!Utilities.isDate(date)) {
            date = Utilities.getDatestamp(0);
        }
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        String query = "INSERT INTO trans (created, modified, date, desc, amt, acctid, userid, envid) VALUES ('" + ts + "', '" + ts + "', '" + date + "', '" + desc + "', " + amt + ", " + acctid + ", " + userid + ", " + envid + ")";
        if (!DBMS.updateDatabase(query)) {
            return null;
        }
        // sets variables accordingly
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM trans ORDER BY id DESC LIMIT 1");
                // sets variables accordingly
                int i = rs.getInt("id");
                return new Transaction(i, ts, ts, date, desc, amt, acctid, userid, envid, -1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    /**
     * Adds a new user to database. Credentials are used for authentication,
     * ensuring only valid users can access and update system. All enabled users
     * are considered valid users. Each user is assigned a type. New users are
     * of type 0 and do not have admin rights, type 1 has admin rights, and type
     * 2 is for the single gmail account associated with the server.
     *
     * @param un Username of credential (ex. 'derekw')
     * @param pw Password of credential (ex. 'My$e(r3tP@$$W0rd'). Note: password
     * is stored as an MD5 hash for all newly created users. The Gmail password
     * is the only password stored in clear text.
     * @return Credential if successfully added, null otherwise
     */
    static Credential addCredential(String un, String pw) {
        // remove all non-alphanumeric chars and ensure first char is a letter
        un = Utilities.cleanUsername(un);
        // usernames are limited to 20 characters
        if (un.length() > 20) {
            return null;
        }
        // converts password to an MD5 hash
        pw = Utilities.getHash(pw);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        String query = "INSERT INTO creds (created, modified, un, pw, type, enabled) VALUES ('" + ts + "', '" + ts + "', '" + un + "', '" + pw + "', 0, 1)";
        if (!DBMS.updateDatabase(query)) {
            return null;
        }
        // sets variables accordingly
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query (retrieves newest record)
                ResultSet rs = stmt.executeQuery("SELECT * FROM creds ORDER BY id DESC LIMIT 1");
                // sets variables accordingly
                int i = rs.getInt("id");
                return new Credential(i, ts, ts, un, pw, 0, true);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    /**
     * Adds a new email address to database. NOTE: Email addresses not tied to valid
     * users will not be able to send commands for processing. Attempts will be
     * tracked and only 5 maximum attempts are allowed before permanent lockout.
     * Newly added emails start with a 1 attempt count.
     * @param addr email address
     * @return Email if successfully added, null otherwise
     */
    static Email addEmail(String addr) {
        // formats input
        addr = addr.toLowerCase();
        addr = addr.trim();
        // prevent sql injection attacks 
        if (!Utilities.isValidEmailAddress(addr)) {
            return null;
        }
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // creates new address in database (1 attempt, without user)
        String query = "INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + addr + "')";
        if (!DBMS.updateDatabase(query)) {
            return null;
        }
        // sets variables accordingly
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM email ORDER BY id DESC LIMIT 1");
                // sets variables accordingly
                int i = rs.getInt("id");
                return new Email(i, ts, ts, 1, -1, addr);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    // DELETE RECORDS FROM DATABASE
    /**
     * Removes specified record from database. NOTE: this method does not check
     * for a corresponding transaction as part of a transfer, and thus use this
     * method twice to remove the pair (i.e. one for each transaction).
     *
     * @param id ID of record
     */
    static void deleteTransaction(int id) {
        updateDatabase("DELETE FROM trans WHERE id=" + id);
    }

    /**
     * Removes specified record from database. NOTE: only remove envelopes with
     * no transactions in them. This method does not check to ensure this is the
     * case.
     *
     * @param id ID of record
     */
    static void deleteEnvelope(int id) {
        updateDatabase("DELETE FROM envs WHERE id=" + id);
    }

    /**
     * Removes specified record from database.
     *
     * @param id ID of record
     */
    static void deleteCategory(int id) {
        updateDatabase("DELETE FROM cats WHERE id=" + id);
    }

    // SETTERS (MODIFY RECORDS)
    /**
     * Modifies specified email with given info and updates modified date of
     * record
     *
     * @param id ID of email record
     * @param attempt 0 = auth, 1-5 = failed attempt, +6 = locked out
     * @param userid -1 = no user, ID of user otherwise
     */
    static void modifyEmail(int id, int attempt, int userid) {
        updateDatabase("UPDATE email SET modified='" + Utilities.getTimestamp() + "', attempt=" + attempt + ", userid=" + userid + " WHERE id=" + id);
    }

    static void modifyTransaction(int id, String date, String desc, int amt, int acctid, int envid) {
        // format description
        desc = Utilities.cleanTransactionDesc(desc);
        // sets current date if date format is invalid
        if (!Utilities.isDate(date)) {
            date = Utilities.getDatestamp(0);
        }
        updateDatabase("UPDATE trans SET modified='" + Utilities.getTimestamp() + "', date='" + date + "', desc='" + desc + "', amt=" + amt + ", acctid=" + acctid + ", envid=" + envid + " WHERE id=" + id);
    }

    static void setTransactionTransfer(int id1, int id2) {        
        // get transactions
        Transaction t1 = DBMS.getTransaction(id1);
        Transaction t2 = DBMS.getTransaction(id2);
        // check if they exist
        if(t1!=null && t2!=null) {
            // update transactions
            updateDatabase("UPDATE trans SET modified='" + Utilities.getTimestamp() + "', txid=" + id2 + " WHERE id=" + id1);
            updateDatabase("UPDATE trans SET modified='" + Utilities.getTimestamp() + "', txid=" + id1 + " WHERE id=" + id2);
        }
    }

    /**
     * Updates specified credential. NOTE: password is stored as specified (i.e.
     * no hash is calculated). You must provide the hashed passwords for types 0
     * and 1 credentials to maintain security of this info.
     *
     * @param id ID of record
     * @param un username
     * @param pw password (should provide hash password for types 0 and 1 creds
     * @param en true = enabled, false = disabled (never delete credentials, as
     * they are tied to transaction records)
     */
    static void modifyCredential(int id, String un, String pw, boolean en) {
        // remove all non-alphanumeric chars and ensure first char is a letter
        un = Utilities.cleanUsername(un);
        // usernames are limited to 20 characters
        if (un.length() > 0 && un.length() <= 20) {
            if (en) {
                updateDatabase("UPDATE creds SET modified='" + Utilities.getTimestamp() + "', un='" + un + "', pw='" + pw + "', enabled=1 WHERE id=" + id);
            } else {
                updateDatabase("UPDATE creds SET modified='" + Utilities.getTimestamp() + "', un='" + un + "', pw='" + pw + "', enabled=0 WHERE id=" + id);
            }
        }
    }

    /**
     * Updates specified account. NOTE: accounts are not deleted since they are
     * tied to transactions.
     *
     * @param id ID of record
     * @param name Account name
     * @param en true = enabled, false = disabled
     */
    static void modifyAccount(int id, String name, boolean en) {
        // lowercases all letters and removes invalid characters
        name = Utilities.cleanContainerName(name);
        // names are limited to 20 characters
        if (name.length() > 0 && name.length() <= 20) {
            if (en) {
                updateDatabase("UPDATE accts SET modified='" + Utilities.getTimestamp() + "', name='" + name + "', enabled=1 WHERE id=" + id);
            } else {
                updateDatabase("UPDATE accts SET modified='" + Utilities.getTimestamp() + "', name='" + name + "', enabled=0 WHERE id=" + id);
            }
        }
    }

    static void modifyEnvelope(int id, String name, int catid) {
        // lowercases all letters and removes invalid characters
        name = Utilities.cleanContainerName(name);
        // names are limited to 20 characters
        if (name.length() > 0 && name.length() <= 20) {
            updateDatabase("UPDATE envs SET modified='" + Utilities.getTimestamp() + "', name='" + name + "', catid=" + catid + " WHERE id=" + id);
        }
    }

    /**
     * Moves all transactions from one envelope to another, then removes the
     * empty envelope
     * @param fromID record ID of envelope to be removed
     * @param toID record ID of envelope to be merged into
     */
    static void mergeEnvelopes(int fromID, int toID) {
        // move transactions from one envelope to the other
        updateDatabase("UPDATE trans SET modified='" + Utilities.getTimestamp() + "', envid=" + toID + " WHERE envid=" + fromID);
        // remove empty envelope
        updateDatabase("DELETE FROM envs WHERE id=" + fromID);
    }
    
    static void modifyCategory(int id, String name) {
        // lowercases all letters and removes invalid characters
        name = Utilities.cleanContainerName(name);
        // names are limited to 20 characters
        if (name.length() > 0 && name.length() <= 20) {
            updateDatabase("UPDATE cats SET modified='" + Utilities.getTimestamp() + "', name='" + name + "' WHERE id=" + id);
        }
    }

    // GETTERS (GET RECORDS)
    static LinkedList<Category> getCategories() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM cats");
                LinkedList<Category> cats = new LinkedList();
                while (rs.next()) {
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    String n = rs.getString("name");
                    // store info in Record object
                    cats.add(new Category(i, c, m, n, DBMS.getCategoryAmount(i, "ALL")));
                }
                return cats;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    static LinkedList<Envelope> getEnvelopes() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM envs");
                LinkedList<Envelope> envs = new LinkedList();
                while (rs.next()) {
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    String n = rs.getString("name");
                    int cid = rs.getInt("catid");
                    // store info in Record object
                    envs.add(new Envelope(i, c, m, n, DBMS.getEnvelopeAmount(i, "ALL"), cid));
                }
                return envs;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    static LinkedList<Account> getAccounts() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM accts");
                LinkedList<Account> accts = new LinkedList();
                while (rs.next()) {
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    String n = rs.getString("name");
                    int e = rs.getInt("enabled");
                    // store info in Record object
                    if (e == 1) {
                        accts.add(new Account(i, c, m, n, DBMS.getAccountAmount(i, "ALL"), true));
                    } else {
                        accts.add(new Account(i, c, m, n, DBMS.getAccountAmount(i, "ALL"), false));
                    }
                }
                return accts;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    static LinkedList<Email> getEmail() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM email");
                LinkedList<Email> email = new LinkedList();
                while (rs.next()) {
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    int at = rs.getInt("attempt");
                    int u = rs.getInt("userid");
                    String ad = rs.getString("addr");
                    // store info in Record object
                    email.add(new Email(i, c, m, at, u, ad));
                }
                return email;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    static LinkedList<Credential> getCredentials() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM creds");
                LinkedList<Credential> creds = new LinkedList();
                while (rs.next()) {
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    String u = rs.getString("un");
                    String p = rs.getString("pw");
                    int t = rs.getInt("type");
                    int e = rs.getInt("enabled");
                    // store info in Record object
                    if (e == 1) {
                        creds.add(new Credential(i, c, m, u, p, t, true));
                    } else {
                        creds.add(new Credential(i, c, m, u, p, t, false));
                    }
                }
                return creds;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    /**
     * Retrieves/calculates the total amount in the given envelope
     * @param id ID of envelope, or -1 for all envelopes
     * @param asOfDate date before which transactions will be tallied; specify
     * 'ALL' to get total amount regardless of date
     * @return total amount in specified envelope as of the given date; return
     * -999999999 if envelope does not exist
     */
    static int getEnvelopeAmount(int id, String asOfDate) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // set query
                String query;
                if (Utilities.isDate(asOfDate)) {
                    if(id==-1) {
                        query = "SELECT sum(amt) FROM trans WHERE envid!=-1 AND date<='" + asOfDate + "'";
                    } else {
                        query = "SELECT sum(amt) FROM trans WHERE envid=" + id + " AND date<='" + asOfDate + "'";
                    }
                } else {
                    if(id==-1) {
                        query = "SELECT sum(amt) FROM trans WHERE envid!=-1";
                    } else {
                        query = "SELECT sum(amt) FROM trans WHERE envid=" + id;
                    }
                }
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -999999999;
        }
    }

    /**
     * Retrieves/calculates the total amount in the given account
     * @param id ID of account, or -1 for all accounts
     * @param asOfDate date before which transactions will be tallied; specify
     * 'ALL' to get total amount regardless of date
     * @return total amount in specified account as of the given date; return
     * -999999999 if account does not exist
     */
    static int getAccountAmount(int id, String asOfDate) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // set query
                String query;
                if (Utilities.isDate(asOfDate)) {
                    if(id==-1) { // total for all accounts by date
                        query = "SELECT sum(amt) FROM trans WHERE acctid!=-1 AND date<='" + asOfDate + "'";
                    } else {     // total for specifiec account by date
                        query = "SELECT sum(amt) FROM trans WHERE acctid=" + id + " AND date<='" + asOfDate + "'";
                    }
                } else {
                    if(id==-1) { // total for all accounts
                        query = "SELECT sum(amt) FROM trans WHERE acctid!=-1";
                    } else {     // total for specific account
                        query = "SELECT sum(amt) FROM trans WHERE acctid=" + id;
                    }
                }
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -999999999;
        }
    }

    /**
     * Retrieves/calculates the total amount in the given category
     *
     * @param id ID of category
     * @param asOfDate date before which transactions will be tallied; specify
     * 'ALL' to get total amount regardless of date
     * @return total amount in specified category as of the given date; return
     * -999999999 if category does not exist
     */
    static int getCategoryAmount(int id, String asOfDate) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // set query
                String query;
                if (Utilities.isDate(asOfDate)) {
                    query = "SELECT sum(amt) FROM trans JOIN envs ON trans.envid=envs.id WHERE envs.catid=" + id + " AND date<='" + asOfDate + "' ORDER BY date, trans.modified";
                } else {
                    query = "SELECT sum(amt) FROM trans JOIN envs ON trans.envid=envs.id WHERE envs.catid=" + id;
                }
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            return -999999999;
        }
    }

    static LinkedList<Transaction> getAllTransactions(javax.swing.JProgressBar reportProgressBar) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM trans ORDER BY date, id");
                int curr = 0;
                int max = rs.getFetchSize();
                LinkedList<Transaction> trans = new LinkedList();
                while (rs.next()) {
                    reportProgressBar.setValue((++curr)*100/max);
                    // get info from record
                    int i = rs.getInt("id");
                    String c = rs.getString("created");
                    String m = rs.getString("modified");
                    String da = rs.getString("date");
                    String de = rs.getString("desc");
                    int am = rs.getInt("amt");
                    int ac = rs.getInt("acctid");
                    int u = rs.getInt("userid");
                    int e = rs.getInt("envid");
                    int t = rs.getInt("txid");
                    // store info in Record object
                    trans.add(new Transaction(i, c, m, da, de, am, ac, u, e, t));
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }
    
    /**
     * Retrieves transactions for a given envelope, account, and date range
     * @param acctid ID of specific account, or -1 for any/all accounts
     * @param envid ID of specific envelope, or -1 for any/all envelopes
     * @param from starting date of date range (inclusive), format yyyy-mm-dd
     * @param to ending date of date range (inclusive), format yyyy-mm-dd
     * @param hideTx true = hide transfer transactions, false = show transfer
     * transactions
     * @return list of transactions matching given criteria
     */
    static LinkedList<Transaction> getTransactions(int acctid, int envid, String from, String to, boolean hideTx) {
        if (!Utilities.isDate(from) || !Utilities.isDate(to)) { // checks for valid dates
            return null;
        }
        String criteria, query;
        String whereContainer = "";
        
        if (from.compareToIgnoreCase(to)>0) { // makes 'from' the earlier date
            String tmp = from;
            from = to;
            to = tmp;
        }
        String whereDate = " date>='" + from + "' AND date<='" + to + "'";
        
        // set search criteria as necessary according to XX (AE) where:
        // 0X = account not specified   1X = account specified
        // X0 = envelope not specified  X1 = envelope specified
        if(acctid==-1 && envid!=-1) {        // 01
            criteria = "01";
            whereContainer = " envid="  + envid + " AND";
        } else if(acctid!=-1 && envid==-1) { // 10
            criteria = "10";
            whereContainer = " acctid=" + acctid + " AND";
        } else if(acctid!=-1 && envid!=-1) { // 11
            criteria = "11";
            whereContainer = " acctid=" + acctid + " AND envid="  + envid + " AND";
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
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    // get info from record
                    int i     = rs.getInt("id");
                    String c  = rs.getString("created");
                    String m  = rs.getString("modified");
                    String da = rs.getString("date");
                    String de = rs.getString("desc");
                    int am    = rs.getInt("amt");
                    int ac    = rs.getInt("acctid");
                    int u     = rs.getInt("userid");
                    int e     = rs.getInt("envid");
                    int t     = rs.getInt("txid");
                    // store info in Record object
                    trans.add(new Transaction(i, c, m, da, de, am, ac, u, e, t));
                }
                // only show running total from most recent transaction and when:
                // 00 (any acct, any env)
                // 10 (specific acct, any env)
                // 01 (any acct, specific env)
                if(trans.size()>0 && !criteria.equalsIgnoreCase("11")) { // only if acct, env, or none is specified
                    int runTot;
                    if(criteria.equalsIgnoreCase("10")) {        // 10 (running total for only the specified account)
                        runTot = DBMS.getAccountAmount(acctid, to);
                    } else if(criteria.equalsIgnoreCase("01")) { // 01 (running total for only the specified envelope)
                        runTot = DBMS.getEnvelopeAmount(envid, to);
                    } else {                                     // 00 (running total for all transactions)
                        runTot = DBMS.getAccountAmount(-1, to);
                    }
                    // sets running total for each transaction
                    for (Transaction t : trans) {
                        t.setRunningTotal(Utilities.amountToString(runTot));
                        runTot -= t.getAmount();
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Retrieves transactions for a given envelope, account, and index range,
     * where index corresponds to the transaction number when ordered by
     * transaction date (then modified date).
     * @param acctid ID of specific account, or -1 for any/all accounts
     * @param envid ID of specific envelope, or -1 for any/all envelopes
     * @param from starting index of range (first transaction is 1)
     * @param to ending index of range
     * @param hideTx true = hide transfer transactions, false = show transfer
     * transactions
     * @return list of transactions matching given criteria
     */
    static LinkedList<Transaction> getTransactions(int acctid, int envid, int from, int to, boolean hideTx) {
        String criteria, query;
        String where = "", hideTxCriteria = "";
        
        if(to < from) { // ensures to is bigger index
            int tmp = from;
            from = to;
            to = tmp;
        }
        
        // set search criteria as necessary according to XX (AE) where:
        // 0X = account not specified   1X = account specified
        // X0 = envelope not specified  X1 = envelope specified
        if(hideTx) {
            hideTxCriteria = " (acctid!=-1 AND envid!=-1) AND";
        }
        if(acctid==-1 && envid!=-1) {        // 01
            criteria = "01";
            where = " WHERE" + hideTxCriteria + " envid="  + envid;
        } else if(acctid!=-1 && envid==-1) { // 10
            criteria = "10";
            where = " WHERE" + hideTxCriteria + " acctid=" + acctid;
        } else if(acctid!=-1 && envid!=-1) { // 11
            criteria = "11";
            where = " WHERE" + hideTxCriteria + " acctid=" + acctid + " AND envid="  + envid;
        } else {                             // 00
            criteria = "00";
            if(hideTx) {
                where = " WHERE (acctid!=-1 AND envid!=-1)";
            }
        }
        query = "SELECT * FROM (SELECT * FROM (SELECT * FROM trans" + where + " ORDER BY date DESC, id DESC LIMIT " + to + ") ORDER BY date, id LIMIT " + (to-from+1) + ") ORDER BY date DESC, id DESC";
        
        /*
        Example query when selecting records 16 thru 20:
        SELECT * FROM 
        (
            SELECT * FROM 
            (
                SELECT * FROM
                    trans
                ORDER BY
                    date DESC, id DESC
                LIMIT 20
            )
            ORDER BY
                date, modified, id
            LIMIT 5
        ) ORDER BY
            date DESC, id DESC
        */
        
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery(query);
                LinkedList<Transaction> trans = new LinkedList();
                while(rs.next()) {
                    // get info from record
                    int i     = rs.getInt("id");
                    String c  = rs.getString("created");
                    String m  = rs.getString("modified");
                    String da = rs.getString("date");
                    String de = rs.getString("desc");
                    int am    = rs.getInt("amt");
                    int ac    = rs.getInt("acctid");
                    int u     = rs.getInt("userid");
                    int e     = rs.getInt("envid");
                    int t     = rs.getInt("txid");
                    // store info in Record object
                    trans.add(new Transaction(i, c, m, da, de, am, ac, u, e, t));
                }
                
                if(from==1 && to>0 && trans.size()>0 && !criteria.equalsIgnoreCase("11")) { // only if acct, env, or none is specified
                    int runTot;
                    if(criteria.equalsIgnoreCase("10")) {        // 10 (running total for only the specified account)
                        runTot = DBMS.getAccountAmount(acctid, "ALL");
                    } else if(criteria.equalsIgnoreCase("01")) { // 01 (running total for only the specified envelope)
                        runTot = DBMS.getEnvelopeAmount(envid, "ALL");
                    } else {                                     // 00 (running total for all transactions)
                        runTot = DBMS.getAccountAmount(-1, "ALL");
                    }
                    // sets running total for each transaction
                    for (Transaction t : trans) {
                        t.setRunningTotal(Utilities.amountToString(runTot));
                        runTot -= t.getAmount();
                    }
                }
                return trans;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    static Transaction getTransaction(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM trans WHERE id=" + id);
                // get info from record
                int i     = rs.getInt("id");
                String c  = rs.getString("created");
                String m  = rs.getString("modified");
                String da = rs.getString("date");
                String de = rs.getString("desc");
                int am    = rs.getInt("amt");
                int ac    = rs.getInt("acctid");
                int u     = rs.getInt("userid");
                int e     = rs.getInt("envid");
                int t     = rs.getInt("txid");
                // store info in Record object
                return new Transaction(i, c, m, da, de, am, ac, u, e, t);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return null;
        }
    }
    
    /**
     * Provides number of transactions in the database with the specified envid
     * @param envid ID of envelope record
     * @return the quantity of transactions for specified envelope
     */
    static int getTransactionCount(int envid) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT count(id) FROM trans WHERE envid=" + envid);
                return rs.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            return 0;
        }
    }
}
