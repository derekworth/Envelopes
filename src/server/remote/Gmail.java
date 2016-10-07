package server.remote;

import database.DBMS;
import database.Email;
import database.User;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import misc.Utilities;

/**
 * Created on Aug 16, 2013
 * @author Derek Worth
 */
public class Gmail {
    
    private static final String[] TEXT_EXTENSIONS = {
        "cingularme.com", "email.uscc.net", "message.alltel.com", 
        "messaging.nextel.com", "messaging.sprintpcs.com", "mms.att.net", 
        "mms.uscc.net", "pm.sprint.com", "tmomail.net",  "txt.att.net", 
        "vmobl.com", "vtext.com", "vzwpix.com"};
    
    public static void send(String recipient, String msg) {
        // Processes 'send' only if message is 1 or more characters long
        if (msg.length() > 0) {
            try {
                // Set properties
                Properties props = new Properties();
                props.setProperty("mail.transport.protocol", "smtps");
                props.setProperty("mail.smtps.host", "smtp.gmail.com");
                props.setProperty("mail.smtps.auth", "true");
                
                // Start mail session
                Session mailSession = Session.getDefaultInstance(props);
                Transport transport = mailSession.getTransport();
                User gmail = DBMS.getGmail();
                transport.connect("smtp.gmail.com", 465, gmail.getUsername(), gmail.getPassword());
                
                if (isFromText(recipient)){
                    // Extracts words from message
                    String[] lines = msg.split("\n");
                    
                    // Groups words into SMS sized blocks
                    LinkedList<String> messages = new LinkedList();
                    int msgCounter = 0;
                    int lineCounter = 0;
                    String message = "";
                    // adds all but last message to messages list
                    for(int i = 0; i < lines.length; i++) {
                        if(lines[i].length() < 150) { // skips lines longer than message can hold
                            if (lines[i].length() + message.length() + lineCounter < 150) {
                                message += lines[i] + "\n";
                                lineCounter++;
                            } else {
                                messages.add(message); // add message to end of list
                                msgCounter++;
                                message = ""; // reset for new message
                                i--; // allows re-check of last word
                            }
                        }
                    }
                    
                    // adds last message to messages list
                    messages.add(message);
                    msgCounter++;
                    // Appends count headers to top of messages
                    if (msgCounter > 1) {
                        for(int i = 1; i <= msgCounter; i++) {
                            String tmp = messages.pop(); // removes from front of list
                            tmp = i + " of " + msgCounter + "\n" + tmp;
                            messages.add(tmp); // returns to end of list
                        }
                    }
                    
                    // Send messages
                    for(int i = 0; i < msgCounter; i++) {
                        // set message properties
                        MimeMessage mimeMessage = new MimeMessage(mailSession);
                        mimeMessage.setContent(messages.pop(), "text/plain");
                        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                        // send message
                        transport.sendMessage(mimeMessage, mimeMessage.getRecipients(Message.RecipientType.TO));
                    }
                } else {
                    // set message properties
                    MimeMessage mimeMessage = new MimeMessage(mailSession);
                    mimeMessage.setSubject("Envelopes");
                    mimeMessage.setContent(msg, "text/plain");
                    mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                    // send message
                    transport.sendMessage(mimeMessage, mimeMessage.getRecipients(Message.RecipientType.TO));
                }
                // log response to recipient
                msg = formatMessage(msg);
                int msgLength = msg.length();
                // limit message length to 500 characters
                if(msg.length()>500) {
                    msgLength = 500;
                }
                // End mail session
                transport.close();
            } catch (NoSuchProviderException ex) {
            } catch (MessagingException ex) {
            }
        }
    }
    
    public static boolean isValidCredentials(String username, String password) {
        try {
        // Set properties
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtps");
        props.setProperty("mail.smtps.host", "smtp.gmail.com");
        props.setProperty("mail.smtps.auth", "true");
        
        // Start mail session
        Session mailSession = Session.getDefaultInstance(props);
        Transport transport = mailSession.getTransport();
        transport.connect("smtp.gmail.com", 465, username, password);
        
        // End mail session
        transport.close();
        return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean receive() {
        int count;
        try {
            // Get a session.  Use a blank Properties object.
            Session session = Session.getInstance(new Properties());
            
            // Connect to store
            Store store = session.getStore("imaps");
            User gmail = DBMS.getGmail();
            store.connect("imap.gmail.com", gmail.getUsername() + "@gmail.com", gmail.getPassword());
            // Get "INBOX"
            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_WRITE);
            count = inbox.getMessageCount();
            // Process each email
            for(int i = 1; i <= count; i++) {
                // Get an email by its sequence number
                Message m = inbox.getMessage(i);
                // Get sender
                String addr = Utilities.getAddress(m.getFrom()[0].toString());
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(m.getReceivedDate());
                // Get commands (content/body of message)
                String msg = formatMessage(processPart(m));
                int msgLength = msg.length();
                if(msg.length()>500) {
                    msgLength = 500;
                }
                // process commands
                if (isAuthorized(addr)) {
                    Commands commands = new Commands(DBMS.getEmail(addr).getUser(), date, msg);
                    send(addr, commands.executeCommands());
                } else {
                    String[] tokens = msg.split(" ");
                    String un, pw;
                    if(tokens.length==2) {
                        un = tokens[0];
                        pw = tokens[1];
                    } else {
                        un = "";
                        pw = "";
                    }
                    
                    if(authorize(addr, un, pw)) {
                        send(addr, "Congratulations, you have been authenticated.");
                    } else {
                        int attemptCount = DBMS.getEmail(addr).getAttempt();
                        if (attemptCount >= 5) {
                            send(addr, "You are permanently locked out. See system administrator for access.");
                        } else {
                            send(addr, "Invalid credentials. You have " + (5 - attemptCount) + " attempts remaining. Please send: <un> <pw>");
                        }
                    }
                }
                
                // delete messgage from sender
                m.setFlag(Flags.Flag.DELETED,true);
            }
            
            inbox.close(true);
            store.close();
            if(count>0) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Determines whether an email address has been authenticated or not
     * @param address Address of email to check
     * @return true if email is authorized, false otherwise
     */
    public static boolean isAuthorized(String address) {
        Email email = DBMS.getEmail(address);
        if (email!=null) { // email not in the system
            User user = email.getUser();
            if (user !=null && user.isInDatabase() && user.isEnabled() && email.getAttempt()==0) { // email has been authorized
                return true;
            }
        }
        return false;
    }
    
    public static boolean authorize(String address, String username, String password) {
        // validates address
        if(address==null) {
            return false;
        }
        Email email = DBMS.getEmail(address);
        
        // validates username and password
        if(username==null || password==null || username.length()==0 || password.length()==0) {
            // checks for email in database
            if (email==null) { // email address not yet in database
                DBMS.newEmail(address);
            } else { // email already in database
                email.setAttempt(email.getAttempt()+1); // increase attempt count
            }
            return false;
        }
        User user = DBMS.getUser(username, true);
        password = Utilities.getHash(password);
        
        // returns true if already authorized (no need to authorize again)
        if (Gmail.isAuthorized(address)) {
            return true;
        }
        
        // checks for email in database
        if (email==null) { // email address not yet in database
            email = DBMS.newEmail(address);
        } else { // email already in database
            email.setAttempt(email.getAttempt()+1); // increase attempt count
        }
        
        // permanently lock out address after 5 attempts
        if (email.getAttempt()>5) {
            return false;
        }
        
        if (user==null) { // no user by that name
            return false;
        } else { // user exists, now lets check the password
            if (user.getPassword().equals(password)) { // checks for correct pw
                email.setUser(user);
                email.setAttempt(0);
                return true;
            } else {
                return false;
            }
        }
    }
    
    private static String processMultipart(Multipart mp) throws MessagingException {
        String msg = "";
        for (int i = 0; i < mp.getCount(); i++) {
            msg += processPart(mp.getBodyPart(i));
        }
        return msg;
    }
    
    private static String processPart(Part p) {
        try {
            String contentType = p.getContentType();
            if (contentType.toLowerCase().startsWith("text/plain")){
                return p.getContent().toString();
            } else if (contentType.toLowerCase().startsWith("multipart/")) {
                return processMultipart((Multipart)  p.getContent() );
            }
        }
        catch (MessagingException | IOException ex) {
        }
        return "";
    }
    
    /**
     * Determines if an email address is tied to a cellular provider (for SMS/text messaging)
     * @param addr address to check
     * @return returns true if address is a text address and false if address is a email address
     */
    private static boolean isFromText(String addr){
        
        boolean fromText = false;
        for(int i = 0; i < TEXT_EXTENSIONS.length; i++) {
            if (TEXT_EXTENSIONS[i].equalsIgnoreCase(addr.substring(addr.indexOf('@') + 1))) {
                fromText = true;
            }
        }
        return fromText;
    }
    
    public static String formatMessage(String msg){
        // remove space at beginning or end
        msg = msg.trim();
        // removes carriage returns from message
        String tmp = "";
        char prevLetter = 0;
        for (int i = 0 ; i < msg.length(); i++){
            char letter = msg.charAt(i);
            // 13 = carriage return, 10 = new line
            if(letter == 13) {
                // do nothing (get rid of all carriage returns
            } else if(letter == 10 && prevLetter != ',') {
                tmp += ',';
            } else if(letter == 10) {
                // do nothing
            } else {
                tmp += letter;
                prevLetter = letter;
            } 
        }
        return tmp;
    }
}
