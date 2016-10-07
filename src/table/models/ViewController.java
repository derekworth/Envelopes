/*
* The purpose of this controller is to efficiently manage and update the tables
* and dropdown menus for the console.
*/

package table.models;

import database.DBMS;
import javax.swing.SwingUtilities;
import server.local.Console;

/**
 *
 * @author Derek
 */
public class ViewController {
    public Console con;
    private final EnvelopesTableModel    envelopesTM    = new EnvelopesTableModel(this);
    private final AccountsTableModel     accountsTM     = new AccountsTableModel();
    private final TransactionsTableModel transactionsTM = new TransactionsTableModel(this);
    private final EmailTableModel        emailTM        = new EmailTableModel();
    
    public ViewController(Console console) {
        con = console;
        initializeTables();
    }
    
    private void initializeTables() {        
            // initialized envelopes table
            con.envelopesTable.setModel(envelopesTM);
            con.envelopesTable.getColumnModel().getColumn(0).setCellRenderer(envelopesTM.getBoldRenderer());
            con.envelopesTable.getColumnModel().getColumn(1).setCellRenderer(envelopesTM.getBoldRenderer());
            con.envelopesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            // initialize accounts table
            con.accountsTable.setModel(accountsTM);
            con.accountsTable.getColumnModel().getColumn(0).setCellRenderer(accountsTM.getBoldRenderer());
            con.accountsTable.getColumnModel().getColumn(1).setCellRenderer(accountsTM.getBoldRenderer());
            con.accountsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
            // initialize transactions table
            con.transactionsTable.setModel(transactionsTM);
            con.transactionsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
            con.transactionsTable.getColumnModel().getColumn(0).setMaxWidth(80);
            con.transactionsTable.getColumnModel().getColumn(1).setPreferredWidth(240);
            con.transactionsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
            con.transactionsTable.getColumnModel().getColumn(2).setMaxWidth(120);
            con.transactionsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
            con.transactionsTable.getColumnModel().getColumn(3).setMaxWidth(120);
            con.transactionsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
            con.transactionsTable.getColumnModel().getColumn(4).setMaxWidth(120);
            for(int i = 0; i < con.transactionsTable.getColumnCount(); i++) {
                con.transactionsTable.getColumnModel().getColumn(i).setCellRenderer(transactionsTM.getRenderer());
            }
            // initialize email table
            con.emailTable.setModel(emailTM);
            con.emailTable.getColumnModel().getColumn(0).setMaxWidth(130);
            con.emailTable.getColumnModel().getColumn(0).setPreferredWidth(130);
            con.emailTable.getColumnModel().getColumn(1).setMaxWidth(130);
            con.emailTable.getColumnModel().getColumn(1).setPreferredWidth(130);
            con.emailTable.getColumnModel().getColumn(2).setMaxWidth(60);
            con.emailTable.getColumnModel().getColumn(2).setPreferredWidth(60);
            con.emailTable.getColumnModel().getColumn(3).setMaxWidth(150);
            con.emailTable.getColumnModel().getColumn(3).setPreferredWidth(80);
            for(int i = 0; i < con.emailTable.getColumnCount(); i++) {
                con.emailTable.getColumnModel().getColumn(i).setCellRenderer(emailTM.getRenderer());
            }
    }
    
    public final void updateAccountTable() {
        accountsTM.refresh();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                con.accountsTable.updateUI();
            }
        });
    }
    
    public final void updateEnvelopeTable() {
        envelopesTM.refresh();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                con.envelopesTable.updateUI();
            }
        });
    }
    
    public final void updateTransactionTable() {
        DBMS.removeZeroAmtTransactions();
        transactionsTM.refresh();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                con.transactionsTable.updateUI();
            }
        });
    }
    
    public final void updateTransactionTableWithMoreTransactions() {
        DBMS.removeZeroAmtTransactions();
        transactionsTM.addMore();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                con.transactionsTable.updateUI();
            }
        });
    }
    
    public final void updateEmailTable() {
        emailTM.refresh();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                con.emailTable.updateUI();
            }
        });
    }    
    
    public final void updateSelectedAmount(char type, String acctSelected, String envSelected) {
        // set selection; type: a = account; thisConsole = category; e = envelope; u = user; n = none
        if(type == 'a') {
            transactionsTM.setAccountForQuery(acctSelected);
        } else if(type == 'e') {
            transactionsTM.setEnvelopeForQuery(envSelected);
        }
    }
    
    public final void exportTransactions(String fileName) {
        transactionsTM.export(fileName);
    }
    
    public final void allowEditing(boolean isAllowed) {
        accountsTM.setEditing(isAllowed);
        envelopesTM.setEditing(isAllowed);
        transactionsTM.setEditing(isAllowed);
    }
}