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
public class Event {
    private String created;
    private int id;
    private String event;
    
    // CONSTRUCTOR
    
    Event(int id) {
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM log WHERE id = " + id);
                // sets variables accordingly
                this.created = rs.getString("created");
                this.id = id;
                this.event = rs.getString("event");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;
        }
    }
    
    Event(String event) {
        event = Utilities.doubleApostrophes(event);
        // sets created/modified date/time
        String ts = Utilities.getTimestamp();
        // creates new account in database
        DBMS.updateDatabase("INSERT INTO log (created, event) VALUES ('" + ts + "', '" + event + "')");
        // sets variables accordingly
        try {
            // register the driver
            Class.forName("org.sqlite.JDBC");
            // connect to database
            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(TIMEOUT);
                // execute query
                ResultSet rs = stmt.executeQuery("SELECT * FROM log ORDER BY id DESC LIMIT 1");
                // sets variables accordingly
                this.created = rs.getString("created");
                this.id = rs.getInt("id");
                this.event = rs.getString("event");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.id = -1;           
        }
    }
    
    // GETTERS
    
    public boolean isInDatabase() {
        return this.id!=-1;
    }
    
    public String getCreated() {
        return created;
    }
    
    public int getId() {
        return id;
    }
    
    public String getEvent() {
        return event;
    }

    @Override
    public String toString() {
        if (isInDatabase())
            return "created: " + created + " | id: " + id + " | event: " + event;
        else
            return "This event did not initialize";
    }
}
