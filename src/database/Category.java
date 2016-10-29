package database;

/**
 * Created on Aug 2, 2013
 * @author Derek Worth
 */
public final class Category extends Container {

    public Category(String created, String modified, int id, boolean enabled, String name) {
        super(created, modified, id, enabled, name, 0);
    }
    
}
