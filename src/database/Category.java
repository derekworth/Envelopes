package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public final class Category {
    private final String created;
    private String modified;
    private boolean enabled;
    private final int id;
    private String name;
    private double amt;
    
    // CONSTRUCTORS
    
    public Category(String created, String modified, boolean enabled, int id, String name, double amt) {
        this.created = created;
        this.modified = modified;
        this.enabled = enabled;
        this.id = id;
        this.name = name;
        this.amt = amt;
    }
        
//    Category(int id) {
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT * FROM cats WHERE id = " + id);
//                // sets variables accordingly
//                this.created = rs.getString("created");
//                this.modified = rs.getString("modified");
//                this.enabled = rs.getInt("enabled");
//                this.id = rs.getInt("id");
//                this.name = rs.getString("name");
//                updateAmt();
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            this.id = -1;
//        }
//    }
//    
//    Category(String name) {
//        // format input
//        name = name.toLowerCase();
//        // sets created/modified date/time
//        String ts = Utilities.getTimestamp();
//        // ensures no envelope already exists with given name
//        if (DBMS.isContainer(name, false)) {            
//            this.id = -1;
//        } else {
//            // creates new category in database
//            DBMS.updateDatabase("INSERT INTO cats (created, modified, enabled, name) VALUES ('" + ts + "', '" + ts + "', 1, '" + name + "')");
//            // sets variables accordingly
//            try {
//                // register the driver
//                Class.forName("org.sqlite.JDBC");
//                // connect to database
//                try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                    stmt.setQueryTimeout(TIMEOUT);
//                    // execute query
//                    ResultSet rs = stmt.executeQuery("SELECT * FROM cats ORDER BY id DESC LIMIT 1");
//                    // sets variables accordingly
//                    this.created = rs.getString("created");
//                    this.modified = rs.getString("modified");
//                    this.enabled = rs.getInt("enabled");
//                    this.id = rs.getInt("id");
//                    this.name = rs.getString("name");
//                    updateAmt();
//                }
//            } catch (ClassNotFoundException | SQLException e) {
//                this.id = -1;
//            }
//        }
//    }
    
    // GETTERS
    
//    public boolean isInDatabase() {
//        return this.id!=-1;
//    }
    
    public String getCreated() {
        return created;
    }
    
    public String getModified() {
        return modified;
    }
    
    public boolean isEnabled() {
        return enabled;
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
        
    public void setEnabled(boolean en) {
        enabled = en;
//        if (!isInDatabase()) {
//            return "Error: category does not exist in database";
//        }
//        String query;
//        String ts = Utilities.getTimestamp();
//        // set enabled variable and update modified getTimestamp
//        if (en) {
//            if (isEnabled()) {
//                return "Category (" + this.name + ") is already enabled";
//            }
//            query = "UPDATE cats SET modified='" + ts + "', enabled=1 WHERE id=" + this.id;
//        } else {
//            if (!isEnabled()) {
//                return "Category (" + this.name + ") is already disabled";
//            }
//            query = "UPDATE cats SET modified='" + ts + "', enabled=0 WHERE id=" + this.id;
//        }
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
//            if (en) {
//                this.modified = ts;
//                this.enabled = 1;
//                return "Category (" + this.name + ") successfully enabled";
//            } else {
//                this.enabled = 0;
//                return "Category (" + this.name + ") successfully disabled";
//            }
//            
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to enable/disable category";
//        }
    }
    
    public void setModified(String newModified) {
        modified = newModified;
    }
    
    public void setName(String newName) {
        name = newName;
//        newName = newName.toLowerCase();
//        if (!isInDatabase()) {
//            return "Error: category does not exist in database";
//        } else if (!isEnabled()) {
//            return "Error: disabled categories cannot be updated";
//        } else if (newName.equalsIgnoreCase(this.name)) {
//            return "Category is already named '" + newName + "'";
//        }
//        String oldName = this.name;
//        String ts = Utilities.getTimestamp();
//        // set new name and updates modified getTimestamp
//        String query = "UPDATE cats SET modified='" + ts + "', name='" + newName + "' WHERE id=" + this.id;
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
//            this.name = newName;
//            return "Category (" + oldName + ") successfully renamed to '" + newName + "'";
//        } catch (ClassNotFoundException | SQLException e) {
//            return "Error: unable to rename category (" + oldName + ") to '" + newName + "'";
//        }
    }
    
    public void setAmt(double newAmt) {
        amt = newAmt;
//        try {
//            // register the driver
//            Class.forName("org.sqlite.JDBC");
//            // connect to database
//            try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
//                stmt.setQueryTimeout(TIMEOUT);
//                // execute query
//                ResultSet rs = stmt.executeQuery("SELECT sum(amt) FROM trans join envs ON envid=envs.id WHERE catid=" + id);
//                // sets variables accordingly
//                amt = rs.getDouble(1);
//            }
//        } catch (ClassNotFoundException | SQLException e) {
//            amt = -999999999;
//        }
    }

    @Override
    public String toString() {
        return "created: " + created + " | modified: " + modified + " | enabled: " + enabled + " | id: " + id + " | name: " + name;
    }
}
