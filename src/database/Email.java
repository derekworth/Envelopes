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
public class Email {
    private String created;
    private String modified;
    private int attempt;
    private int id;
    private User user;
    private String addr;
    
    // CONSTRUCTOR
        
    Email(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM email WHERE id = " + id);
                // sets variables accordingly
                this.created = rs.getString("created");
                this.modified = rs.getString("modified");
                this.attempt = rs.getInt("attempt");
                this.id = rs.getInt("id");
                this.user = new User(rs.getInt("userid"));
                this.addr = rs.getString("addr");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;
        }
    }
    
    Email(String addr) {
        // formats input
        addr = addr.toLowerCase();
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents duplicate addresses
        if (DBMS.isEmail(addr)) {
            this.id = -1;
        } else {
            // creates new address in database (without user)
            DBMS.updateDatabase("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + addr + "')");
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
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.attempt = rs.getInt("attempt");
                    this.id = rs.getInt("id");
                    this.user = new User(rs.getInt("userid"));
                    this.addr = rs.getString("addr");
                }
            } catch (ClassNotFoundException | SQLException e) {
                this.id = -1;
            }
        }
    }
    
    Email(String addr, int userid) {
        // formats input
        addr = addr.toLowerCase();
        // gets user
        User usr = new User(userid);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // prevents duplicate addresses and checks that user exists
        if (DBMS.isEmail(addr)) {
            this.id = -1;
        } else {
            if (!usr.isInDatabase() || usr.isGmail()) {
                // creates new address in database (without user)
                DBMS.updateDatabase("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 1, -1, '" + addr + "')");
            } else {
                // creates new address in database (with user)
                DBMS.updateDatabase("INSERT INTO email (created, modified, attempt, userid, addr) VALUES ('" + ts + "', '" + ts + "', 0, " + usr.getId() + ", '" + addr + "')");
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
                    this.created = rs.getString("created");
                    this.modified = rs.getString("modified");
                    this.attempt = rs.getInt("attempt");
                    this.id = rs.getInt("id");
                    this.user = new User(rs.getInt("userid"));
                    this.addr = rs.getString("addr");
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
    
    public int getAttempt() {
        return attempt;
    }
    
    public int getId() {
        return id;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getAddress() {
        return addr;
    }
    
    // SETTERS
        
    public String setAttempt(int attempt) {
        // 0 = authenticated
        // # = attempt number (max of 5 attempts allowed)
        if (!isInDatabase()) {
            return "Error: email does not exist in database";
        }
        // set enabled variable and update modified getTimestamp
        if (this.attempt==attempt) {
            return "Email (" + this.addr + ") attempt has already been set to " + attempt;
        }
        String ts = Utilities.getTimestamp();
        if (attempt < 0) {
            attempt = 5;
        }
        String query = "UPDATE email SET modified='" + ts + "', attempt=" + attempt + " WHERE id=" + this.id;
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
            this.attempt = attempt;
            return "Email (" + this.addr + ") attempt successfully set to " + this.attempt;
            
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to update attempt number";
        }
    }
    
    public String setUser(User usr) {
        String query;
        String ts = Utilities.getTimestamp();
        if(usr==null) {
            query = "UPDATE email SET modified='" + ts + "', userid=-1 WHERE id=" + this.id;
        } else {
            if (!isInDatabase()) {
                return "Error: email does not exist in database";
            } else if (DBMS.getGmail().getId()==usr.getId()) {
                return "Error: user cannot be set to Gmail account";
            } else if (this.user.getId()==usr.getId()) {
                return "Email (" + this.addr + ") user ID is already set to " + usr;
            } else if (!usr.isInDatabase()) {
                return "Error: user (" + usr + ") does not exist in database";
            }
            // set new name and updates modified getTimestamp
            query = "UPDATE email SET modified='" + ts + "', userid=" + usr.getId() + " WHERE id=" + this.id;
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
            this.user = usr;
            return "Email (" + this.addr + ") successfully set to user";
        } catch (ClassNotFoundException | SQLException e) {
            return "Error: unable to update user";
        }
    }

    @Override
    public String toString() {
        if (isInDatabase())
            return "created: " + created + " | modified: " + modified + " | attempt: " + attempt + " | id: " + id + " | user id: " + user + " | address: " + addr;
        else
            return "This email did not initialize";
    }
}
