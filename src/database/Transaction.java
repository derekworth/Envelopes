package database;

import misc.Utilities;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Transaction {
    private final String created;
    private String modified;
    private final int id;
    private Account acct;
    private Envelope env;
    private final User user;
    private String date;
    private String desc;
    private double amt;
    private String runTot;
    private Transaction tx;
    
    // CONSTRUCTOR
    
    public Transaction(String created, String modified, int id, Account acct, Envelope env, User usr, String date, String desc, double amt, Transaction tx) {
        this.created = created;
        this.modified = modified;
        this.id = id;
        this.acct = acct;
        this.env = env;
        this.user = usr;
        this.date = date;
        this.desc = desc;
        this.amt = amt;
        this.tx = tx;
        this.runTot = "";
    }
    
//    public Transaction(int id, Account account) {
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                String query = "SELECT * FROM trans WHERE id = " + id;
//                ResultSet rs = stmt.executeQuery(query);
//                // sets variables accordingly
//                this.created = rs.getString("created");
//                this.modified = rs.getString("modified");
//                this.id = rs.getInt("id");
//                this.acct = account;
//                this.env = new Envelope(rs.getInt("envid"));
//                this.user = new User(rs.getInt("userid"));
//                this.date = rs.getString("date");
//                this.desc = rs.getString("desc");
//                this.amt = rs.getDouble("amt");
//                this.runTot = "";
//                this.tx = rs.getInt("tx");
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            this.id = -1;
//        }
//    }
//    
//    public Transaction(Account account, Envelope envelope, User user, String date, String desc, double amt, String runTot) {
//        desc = Utilities.trimInvalidCharacters(desc);
//        desc = Utilities.removeDoubleApostrophes(desc);
//        desc = Utilities.doubleApostrophes(desc);
//        while(true) {
//            if (account==null  || envelope==null || user==null || !user.isInDatabase() || !user.isEnabled() || user.isGmail() ||
//                    ((!account.isInDatabase() || !account.isEnabled()) && (!envelope.isInDatabase() || !envelope.isEnabled()))) {
//                this.id = -1;
//                break;
//            } else {
//                // sets created/modified date/time
//                String ts = Utilities.getTimestamp();
//                // sets current date if date format is invalid
//                if (!Utilities.isDate(date)) {
//                    date = ts.substring(0, 10);
//                }
//                String query = "INSERT INTO trans (created, modified, acctid, envid, userid, date, desc, amt) VALUES ('" + ts + "', '" + ts + "', " + account.getId() + ", " + envelope.getId() + ", " + user.getId() + ", '" + date + "', '" + desc + "', " + amt + ")";
//                if(DBMS.executeQuery(query)) {
//                    // sets variables accordingly
//                    try {
//                        // register the driver
//                        Class.forName("org.sqlite.JDBC");
//                        // connect to database
//                        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                            stmt.setQueryTimeout(TIMEOUT);
//                            // execute query
//                            ResultSet rs = stmt.executeQuery("SELECT * FROM trans ORDER BY id DESC LIMIT 1");
//                            // sets variables accordingly
//                            this.created = rs.getString("created");
//                            this.modified = rs.getString("modified");
//                            this.id = rs.getInt("id");
//                            this.acct = account;
//                            this.env = new Envelope(rs.getInt("envid"));
//                            this.user = new User(rs.getInt("userid"));
//                            this.date = rs.getString("date");
//                            this.desc = rs.getString("desc");
//                            this.amt = rs.getDouble("amt");
//                            this.runTot = runTot;
//                            this.tx = rs.getInt("tx");
//                            break;
//                        }
//                    } catch (ClassNotFoundException | SQLException e) {}
//                }
//            }
//            this.id = -1;
//            break;
//        }
//    }
    
    // GETTERS
    
//    public boolean isInDatabase() {
//        return id!=-1;
//    }
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public int getId() {
        return id;
    }
    
    public Account getAcct() {
        return acct;
    }
    
    public Envelope getEnv() {
        return env;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getDate() {
        return date;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public double getAmt() {
        return Double.parseDouble(Utilities.roundAmount(amt));
    }
    
    public String getRunTot() {
        return runTot;
    }
    
    public Transaction getTx() {
        return tx;
    }
    
    // SETTERS
    
    public void setModified(String newModified) {
        modified = newModified;
    }
    
    public void setAccount(Account acct) {
        this.acct = acct;
    }
    
    public void setEnvelope(Envelope env) {
        this.env = env;
    }
    
    public void setDate(String newDate) {
        date = newDate;
//        if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        } else if (!Utilities.isDate(newDate)) {
//            return "Error: '" + newDate + "' is not a valid date or in a valid format (YYYY-MM-DD)";
//        } else if (newDate.equalsIgnoreCase(this.date)) {
//            return "Transaction (" + this.id + ") date already set to '" + newDate + "'";
//        }
//        String ts = Utilities.getTimestamp();
//        // set new name and updates modified getTimestamp
//        String query = "UPDATE trans SET modified='" + ts + "', date='" + newDate + "' WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            this.modified = ts;
//            this.date = newDate;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") date to '" + newDate + "'";
//        }
//        Transaction t = new Transaction(tx);
//        if(t.isInDatabase()) { // update corresponding transfer transaction
//            query = "UPDATE trans SET modified='" + ts + "', date='" + newDate + "' WHERE id=" + t.getId();
//            try {
//                // register the driver
//                Class.forName("org.sqlite.JDBC");
//                // connect to database and execute queries
//                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                    stmt.setQueryTimeout(TIMEOUT);
//                    // execute query
//                    stmt.executeUpdate(query);
//                }
//            } catch (ClassNotFoundException | SQLException e) {
//                return "Error: date changed for transaction but not corresponding transfer transaction";
//            }
//            return "Date successfully changed to '" + this.date + "' for both transaction and corresponding transfer transaction";
//        }
//        return "Transaction (" + this.id + ") date successfully changed to '" + this.date + "'";
    }
    
    public void setDescription(String newDesc) {
        desc = newDesc;
//        newDesc = Utilities.doubleApostrophes(newDesc);
//        if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        } else if (newDesc.equalsIgnoreCase(this.desc)) {
//            return "Transaction (" + this.id + ") description already set to '" + newDesc + "'";
//        }
//        String ts = Utilities.getTimestamp();
//        // set new name and updates modified getTimestamp
//        String query = "UPDATE trans SET modified='" + ts + "', desc='" + newDesc + "' WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            this.modified = ts;
//            this.desc = newDesc;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") desc to '" + newDesc + "'";
//        }
//        Transaction t = new Transaction(tx);
//        if(t.isInDatabase()) { // update corresponding transfer transaction
//            query = "UPDATE trans SET modified='" + ts + "', desc='" + newDesc + "' WHERE id=" + t.getId();
//            try {
//                // register the driver
//                Class.forName("org.sqlite.JDBC");
//                // connect to database and execute queries
//                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                    stmt.setQueryTimeout(TIMEOUT);
//                    // execute query
//                    stmt.executeUpdate(query);
//                }
//            } catch (ClassNotFoundException | SQLException e) {
//                return "Error: desc changed for transaction but not corresponding transfer transaction";
//            }
//            return "Desc successfully changed to '" + this.desc + "' for both transaction and corresponding transfer transaction";
//        }
//        return "Transaction (" + this.id + ") description successfully changed to '" + this.desc + "'";
    }
    
    public void setAmount(double newAmt) {
        amt = newAmt;
//        newAmt = Double.parseDouble(Utilities.roundAmount(newAmt));
//        if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        } else if (this.amt==newAmt) {
//            return "Transaction (" + this.id + ") amount is already set to " + newAmt;
//        }
//        String ts = Utilities.getTimestamp();
//        String query = "UPDATE trans SET modified='" + ts + "', amt=" + newAmt + " WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            this.amt = newAmt;
//            this.modified = ts;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") amount to " + newAmt;
//        }
//        Transaction t = new Transaction(tx);
//        if(t.isInDatabase()) { // update corresponding transfer transaction
//            query = "UPDATE trans SET modified='" + ts + "', amt=" + (-newAmt) + " WHERE id=" + t.getId();
//            try {
//                // register the driver
//                Class.forName("org.sqlite.JDBC");
//                // connect to database and execute queries
//                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                    stmt.setQueryTimeout(TIMEOUT);
//                    // execute query
//                    stmt.executeUpdate(query);
//                }
//            } catch (ClassNotFoundException | SQLException e) {
//                return "Error: amt changed for transaction but not corresponding transfer transaction";
//            }
//            return "Amount successfully changed to '" + this.amt + "' for both transaction and corresponding transfer transaction";
//        }
//        return "Transaction (" + this.id + ") amount successfully set to " + newAmt;
    }
    
    public void setTransferTransaction(Transaction tx) {
        this.tx = tx;
    }
//        if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        }
//        String ts = Utilities.getTimestamp();
//        String query = "UPDATE trans SET modified='" + ts + "', tx=" + txID + " WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            this.tx = txID;
//            this.modified = ts;
//            return "Transaction (" + this.id + ") transfer id successfully set to " + txID;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") transfer id to " + txID;
//        }
//    }
    
    public void setRunningTotal(String runTot) {
        this.runTot = runTot;
    }
    
//    public String setAccount(String newAcctName) {
//        Account newAcct = DBMS.getAccount(newAcctName, true);
//        if(newAcct==null) {
//            return "Error: account (" + newAcctName + ") does not exist.";
//        } else if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        } else if (this.acct.getId()==newAcct.getId()) {
//            return "Account (" + this.acct.getName() + ") is already set";
//        }
//        String ts = Utilities.getTimestamp();
//        String query = "UPDATE trans SET modified='" + ts + "', acctid=" + newAcct.getId() + " WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            acct = newAcct;
//            modified = ts;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") account to " + newAcctName;
//        }
//        Transaction t = new Transaction(tx);
//        if(t.isInDatabase()) {
//            if(desc.length()>0) {
//                desc = " " + desc;
//            }
//            if(amt<0) {
//                if(desc.contains(")")) {
//                    this.setDescription("*(" + acct.getName() + " > " + t.getAcct().getName() + ")" + desc.substring(desc.indexOf(")")+1));
//                } else {
//                    this.setDescription("*(" + acct.getName() + " > " + t.getAcct().getName() + ")" + desc);
//                }
//            } else {
//                if(desc.contains(")")) {
//                    this.setDescription("*(" + t.getAcct().getName() + " > " + acct.getName() + ")" + desc.substring(desc.indexOf(")")+1));
//                } else {
//                    this.setDescription("*(" + t.getAcct().getName() + " > " + acct.getName() + ")" + desc);
//                }
//            }
//        }
//        return "Transaction (" + this.id + ") account successfully set to " + acct.getName();
//    }
//    
//    public String setEnvelope(String newEnvName) {
//        Envelope newEnv = DBMS.getEnvelope(newEnvName, true);
//        if(newEnv==null) {
//            return "Error: envelope (" + newEnvName + ") does not exist.";
//        } else if (!isInDatabase()) {
//            return "Error: transaction does not exist in database";
//        } else if (this.env.getId()==newEnv.getId()) {
//            return "Envelope (" + this.env.getName() + ") is already set";
//        }
//        String ts = Utilities.getTimestamp();
//        String query = "UPDATE trans SET modified='" + ts + "', envid=" + newEnv.getId() + " WHERE id=" + this.id;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database and execute queries
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                stmt.executeUpdate(query);
//            }
//            env = newEnv;
//            modified = ts;
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to change transaction (" + this.id + ") envelope to " + newEnvName;
//        }
//        Transaction t = new Transaction(tx);
//        if(t.isInDatabase()) {
//            if(desc.length()>0) {
//                desc = " " + desc;
//            }
//            if(amt<0) {
//                if(desc.contains(")")) {
//                    this.setDescription("(" + env.getName() + " > " + t.getEnv().getName() + ")" + desc.substring(desc.indexOf(")")+1));
//                } else {
//                    this.setDescription("(" + env.getName() + " > " + t.getEnv().getName() + ")" + desc);
//                }
//            } else {
//                if(desc.contains(")")) {
//                    this.setDescription("(" + t.getEnv().getName() + " > " + env.getName() + ")" + desc.substring(desc.indexOf(")")+1));
//                } else {
//                    this.setDescription("(" + t.getEnv().getName() + " > " + env.getName() + ")" + desc);
//                }
//            }
//        }
//        return "Transaction (" + this.id + ") envelope successfully set to " + env.getName();
//    }
    
    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | id: " + id + " | acct: " + acct.getName() + " | env: " + env.getName() + " | user: " + user.getUsername() + " | date: " + date + " | desc: " + desc + " | amt: " + amt + " | tx: " + tx;
    }
}
