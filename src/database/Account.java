package database;

import static database.DBMS.TIMEOUT;
import static database.DBMS.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import misc.Utilities;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class Account {
    private String created;
    private String modified;
    private int enabled;
    private int id;
    private String name;
    private double amt;
    
    // CONSTRUCTORS
  
    Account(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM accts WHERE id = " + id);
                // sets variables accordingly
                this.created = rs.getString("created");
                this.modified = rs.getString("modified");
                this.enabled = rs.getInt("enabled");
                this.id = rs.getInt("id");
                this.name = rs.getString("name");
                updateAmt();
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;
        }
    }
    
    Account(String name) {
        // format input
        name = name.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // ensures no envelope already exists with given name
        if (DBMS.isContainer(name, false)) {
            this.id = -1;
        } else {
            // creates new account in database
            DBMS.updateDatabase("INSERT INTO accts (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')");
            // sets variables accordingly
            try {
                // register the driver
                Class.forName("org.sqlite.JDBC");
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT * FROM accts ORDER BY id DESC LIMIT 1");
                    // sets variables accordingly
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.enabled = rs.getInt("enabled");
                    this.id = rs.getInt("id");
                    this.name = rs.getString("name");
                    updateAmt();
                }
            } catch (ClassNotFoundException | SQLException e) {
                this.id = -1;
            }
        }
    }
    
    // GETTERS
    
    public boolean isInDatabase() {
        return this.id!=-1;
    }
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public boolean isEnabled() {
        return enabled==1;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public double getAmt() {
        return amt;
    }
    
    // SETTERS
    
    public String setEnabled(boolean en) {
        if (!isInDatabase()) {
            return "Error: account does not exist in database";
        }
        String query;
        String ts = Utilities.getTimestamp();
        // set enabled variable and update modified getTimestamp
        if (en) {
            if (isEnabled()) {
                return "Account (" + this.name + ") is already enabled";
            }
            query = "UPDATE accts SET modified='" + ts + "', enabled=1 WHERE id=" + this.id;
        } else {
            if (!isEnabled()) {
                return "Account (" + this.name + ") is already disabled";
            }
            query = "UPDATE accts SET modified='" + ts + "', enabled=0 WHERE id=" + this.id;
        }
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                stmt.executeUpdate(query);
            }
            this.modified = ts;
            if (en) {
                this.modified = ts;
                this.enabled = 1;
                return "Account (" + this.name + ") successfully enabled";
            } else {
                this.enabled = 0;
                return "Account (" + this.name + ") successfully disabled";
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to enable/disable account";
        }
    }
    
    public String setName(String newName) {
        newName = newName.toLowerCase();
        if (!isInDatabase()) {
            return "Error: account does not exist in database";
        } else if (!isEnabled()) {
            return "Error: disabled accounts cannot be updated";
        } else if (newName.equalsIgnoreCase(this.name)) {
            return "Account is already named '" + newName + "'";
        }
        String oldName = this.name;
        String ts = Utilities.getTimestamp();
        // set new name and updates modified getTimestamp
        String query = "UPDATE accts SET modified='" + ts + "', name='" + newName + "' WHERE id=" + this.id;
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database and execute queries
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                stmt.executeUpdate(query);
            }
            this.modified = ts;
            this.name = newName;
            return "Account (" + oldName + ") successfully renamed to '" + newName + "'";
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to rename account (" + oldName + ") to '" + newName + "'";
        }
    }
    
    public void updateAmt() {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE acctid=" + id);
                // sets variables accordingly
                amt = rs.getDouble(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            amt = -999999999;
        }
    }

    @Override
    public String toString() {
        if (isInDatabase())
            return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | name: " + name + " | amount: " + amt;
        else
            return "This account did not initialize";
    }
}
