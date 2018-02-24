package misc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
    
    public static String amountToStringSimple(String strAmt) {
        String tmp = "";
        for(int i = 0; i < strAmt.length(); i++) {
            if(strAmt.charAt(i)!='('
                    && strAmt.charAt(i)!=')'
                    && strAmt.charAt(i)!=',') {
                tmp += strAmt.charAt(i);
            }
        }
        return tmp;
    }
    
    /**
     * Converts an integer amount (represented in cents) into a string with
     * decimal (e.g. -234563 converts to "-2345.63")
     * @param intAmt integer amount in cents/pennies
     * @return decimal string
     */
    public static String amountToStringSimple(int intAmt) {
        boolean neg = intAmt < 0;
        // convert to string
        String dec, amt = Integer.toString(intAmt);
        // temporarily remove negative
        if(neg) {
            amt = amt.substring(1);
        }
        
        // add decimal
        switch (amt.length()) {
            case 1:
                // pad with 0.0X
                dec = "0" + amt;
                amt = "0";
                break;
            case 2:
                // pad with 0.XX
                dec = amt;
                amt = "0";
                break;
            default:
                // no padding necessary
                dec = amt.substring(amt.length()-2);
                amt = amt.substring(0, amt.length()-2);
                break;
        }
        String amtWithCommas = "";
        
        for(int i = 1; i <= amt.length(); i++) {
            amtWithCommas = amt.charAt(amt.length()-i) + amtWithCommas;
        }
        if(neg)
            return "-" + amtWithCommas + "." + dec;
        else
            return amtWithCommas + "." + dec;
    }
    
    /**
     * Converts an integer amount (represented in cents) into a string with
     * decimal and commas added (e.g. -234563 converts to "(-2,345.63)")
     * @param intAmt integer amount in cents/pennies
     * @return decimal string
     */
    public static String amountToString(int intAmt) {
        boolean neg = intAmt < 0;
        // convert to string
        String dec, amt = Integer.toString(intAmt);
        // temporarily remove negative
        if(neg) {
            amt = amt.substring(1);
        }
        
        // add decimal
        switch (amt.length()) {
            case 1:
                // pad with 0.0X
                dec = "0" + amt;
                amt = "0";
                break;
            case 2:
                // pad with 0.XX
                dec = amt;
                amt = "0";
                break;
            default:
                // no padding necessary
                dec = amt.substring(amt.length()-2);
                amt = amt.substring(0, amt.length()-2);
                break;
        }
        String amtWithCommas = "";
        
        for(int i = 1; i <= amt.length(); i++) {
            if(i%3==0 && i<amt.length()) { // add comma for every 3 digits
                amtWithCommas = "," + amt.charAt(amt.length()-i) + amtWithCommas;
            } else {
                amtWithCommas = amt.charAt(amt.length()-i) + amtWithCommas;
            }
        }
        if(neg)
            return "(-" + amtWithCommas + "." + dec + ")";
        else
            return amtWithCommas + "." + dec;
    }
    
    public static int amountToInteger(String strAmt) {
        // remove invalid characters
        String tmp = "";
        for(int i = 0; i < strAmt.length(); i++) {
            char c = strAmt.charAt(i);
            if((c>='0' && c<='9') || c=='.' || c=='-') {
                tmp += c;
            }
        }
        strAmt = tmp;
        // get sign
        int sign = 1;
        if(strAmt.charAt(0)=='-') {
            sign = -1;
        }
        // remove minus sign and/or extra decimal points
        tmp = "";
        boolean decFound = false;
        for(int i = 0; i < strAmt.length(); i++) {
            char c = strAmt.charAt(i);
            if(c>='0' && c<='9') {
                tmp += c;
            } else if(c=='.' && !decFound) {
                decFound = true;
                tmp += c;
            }
        }
        // find decimal
        int decIndex = tmp.indexOf('.');
        if(decIndex==-1) { // append to end if decimal not found
            tmp += ".";
            decIndex = tmp.indexOf('.');
        }
        // pads string with trailing zeros as necessary
        if(decIndex==-1) {
            // no padding necessary, no decimal found
        } else if(decIndex==tmp.length()-1) { // decimal at end of string
            tmp += "00";
        } else if(decIndex==tmp.length()-2) { // decimal followed by 1 digit
            tmp += "0";
        } else if(decIndex<tmp.length()-3) {  // decimal followed by 3 or more digits
            tmp = tmp.substring(0, decIndex+3);
        }
        // remove decimal
        tmp = tmp.replace(".", "");
        if(tmp.length()==0) {
            return 0;
        }
        return sign * Integer.parseInt(tmp);
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
     * Provides the current timestamp
     * @return timestamp in format: YYYY-MM-DD hh:mm:ss
     */
    public static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }
    
    /**
     * Provides the current timestamp (date only)
     * @param fromToday offset from today (e.g. -1 = yesterday, 0 = today,
     * 2 = day after tomorrow)
     * @return timestamp in format: YYYY-MM-DD
     */
    public static String getDatestamp(int fromToday) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, fromToday);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }
    
    /**
     * Calculates a new date from the date specified, offset by given delta
     * @param dateString original date
     * @param delta number of days between original date and new date (e.g. -3
     * results in 3 days prior to original date)
     * @return string representation of new date, formatted YYYY-MM-DD
     */
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
    
    public static String getShortDesc(String fullDesc) {
        int i = fullDesc.indexOf(")");
        if(i>=0) {
            return fullDesc.substring(i+1).trim();
        } else {
            return fullDesc;
        }
    }
    
    public static int daysInMonth(int yr, int mth) {
        // gets max days in month
        Calendar mycal = new GregorianCalendar(yr, mth-1 , 1);
        // Get the number of days in that month
        return mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Extracts email address from email address header
     * @param fullAddr full address including header
     * @return address after header has been stripped
     */
    public static String stripHeaderFromAddress(String fullAddr) {
        int a = fullAddr.indexOf("<");
        int b = fullAddr.indexOf(">");
        if(a==-1 || b == -1)
            return fullAddr;
        return fullAddr.substring(a+1, b);
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
            switch (intervalType) {
                case INTERVAL_TYPE_MONTHLY:
                    // monthly
                    date = Utilities.getNewDate(date, -30);
                    break;
                case INTERVAL_TYPE_WEEKLY:
                    // weekly
                    date = Utilities.getNewDate(date, -7);
                    break;
                default:
                    // daily
                    date = Utilities.getNewDate(date, -1);
                    break;
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
        
        return !(yr<1884 || yr>2189 ||          // checks year
                mth<1 || mth>12 ||              // checks month
                day<1 || day>daysInMonth ||     // checks day
                dateString.charAt(4)!='-' ||    // checks first dash
                dateString.charAt(7)!='-'       // checks second dash
);
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
        
        return !(hr<0  || hr>23 ||                  // checks hour
                min<0 || min>59 ||                 // checks minute
                sec<0 || sec>59 ||                 // checks second
                timestampString.charAt(13)!=':' || // checks first colon
                timestampString.charAt(16)!=':');
    }
    
    public static boolean isFirstCharacterALetter(String text) {
        // check for null/empty text
        if(text==null || text.length()==0)
            return false;
        // get first character
        char firstChar = text.charAt(0);
        // check character for letter and return false if not a letter
        return (firstChar>='a' && firstChar <='z') || (firstChar>='A' && firstChar <='Z');
    }
    
    // validates name contains only numbers, letters, or the following characters: '-'  '('  ')'
    public static boolean isValidContainerName(String name) {
        if(name==null || name.length()==0 || !isFirstCharacterALetter(name)) {
            return false;
        }
        name = name.toLowerCase();
        // check each letter
        for(int i = 0; i< name.length(); i++) {
            char c = name.charAt(i);
            if( c=='-' || 
                c=='(' || 
                c==')' || 
                (c>='a' && c<='z') ||
                (c>='0' && c<='9')) {
                // do nothing
            } else {
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
        } catch (NumberFormatException ex) {
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
    
    /**
     * Removes all characters except letters, numbers, and dashes, then removes
     * all characters up to the first letter (first char must be a letter), and
     * finally, lowercases all letters
     * @param name Name to be formatted
     * @return properly formatted container name
     */
    public static String cleanContainerName(String name) {
        name = name.toLowerCase();
        String tmp = "";
        boolean firstCharIsLetter = false;
        for(int i = 0; i< name.length(); i++) {
            char c = name.charAt(i);
            if( (c>='a' && c<='z') ||                  // check character for letter
                (c>='A' && c<='Z') ) {
                tmp += c;
                if(!firstCharIsLetter) {
                    firstCharIsLetter = true;
                }
            } else if( c=='-' || (c>='0' && c<='9')) { // check charater for a valid non-letter
                if(firstCharIsLetter) {
                    tmp += c;
                }
            }
        }
        return tmp;
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

    public static String capitalizeFirstCharacter(String desc) {
        // uppercase the first letter of the description
        if(desc.length()>0) { // 1 or more characters in desc
            if(desc.charAt(0)>='a' && desc.charAt(0)<='z') { // first character in desc is lowercase letter
                if(desc.length()==1) { // only one character in desc
                    desc = desc.toUpperCase();
                } else { // more than one character in desc
                    desc = (char)(desc.charAt(0)-32) + desc.substring(1);
                }
            }
        }
        return desc;
    }
    
    public static String cleanTransactionDesc(String desc) {
        String tmp = "";
        for(int i = 0; i< desc.length(); i++) {
            char c = desc.charAt(i);
            if( c=='-' || 
                c=='#' || 
                c=='$' || 
                c=='&' || 
                c==' ' || 
                c=='\'' ||  
                c=='(' ||  
                c==')' ||  
                c=='*' ||  
                c=='<' ||  
                c=='>' ||  
                (c>='a' && c<='z') ||
                (c>='A' && c<='Z') ||
                (c>='0' && c<='9')) {
                tmp += c;
            }
        }
        desc = tmp;
        // remove all double apostrophes
        while (desc.contains("''")) {
            desc = desc.replace("''", "'");
        }
        // add convert single to double apostrophes
        tmp = "";
        for(int i = 0; i < desc.length(); i++) {
            if(desc.charAt(i)=='\'') {
                tmp += "'";
            }
            tmp += desc.charAt(i);
        }
        // set transaction desc if empty
        if(tmp.length() == 0) {
            tmp = "<no description specified>";
        }
        return tmp;
    }
    
    /**
     * Username must consist of lowercase letters and numbers, with a letter as
     * the first character
     * @param un user specified username before formating 
     * @return formated username with all alphanumeric chars removed
     */
    public static String cleanUsername(String un) {
        un = un.toLowerCase();
        String tmp = "";
        boolean firstCharIsLetter = false;
        for(int i = 0; i< un.length(); i++) {
            char c = un.charAt(i);
            // check character for letter
            if( (c>='a' && c<='z') ||
                (c>='A' && c<='Z') ) {
                tmp += c;
                if(!firstCharIsLetter) {
                    firstCharIsLetter = true;
                }
            } else
            // check charater for a valid non-letter
            if((c>='0' && c<='9')) {
                if(firstCharIsLetter) {
                    tmp += c;
                }
            }
        }
        return tmp;
    }
    
    public static boolean isValidEmailAddress(String addr) {
        return addr.contains("@")
                && !(
                    addr.contains(" ")  || 
                    addr.contains("\'") ||
                    addr.contains(";")  ||
                    addr.contains("\t") ||
                    addr.contains("\n") ||
                    addr.contains("\r")
                );
    }
}
