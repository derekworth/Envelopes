package server.remote;

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
import model.ModelController;

/**
 * Created on Aug 16, 2013
 * @author Derek Worth
 */
public class GmailCommunicator {
    
    private static final String[] TEXT_EXTENSIONS = {
        "cingularme.com", "email.uscc.net", "message.alltel.com", 
        "messaging.nextel.com", "messaging.sprintpcs.com", "mms.att.net", 
        "mms.uscc.net", "pm.sprint.com", "tmomail.net",  "txt.att.net", 
        "vmobl.com", "vtext.com", "vzwpix.com"};
    private final ModelController mc;
    
    public GmailCommunicator(ModelController mc) {
        this.mc = mc;
    }
    
    public void send(String recipient, String msg) {
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
                transport.connect("smtp.gmail.com", 465, mc.getGmailUsername(), mc.getGmailPassword());
                
                if (isFromTextMsg(recipient)){
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
                // End mail session
                transport.close();
            } catch (NoSuchProviderException ex) { /* DO NOTHING */
            } catch (MessagingException ex) { /* DO NOTHING */ }
        }
    }
    
    public boolean isValidCredentials(String username, String password) {
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
        } catch (MessagingException ex) {
            return false;
        }
    }
    
    public boolean receive() {
        int count;
        try {
            // Get a session.  Use a blank Properties object.
            Session session = Session.getInstance(new Properties());
            
            // Connect to store
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", mc.getGmailUsername() + "@gmail.com", mc.getGmailPassword());
            // Get "INBOX"
            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_WRITE);
            count = inbox.getMessageCount();
            // Process each email
            for(int i = 1; i <= count; i++) {
                // Get an email by its sequence number
                Message m = inbox.getMessage(i);
                // Get sender
                String addr = Utilities.stripHeaderFromAddress(m.getFrom()[0].toString());
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(m.getReceivedDate());
                int index = mc.getEmailIndex(addr);
                // Get commands (content/body of message)
                String msg = formatMessage(processPart(m));
                // process commands
                if (mc.isEmailAuthenticated(addr)) {
                    Commands commands = new Commands(mc, mc.getEmailUsername(index), date, msg);
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
                        int attemptCount = mc.getEmailAttempt(index);
                        if (attemptCount >= ModelController.MAX_ATTEMPT) {
                            send(addr, "You are permanently locked out. See system administrator for access.");
                        } else {
                            send(addr, "Invalid credentials. You have " + (ModelController.MAX_ATTEMPT - attemptCount) + " attempts remaining. Please send: <un> <pw>");
                        }
                    }
                }
                
                // delete messgage from sender
                m.setFlag(Flags.Flag.DELETED,true);
            }
            
            inbox.close(true);
            store.close();
            return count>0;
        } catch (MessagingException ex) {
            return false;
        }
    }
    
    public boolean authorize(String addr, String un, String pw) {
        // returns true if already authorized (no need to authorize again)
        if (mc.isEmailAuthenticated(addr)) {
            return true;
        }
        // adds email address if not already added (starts with 1 attempt)
        boolean emailNewlyAdded = mc.addEmail(addr);
        // authenticates credentials
        if(!mc.isUserAuthenticated(un, pw)) { // bad username and/or password
            if(!emailNewlyAdded) {
                // increment only if not newly added (newly added already starts with 1 attempt)
                mc.incrementEmailAttempt(addr);
            }
            return false;
        }
        
        // permanently lock out address after 5 attempts
        int index = mc.getEmailIndex(addr);
        if (mc.getEmailAttempt(index)>=ModelController.MAX_ATTEMPT) {
            return false;
        }
        
        // authentication passed! so let's bind email to user
        mc.setEmailUser(addr, un); // this method call sets attempt count to 0
        return true;
    }
    
    private String processMultipart(Multipart mp) throws MessagingException {
        String msg = "";
        for (int i = 0; i < mp.getCount(); i++) {
            msg += processPart(mp.getBodyPart(i));
        }
        return msg;
    }
    
    private String processPart(Part p) {
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
    private boolean isFromTextMsg(String addr){
        
        boolean fromText = false;
        for (String txtExt : TEXT_EXTENSIONS) {
            if (txtExt.equalsIgnoreCase(addr.substring(addr.indexOf('@') + 1))) {
                fromText = true;
            }
        }
        return fromText;
    }
    
    public String formatMessage(String msg){
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
