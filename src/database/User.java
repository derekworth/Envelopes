package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public class User extends Record {
    private boolean enabled;
    private final int type;
    private String un;
    private String pw;
    
    public static final int USER  = 0;
    public static final int GMAIL = 1;
    public static final int ADMIN = 2;
    // CONSTRUCTORS
    
    public User(String created, String modified, int id, boolean enabled, int type, String un, String pw) {
        super(created, modified, id);
        this.enabled = enabled;
        this.type = type;
        this.un = un;
        this.pw = pw;
    }
    
    // GETTERS
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getType() {
        return type;
    }
    
    public boolean isGmail() {
        return type==GMAIL;
    }
    
    public boolean isAdmin() {
        return type==ADMIN;
    }
    
    public String getUsername() {
        return un;
    }
    
    public String getPassword() {
        return pw;
    }
    
    // SETTERS
    
    public void setEnabled(boolean en) {
        enabled = en;
    }
    
    public void setUsername(String newUsername) {
        un = newUsername;
    }
    
    public void setPassword(String newPassword) {
        pw = newPassword;
    }

    @Override
    public String toString() {
        return "created: " + this.getCreated()
                + " | modified: "+ this.getModified()
                + " | enabled: " + this.isEnabled()
                + " | id: " + this.getId()
                + " | type: " + this.getType()
                + " | un: " + this.getUsername()
                + " | pw: " + this.getPassword();
    }
}
