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
    private int aid;
    private int eid;
    private final int uid;
    private String date;
    private String desc;
    private double amt;
    private String runTot;
    private int tid;
    
    // CONSTRUCTOR
    
    public Transaction(String created, String modified, int id, int aid, int eid, int uid, String date, String desc, double amt, int tid) {
        this.created = created;
        this.modified = modified;
        this.id     = id;
        this.aid    = aid;
        this.eid    = eid;
        this.uid    = uid;
        this.date   = date;
        this.desc   = desc;
        this.amt    = amt;
        this.tid    = tid;
        this.runTot = "";
    }
    
    // GETTERS
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public int getId() {
        return id;
    }
    
    public int getAccountId() {
        return aid;
    }
    
    public int getEnvelopeId() {
        return eid;
    }
    
    public int getUserId() {
        return uid;
    }
    
    public String getDate() {
        return date;
    }
    
    public String getDescription() {
        return desc;
    }
    
    public double getAmount() {
        return Double.parseDouble(Utilities.roundAmount(amt));
    }
    
    public String getRunningTotal() {
        return runTot;
    }
    
    public int getTxId() {
        return tid;
    }
    
    // SETTERS
    
    public void setModified(String newModified) {
        modified = newModified;
    }
    
    public void setAccountId(int aid) {
        this.aid = aid;
    }
    
    public void setEnvelopeId(int eid) {
        this.eid = eid;
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
//            this.dat
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
//        return "Transaction (" + this.id + ") date successfully changed to '" + this.date + "'";e = newDate;
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
    
    public void setTxId(int tid) {
        this.tid = tid;
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
    
    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | id: " + id + " | aid: " + aid + " | eid: " + eid + " | uid: " + uid + " | date: " + date + " | desc: " + desc + " | amt: " + amt + " | tid: " + tid;
    }
}
