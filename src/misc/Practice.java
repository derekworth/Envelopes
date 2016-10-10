package misc;

import database.Category;

public class Practice {
    
    public static void main(String args[]) {
        Category c1, c2;
        // Category(String created, String modified, boolean enabled, int id, String name, double amt)
        String ts = Utilities.getTimestamp();
        c1 = new Category(ts, ts, true, 4, "frequent", 32.34);
        c2 = null;
        if((c1==null || c2==null) || c1.getId()==c2.getId())
            System.out.println("Do they equal: yes");
        else
            System.out.println("Do they equal: no");
    }
}
