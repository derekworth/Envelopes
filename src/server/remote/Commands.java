package server.remote;

import database.Account;
import database.Category;
import database.DBMS;
import database.Envelope;
import database.Transaction;
import database.User;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import misc.Utilities;

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
    public static final char UNCATEGORIZED = 'Y';
    
    Account currAcct;
    User user;
    String date;
    String commandsInput;
    LinkedList<Transaction> splitTransactions;
    LinkedList<String> commandsResult;
    Command headCommand, tailCommand;
    int commandsSize;
    
    // CONSTRUCTOR
    
    public Commands(User usr, String date, String input) {
        user = usr;
        if(Utilities.isDate(date)) {
            this.date = date;
        } else  {
            this.date = Utilities.getTimestamp().substring(0, 10);
        }
        commandsResult = new LinkedList();
        splitTransactions = new LinkedList();
        commandsSize = 0;
        commandsInput = input;
        String[] cmds = commandsInput.split(",");
        for (String cmd : cmds) {
            if (cmd.trim().length() > 0) {
                addCommand(cmd.trim());
            }
        }
    }
    
    // PUBLIC METHODS
    
    public String executeCommands() {
        if(user==null) {
            return "Access denied.";
        } else {
            Command curr = headCommand;
            String r = "";
            while(curr != null) {
                commandsResult.add(curr.executeCommand());
                curr = curr.nextCommand;
            }
            boolean isFirst = true;
            for(String str : commandsResult) {
                if(isFirst) {
                    r += str;
                    isFirst = false;
                } else {
                    r += "\n\n" + str;
                }
            }
            headCommand.updateSplitDescriptions();
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
        if(headCommand==null) {
            headCommand = cmd;
            tailCommand = headCommand;
        } else if(headCommand.nextCommand==null) {
            tailCommand = cmd;
            headCommand.nextCommand = tailCommand;
            tailCommand.prevCommand = headCommand;
        } else {
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
            case "user":          return USER;
            case "users":         return USERS;
            case "uncategorized": return UNCATEGORIZED;
            default:              return WORD;
        }
    }
        
    private final class Command {
        Command nextCommand, prevCommand;
        String commandInput;
        String commandType;
        String commandResponse;
        Token headToken, tailToken;   // Head of tokens that make up the command
        int tokenCount;
        int HISTORY_DEFAULT_COUNT = 20;
        double sumRemoveMe;
               
        private Command(String input) {
            commandType = "";
            commandResponse = "";
            tokenCount = 0;
            this.commandInput = input;
            String[] tokens = input.split(" ");
            for (String token : tokens) {
                if (token.trim().length() > 0) {
                    addToken(token.trim());
                }
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
        
        private String acctAcctExp(String acctFrom, String acctTo, String exp) {
            Account from = DBMS.getAccount(acctFrom, true);
            Account to = DBMS.getAccount(acctTo, true);
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
            
            double oldFromAcctAmt = from.getAmt();
            double oldToAcctAmt   = to.getAmt();
            // Create transactions
            Transaction t1 = DBMS.addTransaction(from.getName(), "", user.getUsername(), date, "*(" + from.getName() + " > " + to.getName() + ")", -amt, "");
            Transaction t2 = DBMS.addTransaction(to.getName(),   "", user.getUsername(), date, "*(" + from.getName() + " > " + to.getName() + ")", amt, "");
            DBMS.setTransferRelationship(t1, t2);
            
            return "ACCOUNT TRANSFER:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + "FROM: '" + from.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldFromAcctAmt) + " >> " + Utilities.addCommasToAmount(oldFromAcctAmt-amt) + "\n"
                    + "TO: '" + to.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldToAcctAmt) + " >> " + Utilities.addCommasToAmount(oldToAcctAmt+amt);
        }
        
        private String envEnvExp(String envFrom, String envTo, String exp) {
            Envelope from = DBMS.getEnvelope(envFrom, true);
            Envelope to = DBMS.getEnvelope(envTo, true);
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
                    
            double oldFromAmt = from.getAmt();
            double oldToAmt   = to.getAmt();
            // Create transactions
            Transaction t1 = DBMS.addTransaction("", from.getName(), user.getUsername(), date, "(" + from.getName() + " > " + to.getName() + ")", -amt, "");
            Transaction t2 = DBMS.addTransaction("", to.getName(),   user.getUsername(), date, "(" + from.getName() + " > " + to.getName() + ")", amt, "");
            DBMS.setTransferRelationship(t1, t2);
            
            return "ENVELOPE TRANSFER:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + "FROM: '" + from.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldFromAmt) + " >> " + Utilities.addCommasToAmount(oldFromAmt-amt) + "\n"
                    + "TO: '" + to.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldToAmt) + " >> " + Utilities.addCommasToAmount(oldToAmt+amt);
        }
        
        private String accounts() {
            LinkedList<Account> accts = DBMS.getAccounts(true);
            double sum = 0;
            String response = "ACCOUNTS:";
            for(Account a : accts) {
                response += "\n " + a.getName() + " " + Utilities.addCommasToAmount(a.getAmt());
                sum += a.getAmt();
            }
            response += "\nTOTAL: " + Utilities.addCommasToAmount(sum);
            return response;
        }
        
        private String categories() {
            LinkedList<Category> cats = DBMS.getCategories(true);
            LinkedList<Envelope> envs;
            double sum = 0;
            String response = "CATEGORIES:";
            for(Category c : cats) {
                envs = DBMS.getEnvelopes(c, true);
                // add category with total
                response += "\n " + c.getName().toUpperCase() + " " + Utilities.addCommasToAmount(c.getAmt());
                // add envelopes with totals
                for(Envelope e : envs) {
                    response += "\n   " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
                }
                sum += c.getAmt();
            }
            
            envs = DBMS.getUncategorizedEnvelopes(true);
            double uncatSum = 0;
            // get sum of uncategorized envelopes
            for(Envelope e : envs) {
                uncatSum += e.getAmt();
            }
            if(envs.size()>0) {
                // add category with total
                response += "\n UNCATEGORIZED " + Utilities.addCommasToAmount(uncatSum);
                // add envelopes with totals
                for(Envelope e : envs) {
                    response += "\n   " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
                }
            }
            sum += uncatSum;
            
            response += "\nTOTAL: " + Utilities.addCommasToAmount(sum);
            return response;
        }
        
        private String envelopes() {
            LinkedList<Envelope> envs = DBMS.getEnvelopes(true);
            double sum = 0;
            String response = "ENVELOPES:";
            for(Envelope e : envs) {
                response += "\n " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
                sum += e.getAmt();
            }
            response += "\nTOTAL: " + Utilities.addCommasToAmount(sum);
            return response;
        }
        
        private String help() {
            return "USAGES:\n"
                    + "--create--\n"
                    + " new acc/cat/env <name>\n"
                    + " new env <name> (cat)\n"
                    + " new user <name> <password>\n"
                    + " <acct> [<env> <amt> (desc), ...]\n"
                    + "--view--\n"
                    + " accounts/categories/envelopes/users\n"
                    + " <acc/cat/env>\n"
                    + " history (<acc>/<cat>/<env>) (<qty>/<date> <date>)\n"
                    + "--modify--\n"
                    + " <from> <to> <amt> (desc)\n"
                    + " rename acc/cat/env/user <old> <new>\n"
                    + " remove acc/cat/env/user <name>\n"
                    + " change password <pw>\n"
                    + " <env> <cat>";
        }
        
        private String history() {
            String response = "TRANSACTIONS:\n"
                    + "date | amount | description | account | envelope | user";
            LinkedList<Transaction> trans = DBMS.getTransactions(HISTORY_DEFAULT_COUNT);
            for(Transaction tran : trans) {
                response += "\n" + tran.getDate() + " | " + Utilities.addCommasToAmount(tran.getAmt()) + " | " + Utilities.shortenString(tran.getDesc(), 20) + " | " + tran.getAcct().getName() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
            }
            return response;
        }
        
        private String historyQty(int qty) {
            String response = "TRANSACTIONS:\n"
                    + "date | amount | description | account | envelope | user";
            LinkedList<Transaction> trans = DBMS.getTransactions(qty);
            for(Transaction tran : trans) {
                response += "\n" + tran.getDate() + " | " + Utilities.addCommasToAmount(tran.getAmt()) + " | " + Utilities.shortenString(tran.getDesc(), 20) + " | " + tran.getAcct().getName() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
            }
            return response;
        }
        
        private String users() {
            String response = "USERS:";
            LinkedList<User> users = DBMS.getUsers(true);
            for(User u : users) {
                if(u.isAdmin()) {
                    response += "\n " + u.getUsername() + " (admin)";
                } else {
                    response += "\n " + u.getUsername();
                }
            }
            return response;
        }
        
        private String acct(String acct) {
            Account a = DBMS.getAccount(acct, true);
            return "ACCOUNT:" + "\n " + a.getName() + " " + Utilities.addCommasToAmount(a.getAmt());
        }
        
        private String cat(String cat) {
            Category c = DBMS.getCategory(cat, true);
            LinkedList<Envelope> envs = DBMS.getEnvelopes(c, true);
            String response = "ENVELOPES (" + c.getName() + "):";
            double sum = 0;
            for(Envelope e : envs) {
                response += "\n " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
                sum += e.getAmt();
            }
            response += "\nTOTAL: " + Utilities.addCommasToAmount(sum);
            return response;
        }
        
        private String env(String env) {
            Envelope e = DBMS.getEnvelope(env, true);
            return "ENVELOPE:" + "\n " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
        }
        
        private String uncategorized() {
            String response;
            double sum = 0;
            LinkedList<Envelope> envs = DBMS.getUncategorizedEnvelopes(true);
            response = "ENVELOPES (uncategorized):";
            for(Envelope e : envs) {
                response += "\n " + e.getName() + " " + Utilities.addCommasToAmount(e.getAmt());
                sum += e.getAmt();
            }
            response += "\nTOTAL: " + Utilities.addCommasToAmount(sum);
            return response;
        }
        
        private String envUncategorized(String env) {
            Envelope e = DBMS.getEnvelope(env, true);
            String response = e.setCategory(null);
            return response;
        }
        
        private String historyAcct(String acct) {
            Account a = DBMS.getAccount(acct, true);
            LinkedList<Transaction> trans = DBMS.getTransactions(a, HISTORY_DEFAULT_COUNT);
            String response = "ACCOUNT (" + a.getName() + ") TRANSACTIONS:\n"
                    + "date | amount | description | envelope | user";
            for(Transaction t : trans) {
                response += "\n" + t.getDate() + " | " + Utilities.roundAmount(t.getAmt()) + " | " + Utilities.shortenString(t.getDesc(), 20) + " | " + t.getEnv().getName() + " | " + t.getUser().getUsername();
            }
            return response;
        }
        
        private String historyCat(String cat) {
            Category c = DBMS.getCategory(getToken(2).getPossibilities(), true);
            LinkedList<Transaction> trans = DBMS.getTransactions(c, HISTORY_DEFAULT_COUNT);
            String response = "CATEGORY (" + c.getName() + ") TRANSACTIONS:\n"
                    + "date | amount | description | account | envelope | user";
            for(Transaction t : trans) {
                response += "\n" + t.getDate() + " | " + Utilities.roundAmount(t.getAmt()) + " | " + Utilities.shortenString(t.getDesc(), 20) + " | " + t.getAcct().getName() + " | " + t.getEnv().getName() + " | " + t.getUser().getUsername();
            }
            return response;
        }
        
        private String historyEnv(String env) {
            Envelope e = DBMS.getEnvelope(env, true);
            LinkedList<Transaction> trans = DBMS.getTransactions(e, HISTORY_DEFAULT_COUNT);
            String response = "ENVELOPE (" + e.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | account | user";
            for(Transaction t : trans) {
                response += "\n" + t.getDate() + " | " + Utilities.roundAmount(t.getAmt()) + " | " + Utilities.shortenString(t.getDesc(), 20) + " | " + t.getAcct().getName() + " | " + t.getUser().getUsername();
            }
            return response;
        }
        
        private String envCat(String env, String cat) {
            // get envelope
            Envelope e = DBMS.getEnvelope(env, true);
            // get category
            Category c = DBMS.getCategory(cat, true);
            // add envelope to category
            return e.setCategory(c);
        }
        
        private void updateSplitDescriptions() {
            if(splitTransactions.size() > 1) {
                // Get TOTAL amount
                double total = 0;
                Iterator<Transaction> tranIter = splitTransactions.iterator();
                while(tranIter.hasNext()) {
                    total += tranIter.next().getAmt();
                }
                String splitTotal = Utilities.roundAmount(-total);
                // Update descriptions with "SPLIT <TOTAL> <original description>"
                while(!splitTransactions.isEmpty()) {
                    Transaction tmp = splitTransactions.remove();
                    String oldDesc = tmp.getDesc();
                    tmp.setDescription("SPLIT " + splitTotal + " " + oldDesc);
                }
            }
            splitTransactions = new LinkedList();
        }
        
        private String envExp(String env, String exp) {
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
            if(currAcct==null) {
                return "Account not specified for transaction: '" + env + " " + Utilities.addCommasToAmount(amt) + "'\n"
                        + "Specify account at least once: <acct> [<env> <amt> (desc), ...]";
            }
            Envelope e = DBMS.getEnvelope(env, true);
            double oldAcctAmt = currAcct.getAmt();
            double oldEnvAmt = e.getAmt();
            Transaction t = DBMS.addTransaction(currAcct.getName(), e.getName(), user.getUsername(), date, "<no description specified>", amt, "");
            splitTransactions.add(t);
            // updates envelope and account now that transaction created
            e.updateAmt();
            currAcct.setAmt(oldAcctAmt + amt);
            return "UPDATE:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + " desc: <none specified>\n"
                    + "ENV: '" + e.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldEnvAmt) + " >> " + Utilities.addCommasToAmount(e.getAmt()) + "\n"
                    + "ACCT: '" + currAcct.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldAcctAmt) + " >> " + Utilities.addCommasToAmount(currAcct.getAmt());
        }
        
        private String acctEnvExp(String acct, String env, String exp) {
            updateSplitDescriptions();
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
            Envelope e = DBMS.getEnvelope(env, true);
            currAcct = DBMS.getAccount(acct, true);
            double oldAcctAmt = currAcct.getAmt();
            double oldEnvAmt = e.getAmt();
            Transaction t = DBMS.addTransaction(currAcct.getName(), e.getName(), user.getUsername(), date, "<no description specified>", amt, "");
            splitTransactions.add(t);
            // updates envelope and account now that transaction created
            e.updateAmt();
            currAcct.setAmt(oldAcctAmt + amt);
            return "UPDATE:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + " desc: <none specified>\n"
                    + "ENV: '" + e.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldEnvAmt) + " >> " + Utilities.addCommasToAmount(e.getAmt()) + "\n"
                    + "ACCT: '" + currAcct.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldAcctAmt) + " >> " + Utilities.addCommasToAmount(currAcct.getAmt());
        }
        
        private String envExpWord(String env, String exp) {
            if(currAcct==null) {
                return "Account not specified for transaction: '" + env + " " + exp + "'\n"
                        + "Specify account at least once: <acct> [<env> <amt> (desc), ...]";
            }
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
            Envelope e = DBMS.getEnvelope(env, true);
            double oldAcctAmt = currAcct.getAmt();
            double oldEnvAmt = e.getAmt();
            // gets description from remaining tokens
            Token curr = getToken(3);
            String desc = "";
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
            // new transaction
            Transaction t = DBMS.addTransaction(currAcct.getName(), e.getName(), user.getUsername(), date, desc, amt, "");
            splitTransactions.add(t);
            // shorten description for response
            desc = Utilities.shortenString(t.getDesc(),15);
            // updates envelope and account now that transaction created
            e.updateAmt();
            currAcct.setAmt(oldAcctAmt + amt);
            return "UPDATE:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + " desc: " + desc + "\n"
                    + "ENV: '" + e.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldEnvAmt) + " >> " + Utilities.addCommasToAmount(e.getAmt()) + "\n"
                    + "ACCT: '" + currAcct.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldAcctAmt) + " >> " + Utilities.addCommasToAmount(currAcct.getAmt());
        }
        
        private String acctEnvExpWord(String acct, String env, String exp) {
            updateSplitDescriptions();
            double amt = Double.parseDouble(Utilities.roundAmount(Double.parseDouble(exp)));
            Envelope e = DBMS.getEnvelope(env, true);
            currAcct = DBMS.getAccount(acct, true);
            double oldAcctAmt = currAcct.getAmt();
            double oldEnvAmt = e.getAmt();
            // gets description from remaining tokens
            Token curr = getToken(4);
            String desc = "";
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
            // new transaction
            Transaction t = DBMS.addTransaction(currAcct.getName(), e.getName(), user.getUsername(), date, desc, amt, "");
            splitTransactions.add(t);
            // shorten description for response
            desc = Utilities.shortenString(t.getDesc(),15);
            // updates envelope and account now that transaction created
            e.updateAmt();
            currAcct.setAmt(oldAcctAmt + amt);
            return "UPDATE:\n"
                    + " amt: " + Utilities.addCommasToAmount(amt) + "\n"
                    + " desc: " + desc + "\n"
                    + "ENV: '" + e.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldEnvAmt) + " >> " + Utilities.addCommasToAmount(e.getAmt()) + "\n"
                    + "ACCT: '" + currAcct.getName() + "'\n"
                    + " " + Utilities.addCommasToAmount(oldAcctAmt) + " >> " + Utilities.addCommasToAmount(currAcct.getAmt());
        }
        
        private String executeCommand() {
            Account acct;
            Category cat;
            Envelope env;
            User usr;
            double diff;
            double oldEnvAmt, oldAcctAmt;
            int qty;
            String cmdType = getCommandType();
            if(cmdType.contains("" + MULTI)) {
                String[] words = commandInput.split(" ");
                commandResponse = "For '" + words[cmdType.indexOf(MULTI)] + "' " + getToken(cmdType.indexOf(MULTI)+1).getPossibilities();
            } else {
                LinkedList<Transaction> trans;
                LinkedList<Envelope> envs;
                LinkedList<User> usrs;
                if(cmdType.startsWith(ENV + "" + EXP + "" + WORD)) {
                    cmdType = cmdType.substring(0, 3);
                } else if(cmdType.startsWith(ACCT + "" + ACCT + "" + EXP + "" + WORD) ||
                        cmdType.startsWith(ACCT + "" + ENV + "" + EXP + "" + WORD) ||
                        cmdType.startsWith(ENV + "" + ENV + "" + EXP + "" + WORD)) {
                    cmdType = cmdType.substring(0, 4);
                }
                switch(cmdType) {
                    case ACCOUNTS       + ""                    : return accounts();
                    case CATEGORIES     + ""                    : return categories();
                    case ENVELOPES      + ""                    : return envelopes();
                    case HELP           + ""                    : return help();
                    case HISTORY        + ""                    : return history();
                    case HISTORY        + "" + QTY              : return historyQty(Integer.parseInt(getToken(2).getPossibilities()));
                    case USERS          + ""                    : return users();
                    case ACCT           + ""                    : return acct(getToken(1).getPossibilities());
                    case CAT            + ""                    : return cat(getToken(1).getPossibilities());
                    case ENV            + ""                    : return env(getToken(1).getPossibilities());
                    case UNCATEGORIZED  + ""                    : return uncategorized();
                    case ENV            + "" + UNCATEGORIZED    : return envUncategorized(getToken(1).getPossibilities());
                    case HISTORY        + "" + ACCT             : return historyAcct(getToken(2).getPossibilities());
                    case HISTORY        + "" + CAT              : return historyCat(getToken(2).getPossibilities());
                    case HISTORY        + "" + ENV              : return historyEnv(getToken(2).getPossibilities());
                    case ENV            + "" + CAT              : return envCat(getToken(1).getPossibilities(), getToken(2).getPossibilities()); // <-----------------------------------------this and up already tested
                    case ENV + "" + EXP: return envExp(getToken(1).getPossibilities(), getToken(2).getPossibilities());
                    case CHANGE     + "" + PASSWORD + "" + WORD:
                        commandResponse = user.setPassword(getToken(3).getPossibilities());
                        break;
                    case HISTORY    + "" + ACCT      + "" + QTY:
                        acct = DBMS.getAccount(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(acct, Integer.parseInt(getToken(3).getPossibilities()));
                        commandResponse = "ACCOUNT (" + acct.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | envelope | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case HISTORY    + "" + CAT     + "" + QTY:
                        cat = DBMS.getCategory(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(cat, Integer.parseInt(getToken(3).getPossibilities()));
                        commandResponse = "CATEGORY (" + cat.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | account | envelope | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getAcct().getName() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case HISTORY    + "" + ENV     + "" + QTY:
                        env = DBMS.getEnvelope(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(env, Integer.parseInt(getToken(3).getPossibilities()));
                        commandResponse = "ENVELOPE (" + env.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | account | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getAcct().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case NEW        + "" + ACCOUNT  + "" + WORD:
                        String accName = getToken(3).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(accName, true)) {
                            commandResponse = "The name '" + accName + "' is already in use.";
                            break;
                        } else if(DBMS.isContainer(accName, false)) {
                            //check disabled accts
                            if(DBMS.isAccount(accName, false)) {
                                Account a = DBMS.getAccount(accName, false);
                                // rename old disabled account
                                a.setEnabled(true); // account must be enabled before it can be updated
                                a.setName(Utilities.renameContainer(a.getName()));
                                a.setEnabled(false); // return account to disabled state
                                // check disabled cats
                            } else if(DBMS.isCategory(accName, false)) {
                                Category c = DBMS.getCategory(accName, false);
                                // rename old disabled category
                                c.setEnabled(true); // category must be enabled before it can be updated
                                c.setName(Utilities.renameContainer(c.getName()));
                                c.setEnabled(false); // return category to disabled state
                                // check disabled envs
                            } else if(DBMS.isEnvelope(accName, false)) {
                                Envelope e = DBMS.getEnvelope(accName, false);
                                // rename old disabled envelope
                                e.setEnabled(true); // envelope must be enabled before it can be updated
                                e.setName(Utilities.renameContainer(e.getName()));
                                e.setEnabled(false); // return envelope to disabled state
                            }
                        }
                        // create acct
                        acct = DBMS.addAccount(getToken(3).getPossibilities());
                        
                        if(acct==null) {
                            commandResponse = "Error: could not create '" + getToken(3).getPossibilities() + "'.";
                        } else {
                            commandResponse = "Account '" + acct.getName() + "' successfully created.";
                        }
                        break;
                    case NEW        + "" + CATEGORY + "" + WORD:
                        String catName = getToken(3).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(catName, true)) {
                            commandResponse = "The name '" + catName + "' is already in use.";
                            break;
                        } else if(DBMS.isContainer(catName, false)) {
                            //check disabled accts
                            if(DBMS.isAccount(catName, false)) {
                                Account a = DBMS.getAccount(catName, false);
                                // rename old disabled account
                                a.setEnabled(true); // account must be enabled before it can be updated
                                a.setName(Utilities.renameContainer(a.getName()));
                                a.setEnabled(false); // return account to disabled state
                                // check disabled cats
                            } else if(DBMS.isCategory(catName, false)) {
                                Category c = DBMS.getCategory(catName, false);
                                // rename old disabled category
                                c.setEnabled(true); // category must be enabled before it can be updated
                                c.setName(Utilities.renameContainer(c.getName()));
                                c.setEnabled(false); // return category to disabled state
                                // check disabled envs
                            } else if(DBMS.isEnvelope(catName, false)) {
                                Envelope e = DBMS.getEnvelope(catName, false);
                                // rename old disabled envelope
                                e.setEnabled(true); // envelope must be enabled before it can be updated
                                e.setName(Utilities.renameContainer(e.getName()));
                                e.setEnabled(false); // return envelope to disabled state
                            }
                        }
                        // create acct
                        cat = DBMS.addCategory(getToken(3).getPossibilities());
                        
                        if(cat==null) {
                            commandResponse = "Error: could not create '" + getToken(3).getPossibilities() + "'.";
                        } else {
                            commandResponse = "Category '" + cat.getName() + "' successfully created.";
                        }
                        break;
                    case NEW        + "" + ENVELOPE + "" + WORD:
                        String envName = getToken(3).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(envName, true)) {
                            commandResponse = "The name '" + envName + "' is already in use.";
                            break;
                        } else if(DBMS.isContainer(envName, false)) {
                            //check disabled accts, and renames if exists
                            if(DBMS.isAccount(envName, false)) {
                                Account a = DBMS.getAccount(envName, false);
                                // rename old disabled account
                                a.setEnabled(true); // account must be enabled before it can be updated
                                a.setName(Utilities.renameContainer(a.getName()));
                                a.setEnabled(false); // return account to disabled state
                                // check disabled cats
                            } else if(DBMS.isCategory(envName, false)) {
                                Category c = DBMS.getCategory(envName, false);
                                // rename old disabled category
                                c.setEnabled(true); // category must be enabled before it can be updated
                                c.setName(Utilities.renameContainer(c.getName()));
                                c.setEnabled(false); // return category to disabled state
                                // check disabled envs
                            } else if(DBMS.isEnvelope(envName, false)) {
                                Envelope e = DBMS.getEnvelope(envName, false);
                                // rename old disabled envelope
                                e.setEnabled(true); // envelope must be enabled before it can be updated
                                e.setName(Utilities.renameContainer(e.getName()));
                                e.setEnabled(false); // return envelope to disabled state
                            }
                        }
                        // create acct
                        env = DBMS.addEnvelope(getToken(3).getPossibilities());
                        
                        if(env==null) {
                            commandResponse = "Error: could not create '" + getToken(3).getPossibilities() + "'.";
                        } else {
                            commandResponse = "Envelope '" + env.getName() + "' successfully created.";
                        }
                        break;
                    case REMOVE     + "" + ACCOUNT  + "" + WORD:
                        acct = DBMS.getAccount(getToken(3).getPossibilities(), true);
                        if(acct==null) {
                            commandResponse = "Account '" + getToken(3).getPossibilities() + "' does not exist.";
                        } else {
                            if(Utilities.roundAmount(acct.getAmt()).equalsIgnoreCase("0.00")) {
                                acct.setEnabled(false);
                                commandResponse = "Account '" + acct.getName() + "' successfully removed.";
                            } else {
                                commandResponse = "Account '" + acct.getName() + "' must be empty before removal. Remaining balance: " + Utilities.roundAmount(acct.getAmt());
                            }
                        }
                        break;
                    case REMOVE     + "" + CATEGORY + "" + WORD:
                        cat = DBMS.getCategory(getToken(3).getPossibilities(), true);
                        if(getToken(3).getPossibilities().equalsIgnoreCase("uncategorized")) {
                            commandResponse = "Category 'uncategorized' cannot be removed.";
                        } else if(cat==null) {
                            commandResponse = "Category '" + getToken(3).getPossibilities() + "' does not exist.";
                        } else {
                            envs = DBMS.getEnvelopes(cat, true);
                            for(Envelope e : envs) {
                                e.setCategory(null);
                            }
                            cat.setEnabled(false);
                            commandResponse = "Category '" + cat.getName() + "' successfully removed.";
                        }
                        break;
                    case REMOVE     + "" + ENVELOPE + "" + WORD:
                        env = DBMS.getEnvelope(getToken(3).getPossibilities(), true);
                        if(env==null) {
                            commandResponse = "Envelope '" + getToken(3).getPossibilities() + "' does not exist.";
                        } else {
                            if(Utilities.roundAmount(env.getAmt()).equalsIgnoreCase("0.00")) {
                                env.setEnabled(false);
                                commandResponse = "Envelope '" + env.getName() + "' successfully removed.";
                            } else {
                                commandResponse = "Envelope '" + env.getName() + "' must be empty before removal. Remaining balance: " + Utilities.roundAmount(env.getAmt());
                            }
                        }
                        break;
                    case REMOVE     + "" + USER     + "" + WORD:
                        usr = DBMS.getUser(getToken(3).getPossibilities(), true);
                        if(usr==null) {
                            commandResponse = "User '" + getToken(3).getPossibilities() + "' does not exist.";
                        } else if(usr.getUsername().equalsIgnoreCase(user.getUsername())) {
                            commandResponse = "You cannot remove yourself.";
                        } else if(usr.isAdmin()) {
                            commandResponse = "Admin account cannot be removed.";
                        } else if(usr.isGmail()) {
                            commandResponse = "Gmail account cannot be removed.";
                        } else {
                            usr.setEnabled(false);
                            commandResponse = "User '" + usr.getUsername() + "' successfully removed.";
                        }
                        break;
                    case ACCT        + "" + ACCT      + "" + EXP: return acctAcctExp(getToken(1).getPossibilities(),getToken(2).getPossibilities(),getToken(3).getPossibilities());
                    case ACCT        + "" + ENV       + "" + EXP: return acctEnvExp(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    case ENV       + "" + ENV         + "" + EXP: return envEnvExp(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    case ENV       + "" + EXP   + "" + WORD: return envExpWord(getToken(1).getPossibilities(), getToken(2).getPossibilities());
                    case HISTORY    + "" + ACCT      + "" + DATE       + "" + DATE:
                        acct = DBMS.getAccount(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(acct, getToken(3).getPossibilities(), getToken(4).getPossibilities());
                        commandResponse = "ACCOUNT (" + acct.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | envelope | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case HISTORY    + "" + CAT     + "" + DATE       + "" + DATE:
                        cat = DBMS.getCategory(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(cat, getToken(3).getPossibilities(), getToken(4).getPossibilities());
                        commandResponse = "CATEGORY (" + cat.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | account | envelope | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getAcct().getName() + " | " + tran.getEnv().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case HISTORY    + "" + ENV     + "" + DATE       + "" + DATE:
                        env = DBMS.getEnvelope(getToken(2).getPossibilities(), true);
                        trans = DBMS.getTransactions(env, getToken(3).getPossibilities(), getToken(4).getPossibilities());
                        commandResponse = "ENVELOPE (" + env.getName() + ") TRANSACTIONS:\n"
                                + "date | amount | description | account | user";
                        for(Transaction tran : trans) {
                            commandResponse += "\n" + tran.getDate() + " | " + Utilities.roundAmount(tran.getAmt()) + " | " + tran.getDesc() + " | " + tran.getAcct().getName() + " | " + tran.getUser().getUsername();
                        }
                        break;
                    case NEW        + "" + ENVELOPE + "" + WORD       + "" + CAT:
                        env = DBMS.newEnvelope(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                        if(env==null) {
                            commandResponse = "The name '" + getToken(3).getPossibilities() + "' is already in use.";
                        } else {
                            commandResponse = "Envelope '" + env.getName() + "' successfully created.";
                        }
                        break;
                    case NEW        + "" + USER     + "" + WORD       + "" + WORD:
                        usr = DBMS.addUser(getToken(3).getPossibilities(), getToken(4).getPossibilities());
                        if(usr==null) {
                            commandResponse = "The username '" + getToken(3).getPossibilities() + "' is already in use.";
                        } else {
                            commandResponse = "User '" + usr.getUsername() + "' successfully created.";
                        }
                        break;
                    case RENAME     + "" + ACCOUNT  + "" + WORD       + "" + WORD:
                        acct = DBMS.getAccount(getToken(3).getPossibilities(), true);
                        String newName = getToken(4).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(newName, true)) {
                            commandResponse = "The name '" + newName + "' is already in use.";
                        } else {
                            if(acct==null) {
                                commandResponse = "Account '" + getToken(3).getPossibilities() + "' does not exist.";
                            } else {
                                //check disabled accts
                                if(DBMS.isAccount(newName, false)) {
                                    Account a = DBMS.getAccount(newName, false);
                                    // rename old disabled account
                                    a.setEnabled(true); // account must be enabled before it can be updated
                                    a.setName(Utilities.renameContainer(a.getName()));
                                    a.setEnabled(false); // return account to disabled state
                                } else if(DBMS.isCategory(newName, false)) {
                                    Category c = DBMS.getCategory(newName, false);
                                    // rename old disabled category
                                    c.setEnabled(true); // category must be enabled before it can be updated
                                    c.setName(Utilities.renameContainer(c.getName()));
                                    c.setEnabled(false); // return category to disabled state
                                } else if(DBMS.isEnvelope(newName, false)) {
                                    Envelope e = DBMS.getEnvelope(newName, false);
                                    // rename old disabled envelope
                                    e.setEnabled(true); // envelope must be enabled before it can be updated
                                    e.setName(Utilities.renameContainer(e.getName()));
                                    e.setEnabled(false); // return envelope to disabled state
                                }
                                commandResponse = DBMS.updateAccountName(acct.getId(), newName);
                                acct.setName(newName);
                            }
                        }
                        break;
                    case RENAME     + "" + CATEGORY + "" + WORD       + "" + WORD:
                        cat = DBMS.getCategory(getToken(3).getPossibilities(), true);
                        String newName2 = getToken(4).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(newName2, true)) {
                            commandResponse = "The name '" + newName2 + "' is already in use.";
                        } else {
                            if(cat==null) {
                                commandResponse = "Category '" + getToken(3).getPossibilities() + "' does not exist.";
                            } else {
                                //check disabled accts
                                if(DBMS.isAccount(newName2, false)) {
                                    Account a = DBMS.getAccount(newName2, false);
                                    // rename old disabled account
                                    a.setEnabled(true); // account must be enabled before it can be updated
                                    a.setName(Utilities.renameContainer(a.getName()));
                                    a.setEnabled(false); // return account to disabled state
                                } else if(DBMS.isCategory(newName2, false)) {
                                    Category c = DBMS.getCategory(newName2, false);
                                    // rename old disabled category
                                    c.setEnabled(true); // category must be enabled before it can be updated
                                    c.setName(Utilities.renameContainer(c.getName()));
                                    c.setEnabled(false); // return category to disabled state
                                } else if(DBMS.isEnvelope(newName2, false)) {
                                    Envelope e = DBMS.getEnvelope(newName2, false);
                                    // rename old disabled envelope
                                    e.setEnabled(true); // envelope must be enabled before it can be updated
                                    e.setName(Utilities.renameContainer(e.getName()));
                                    e.setEnabled(false); // return envelope to disabled state
                                }
                                commandResponse = cat.setName(newName2);
                            }
                        }
                        break;
                    case RENAME     + "" + ENVELOPE + "" + WORD       + "" + WORD:
                        env = DBMS.getEnvelope(getToken(3).getPossibilities(), true);
                        String newName3 = getToken(4).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isContainer(newName3, true)) {
                            commandResponse = "The name '" + newName3 + "' is already in use.";
                        } else {
                            if(env==null) {
                                commandResponse = "Envelope '" + getToken(3).getPossibilities() + "' does not exist.";
                            } else {
                                //check disabled accts
                                if(DBMS.isAccount(newName3, false)) {
                                    Account a = DBMS.getAccount(newName3, false);
                                    // rename old disabled account
                                    a.setEnabled(true); // account must be enabled before it can be updated
                                    a.setName(Utilities.renameContainer(a.getName()));
                                    a.setEnabled(false); // return account to disabled state
                                } else if(DBMS.isCategory(newName3, false)) {
                                    Category c = DBMS.getCategory(newName3, false);
                                    // rename old disabled category
                                    c.setEnabled(true); // category must be enabled before it can be updated
                                    c.setName(Utilities.renameContainer(c.getName()));
                                    c.setEnabled(false); // return category to disabled state
                                } else if(DBMS.isEnvelope(newName3, false)) {
                                    Envelope e = DBMS.getEnvelope(newName3, false);
                                    // rename old disabled envelope
                                    e.setEnabled(true); // envelope must be enabled before it can be updated
                                    e.setName(Utilities.renameContainer(e.getName()));
                                    e.setEnabled(false); // return envelope to disabled state
                                }
                                commandResponse = env.setName(newName3);
                            }
                        }
                        break;
                    case RENAME     + "" + USER     + "" + WORD       + "" + WORD:
                        usr = DBMS.getUser(getToken(3).getPossibilities(), true);
                        String newName4 = getToken(4).getPossibilities();
                        // checks to see if new name is already in database
                        if(DBMS.isUser(newName4, true)) {
                            commandResponse = "The name '" + newName4 + "' is already in use.";
                        } else {
                            if(usr==null) {
                                commandResponse = "User '" + getToken(3).getPossibilities() + "' does not exist.";
                            } else {
                                //check disabled accts
                                if(DBMS.isUser(newName4, false)) {
                                    User u = DBMS.getUser(newName4, false);
                                    // rename old disabled account
                                    u.setEnabled(true); // account must be enabled before it can be updated
                                    u.setUsername(Utilities.renameUser(u.getUsername()));
                                    u.setEnabled(false); // return account to disabled state
                                }
                                commandResponse = usr.setUsername(newName4);
                            }
                        }
                        break;
                    case ACCT        + "" + ENV     + "" + EXP + "" + WORD: return acctEnvExpWord(getToken(1).getPossibilities(), getToken(2).getPossibilities(), getToken(3).getPossibilities());
                    default:
                        commandResponse = "Invalid command. Send 'help' for usages.";
                        break;
                }
            }
            return commandResponse;
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
            commandType += tmp.getTokenType();
        }
        
        private String getCommandType() {
            return commandType;
        }
        
        private final class Token {
            boolean exactMatch = false;
            Token next, prev;                 // used for linked list
            LinkedList<String> possibilities; // stores matching possibilites
            char tokenType;                         // stores token type
            
            void setInput(String input) {
                possibilities = new LinkedList();
                tokenType = EMPTY; // defaults to empty
                if(input.length()>0) {
                    out: while(true) {
                        if(tokenCount==0) { // this is the first token
                            input = input.toLowerCase();
                            // possible reserve words for first token
                            String[] res = {"accounts", "categories", "change", "envelopes", "help", "history", "new", "remove", "rename", "users", "uncategorized"};
                            // adds any reserve words that match given commandsInput
                            for(String str : res) {
                                if(str.startsWith(input)) {
                                    possibilities.add(str);
                                    tokenType = getReserveWordType(possibilities.peek());
                                    if(str.equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            }
                            // gets any accounts, categories, or envelopes that match given commandsInput...
                            LinkedList<Object> objs = DBMS.getContainers(input, true);
                            // ...and then addes them to the possibilities list
                            for(Object obj : objs) {
                                if(obj instanceof Account) {
                                    possibilities.add(((Account) obj).getName());
                                    // change type to account
                                    tokenType = ACCT;
                                    if(((Account) obj).getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                } else if(obj instanceof Category) {
                                    possibilities.add(((Category) obj).getName());
                                    // change type to category
                                    tokenType = CAT;
                                    if(((Category) obj).getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                } else if(obj instanceof Envelope) {
                                    possibilities.add(((Envelope) obj).getName());
                                    // change type to envelope
                                    tokenType = ENV;
                                    if(((Envelope) obj).getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            }
                        } else if(tokenCount==1) { // this is the second token
                            if(prev.tokenType==ACCT) {
                                input = input.toLowerCase();
                                // checks for accounts
                                LinkedList<Account> accts = DBMS.getAccounts(input, true);
                                for(Account acct : accts) {
                                    possibilities.add(acct.getName());
                                    // change type to account
                                    tokenType = ACCT;
                                    if(acct.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for envelopes
                                LinkedList<Envelope> envs = DBMS.getEnvelopes(input, true);
                                for(Envelope env : envs) {
                                    possibilities.add(env.getName());
                                    // change type to envelope
                                    tokenType = ENV;
                                    if(env.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            } else if(prev.tokenType==ENV) {
                                input = input.toLowerCase();
                                if("uncategorized".startsWith(input)) {
                                    possibilities.add("uncategorized");
                                    tokenType = UNCATEGORIZED;
                                    if("uncategorized".equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for categories
                                LinkedList<Category> cats = DBMS.getCategories(input, true);
                                for(Category cat : cats) {
                                    possibilities.add(cat.getName());
                                    // change type to envelope
                                    tokenType = CAT;
                                    if(cat.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for envelopes
                                LinkedList<Envelope> envs = DBMS.getEnvelopes(input, true);
                                for(Envelope env : envs) {
                                    possibilities.add(env.getName());
                                    // change type to envelope
                                    tokenType = ENV;
                                    if(env.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for expression
                                try {
                                    double solution = Utilities.evaluate(input);
                                    possibilities.add(Double.toString(solution));
                                    tokenType = EXP;
                                    if(Double.toString(solution).equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                } catch (Exception e) {}
                            } else if(prev.tokenType==CHANGE) {
                                input = input.toLowerCase();
                                if("password".startsWith(input)) {
                                    possibilities.add("password");
                                    tokenType = PASSWORD;
                                    if("password".equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            } else if(prev.tokenType==HISTORY) {
                                // checks for quantity
                                try {
                                    int qty = Integer.parseInt(input);
                                    if(qty>=0) {
                                        possibilities.add(input);
                                        tokenType = QTY;
                                        break out;
                                    }
                                } catch(Exception e1) {}
                                input = input.toLowerCase();
                                // checks for accounts
                                LinkedList<Account> accts = DBMS.getAccounts(input, true);
                                for(Account acct : accts) {
                                    possibilities.add(acct.getName());
                                    // change type to account
                                    tokenType = ACCT;
                                    if(acct.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for categories
                                LinkedList<Category> cats = DBMS.getCategories(input, true);
                                for(Category cat : cats) {
                                    possibilities.add(cat.getName());
                                    // change type to envelope
                                    tokenType = CAT;
                                    if(cat.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                                // checks for envelopes
                                LinkedList<Envelope> envs = DBMS.getEnvelopes(input, true);
                                for(Envelope env : envs) {
                                    possibilities.add(env.getName());
                                    // change type to envelope
                                    tokenType = ENV;
                                    if(env.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            } else if(prev.tokenType==NEW || prev.tokenType==REMOVE || prev.tokenType==RENAME) {
                                input = input.toLowerCase();
                                // possible reserve words
                                String[] res = {"account", "category", "envelope", "user"};
                                // adds any reserve words that match given commandsInput
                                for(String str : res) {
                                    if(str.startsWith(input)) {
                                        possibilities.add(str);
                                        tokenType = getReserveWordType(possibilities.peek());
                                        if(str.equalsIgnoreCase(input)) {
                                            break out;
                                        }
                                    }
                                }
                            }
                        } else if(tokenCount==2) { // this is the third token
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
                                    } else {
                                        // checks for expression
                                        try {
                                            double solution = Utilities.evaluate(input);
                                            possibilities.add(Double.toString(solution));
                                            tokenType = EXP;
                                            if(Double.toString(solution).equalsIgnoreCase(input)) {
                                                break out;
                                            }
                                        } catch (Exception e2) {}
                                    }
                                } catch(Exception e1) {
                                    // checks for expression
                                    try {
                                        double solution = Utilities.evaluate(input);
                                        possibilities.add(Double.toString(solution));
                                        tokenType = EXP;
                                        if(Double.toString(solution).equalsIgnoreCase(input)) {
                                            break out;
                                        }
                                    } catch (Exception e2) {}
                                }
                            } else if(prev.tokenType==CAT) {
                                input = input.toLowerCase();
                                // checks for quantity
                                try {
                                    int qty = Integer.parseInt(input);
                                    if(qty>=0 && prev.prev.tokenType==HISTORY) {
                                        possibilities.add(input);
                                        tokenType = QTY;
                                    }
                                } catch(Exception e1) {}
                                // checks for date
                                if(Utilities.isDate(input)) {
                                    possibilities.add(input);
                                    tokenType = DATE;
                                }
                            }
                        } else if(tokenCount==3) { // this is the fourth token
                            if(prev.tokenType==DATE) {
                                // checks for date
                                if(Utilities.isDate(input)) {
                                    possibilities.add(input);
                                    tokenType = DATE;
                                }
                            } else if(prev.tokenType==WORD && prev.prev.tokenType==ENVELOPE && prev.prev.prev.tokenType==NEW) {
                                input = input.toLowerCase();
                                // checks for categories
                                LinkedList<Category> cats = DBMS.getCategories(input, true);
                                for(Category cat : cats) {
                                    possibilities.add(cat.getName());
                                    // change type to envelope
                                    tokenType = CAT;
                                    if(cat.getName().equalsIgnoreCase(input)) {
                                        break out;
                                    }
                                }
                            }
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
