package table.models;

import database.Account;
import database.Category;
import database.Envelope;
import javax.swing.table.TableModel;
import database.DBMS;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
public final class EnvelopesTableModel implements TableModel {
    
    boolean canEdit = false;
    
    LinkedList<Object> containers;
    
    Object[] columnNames = {"Envelope", "Amount"};
    Console con;
    
    public EnvelopesTableModel(Console c) {
        setEditing(false);
        con = c;
        refresh();
    }
    
    public void refresh() {
        containers = new LinkedList();
        LinkedList<Envelope> envs;
        LinkedList<Category> cats;
        boolean isCategorized;
        if(con==null) {
            isCategorized = true;
        } else {
            isCategorized = con.categorizedCheckBox.isSelected();
        }
        if(isCategorized) {
            // add categories
            cats = DBMS.getCategories(true);
            // add envelopes for each category
            for(Category cat : cats) {
                containers.add(cat);
                envs = DBMS.getEnvelopes(cat, true);
                for(Envelope env : envs) {
                    containers.add(env);
                }
            }
            // add uncategorized envelopes
            envs = DBMS.getUncategorizedEnvelopes(true);
            if(envs.size()>0) {
                containers.add(new Total("uncategorized", DBMS.getUncategorizedTotal()));
                for(Envelope env : envs) {
                    containers.add(env);
                }
            }
        } else {
            envs = DBMS.getEnvelopes(true);
            for(Envelope env : envs) {
                containers.add(env);
            }
        }
        containers.add(new Total());
    }
    
    public void setEditing(boolean allowEditing) {
        canEdit = allowEditing;
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
        if(container instanceof Category) {
            if(col==0) {
                return ((Category) container).getName().toUpperCase();
            } else {
                return Utilities.addCommasToAmount(((Category) container).getAmount());
            }
        } else if(container instanceof Envelope) {
            if(col==0) {
                return ((Envelope) container).getName();
            } else {
                return Utilities.addCommasToAmount(((Envelope) container).getAmount());
            }
        } else {
            if(col==0) {
                return ((Total) container).getName().toUpperCase();
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
        if(DBMS.isContainer(newName, false)) {      // name already in use by enabled or disabled container
            if(!DBMS.isContainer(newName, true)) {   // name already in use by disabled container
                // renames disabled container that matches the specified name
                if(DBMS.isAccount(newName, false)) {
                    Account disabledAcct = DBMS.getAccount(newName, false);
                    disabledAcct.setEnabled(true);
                    disabledAcct.setName(Utilities.renameContainer(newName));
                    disabledAcct.setEnabled(false);
                } else if(DBMS.isCategory(newName, false)) {
                    Category disabledCat = DBMS.getCategory(newName, false);
                    disabledCat.setEnabled(true);
                    disabledCat.setName(Utilities.renameContainer(newName));
                    disabledCat.setEnabled(false);
                } else if (DBMS.isEnvelope(newName, false)) {
                    Envelope disabledEnv = DBMS.getEnvelope(newName, false);
                    disabledEnv.setEnabled(true);
                    disabledEnv.setName(Utilities.renameContainer(newName));
                    disabledEnv.setEnabled(false);
                }
            }
        }
        if(!DBMS.isContainer(newName, true)) {
            if(container instanceof Envelope && col==0) {
                ((Envelope) container).setName(newName);
            } else if (container instanceof Category && col==0) {
                ((Category) container).setName(newName);
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
        String name;
        // constructor for category total
        public Total(String name, double amt) {
            this.amt = amt;
            this.name = name;
        }
        // constructor for envelopes total
        public Total() {
            // queries accounts total from the database
            amt = DBMS.getEnvelopesTotal();
            name = "total";
        }
        // returns stored envelopes/category total
        public double getAmt() {
            return amt;
        }
        // return total title
        public String getName() {
            return name;
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
            if(containers.get(row) instanceof Category || containers.get(row) instanceof Total){
                cell.setBackground(Color.LIGHT_GRAY);
                cell.setForeground(Color.BLACK);
                cell.setFont(new Font("", Font.BOLD, 12));
            } else {
                Double amtDouble = ((Envelope)containers.get(row)).getAmount();
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
