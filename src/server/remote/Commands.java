package server.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import misc.Utilities;
import model.ModelController;

/**
 * Created on Aug 31, 2013
 * @author Derek Worth
 */
public class Commands {
    
    public static final char ACCOUNT       = 'A';
    public static final char ACCOUNTS      = 'B';
    public static final char CATEGORY      = 'C';
    public static final char CATEGORIES    = 'D';
    public static final char CHANGE        = 'E';
    public static final char ENVELOPE      = 'F';
    public static final char ENVELOPES     = 'G';
    public static final char HELP          = 'H';
    public static final char HISTORY       = 'I';
    public static final char NEW           = 'J';
    public static final char PASSWORD      = 'K';
    public static final char REMOVE        = 'L';
    public static final char RENAME        = 'M';
    public static final char USER          = 'N';
    public static final char USERS         = 'O';
    public static final char ACCT          = 'P';
    public static final char CAT           = 'Q';
    public static final char DATE          = 'R';
    public static final char ENV           = 'S';
    public static final char EXP           = 'T';
    public static final char QTY           = 'U';
    public static final char WORD          = 'V';
    public static final char MULTI         = 'W';
    public static final char EMPTY         = 'X';
    
    public static final int HISTORY_LENGTH = 40;
    
    String currAcct, un, date, commandsInput;
    LinkedList<String> commandsResult;
    Command headCommand, tailCommand;
    int commandsSize;
    ModelController mc;
    
    
    
    // CONSTRUCTOR
    
    public Commands(ModelController mc, String un, String date, String input) {
        currAcct = null;
        this.mc = mc;
        this.un = un;
        if(Utilities.isDate(date)) {
            this.date = date;
        } else  {
            this.date = Utilities.getTimestamp().substring(0, 10);
        }
        commandsResult = new LinkedList();
        commandsSize = 0;
        commandsInput = input;
        commandsInput = commandsInput.replaceAll("\n", "");
        commandsInput = commandsInput.replaceAll("\r", "");
        String[] cmds = commandsInput.split(",");
        for (String cmd : cmds) {
            if (cmd.trim().length() > 0) {
                addCommand(cmd.trim());
            }
        }
    }
    
    // PUBLIC METHODS
    
    public void preprocessCommands() {
        String cmdType = "";
        
        
        switch(cmdType) {
            case ENV            + "" + EXP                              : return ;
            case ACCT           + "" + ENV      + "" + EXP              : return ;
            case ENV            + "" + EXP      + "" + WORD             : return ;
            case ACCT           + "" + ENV      + "" + EXP  + "" + WORD : return ;
            default:
        }
    }
    
    public String executeCommands() {
        if(un==null) {
            return "Access denied.";
        } else {
            int splits[] = new int[commandsSize];
            int index = 0;
            int splitID = 0;
            boolean prevSplit = false;
            HashMap<Integer, Integer> splitAmts = new HashMap();
            HashMap<Integer, Integer> splitCnts = new HashMap();
            Command curr;
            
            // Identify SPLIT transactions
            curr = headCommand; // start with first command
            while(curr != null && index < commandsSize) {
                switch(curr.getCommandType()) {
                    case ENV  + "" + EXP :
                    case ENV  + "" + EXP + "" + WORD :
                        if(prevSplit) {
                            splits[index] = splitID; // store split ID
                            splitAmts.put(splitID, Utilities.amountToInteger(curr.getToken(2).getPossibilities()) + splitAmts.get(splitID));
                            splitCnts.put(splitID, splitCnts.get(splitID) + 1);
                        } else {
                            splits[index] = -1; // command not part of a split
                        }
                        break;
                    case ACCT + "" + ENV + "" + EXP :
                    case ACCT + "" + ENV + "" + EXP  + "" + WORD :
                        prevSplit = true;
                        splitID++; // increment to new split ID
                        splits[index] = splitID; // store split ID
                        splitAmts.put(splitID, Utilities.amountToInteger(curr.getToken(3).getPossibilities()));
                        splitCnts.put(splitID, 1);
                        break;
                    default:
                        splits[index] = -1; // command not part of a split
                        prevSplit = false;
                }
                // increment to next command
                curr = curr.nextCommand;
                index++;
            }
            
            // Set SPLIT amounts
            curr = headCommand; // start with first command
            index = 0;
            while(curr != null && index < commandsSize) {
                if(splits[index]!=-1 && splitCnts.get(splits[index]) > 1) {
                    curr.setSplitAmt(splitAmts.get(splits[index]));
                }
                // increment to next command
                curr = curr.nextCommand;
                index++;
            }
            
            // Calculate split amounts
            curr = headCommand; // start with first command
            int sum = 0;
            while(curr != null && index < commandsSize) {
                switch(curr.getCommandType()) {
                    case ENV  + "" + EXP :
                    case ENV  + "" + EXP + "" + WORD :
                        String exp = curr.getToken(2).getPossibilities();
                        sum += Utilities.amountToInteger(exp);
                        break;
                    case ACCT + "" + ENV + "" + EXP :
                    case ACCT + "" + ENV + "" + EXP  + "" + WORD :
                    default:
                    
                }
                // increment to next command
                curr = curr.nextCommand;
                index++;
            }
            
            curr = headCommand; // start with first command
            while(curr != null) {
                commandsResult.add(curr.executeCommand());
                curr = curr.nextCommand;
            }
            
            boolean isFirst = true;
            String r = "";
            for(String str : commandsResult) {
                if(isFirst) {
                    r += str;
                    isFirst = false;
                } else {
                    r += "\n\n" + str;
                }
            }
            // empties commands
            currAcct = null;
            commandsInput = "";
            commandsResult = new LinkedList();
            headCommand = null;
            tailCommand = null;
            commandsSize = 0;
            return r;
        }
    }
    
    public void print() {
        printHelper(headCommand);
    }
    
    // PRIVATE METHODS
    
    private void printHelper(Command cmd) {
        if(cmd!=null) {
            System.out.println("Command: <" + cmd.commandInput + "> (Type: " + cmd.getCommandType() + ")");
            cmd.print();
            printHelper(cmd.nextCommand);
        }
    }
    
    private void addCommand(String input) {
        Command cmd = new Command(input);
        if(headCommand==null) {                    // add first cmd
            headCommand = cmd;
            tailCommand = headCommand;
        } else if(headCommand.nextCommand==null) { // add second cmd to tail
            tailCommand = cmd;
            headCommand.nextCommand = tailCommand;
            tailCommand.prevCommand = headCommand;
        } else {                                   // add next cmd to tail
            tailCommand.nextCommand = cmd;
            cmd.prevCommand = tailCommand;
            tailCommand = cmd;
        }
        commandsSize++;
    }
    
    private char getReserveWordType(String input) {
        switch(input) {
            case "account":       return ACCOUNT;
            case "accounts":      return ACCOUNTS;
            case "category":      return CATEGORY;
            case "categories":    return CATEGORIES;
            case "change":        return CHANGE;
            case "envelope":      return ENVELOPE;
            case "envelopes":     return ENVELOPES;
            case "help":          return HELP;
            case "history":       return HISTORY;
            case "new":           return NEW;
            case "password":      return PASSWORD;
            case "remove":        return REMOVE;
            case "rename":        return RENAME;
            case "uncategorized": return CAT;
            case "user":          return USER;
            case "users":         return USERS;
            default:              return WORD;
        }
    }
    
    private final class Command {
        Command nextCommand, prevCommand;
        String commandInput, commandType;
        Token headToken, tailToken;   // Head of tokens that make up the command
        int tokenCount, splitAmt;
        int HISTORY_DEFAULT_COUNT = 20;
        double sumRemoveMe;
               
        private Command(String input) {
            commandType = "";
            splitAmt = 0;
            tokenCount = 0;
            this.commandInput = input;
            String[] tokens = input.split(" ");
            for (String token : tokens) {
                if (token.trim().length() > 0) {
                    addToken(token.trim());
                }
            }
        }
        
        private void setSplitAmt(int amt) {
            splitAmt = amt;
        }
        
        private String getSplitDesc() {
            if(splitAmt == 0) {
                return "";
            } else {
                return "SPLIT " + Utilities.amountToString(splitAmt) + " ";
            }
        }
        
        private Token getToken(int num) {
            Token curr = headToken;
            int count = 1;
            while(curr!=null) {
                if(count==num) {
                    return curr;
                }
                count++;
                curr = curr.next;
            }
            return null;
        }
        
        private String accounts() {
            String response = "ACCOUNTS:";
            for(int i = 0; i < mc.getAccountCount()-1; i++) {
                response += "\n  " + mc.getAccountName(i) + " " + mc.getAccountAmount(i);
            }
            response += "\nTOTAL: " + mc.getAccountAmount(mc.getAccountCount()-1);
            return response;
        }
        
        private String categories() {
            String response = "CATEGORIES:";
            for(int i = 0; i < mc.getEnvelopeCCount()-1; i++) {
                if (mc.isEnvelope(i)) {
                    response += "\n  " + mc.getEnvelopeCName(i) + " " + mc.getEnvelopeCAmount(i);
                } else {
                    response += "\n" + mc.getEnvelopeCName(i) + " " + mc.getEnvelopeCAmount(i);
                }
            }
            response += "\nTOTAL: " + mc.getEnvelopeCAmount(mc.getEnvelopeCCount()-1);
            return response;
        }
        
        private String envelopes() {
            String response = "ENVELOPES:";
            for(int i = 0; i < mc.getEnvelopeUCount()-1; i++) {
                response += "\n  " + mc.getEnvelopeUName(i) + " " + mc.getEnvelopeUAmount(i);
            }
            response += "\nTOTAL: " + mc.getEnvelopeUAmount(mc.getEnvelopeUCount()-1);
            return response;
        }
        
        private String help() {
            return "USAGES:\n"
                    + " (optional), [1 or more], <replace>\n"
                    
                    + "\n--add new transaction(s)--\n"
                    + " <acct> [<env> <amt> (desc), ...]\n"
                    
                    + "\n--add transfer--\n"
                    + " <from> <to> <amt> (desc)\n"
                    
                    + "\n--add new account/category/envelope/user--\n"
                    + " new acc/cat/env <name>\n"
                    + " new user <name> <password>\n"
                    
                    + "\n--add new categorized envelope--\n"
                    + " new env <name> (cat)\n"
                    
                    + "\n--view amounts/transactions--\n"
                    + " accounts/categories/envelopes/users\n"
                    + " <acc/cat/env>\n"
                    + " history (<acc>/<env>) (<qty>/<date> <date>)\n"
                    
                    + "\n--modify--\n"
                    + " rename acc/cat/env/user <old> <new>\n"
                    + " remove acc/cat/env/user <name>\n"
                    + " change password <pw>\n"
                    + " <env> <cat>";
        }
        
        private String history() {
            mc.showTransactionsByIndexRange("-ALL-", "-ALL", 0, HISTORY_DEFAULT_COUNT, false);
            String response = "TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String historyQty(int qty) {
            mc.showTransactionsByIndexRange("-ALL-", "-ALL", 0, qty, false);
            String response = "TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String users() {
            String response = "USERS:";
            for(String name : mc.getUsernames()) {
                response += "\n " + name;
                if(mc.isUserAdmin(name)) {
                    response += " (admin)";
                }
            }
            return response;
        }
        
        private String acct(String acct) {
            return "ACCOUNT:" + "\n " + acct.toLowerCase() + " " + mc.getAccountAmount(acct);
        }
        
        private String cat(String cat) {
            cat = cat.toLowerCase();
            String[][] envsC = mc.getEnvelopes(cat);
            int total = 0;
            String response = "ENVELOPES (" + cat + "):";
            for(int i = 0; i < envsC[0].length; i++) {
                response += "\n " + envsC[0][i] + " " + Utilities.amountToString(Utilities.amountToInteger(envsC[1][i]));
                total += Utilities.amountToInteger(mc.getEnvelopeAmount(envsC[0][i]));
            }
            response += "\nTOTAL: " + Utilities.amountToString(total);
            return response;
        }
        
        private String env(String env) {
            return "ENVELOPE:" + "\n " + env.toLowerCase() + " " + mc.getEnvelopeAmount(env);
        }
        
        private String historyAcct(String acct) {
            mc.showTransactionsByIndexRange(acct, "-ALL", 0, HISTORY_DEFAULT_COUNT, false);
            String response = "ACCOUNT (" + acct + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String historyEnv(String env) {
            mc.showTransactionsByIndexRange("-ALL", env, 0, HISTORY_DEFAULT_COUNT, false);
            String response = "ENVELOPE (" + env + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String envCat(String env, String cat) {
            if(mc.setEnvelopeCategory(env, cat)) {
                return "Envelope (" + env + ") category successfully set to '" + cat + "'";
            } else {
                return "Error: unable to update envelope (" + env + ") to '" + cat + "'";
            }
        }
        
        private String envExp(String env, String exp) {
            if(currAcct==null) {
                return "Account not specified for transaction: '" + env + " " + Utilities.amountToString(Utilities.amountToInteger(exp)) + "'\n"
                        + "Specify account at least once: <acct> [<env> <amt> (desc), ...]";
            }
            String oldEnvAmt  = mc.getEnvelopeAmount(env);
            String oldAcctAmt = mc.getAccountAmount(currAcct);
            String desc = getSplitDesc() + "<no description specified>";
            mc.addTransaction(date, desc, exp, currAcct, un, env);
            return "UPDATE:\n"
                    + " amt: "  + Utilities.amountToString(Utilities.amountToInteger(exp)) + "\n"
                    + " desc: " + Utilities.shortenString(desc, 16) + "\n"
                    + "ENV: '" + env + "'\n"
                    + " " + oldEnvAmt + " >> " + mc.getEnvelopeAmount(env) + "\n"
                    + "ACCT: '" + currAcct + "'\n"
                    + " " + oldAcctAmt + " >> " + mc.getAccountAmount(currAcct);
        }
        
        private String chgPwWord(String pw) {
            if(mc.setUserPassword(un, pw)) {
                return "User (" + un + ") password successfully set";
            } else {
                return "Error: unable to set user (" + un + ") password to '" + pw + "'";
            }
        }
        
        private String histAcctQty(String acct, int qty) {
            mc.showTransactionsByIndexRange(acct, "-ALL", 0, qty, false);
            String response = "ACCOUNT (" + acct + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String histEnvQty(String env, int qty) {
            mc.showTransactionsByIndexRange(" -ALL", env, 0, qty, false);
            String response = "ENVELOPE (" + env + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String newAcctWord(String name) {
            if(mc.addAccount(name)) {
                return "Account '" + name + "' successfully created.";
            } else {
                return "Error: could not create '" + name + "'.";
            }
        }
        
        private String newCatWord(String name) {
            if(mc.addCategory(name)) {
                return "Category '" + name + "' successfully created.";
            } else {
                return "Error: could not create '" + name + "'.";
            }
        }
        
        private String newEnvWord(String name) {
            if(mc.addEnvelope(name)) {
                return "Envelope '" + name + "' successfully created.";
            } else {
                return "Error: could not create '" + name + "'.";
            }
        }
        
        private String remAcctWord(String name) {
            if(mc.disableAccount(name)) {
                return "Account '" + name + "' successfully removed.";
            } else {
                return "Account '" + name + "' must have a zero balance before removal. Remaining balance: " + mc.getAccountAmount(name);
            }
        }
        
        private String remCatWord(String name) {
            if(mc.removeCategory(name)) {
                return "Category '" + name + "' successfully removed.";
            } else {
                return "ERROR: category '" + name + "' cannot be removed.";
            }
        }
        
        private String remEnvWord(String name) {
            if(mc.removeEnvelope(name)) {
                return "Envelope '" + name + "' successfully removed.";
            } else {
                return "Envelope '" + name + "' cannot have any transactions before removal. Either merge with another envelope or remove transactions first.";
            }
        }
        
        private String remUsrWord(String name) {
            if(un.equalsIgnoreCase(name)) {
                return "Error: you cannot remove yourself. Nice try;)";
            } else if(mc.disableUser(name)) {
                return "User '" + name + "' successfully removed.";
            } else {
                return "Error: user '" + name + "' cannot be removed.";
            }
        }
        
        private String acctAcctExp(String acctFrom, String acctTo, String exp) {
            return mc.addTransfer(date, "transfer", exp, acctFrom, acctTo, un);
        }
        
        private String acctEnvExp(String acct, String env, String exp) {
            currAcct = acct;
            String oldEnvAmt  = mc.getEnvelopeAmount(env);
            String oldAcctAmt = mc.getAccountAmount(currAcct);
            String desc = getSplitDesc() + "<no description specified>";
            mc.addTransaction(date, desc, exp, currAcct, un, env);
            return "UPDATE:\n"
                    + " amt: "  + Utilities.amountToString(Utilities.amountToInteger(exp)) + "\n"
                    + " desc: " + Utilities.shortenString(desc, 16) + "\n"
                    + "ENV: '" + env + "'\n"
                    + " " + oldEnvAmt + " >> " + mc.getEnvelopeAmount(env) + "\n"
                    + "ACCT: '" + currAcct + "'\n"
                    + " " + oldAcctAmt + " >> " + mc.getAccountAmount(currAcct);
        }
        
        private String envEnvExp(String envFrom, String envTo, String exp) {
            return mc.addTransfer(date, "transfer", exp, envFrom, envTo, un);
        }
        
        private String envExpWord(String env, String exp) {
            if(currAcct==null) {
                return "Account not specified for transaction: '" + env + " " + Utilities.amountToString(Utilities.amountToInteger(exp)) + "'\n"
                        + "Specify account at least once: <acct> [<env> <amt> (desc), ...]";
            }
            String oldEnvAmt  = mc.getEnvelopeAmount(env);
            String oldAcctAmt = mc.getAccountAmount(currAcct);           
            // gets description from remaining tokens
            Token curr = getToken(3);
            String desc = getSplitDesc();
            boolean first = true;
            while(curr!=null) {
                if(first) {
                    desc += curr.getPossibilities();
                    first = false;
                } else {
                    desc += " " + curr.getPossibilities();
                }
                curr = curr.next;
            }
            mc.addTransaction(date, desc, exp, currAcct, un, env);
            return "UPDATE:\n"
                    + " amt: "  + Utilities.amountToString(Utilities.amountToInteger(exp)) + "\n"
                    + " desc: " + Utilities.shortenString(desc, 16) + "\n"
                    + "ENV: '" + env + "'\n"
                    + " " + oldEnvAmt + " >> " + mc.getEnvelopeAmount(env) + "\n"
                    + "ACCT: '" + currAcct + "'\n"
                    + " " + oldAcctAmt + " >> " + mc.getAccountAmount(currAcct);
        }
        
        private String histDateDate(String from, String to) {
            mc.showTransactionsByDateRange("-ALL", "-ALL", from, to, false);
            String response = "TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String histAcctDateDate(String acct, String from, String to) {
            mc.showTransactionsByDateRange(acct, "-ALL", from, to, false);
            String response = "ACCOUNT (" + acct + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String histEnvDateDate(String env, String from, String to) {
            mc.showTransactionsByDateRange("-ALL", env, from, to, false);
            String response = "ENVELOPE (" + env + ") TRANSACTIONS:"
                    + "\ndate | user | amount | account | envelope | description";
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                response += "\n" + mc.getTransactionDate(i)
                         + " | " + mc.getTransactionUser(i)
                         + " | " + mc.getTransactionAmountString(i)
                         + " | " + mc.getTransactionAccount(i)
                         + " | " + mc.getTransactionEnvelope(i)
                         + " | " + Utilities.shortenString(mc.getTransactionDesc(i), HISTORY_LENGTH);
            }
            return response;
        }
        
        private String newEnvWordCat(String env, String cat) {
            if(mc.addEnvelope(env)) {
                if(mc.setEnvelopeCategory(env, cat)) {
                    return "Envelope '" + env + "' successfully created under category '" + cat + "'.";
                } else {
                    return "Envelope '" + env + "' successfully created, but could not set category to '" + cat + "'.";
                }
            } else {
                return "ERROR: envelope '" + env + "' cannot be created.";
            }
        }
        
        private String newUsrWordWord(String un, String pw) {
            if(mc.addUser(un, pw)) {
                return "User '" + un + "' successfully created.";
            } else {
                return "ERROR: user '" + un + "' could not be created.";
            }
        }
        
        private String renAcctWordWord(String oldName, String newName) {
            if(mc.renameAccount(oldName, newName)) {
                return "Account '" + oldName + "' successfully renamed '" + newName + "'.";
            } else {
                return "ERROR: account '" + oldName + "' could not be renamed to '" + newName + "'.";
            }
        }
        
        private String renCatWordWord(String oldName, String newName) {
            if(mc.renameCategory(oldName, newName)) {
                return "Category '" + oldName + "' successfully renamed '" + newName + "'.";
            } else {
                return "ERROR: category '" + oldName + "' could not be renamed to '" + newName + "'.";
            }
        }
        
        private String renEnvWordWord(String oldName, String newName) {
            if(mc.renameEnvelope(oldName, newName)) {
                return "Envelope '" + oldName + "' successfully renamed '" + newName + "'.";
            } else {
                return "ERROR: envelope '" + oldName + "' could not be renamed to '" + newName + "'.";
            }
        }
        
        private String renUsrWordWord(String oldName, String newName) {
            if(mc.renameUser(oldName, newName)) {
                return "User '" + oldName + "' successfully renamed '" + newName + "'.";
            } else {
                return "ERROR: user '" + oldName + "' could not be renamed to '" + newName + "'.";
            }
        }
        
        private String acctEnvExpWord(String acct, String env, String exp) {
            currAcct = acct;
            String oldEnvAmt  = mc.getEnvelopeAmount(env);
            String oldAcctAmt = mc.getAccountAmount(currAcct);           
            // gets description from remaining tokens
            Token curr = getToken(4);
            String desc = getSplitDesc();
            boolean first = true;
            while(curr!=null) {
                if(first) {
                    desc += curr.getPossibilities();
                    first = false;
                } else {
                    desc += " " + curr.getPossibilities();
                }
                curr = curr.next;
            }
            mc.addTransaction(date, desc, exp, currAcct, un, env);
            return "UPDATE:\n"
                    + " amt: "  + Utilities.amountToString(Utilities.amountToInteger(exp)) + "\n"
                    + " desc: " + Utilities.shortenString(desc, 16) + "\n"
                    + "ENV: '" + env + "'\n"
                    + " " + oldEnvAmt + " >> " + mc.getEnvelopeAmount(env) + "\n"
                    + "ACCT: '" + currAcct + "'\n"
                    + " " + oldAcctAmt + " >> " + mc.getAccountAmount(currAcct);
        }
        
        private String executeCommand() {
            String cmdType = getCommandType();
            if(cmdType.contains("" + MULTI)) { // multiple potential types found
                String[] words = commandInput.split(" ");
                // returns message:
                // For '<word specified with multiple matches>' did you mean:
                // '<possibility 1>'
                // '<possibility 2>'
                // '<etc...>'
                return "For '" + words[cmdType.indexOf(MULTI)] + "' " + getToken(cmdType.indexOf(MULTI)+1).getPossibilities();
            } else {
                // reset current account if not part of a split transaction
                switch(cmdType) {
                    case ENV  + "" + EXP                         :
                    case ACCT + "" + ENV + "" + EXP              :
                    case ENV  + "" + EXP + "" + WORD             :
                    case ACCT + "" + ENV + "" + EXP  + "" + WORD :
                        break;
                    default:
                        currAcct = null;
                }
                // processes command
                switch(cmdType) {
                    case ACCOUNTS   + ""                                    : return accounts();
                    case CATEGORIES + ""                                    : return categories();
                    case ENVELOPES  + ""                                    : return envelopes();
                    case HELP       + ""                                    : return help();
                    case HISTORY    + ""                                    : return history();
                    case HISTORY    + "" + QTY                              : return historyQty(Integer.parseInt(getToken(2).getPossibilities()));
                    case USERS      + ""                                    : return users();
                    case ACCT       + ""                                    : return acct(getToken(1).getPossibilities());
                    case CAT        + ""                                    : return cat(getToken(1).getPossibilities());
                    case ENV        + ""                                    : return env(getToken(1).getPossibilities());
                    case HISTORY    + "" + ACCT                             : return historyAcct(getToken(2).getPossibilities());
                    case HISTORY    + "" + ENV                              : return historyEnv(getToken(2).getPossibilities());
                    case ENV        + "" + CAT                              : return envCat(getToken(1).getPossibilities(), getToken(2).getPossibilities());
                    case ENV        + "" + EXP                              : return envExp(getToken(1).getPossibilities(), getToken(2).getPossibilities());
                    case CHANGE     + "" + PASSWORD + "" + WORD             : return chgPwWord(getToken(3).getPossibilities());
                    case HISTORY    + "" + ACCT     + "" + QTY              : return histAcctQty(getToken(2).getPossibilities(), Integer.parseInt(getToken(3).getPossibilities()));
                    case HISTORY    + "" + DATE     + "" + DATE             : return histDateDate(getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    case HISTORY    + "" + ENV      + "" + QTY              : return histEnvQty(getToken(2).getPossibilities(), Integer.parseInt(getToken(3).getPossibilities()));
                    case NEW        + "" + ACCOUNT  + "" + WORD             : return newAcctWord(getToken(3).getPossibilities());
                    case NEW        + "" + CATEGORY + "" + WORD             : return newCatWord(getToken(3).getPossibilities());
                    case NEW        + "" + ENVELOPE + "" + WORD             : return newEnvWord(getToken(3).getPossibilities());
                    case REMOVE     + "" + ACCOUNT  + "" + WORD             : return remAcctWord(getToken(3).getPossibilities());
                    case REMOVE     + "" + CATEGORY + "" + WORD             : return remCatWord(getToken(3).getPossibilities());
                    case REMOVE     + "" + ENVELOPE + "" + WORD             : return remEnvWord(getToken(3).getPossibilities());
                    case REMOVE     + "" + USER     + "" + WORD             : return remUsrWord(getToken(3).getPossibilities());
                    case ACCT       + "" + ACCT     + "" + EXP              : return acctAcctExp(getToken(1).getPossibilities(),getToken(2).getPossibilities(),getToken(3).getPossibilities());
                    case ACCT       + "" + ENV      + "" + EXP              : return acctEnvExp(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    case ENV        + "" + ENV      + "" + EXP              : return envEnvExp(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    case ENV        + "" + EXP      + "" + WORD             : return envExpWord(getToken(1).getPossibilities(), getToken(2).getPossibilities());
                    case HISTORY    + "" + ACCT     + "" + DATE + "" + DATE : return histAcctDateDate(getToken(2).getPossibilities(), getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case HISTORY    + "" + ENV      + "" + DATE + "" + DATE : return histEnvDateDate(getToken(2).getPossibilities(), getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case NEW        + "" + ENVELOPE + "" + WORD + "" + CAT  : return newEnvWordCat(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case NEW        + "" + USER     + "" + WORD + "" + WORD : return newUsrWordWord(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case RENAME     + "" + ACCOUNT  + "" + WORD + "" + WORD : return renAcctWordWord(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case RENAME     + "" + CATEGORY + "" + WORD + "" + WORD : return renCatWordWord(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case RENAME     + "" + ENVELOPE + "" + WORD + "" + WORD : return renEnvWordWord(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case RENAME     + "" + USER     + "" + WORD + "" + WORD : return renUsrWordWord(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                    case ACCT       + "" + ENV      + "" + EXP  + "" + WORD : return acctEnvExpWord(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    default:                                                      return "Invalid command. Send 'help' for usages.";
                }
            }
        }
        
        private void print() {
            printHelper(headToken);
        }
        
        private void printHelper(Token t) {
            if(t!=null) {
                System.out.println(t.getTokenType() + ":" + t.getPossibilities());
                printHelper(t.next);
            } else {
                System.out.println();
            }
        }
        
        private void addToken(String str) {
            Token tmp = new Token();
            if(headToken==null) {
                headToken = tmp;
                tailToken = headToken;
            } else if(headToken.next==null) {
                tailToken = tmp;
                headToken.next = tailToken;
                tailToken.prev = headToken;
            } else {
                tailToken.next = tmp;
                tmp.prev = tailToken;
                tailToken = tmp;
            }
            // set after insert because token setting relies on previous token type
            tmp.setInput(str);
            tokenCount++;
            
            // removes extra [WORD] types from end
            if(!commandType.startsWith(ENV + "" + EXP + "" + WORD) &&               // simplifies [ENV][EXP][WORD]<more [WORD]'s>...
               !commandType.startsWith(ACCT + "" + ACCT + "" + EXP + "" + WORD) &&  // simplifies [ACCT][ACCT][EXP][WORD]<more [WORD]'s>...
               !commandType.startsWith(ACCT + "" + ENV  + "" + EXP + "" + WORD) &&  // simplifies [ACCT][ENV] [EXP][WORD]<more [WORD]'s>...
               !commandType.startsWith(ENV  + "" + ENV  + "" + EXP + "" + WORD) ) { // simplifies [ENV] [ENV] [EXP][WORD]<more [WORD]'s>...
                commandType += tmp.getTokenType();
            }
            
        }
        
        public String getCommandType() {
            return commandType;
        }
        
        private final class Token {
            boolean exactMatch = false;
            Token next, prev;                 // used for linked list
            LinkedList<String> possibilities; // stores matching possibilites
            char tokenType;                   // stores token type
            
            void setInput(String input) {
                possibilities = new LinkedList();
                tokenType = EMPTY; // defaults to empty
                if(input.length()>0) {
                    out: while(true) {
                        switch (tokenCount) {
                            case 0:
                                // this is the first token
                                input = input.toLowerCase();
                                // possible reserve words for first token
                                String[] res1 = {"accounts", "categories", "change", "envelopes", "help", "history", "new", "remove", "rename", "users", "uncategorized"};
                                // adds any reserve words that match given commandsInput
                                for(String str : res1) {
                                    if(str.startsWith(input)) {
                                        possibilities.add(str);
                                        tokenType = getReserveWordType(possibilities.peek());
                                        if(str.equalsIgnoreCase(input)) {
                                            possibilities.clear();
                                            possibilities.add(input);
                                            break out;
                                        }
                                    }
                                }
                                // gets any accounts, categories, or envelopes that match given commandsInput
                                for(String name : mc.getEnvelopeNames()) {
                                    if(name.startsWith(input)) {
                                        // change type to envelope
                                        tokenType = ENV;
                                        possibilities.add(name);
                                        if(name.equalsIgnoreCase(input)) {
                                            possibilities.clear();
                                            possibilities.add(input);
                                            break out;
                                        }
                                    }
                                }
                                for(String name : mc.getAccountNames()) {
                                    if(name.startsWith(input)) {
                                        // change type to account
                                        tokenType = ACCT;
                                        possibilities.add(name);
                                        if(name.equalsIgnoreCase(input)) {
                                            possibilities.clear();
                                            possibilities.add(input);
                                            break out;
                                        }
                                    }
                                }
                                for(String name : mc.getCategoryNames()) {
                                    if(name.startsWith(input)) {
                                        // change type to category
                                        tokenType = CAT;
                                        possibilities.add(name);
                                        if(name.equalsIgnoreCase(input)) {
                                            possibilities.clear();
                                            possibilities.add(input);
                                            break out;
                                        }
                                    }
                                }   
                                break;
                            case 1:
                                // this is the second token
                                switch (prev.tokenType) {
                                    case ACCT:
                                        input = input.toLowerCase();
                                        // checks for accounts
                                        for(String name : mc.getAccountNames()) {
                                            if(name.startsWith(input)) {
                                                possibilities.add(name);
                                                // change type to account
                                                tokenType = ACCT;
                                                if(name.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        // checks for envelopes
                                        for(String name : mc.getEnvelopeNames()) {
                                            if(name.startsWith(input)) {
                                                possibilities.add(name);
                                                // change type to envelope
                                                tokenType = ENV;
                                                if(name.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        break;
                                    case ENV:
                                        {
                                            input = input.toLowerCase();
                                            // checks for categories
                                            for(String name : mc.getCategoryNames()) {
                                                if(name.startsWith(input)) {
                                                    possibilities.add(name);
                                                    // change type to envelope
                                                    tokenType = CAT;
                                                    if(name.equalsIgnoreCase(input)) {
                                                        possibilities.clear();
                                                        possibilities.add(input);
                                                        break out;
                                                    }
                                                }
                                            }
                                            // checks for envelopes
                                            for(String name : mc.getEnvelopeNames()) {
                                                if(name.startsWith(input)) {
                                                    possibilities.add(name);
                                                    // change type to envelope
                                                    tokenType = ENV;
                                                    if(name.equalsIgnoreCase(input)) {
                                                        possibilities.clear();
                                                        possibilities.add(input);
                                                        break out;
                                                    }
                                                }
                                            }
                                            // checks for expression
                                            try {
                                                possibilities.add(Double.toString(Double.parseDouble(input)));
                                                tokenType = EXP;
                                                break out;
                                            } catch (NumberFormatException e) {}
                                            break;
                                        }
                                    case CHANGE:
                                        input = input.toLowerCase();
                                        if("password".startsWith(input)) {
                                            possibilities.add("password");
                                            tokenType = PASSWORD;
                                            if("password".equalsIgnoreCase(input)) {
                                                possibilities.clear();
                                                possibilities.add(input);
                                                break out;
                                            }
                                        }
                                        break;
                                    case HISTORY:
                                        // checks for quantity
                                        try {
                                            int qty = Integer.parseInt(input);
                                            if(qty>=0) {
                                                possibilities.add(input);
                                                tokenType = QTY;
                                                break out;
                                            }
                                        } catch(NumberFormatException e1) {}
                                        input = input.toLowerCase();
                                        // checks for accounts
                                        for(String name : mc.getAccountNames()) {
                                            if(name.startsWith(input)) {
                                                possibilities.add(name);
                                                // change type to account
                                                tokenType = ACCT;
                                                if(name.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        // checks for categories
                                        for(String name : mc.getCategoryNames()) {
                                            if(name.startsWith(input)) {
                                                // change type to category
                                                tokenType = CAT;
                                                possibilities.add(name);
                                                if(name.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        // checks for envelopes
                                        for(String name : mc.getEnvelopeNames()) {
                                            if(name.startsWith(input)) {
                                                possibilities.add(name);
                                                // change type to envelope
                                                tokenType = ENV;
                                                if(name.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        break;
                                    case NEW:
                                    case REMOVE:
                                    case RENAME:
                                        input = input.toLowerCase();
                                        // possible reserve words
                                        String[] res2 = {"account", "category", "envelope", "user"};
                                        // adds any reserve words that match given commandsInput
                                        for(String str : res2) {
                                            if(str.startsWith(input)) {
                                                possibilities.add(str);
                                                tokenType = getReserveWordType(possibilities.peek());
                                                if(str.equalsIgnoreCase(input)) {
                                                    possibilities.clear();
                                                    possibilities.add(input);
                                                    break out;
                                                }
                                            }
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case 2:
                                // this is the third token
                                if(prev.tokenType==ACCT || prev.tokenType==ENV) {
                                    // checks for date
                                    if(Utilities.isDate(input)) {
                                        possibilities.add(input);
                                        tokenType = DATE;
                                        break out;
                                    }
                                    input = input.toLowerCase();
                                    // checks for quantity
                                    try {
                                        int qty = Integer.parseInt(input);
                                        if(qty>=0 && prev.prev.tokenType==HISTORY) {
                                            possibilities.add(input);
                                            tokenType = QTY;
                                            break out;
                                        }
                                    } catch(NumberFormatException e1) { }
                                    // checks for expression
                                    try {
                                        Double.parseDouble(input);
                                        possibilities.add(input);
                                        tokenType = EXP;
                                        break out;
                                    } catch (NumberFormatException e) { }
                                } else if(prev.tokenType==CAT) {
                                    input = input.toLowerCase();
                                    // checks for quantity
                                    try {
                                        int qty = Integer.parseInt(input);
                                        if(qty>=0 && prev.prev.tokenType==HISTORY) {
                                            possibilities.add(input);
                                            tokenType = QTY;
                                        }
                                    } catch(NumberFormatException e1) { }
                                    // checks for date
                                    if(Utilities.isDate(input)) {
                                        possibilities.add(input);
                                        tokenType = DATE;
                                    }
                                }
                                break;
                            case 3:
                                // this is the fourth token
                                if(prev.tokenType==DATE) {
                                    // checks for date
                                    if(Utilities.isDate(input)) {
                                        possibilities.add(input);
                                        tokenType = DATE;
                                    }
                                } else if(prev.tokenType==WORD && prev.prev.tokenType==ENVELOPE && prev.prev.prev.tokenType==NEW) {
                                    input = input.toLowerCase();
                                    // checks for categories
                                    for(String name : mc.getCategoryNames()) {
                                        possibilities.add(name);
                                        // change type to envelope
                                        tokenType = CAT;
                                        if(name.equalsIgnoreCase(input)) {
                                            possibilities.clear();
                                            possibilities.add(input);
                                            break out;
                                        }
                                    }
                                }   break;
                            default:
                                break;
                        }
                        break;
                    }
                    
                    if(possibilities.size()<1) { // no match was found; defaults to a <word>
                        possibilities.add(input);
                        tokenType = WORD;
                    } else if(possibilities.size()>1) { // multiple matches were found
                        tokenType = MULTI;
                    }
                    
                    // sorts the possibilities
                    Collections.sort(possibilities);
                }
            }
            
            String getPossibilities() {
                if(possibilities.size()==1) {
                    return possibilities.peek();
                } else {
                    String results = "did you mean:";
                    for(String s : possibilities) {
                        results += "\n'" + s + "'";
                    }
                    return results;
                }
            }
            
            char getTokenType() {
                return tokenType;
            }
        }
    }
}
