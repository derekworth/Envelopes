package model.tables;

import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import misc.Utilities;
import model.ModelController;

/**
 * Created on Sep 22, 2013
 * @author Derek Worth
 */
public final class TransactionsTableModel implements TableModel {
    
    boolean canEdit;
    private final ModelController mc;
    
    Object[] columnNames = {"Date", "Description", "(+)", "(-)", "Run Tot", "Account", "Envelope", "User"};
    
    public TransactionsTableModel(ModelController mc) {
        canEdit = false;
        this.mc = mc;
    }
    
    public void setEditing(boolean allowEditing) {
        canEdit = allowEditing;
    }
    
    @Override
    public int getRowCount() {
        return mc.getTransactionCount();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:                                        // DATE
                return mc.getTransactionDate(row);
            case 1:                                        // DESC
                return Utilities.removeDoubleApostrophes(mc.getTransactionDesc(row));
            case 2:                                        // +AMT
                if(mc.getTransactionAmountInteger(row)>0) {
                    return mc.getTransactionAmountString(row);
                }
                return "";
            case 3:                                        // -AMT
                if(mc.getTransactionAmountInteger(row)<0) {
                    return mc.getTransactionAmountString(row);
                }
                return "";
            case 4:                                        // RUN TOT
                return mc.getTransactionRunTotal(row);
            case 5:                                        // ACCOUNT
                return mc.getTransactionAccount(row);
            case 6:                                        // ENVELOPE
                return mc.getTransactionEnvelope(row);
            default:                                       // USER
                return mc.getTransactionUser(row);
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
            return canEdit;
        else
            return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        switch (col) {
            case 0:
                // DATE
                mc.setTransactionDate(row, (String) aValue);
                break;
            case 1:
                // DESC
                mc.setTransactionDesc(row, (String) aValue);
                break;
            case 2:
            case 3:
                // AMT
                int amt = Utilities.amountToInteger((String) aValue);
                mc.setTransactionAmount(row, amt);
                break;
            case 5:
                // ACCOUNT
                mc.setTransactionAccount(row, (String) aValue);
                break;
            case 6:
                // ENVELOPE
                mc.setTransactionEnvelope(row, (String) aValue);
                break;
            default:
                break;
        }
    }

    @Override
    public void addTableModelListener(TableModelListener tl) {
        // do nothing
    }

    @Override
    public void removeTableModelListener(TableModelListener tl) {
        // do nothing
    }
    
    public TableCellRenderer getRenderer() {
        return new TableCellRenderer();
    }

    public boolean export(String filename) {
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
            for(int i = 0; i < mc.getTransactionCount(); i++) {
                String date, desc, amt, acct, cat, env, usr;
                date = mc.getTransactionDate(i);
                desc = mc.getTransactionDesc(i);
                amt  = Utilities.amountToStringSimple(mc.getTransactionAmountString(i));
                acct = mc.getTransactionAccount(i);
                env  = mc.getTransactionEnvelope(i);
                cat  = mc.getTransactionCategory(i);
                usr  = mc.getTransactionUser(i);
                writer.write("\n" + date + "," + desc + "," + amt + "," + acct + "," + cat + "," + env + "," + usr);
            }
            writer.flush();
            return true;
        } catch (IOException ex) {
            return false;
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
