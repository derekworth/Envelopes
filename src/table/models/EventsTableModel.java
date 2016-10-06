package table.models;

import database.DBMS;
import database.Event;
import java.awt.Component;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import misc.Utilities;

/**
 * Created on Sep 26, 2013
 * @author Derek Worth
 */
public class EventsTableModel implements TableModel {
    
    LinkedList<Event> events;
    
    Object[] columnNames = {"Timestamp", "Event"};
    ViewController vc;
    
    public EventsTableModel(ViewController viewCtr) {
        vc = viewCtr;
        refresh();
    }
    
    public final void refresh() {
        events = new LinkedList();
        boolean byDateRange;
        int qty;
        if(vc.con==null) {
            byDateRange = false;
            qty = 35;
        } else {
            byDateRange = vc.con.dateRangeCheckBox1.isSelected();
            try{
                qty = Integer.parseInt(vc.con.eventsCountField.getText());
            } catch(NumberFormatException ex) {
                qty = 0;
            }
        }
        if(byDateRange) {
            String from = vc.con.eventsFromField.getText();
            String to = vc.con.eventsToField.getText();
            LinkedList<Event> tmp;
            if(from==null || to==null || !Utilities.isDate(from) || !Utilities.isDate(to))
                tmp = DBMS.getEvents(0);
            else
                tmp = DBMS.getEvents(from, to);
            // reverse order
            for(Event trans : tmp) {
                events.addFirst(trans);
            }
        } else {
            LinkedList<Event> tmp = DBMS.getEvents(qty);
            // reverse order
            for(Event trans : tmp) {
                events.addFirst(trans);
            }
        }
    }
    
    @Override
    public int getRowCount() {
        return events.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Event evt = events.get(row);
        if(col==0) {
            return evt.getCreated();
        } else {
            return evt.getEvent();
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