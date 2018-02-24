package model.tables;

import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
public final class AccountsTableModel implements TableModel {
    
    boolean canEdit;
    Object[] columnNames = {"Account", "Amount"};
    private final ModelController mc;
    
    public AccountsTableModel(ModelController mc) {
        canEdit = false;
        this.mc = mc;
    }
    
    public void setEditing(boolean canEdit) {
        this.canEdit = canEdit;
    }
    
    @Override
    public int getRowCount() {
        return mc.getAccountCount();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if(col==0) {
            return mc.getAccountName(row);
        } else {
            return mc.getAccountAmount(row);
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
        return mc.isAccount(row) && canEdit && col==0;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        String newName = (String) aValue;
        mc.renameAccount(mc.getAccountName(row), newName);
    }

    @Override
    public void addTableModelListener(TableModelListener tl) {
        // do nothing
    }

    @Override
    public void removeTableModelListener(TableModelListener tl) {
        // do nothing
    }
    
    public BoldTableCellRenderer getBoldRenderer() {
        return new BoldTableCellRenderer();
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
            if(!mc.isAccount(row)) {
                cell.setBackground(Color.LIGHT_GRAY);
                cell.setForeground(Color.BLACK);
                cell.setFont(new Font("", Font.BOLD, 12));
            } else {
                // amount is negative
                if(Utilities.amountToInteger(mc.getAccountAmount(row))<0) {
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
