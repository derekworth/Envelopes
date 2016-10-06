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
public class Envelope {
    private String created;
    private String modified;
    private int enabled;
    private int id;
    private Category category;
    private String name;
    private double amt;
    
    // CONSTRUCTORS
        
    Envelope(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM envs WHERE id = " + id);
                // sets variables accordingly
                this.created = rs.getString("created");
                this.modified = rs.getString("modified");
                this.enabled = rs.getInt("enabled");
                this.id = rs.getInt("id");
                this.category = new Category(rs.getInt("catid"));
                this.name = rs.getString("name");
                updateAmt();
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;
        }
    }
    
    Envelope(String name) {
        // formats input
        name = name.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents duplicate naming
        if (DBMS.isContainer(name, false)) {
            this.id = -1;
        } else {
            // creates new envelope in database
            DBMS.updateDatabase("INSERT INTO envs (created, modified, enabled, catid, name) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + name + "')");
            // sets variables accordingly
            try {
                // register the driver
                Class.forName("org.sqlite.JDBC");
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT * FROM envs ORDER BY id DESC LIMIT 1");
                    // sets variables accordingly
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.enabled = rs.getInt("enabled");
                    this.id = rs.getInt("id");
                    this.category = new Category(rs.getInt("catid"));
                    this.name = rs.getString("name");
                    updateAmt();
                }
            } catch (ClassNotFoundException | SQLException e) {
                this.id = -1;
            }
        }
    }
    
    Envelope(String name, Category category) {
        // formats inputs
        name = name.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents duplicate naming
        if (DBMS.isContainer(name, false)) {
            this.id = -1;
        } else {
            if (category!=null && category.isInDatabase() && category.isEnabled()) {
                // creates new envelope in database under given category
                DBMS.updateDatabase("INSERT INTO envs (created, modified, enabled, catid, name) VALUES ('" + ts + "', '" + ts + "', 1, " + category.getId() + ", '" + name + "')");
            } else {
                // creates new uncategorized envelope in database
                DBMS.updateDatabase("INSERT INTO envs (created, modified, enabled, catid, name) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + name + "')");
            }
            // sets variables accordingly
            try {
                // register the driver
                Class.forName("org.sqlite.JDBC");
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT * FROM envs ORDER BY id DESC LIMIT 1");
                    // sets variables accordingly
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.enabled = rs.getInt("enabled");
                    this.id = rs.getInt("id");
                    this.category = new Category(rs.getInt("catid"));
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
    
    public Category getCat() {
        return category;
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
            return "Error: envelope does not exist in database";
        }
        String query;
        String ts = Utilities.getTimestamp();
        // set enabled variable and update modified getTimestamp
        if (en) {
            if (isEnabled()) {
                return "Envelope (" + this.name + ") is already enabled";
            }
            query = "UPDATE envs SET modified='" + ts + "', enabled=1 WHERE id=" + this.id;
        } else {
            if (!isEnabled()) {
                return "Envelope (" + this.name + ") is already disabled";
            }
            query = "UPDATE envs SET modified='" + ts + "', enabled=0 WHERE id=" + this.id;
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
                this.enabled = 1;
                return "Envelope (" + this.name + ") successfully enabled";
            } else {
                this.enabled = 0;
                return "Envelope (" + this.name + ") successfully disabled";
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to enable/disable envelope";
        }
    }
    
    public String setCategory(Category cat) {
        if (!isInDatabase()) {                                           // ensures envelope exists
            return "Error: envelope does not exist in database";
        }
        Category oldCat = category;
        String query;
        String ts = Utilities.getTimestamp();
        if (!isEnabled()) {                                          // ensures envelope is enabled
            return "Error: disabled envelopes cannot be updated";
        } else if (cat==null) {                         // checks if new category exists
            query = "UPDATE envs SET modified='" + ts + "', catid=-1 WHERE id=" + id;
        } else if (cat.getId()==category.getId()) {                              // checks if envelope is already assigned to new category
            return "Envelope (" + name + ") category is already set to '" + oldCat.getName() + "'";
        } else {
            query = "UPDATE envs SET modified='" + ts + "', catid=" + cat.getId() + " WHERE id=" + id;
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
            modified = ts;
            category = cat;
            if(cat==null) {
                return "Envelope (" + name + ") successfully uncategorized";
            } else {
                return "Envelope (" + name + ") category successfully set to '" + cat.getName() + "'";
            }
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to update envelope (" + name + ") to '" + cat.getName() + "'";
        }
    }
    
    public String setName(String newName) {
        newName = newName.toLowerCase();
        if (!isInDatabase()) {
            return "Error: envelope does not exist in database";
        } else if (!isEnabled()) {
            return "Error: disabled envelopes cannot be updated";
        } else if (newName.equalsIgnoreCase(this.name)) {
            return "Envelope is already named '" + newName + "'";
        }
        String oldName = this.name;
        String ts = Utilities.getTimestamp();
        // set new name and updates modified getTimestamp
        String query = "UPDATE envs SET modified='" + ts + "', name='" + newName + "' WHERE id=" + this.id;
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
            return "Envelope (" + oldName + ") successfully renamed to '" + newName + "'";
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to rename envelope (" + oldName + ") to '" + newName + "'";
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
                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans WHERE envid=" + id);
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
            return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | category: " + category.getName() + " | name: " + name + " | amount: " + amt;
        else
            return "This envelope did not initialize";
    }
}
