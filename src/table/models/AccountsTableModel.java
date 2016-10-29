package table.models;

import database.Account;
import database.Category;
import javax.swing.table.TableModel;
import database.Model;
import database.Envelope;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import misc.Utilities;

/**
 * Created on Sep 22, 2013
 * @author Derek Worth
 */
public final class AccountsTableModel implements TableModel {
    
    boolean canEdit;
    LinkedList<Object> containers;
    Object[] columnNames = {"Account", "Amount"};
    
    public AccountsTableModel() {
        setEditing(false);
        refresh();
    }
    
    public void refresh() {
        containers = new LinkedList();
        LinkedList<Account> accts = Model.getAccounts(true);
        for(Account acct : accts) {
            containers.add(acct);
        }
        containers.add(new Total());
    }
    
    public void setEditing(boolean onOff) {
        canEdit = onOff;
    }
    
    @Override
    public int getRowCount() {
        return containers.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Object container = containers.get(row);
        if(container instanceof Account) {
            if(col==0) {
                return ((Account) container).getName();
            } else {
                return Utilities.addCommasToAmount(((Account) container).getAmount());
            }
        } else {
            if(col==0) {
                return "total".toUpperCase();
            } else {
                return Utilities.addCommasToAmount(((Total) container).getAmt());
            }
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
        Object container = containers.get(row);
        if(container instanceof String) {
            return false;
        }
        if(col==0)
            return true && canEdit;
        else
            return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        String newName = (String) aValue;
        if(newName==null || newName.length()==0 || !Utilities.isValidContainerName(newName)) {
            return;
        }
        Object container = containers.get(row);
        // rename disabled container if exists
        if(Model.isContainer(newName, false)) {      // name already in use by enabled or disabled container
            if(!Model.isContainer(newName, true)) {   // name already in use by disabled container
                // renames disabled container that matches the specified name
                if(Model.isAccount(newName, false)) {
                    Account disabledAcct = Model.getAccount(newName, false);
                    disabledAcct.setEnabled(true);
                    disabledAcct.setName(Utilities.renameContainer(newName));
                    disabledAcct.setEnabled(false);
                } else if(Model.isCategory(newName, false)) {
                    Category disabledCat = Model.getCategory(newName, false);
                    disabledCat.setEnabled(true);
                    disabledCat.setName(Utilities.renameContainer(newName));
                    disabledCat.setEnabled(false);
                } else if (Model.isEnvelope(newName, false)) {
                    Envelope disabledEnv = Model.getEnvelope(newName, false);
                    disabledEnv.setEnabled(true);
                    disabledEnv.setName(Utilities.renameContainer(newName));
                    disabledEnv.setEnabled(false);
                }
            }
        }
        if(!Model.isContainer(newName, true)) {
            if(container instanceof Account && col==0) {
                ((Account) container).setName(newName);
            }
        }
    }

    @Override
    public void addTableModelListener(TableModelListener tl) {
        
    }

    @Override
    public void removeTableModelListener(TableModelListener tl) {
    
    }
    
    public BoldTableCellRenderer getBoldRenderer() {
        return new BoldTableCellRenderer();
    }
    
    public class Total {
        double amt;
        public Total() {
            // queries accounts total from the database
            amt = Model.getAccountsTotal();
        }
        // returns stored accounts total
        public double getAmt() {
            return amt;
        }
    }
    
    public class BoldTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
            
            // set alignment
            if(column==0) {
                setHorizontalAlignment(JLabel.LEFT);
            } else {
                setHorizontalAlignment(JLabel.RIGHT);
            }
            
            // set back/foreground
            if(containers.get(row) instanceof Total){
                cell.setBackground(Color.LIGHT_GRAY);
                cell.setForeground(Color.BLACK);
                cell.setFont(new Font("", Font.BOLD, 12));
            } else {
                Double amtDouble = ((Account)containers.get(row)).getAmount();
                String amtString = Utilities.roundAmount(amtDouble);
                // amount is negative
                if(Double.parseDouble(amtString)<0) {
                    cell.setBackground(new Color(255,222,222));
                    cell.setForeground(Color.BLACK);
                } else {
                    // reset cells to normal (no highlights)
                    cell.setBackground(javax.swing.UIManager.getColor("Table.dropCellForeground"));
                    cell.setForeground(javax.swing.UIManager.getColor("Table.dropCellForeground"));
                }
            }
            
            return cell;
        }
    }
}
