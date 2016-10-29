package table.models;

import database.Model;
import database.Email;
import java.awt.Component;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Created on Oct 12, 2013
 * @author Derek Worth
 */
public final class EmailTableModel implements TableModel {
    
    LinkedList<Email> emails;
    
    Object[] columnNames = {"Created", "Modified", "Attempt", "User", "Address"};
    
    public EmailTableModel() {
        refresh();
    }
    
    public void refresh() {
        emails = Model.getEmail();
    }
    
    @Override
    public int getRowCount() {
        return emails.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Email email = emails.get(row);
        switch (col) {
            case 0:
                return email.getCreated();
            case 1:
                return email.getModified();
            case 2:
                return email.getAttempt();
            case 3:
                return email.getUser().getUsername();
            default:
                return email.getAddress();
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