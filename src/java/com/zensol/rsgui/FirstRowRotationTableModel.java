package com.zensol.rsgui;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

public class FirstRowRotationTableModel implements TableModel {

    private static final String[] COLS = { "Column", "Value" };
    private TableModel delegate;

    public FirstRowRotationTableModel(TableModel delegate) {
        this.delegate = delegate;
    }

    public void addTableModelListener(TableModelListener tableModelListener) {
        delegate.addTableModelListener(tableModelListener);
    }

    public Class getColumnClass(int n) {
        return delegate.getColumnClass(n);
    }

    public int getColumnCount() {
        return COLS.length;
    }

    public String getColumnName(int n) {
        return COLS[n];
    }

    public boolean isCellEditable(int row, int col) {
        return delegate.isCellEditable(col, row);
    }

    public void removeTableModelListener(TableModelListener tableModelListener) {
        delegate.removeTableModelListener(tableModelListener);
    }

    public void setValueAt(Object object, int row, int col) {
        delegate.setValueAt(object, col, row);
    }

    public int getRowCount() {
        return delegate.getColumnCount();
    }

    public Object getValueAt(int row, int col) {
        return (col == 0) ?
            delegate.getColumnName(row) :
            delegate.getValueAt(0, row);
    }
}
