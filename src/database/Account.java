package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public final class Account extends Container {

    public Account(String created, String modified, int id, boolean enabled, String name, double amt) {
        super(created, modified, id, enabled, name, amt);
    }
    
}
