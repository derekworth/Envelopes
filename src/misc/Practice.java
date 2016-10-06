package misc;

public class Practice {
    
    public static void main(String args[]) {
        try {
            double ans = Utilities.evaluate("45+77");
            System.out.println("Worked: " + ans);
        } catch (Exception ex) {
            System.out.println("didn't work!");
        }
    }
}
