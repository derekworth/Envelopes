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
public class User {
    private String created;
    private String modified;
    private int enabled;
    private int id;
    private int type;
    private String un;
    private String pw;
    
    // CONSTRUCTORS
        
    User(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = " + id);
                // sets variables accordingly
                this.created = rs.getString("created");
                this.modified = rs.getString("modified");
                this.enabled = rs.getInt("enabled");
                this.id = rs.getInt("id");
                this.type = rs.getInt("type");
                this.un = rs.getString("un");
                this.pw = rs.getString("pw");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;
        }
    }
    
    User(String un, String pw) {
        // format input
        un = un.toLowerCase();
        pw = Utilities.getHash(pw);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // ensures no user already exists with given username
        if (DBMS.isUser(un, false)) {
            this.id = -1;
        } else {
            // creates new account in database
            DBMS.updateDatabase("INSERT INTO users (created, modified, enabled, type, un, pw) VALUES ('" + ts + "', '" + ts + "', 1, 0, '" + un + "', '" + pw + "')");
            // sets variables accordingly
            try {
                // register the driver
                Class.forName("org.sqlite.JDBC");
                // connect to database
                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(TIMEOUT);
                    // execute query
                    ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id DESC LIMIT 1");
                    // sets variables accordingly
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.enabled = rs.getInt("enabled");
                    this.id = rs.getInt("id");
                    this.type = rs.getInt("type");
                    this.un = rs.getString("un");
                    this.pw = rs.getString("pw");
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
    
    public boolean isGmail() {
        return type==1;
    }
    
    public boolean isAdmin() {
        return type==2;
    }
    
    public String getUsername() {
        return un;
    }
    
    public String getPassword() {
        return pw;
    }
    
    // SETTERS
        
    public String setEnabled(boolean en) {
        if (!isInDatabase()) {
            return "Error: user does not exist in database";
        }
        String query;
        String ts = Utilities.getTimestamp();
        // set enabled variable and update modified getTimestamp
        if (en) {
            if (isEnabled()) {
                return "User (" + this.un + ") is already enabled";
            }
            query = "UPDATE users SET modified='" + ts + "', enabled=1 WHERE id=" + this.id;
        } else {
            if (!isEnabled()) {
                return "User (" + this.un + ") is already disabled";
            }
            query = "UPDATE users SET modified='" + ts + "', enabled=0 WHERE id=" + this.id;
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
                return "User (" + this.un + ") successfully enabled";
            } else {
                this.enabled = 0;
                return "User (" + this.un + ") successfully disabled";
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to enable/disable user";
        }
    }
    
    public String setUsername(String newUsername) {
        newUsername = newUsername.toLowerCase();
        if (!isInDatabase()) {
            return "Error: user does not exist in database";
        } else if (!isEnabled()) {
            return "Disabled users cannot be updated";
        } else if (newUsername.equalsIgnoreCase(this.un)) {
            return "User is already named '" + newUsername + "'";
        }
        String oldName = this.un;
        String ts = Utilities.getTimestamp();
        // set new name and updates modified getTimestamp
        String query = "UPDATE users SET modified='" + ts + "', un='" + newUsername + "' WHERE id=" + this.id;
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
            this.un = newUsername;
            return "User (" + oldName + ") successfully renamed to '" + newUsername + "'";
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to rename user (" + oldName + ") to '" + newUsername + "'";
        }
    }
    
    public String setPassword(String newPassword) {
        // hashes password if user is not the Gmail account
        if (!this.isGmail()) {
            newPassword = Utilities.getHash(newPassword);
        }
        
        if (!isInDatabase()) {
            return "Error: user does not exist in database";
        } else if (!isEnabled()) {
            return "Disabled users cannot be updated";
        } else if (newPassword.equalsIgnoreCase(this.pw)) {
            return "User (" + this.un + ") password is already set to specified password";
        }
        String ts = Utilities.getTimestamp();
        // set new name and updates modified getTimestamp
        String query = "UPDATE users SET modified='" + ts + "', pw='" + newPassword + "' WHERE id=" + this.id;
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
            this.pw = newPassword;
            return "User (" + this.un + ") password successfully set";
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to set user (" + this.un + ") password to '" + newPassword + "'";
        }
    }

    @Override
    public String toString() {
        if (isInDatabase())
            return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | type: " + type + " | un: " + un + " | pw: " + pw;
        else
            return "This user did not initialize";
    }
}
