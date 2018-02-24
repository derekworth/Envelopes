package model.tables;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import model.ModelController;

/**
 * Created on Oct 12, 2013
 * @author Derek Worth
 */
public final class EmailTableModel implements TableModel {
    
    private final ModelController mc;
    private final Object[] columnNames = {"Created", "Modified", "Attempt", "User", "Address"};
    
    public EmailTableModel(ModelController mc) {
        this.mc = mc;
    }
    
    @Override
    public int getRowCount() {
        return mc.getEmailCount();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return mc.getEmailCreated(row);
            case 1:
                return mc.getEmailModified(row);
            case 2:
                return mc.getEmailAttempt(row);
            case 3:
                return mc.getEmailUsername(row);
            default:
                return mc.getEmailAddress(row);
        }
    }

    @Override
    public String getColumnName(int i) {
        return (String) columnNames[i];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if(col==2) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        // to nothing (no editing allowed)
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
    
    // "Created", "Modified", "Attempt Count", "User", "Address"
    public class TableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.LEFT);
            return cell;
        }
    }
}