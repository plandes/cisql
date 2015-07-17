package com.zensol.rsgui;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

public class BestGuessPresTableModel implements TableModel {

    private TableModel delegate;

    public BestGuessPresTableModel(TableModel delegate) {
        if (delegate.getRowCount() == 1) {
            this.delegate = new FirstRowRotationTableModel(delegate);
        } else {
            this.delegate = delegate;
        }
    }

    public void addTableModelListener(TableModelListener tableModelListener) {
        delegate.addTableModelListener(tableModelListener);
    }

    public Class getColumnClass(int n) {
        return delegate.getColumnClass(n);
    }

    public int getColumnCount() {
        return delegate.getColumnCount();
    }

    public String getColumnName(int n) {
        return delegate.getColumnName(n);
    }

    public boolean isCellEditable(int row, int col) {
        return delegate.isCellEditable(row, col);
    }

    public void removeTableModelListener(TableModelListener tableModelListener) {
        delegate.removeTableModelListener(tableModelListener);
    }

    public void setValueAt(Object object, int row, int col) {
        delegate.setValueAt(object, row, col);
    }

    public int getRowCount() {
        return delegate.getRowCount();
    }

    public Object getValueAt(int row, int col) {
        return delegate.getValueAt(row, col);
    }
}
