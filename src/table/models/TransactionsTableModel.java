package table.models;

import javax.swing.table.TableModel;
import database.DBMS;
import database.Transaction;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import misc.Utilities;
import server.local.Console;

/**
 * Created on Sep 22, 2013
 * @author Derek Worth
 */
public final class TransactionsTableModel implements TableModel {
    
    boolean canEdit = false;
    String accountForQuery, categoryForQuery, envelopeForQuery, userForQuery;
    
    LinkedList<Transaction> transactions;
    
    Object[] columnNames = {"Date", "Description", "(+)", "(-)", "Run Tot", "Account", "Envelope", "User"};
    Console con;
    
    public TransactionsTableModel(Console c) {
        setEditing(false);
        accountForQuery = "";
        categoryForQuery = "";
        envelopeForQuery = "";
        userForQuery = "";
        con = c;
        refresh();
    }
    
    public void refresh() {
        boolean byDateRange, hideTx;
        int qty;
        if(con==null) {
            byDateRange = false;
            hideTx = false;
            qty = 35;
        } else {
            byDateRange = con.dateRangeCheckBox.isSelected();
            hideTx = con.hideTransfersToggleButton.isSelected();
            try{
                qty = Integer.parseInt(con.transactionQtyButton.getText());
            } catch(NumberFormatException ex) { // returns no transactions if transaction count is not an integer
                qty = 0;
            }
        }
        if(byDateRange) {
            String from = con.transFromField.getText();
            String to = con.transToField.getText();
            transactions = new LinkedList();
            if(from==null || to==null || !Utilities.isDate(from) || !Utilities.isDate(to))
                transactions = DBMS.getTransactions(0);
            else
                transactions = DBMS.getTransactions(from, to, DBMS.getAccount(accountForQuery, true), DBMS.getEnvelope(envelopeForQuery, true), hideTx);
        } else {
            transactions = DBMS.getTransactions(qty, DBMS.getAccount(accountForQuery, true), DBMS.getEnvelope(envelopeForQuery, true), hideTx);
        }
    }
    
    public void addMore() {
        boolean hideTx = con.hideTransfersToggleButton.isSelected();
        int qty = transactions.size();
        LinkedList<Transaction> moreTrans = DBMS.getMoreTransactions(qty, DBMS.getAccount(accountForQuery, true), DBMS.getEnvelope(envelopeForQuery, true), hideTx, transactions.getLast());
        while(!moreTrans.isEmpty()) {
            transactions.add(moreTrans.remove());
        }
    }
    
    public void setAccountForQuery(String name) {
        if(DBMS.isAccount(name, true)) {
            accountForQuery = name;
        } else {
            accountForQuery = "";
        }
    }
    
    public void setCategoryForQuery(String name) {
        categoryForQuery = "";
    }
    
    public void setEnvelopeForQuery(String name) {
        if(DBMS.isEnvelope(name, true)) {
            envelopeForQuery = name;
        } else {
            envelopeForQuery = "";
        }
    }
    
    public void setUserForQuery(String name) {
        userForQuery = "";
    }
    
    public void setEditing(boolean allowEditing) {
        canEdit = allowEditing;
    }
    
    @Override
    public int getRowCount() {
        return transactions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Transaction tran = transactions.get(row);
        if(col==0) {        // DATE
            return tran.getDate();
        } else if(col==1) { // DESC
            return Utilities.removeDoubleApostrophes(tran.getDesc());
        } else if(col==2) { // +AMT
            if(tran.getAmt()>0) {
                return Utilities.addCommasToAmount(tran.getAmt());
            }
            return "";
        } else if(col==3) { // -AMT
            if(tran.getAmt()<0) {
                return Utilities.addCommasToAmount(-tran.getAmt());
            }
            return "";
        } else if(col==4) { // RUN TOT
            return tran.getRunTot();
        } else if(col==5) { // ACCOUNT
            return tran.getAcct().getName();
        } else if(col==6) { // ENVELOPE
            return tran.getEnv().getName();
        } else {            // USER
            return tran.getUser().getUsername();
        }
    }

    @Override
    public String getColumnName(int i) {
        return (String) columnNames[i];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if(col<=3 || col==5 || col==6)
            return true && canEdit;
        else
            return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        Transaction tran = transactions.get(row);
        if(col==0) {                                           // DATE
            tran.setDate((String) aValue);
        } else if(col==1) {                                    // DESC
            tran.setDescription((String) aValue);
        } else if(col==2) {                                    // +AMT
            String strAmt = (String) aValue;
            strAmt = strAmt.replaceAll(",", ""); // remove all commas
            try {
                double amt = Double.parseDouble(strAmt);
                tran.setAmount(amt);
            } catch(NumberFormatException ex) {
                // do not set value
            }
            con.updateAccountTable();
            con.updateEnvelopeTable();
        } else if(col==3) {                                    // -AMT
            String strAmt = (String) aValue;
            strAmt = strAmt.replaceAll(",", ""); // remove all commas
            try {
                double amt = Double.parseDouble(strAmt);
                tran.setAmount(-amt);
            } catch(NumberFormatException ex) {
                // do not set value
            }
            con.updateAccountTable();
            con.updateEnvelopeTable();
        } else if(col==5 && this.getValueAt(row, col)!=null) { // ACCOUNT
            tran.setAccount((String) aValue);
            con.updateAccountTable();
        } else if(col==6 && this.getValueAt(row, col)!=null) { // ENVELOPE
            tran.setEnvelope((String) aValue);
            con.updateEnvelopeTable();
        }
        con.updateTransactionTable();
    }

    @Override
    public void addTableModelListener(TableModelListener tl) {
        
    }

    @Override
    public void removeTableModelListener(TableModelListener tl) {
    
    }
    
    public TableCellRenderer getRenderer() {
        return new TableCellRenderer();
    }

    public void export(String filename) {
        // remove file extension so we can add it after incremental append to duplicates
        if(filename.endsWith(".csv")) {
            filename = filename.substring(0, filename.length()-4);
        }
        File f = new File(filename + ".csv");
        int count = 1;
        while(f.exists()) {
            f = new File(filename + "(" + count++ + ")" + ".csv");
        }
        try (FileWriter writer = new FileWriter(f)) {
            writer.write("Date,Description,Amount,Account,Category,Envelope,User");
            for(Transaction t : transactions) {
                String date, desc, amt, acct, cat, env, usr;
                date = t.getDate();
                desc = t.getDesc();
                amt = Utilities.roundAmount(t.getAmt());
                if(t.getAcct()==null || t.getAcct().getId()==-1) {
                    acct = "--";
                } else {
                    acct = t.getAcct().getName();
                }
                if(t.getEnv().getCat()==null || t.getEnv().getCat().getId()==-1) {
                    cat = "--";
                } else {
                    cat = t.getEnv().getCat().getName();
                }
                if(t.getEnv()==null || t.getEnv().getId()==-1) {
                    env = "--";
                } else {
                    env = t.getEnv().getName();
                }
                if(t.getUser()==null) {
                    usr = "--";
                } else {
                    usr = t.getUser().getUsername();
                }
                
                writer.write("\n" + date + "," + desc + "," + amt + "," + acct + "," + cat + "," + env + "," + usr);
            }
            writer.flush();
        } catch (IOException ex) {
        } 
    }
    
    public class TableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
            if(column==2 || column==3 || column==4) { // cash values should be right aligned
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }
            return cell;
        }
    }
}
