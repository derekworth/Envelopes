package misc;

import database.Model;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Created on Sep 29, 2013
 * @author Derek Worth
 */
public class Utilities {
    // used in getDatesByInterval method
    public static final int INTERVAL_TYPE_MONTHLY = 0;
    public static final int INTERVAL_TYPE_WEEKLY  = 1;
    public static final int INTERVAL_TYPE_DAILY   = 2;
    
    /**
     * Performs an MD5 hash function on given string and returns the hash
     * @param password String to be MD5 hashed
     * @return an MD5 hash of the given string
     */
    public static String getHash(String password) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte byteData[] = md.digest();
            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }
    
    /**
     * Rounds money amounts by cutting off all digits past two decimal places
     * @param amount The amount requiring money formating
     * @return true if successful, false otherwise
     */
    public static String roundAmount(double amount) {
        String amt = Double.toString(amount);
        boolean hasExpon = amt.contains("E");
        int expon;
        if(hasExpon) {
            // pulls out exponent
            expon = Integer.parseInt(amt.substring(amt.indexOf("E") + 1));
            // negative expressions get rounded down to zero
            if(expon<0) {
                return "0.00";
            }
            int exponIndex = amt.indexOf("E");
            int decimalIndex = amt.indexOf(".");
            String beforeDecimalA = amt.substring(0, decimalIndex);
            String beforeDecimalB = amt.substring(decimalIndex+1, decimalIndex+expon+1) + ".";
            String afterdecimal = amt.substring(decimalIndex+expon+1, exponIndex);
            amt = beforeDecimalA + beforeDecimalB + afterdecimal;
        }
        amt += "00";
        int decimalIndex = amt.indexOf(".");
        if(amt.charAt(decimalIndex+3)=='9') {
            if(amt.charAt(0)=='-') {
                return roundAmount(amount-0.001);
            }
            return roundAmount(amount+0.001);
        } else {
            String roundedAmt = amt.substring(0, decimalIndex+3);
            if(roundedAmt.equalsIgnoreCase("-0.00")) {
                return "0.00";
            }                
            return roundedAmt;
        }
    }
    
    public static String addCommasToAmount(double amount) {
        String amt = roundAmount(amount);
        // pull decimal
        String amtWithCommas = amt.substring(amt.length()-3, amt.length());
        
        for(int i = 1; i <= amt.length()-3; i++) {
            if(i%3==0 && i<amt.length()-3 && amt.charAt(amt.length()-4-i)!='-') { // add comma for every 3 digits
                amtWithCommas = "," + amt.charAt(amt.length()-3-i) + amtWithCommas;
            } else {
                amtWithCommas = amt.charAt(amt.length()-3-i) + amtWithCommas;
            }
        }
        return amtWithCommas;
    }

    public static String renameContainer(String oldName) {
        return "";
//        int left = -1, right;
//        // find index to left and right parenthesis
//        int i = oldName.length() - 1;
//        if(oldName.charAt(i)==')') { // right parenthesis found
//            right = i;
//            i--;
//            // find left parenthesis
//            for(; i>=0; i--){
//                if(oldName.charAt(i)=='(') {
//                    left = i;
//                    break;
//                }
//            }
//            if(left!=-1) { // left parenthesis found
//                try {
//                    String newName = oldName.substring(0, left) + "(" + (Integer.parseInt(oldName.substring(left+1, right))+1) + ")";
//                    if(Model.isContainer(newName, false)) {
//                        return renameContainer(newName);
//                    } else {
//                        return newName;
//                    }
//                } catch(Exception e) {}
//            }
//        }
//        return renameContainer(oldName + "(0)");
    }

    public static String renameUser(String oldName) {
        return "";
//        int left = -1, right;
//        // find index to left and right parenthesis
//        int i = oldName.length() - 1;
//        if(oldName.charAt(i)==')') { // right parenthesis found
//            right = i;
//            i--;
//            // find left parenthesis
//            for(; i>=0; i--){
//                if(oldName.charAt(i)=='(') {
//                    left = i;
//                    break;
//                }
//            }
//            if(left!=-1) { // left parenthesis found
//                try {
//                    String newName = oldName.substring(0, left) + "(" + (Integer.parseInt(oldName.substring(left+1, right))+1) + ")";
//                    if(Model.isUser(newName, false)) {
//                        return renameUser(newName);
//                    } else {
//                        return newName;
//                    }
//                } catch(Exception e) {}
//            }
//        }
//        return renameUser(oldName + "(0)");
    }
        
//    /**
//     * Evaluates the given input expression and outputs the calculated value
//     * @param expression String representation of a mathematical expression
//     * @return the evaluated solution to the mathematical expression
//     * @throws ScriptException thrown if expression is invalid
//     */
//    public static double evaluate(String expression) throws Exception {
//        // convert number(s) to expression(s)
//        String result = "";
//        String tmp = "";
//        for(int i = 0; i < expression.length(); i++) {
//            if((expression.charAt(i)>='0' && expression.charAt(i)<='9') || expression.charAt(i)=='.') {
//                // build number
//                tmp += expression.charAt(i);
//            } else {
//                // convert number to faction and add to result
//                result += toFraction(tmp) + expression.charAt(i);
//                tmp = "";
//            }
//        }
//        result += toFraction(tmp);
//        expression = result;
//        for(int i = 0; i < expression.length(); i++) {
//            // limits expression input to the following characters: 0123456789/*+-()
//            if ((expression.charAt(i)<'0' || expression.charAt(i)>'9')
//                    && expression.charAt(i)!='*'
//                    && expression.charAt(i)!='/'
//                    && expression.charAt(i)!='+'
//                    && expression.charAt(i)!='-'
//                    && expression.charAt(i)!='('
//                    && expression.charAt(i)!=')') {
//                throw new Exception();
//            }
//        }
//        ScriptEngineManager mgr = new ScriptEngineManager();
//        ScriptEngine engine = mgr.getEngineByName("JavaScript");
//        double solution = (double) engine.eval(expression);
//        solution = Double.parseDouble(roundAmount(solution));
//        return solution;
//    }
    
    public static double evaluate(String expression) throws Exception {
        double solution = Double.parseDouble(expression);
        solution = Double.parseDouble(roundAmount(solution));
        return solution;
    }
    
    public static String toFraction(String number) throws Exception {
        
        // limits expression input to the following characters: 0123456789/*+-()
        for(int i = 0; i < number.length(); i++) {
            if ((number.charAt(i)<'0' || number.charAt(i)>'9')
                    && number.charAt(i)!='.') {
                throw new Exception();
            }
        }
        
        // removes decimals
        String result = "";
        String whole = "";
        String numer = "";
        String denom = "1";
        char curr;
        boolean afterDec = false;
        boolean beforeDec = false;
        
        // adds decimal to end if missing
        if(!number.contains(".")) {
            number += ".0";
        }
        // extract whole number and fraction
        for(int i = 0; i<number.length(); i++) {
            curr = number.charAt(i);
            if(!afterDec) {
                if(!beforeDec) {
                    if(curr=='.') {
                        afterDec = true;
                        beforeDec = false;
                    } else if(curr>='0' && curr<='9') {
                        beforeDec = true;
                        whole += curr;
                    } else {
                        result += curr;
                    }
                } else {
                    if(curr=='.') {
                        afterDec = true;
                        beforeDec = false;
                    } else if(curr>='0' && curr<='9') {
                        whole += curr;
                    } else {
                        beforeDec = false;
                        result += whole + curr;
                        whole = "";
                    }
                }
            } else {
                if(curr>='0' && curr<='9') {
                    numer += curr;
                    denom += "0";
                } else {
                    afterDec = false;
                    if(whole.length()>0) {
                        if(numer.length()>0) {
                            result += "(" + whole + "+" + numer + "/" + denom + ")";
                        } else {
                            result += whole;
                        }
                    } else {
                        if(numer.length()>0) {
                            result += numer + "/" + denom;
                        }
                    }
                    whole = "";
                    numer = "";
                    denom = "1";
                    result += curr;
                }
            }
            
            // after all characters have been processed, convert to format: whole+numer/denom
            if(i+1==number.length()) {
                if(whole.length()>0) {
                    if(numer.length()>0) {
                        result += "(" + whole + "+" + numer + "/" + denom + ")";
                    } else {
                        result += "(" + whole + "+0/10)";
                    }
                } else {
                    if(numer.length()>0) {
                        result += "(" + numer + "/" + denom + ")";
                    }
                }
            }
        }
        return result;
    }
    
    public static String getDuration(long sec) {
        long remainder = sec;
        long y, d, h, m, s;
        String duration = "";
        
        y = remainder / 31536000;
        remainder %= 31536000;
        if(y>0)
            duration += y + " yr(s) : ";
        
        d = remainder / 86400;
        remainder %= 86400;
        if(d>0)
            duration += d + " day(s) : ";
        
        h = remainder / 3600;
        remainder %= 3600;
        if(h>0)
            duration += h + " hr(s) : ";
        
        m = remainder / 60;
        remainder %= 60;
        if(m>0)
            duration += m + " min(s) : ";
        
        s = remainder / 1;
        if(s>0) 
            duration += s + " sec(s)";
        
        if(duration.endsWith(" : ")) 
            duration = duration.substring(0, duration.length()-3);
        
        return duration;
    }
    
    /**
     * Provides the current getTimestamp
     * @return getTimestamp in format: YYYY-MM-DD hh:mm:ss
     */
    public static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }
    
    public static void printTimestamp() {
        System.out.println("Current time: " + getTimestamp());
    }
    
    public static void printLinkedList(LinkedList<Object> objs) {
        for(Object o : objs) {
            System.out.println(o);
        }
    }
    
    public static String getDatestamp(int fromToday) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, fromToday);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }
    
    public static String getNewDate(String dateString, int delta) {
        
        if(!isDate(dateString)) {
            return "";
        }
        
        int yr  = Integer.parseInt(dateString.substring(0, 4));
        int mth = Integer.parseInt(dateString.substring(5, 7));
        int day = Integer.parseInt(dateString.substring(8, 10));
        
        if(delta==0) {
            return dateString;
        } else if (delta < 0) {
            day += delta;
            while(day<1) {
                mth--; // move to next prior month
                if(mth<1) {
                    yr--; // move to next prior year
                    mth = 12;
                }
                day = daysInMonth(yr, mth) + day;
            }
        } else {
            day += delta;
            while(day>daysInMonth(yr, mth)) {
                day -= daysInMonth(yr, mth);
                mth++; // move to next month
                if(mth>12) {
                    yr++; // move to next year
                    mth = 1;
                }
            }
        }
        
        String newDate = yr + "-";
        if(mth<10) {
            newDate += "0" + mth + "-";
        } else {
            newDate += mth + "-";
        }
        if(day<10) {
            newDate += "0" + day;
        } else {
            newDate += day;
        }
        return newDate;
    }
    
    public static int daysInMonth(int yr, int mth) {
        // gets max days in month
        Calendar mycal = new GregorianCalendar(yr, mth-1 , 1);
        // Get the number of days in that month
        return mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    public static String getAddress(String addr) {
        int a = addr.indexOf("<");
        int b = addr.indexOf(">");
        if(a==-1 || b == -1)
            return addr;
        return addr.substring(a+1, b);
    }
    
    public static String validateDate(String date) {
        String newDate = "";
        // remove non-digit characters from date
        for(int i = 0; i < date.length(); i++) {
            if(date.charAt(i)>='0' && date.charAt(i)<='9') {
                newDate += date.charAt(i);
            }
        }
        // add dashes to date
        if(newDate.length()==8) {
            newDate = newDate.substring(0, 4) + "-" + newDate.substring(4, 6) + "-" + newDate.substring(6, 8);
            if(Utilities.isDate(newDate)) {
                return newDate;
            }
        }
        return "";
    }
    
    /**
     * Produces an array of dates, starting with today, for specified interval and quantity
     * @param intervalType INTERVAL_TYPE_MONTLY = 0, INTERVAL_TYPE_WEEKLY = 1, INTERVAL_TYPE_DAILY = 2; monthly is every 30 days, weekly 7 days, and daily self-explanatory
     * @param intervalCount number dates requested
     * @return String array containing specified number of dates (format 'yyyy-mm-dd') at given interval
     */
    public static String[] getDatesByInterval(int intervalType, int intervalCount) {
        String date = Utilities.getDatestamp(0); // sets initial date for today
        String [] dates = new String[intervalCount];
        
        for(int i = 1; i<=intervalCount; i++) {
            dates[i-1] = date;
            if(intervalType==INTERVAL_TYPE_MONTHLY) {       // monthly
                date = Utilities.getNewDate(date, -30);
            } else if(intervalType==INTERVAL_TYPE_WEEKLY) { // weekly
                date = Utilities.getNewDate(date, -7);
            } else {                                        // daily
                date = Utilities.getNewDate(date, -1);
            }
        }
        
        return dates;
    }
    
    /**
     * Checks that string representation of date is in the format: YYYY-MM-DD
     * @param dateString String representation of a date
     * @return true if successful, false otherwise
     */
    public static boolean isDate(String dateString) {
        // checks for correct length
        if (dateString.length()!=10) {
            return false;
        }
        // checks that date values are numerical
        int yr, mth, day;
        try{
            yr  = Integer.parseInt(dateString.substring(0, 4));
            mth = Integer.parseInt(dateString.substring(5, 7));
            day = Integer.parseInt(dateString.substring(8, 10));
        } catch(NumberFormatException e) {
            return false;
        }
        
        // gets max days in month
        Calendar mycal = new GregorianCalendar(yr, mth-1 , 1);
        // Get the number of days in that month
        int daysInMonth = mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        if (    yr<1884 || yr>2189 ||           // checks year
                mth<1 || mth>12 ||              // checks month
                day<1 || day>daysInMonth ||     // checks day
                dateString.charAt(4)!='-' ||    // checks first dash
                dateString.charAt(7)!='-'       // checks second dash
                ){
            return false;
        }
        return true;
    }
    
    /**
     * Checks that string representation of getTimestamp is in the format: 
     * YYYY-MM-DD hh:mm:ss
     * @param timestampString String representation of a getTimestamp
     * @return true if successful, false otherwise
     */
    public static boolean isTimestamp(String timestampString) {
        // checks for correct length
        if (timestampString.length()!=19) {
            return false;
        } else if (!isDate(timestampString.substring(0,10))) { // year/month/day
            return false;
        }
        // checks the time
        int hr, min, sec;
        try{
            hr  = Integer.parseInt(timestampString.substring(11, 13));
            min = Integer.parseInt(timestampString.substring(14, 16));
            sec = Integer.parseInt(timestampString.substring(17, 19));
        } catch(NumberFormatException e) {
            return false;
        }
        
        if (hr<0  || hr>23 ||                  // checks hour
           min<0 || min>59 ||                 // checks minute
           sec<0 || sec>59 ||                 // checks second
           timestampString.charAt(13)!=':' || // checks first colon
           timestampString.charAt(16)!=':'){  // checks second colon
            return false;
        }
        return true;
    }
    
    // validates name begins with a letter and contains only numbers,
    // letters, or the following characters: '-'  '('  ')'
    public static boolean isValidContainerName(String name) {
        name = name.toLowerCase();
        // checks if first character is a letter
        if(name.isEmpty() || name.charAt(0)<'a' || name.charAt(0)>'z') {
            return false;
        }

        // check each letter
        for(int i = 0; i< name.length(); i++) {
            char c = name.charAt(i);
            if( c!='-' && 
                c!='(' && 
                c!=')' &&
              !(c>='a' && c<='z') &&
              !(c>='0' && c<='9')) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isValidDescription(String desc) {
        if(desc.length()>100) {
            return false;
        }
        // check each letter
        // valid characters include A-Z a-z 0-9 - ( ) @ # $ & '
        for(int i = 0; i< desc.length(); i++) {
            char c = desc.charAt(i);
            if( c=='-' || 
                c=='<' || 
                c=='>' || 
                c=='(' || 
                c==')' || 
                c=='@' || 
                c=='#' || 
                c=='$' || 
                c=='&' || 
                c=='.' || 
                c==' ' || 
                c=='\'' ||  
                c=='/' || 
                c=='\t' || 
                (c>='a' && c<='z') ||
                (c>='A' && c<='Z') ||
                (c>='0' && c<='9')) {
                // do nothing
            } else {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isValidAmount(String amt) {
        try {
            Double.parseDouble(amt);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isValidUsername(String un) {
        un = un.toLowerCase();
        if(un.length()>0) { // must contain at least 1 letter
            if((un.charAt(0)>='a' && un.charAt(0)<='z')) { // first character must be a letter
                for(int i = 1; i < un.length(); i++) {
                    if((un.charAt(i)<'a' || un.charAt(i)>'z') && (un.charAt(i)<'0' || un.charAt(i)>'9')) { // must contain letters and numbers only
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    public static boolean isValidPassword(String pw) {
        if(pw.length()>3) { // must contain at least 4 characters
            for(int i = 1; i < pw.length(); i++) {
                if(pw.charAt(i)==' ' || pw.charAt(i)=='\n' || pw.charAt(i)=='\t' || pw.charAt(i)=='\r') { // must not contain white spaces 
                    return false;
                }
//                if((pw.charAt(i)<'a' || pw.charAt(i)>'z') && (pw.charAt(i)<'0' || pw.charAt(i)>'9')) { // must contain letters, numbers, and/or the following special characters 
//                    return false;
//                }
            }
            return true;
        }
        return false;
    }
    
    public static String shortenString(String text, int shortenTo) {
        if(text.length()>shortenTo) {
            text = text.substring(0,shortenTo) + "...";
        }
        return text;
    }
    
    public static String doubleApostrophes(String txt) {
        String tmp = "";
        for(int i = 0; i < txt.length(); i++) {
            if(txt.charAt(i)=='\'') {
                tmp += "'";
            }
            tmp += txt.charAt(i);
        }
        return tmp;
    }
    
    public static String removeDoubleApostrophes(String txt) {
        while(txt.contains("''")) {
            txt = txt.replace("''", "'");
        }
        return txt;
    }
    
    public static String removeCommas(String txt) {
        while(txt.contains(",")) {
            txt = txt.replace(",", "");
        }
        return txt;
    }
    
    public static String trimInvalidCharacters(String desc) {
        String tmp = "";
        for(int i = 0; i< desc.length(); i++) {
            char c = desc.charAt(i);
            if( c=='-' || 
                c=='<' || 
                c=='>' || 
                c=='(' || 
                c==')' || 
                c=='.' || 
                c=='*' || 
                c=='@' || 
                c=='#' || 
                c=='$' || 
                c=='&' || 
                c==' ' || 
                c=='\'' ||  
                c=='/' || 
                c=='\t' || 
                (c>='a' && c<='z') ||
                (c>='A' && c<='Z') ||
                (c>='0' && c<='9')) {
                tmp += c;
            }
        }
        return tmp;
    }
    
    /**
     * Provides an easy and intuitive way to check the order between two strings;
     * checks the strings are in alphabetical order as specified.
     * @param before the first string that is tested to be alphabetically before
     * 'after' string
     * @param after the second string that is tested to be alphabetically after
     * 'before' string
     * @return true if before and after are ordered alphabetically, false otherwise
     */
    public static boolean isOrdered(String before, String after) {
        return before.compareToIgnoreCase(after) <= 0;
    }
}
