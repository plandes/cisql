package com.zensol.rsgui;

import javax.swing.table.AbstractTableModel;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

public class CachedRSTableModel
    extends javax.swing.table.AbstractTableModel {

    private String[] columns;
    private List data;
    
    @SuppressWarnings("unchecked")
    public CachedRSTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        columns = new String[cols];
        for(int i = 0; i < cols; i++) {
            columns[i] = meta.getColumnName(i + 1);
        }

        data = new java.util.LinkedList();
        while (rs.next()) {
            Object[] row = new Object[cols];
            for(int i = 0; i < cols; i++) {
                row[i] = rs.getObject(i + 1);
            }
            data.add(row);
        }
        data = new java.util.ArrayList(data);
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int row, int column) {
        Object[] rowData = (Object[])data.get(row);
        return rowData[column];
    }
}
