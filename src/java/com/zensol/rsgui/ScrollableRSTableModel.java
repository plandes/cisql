package com.zensol.rsgui;

import javax.swing.table.AbstractTableModel;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ScrollableRSTableModel
    extends javax.swing.table.AbstractTableModel {

    private ResultSet rs;
    private String[] columns;
    private int rowCount;
    
    public ScrollableRSTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        columns = new String[cols];
        for(int i = 0; i < cols; i++) {
            columns[i] = meta.getColumnLabel(i + 1);
        }

        rs.last();
        rowCount = rs.getRow();

        this.rs = rs;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int row, int column) {
        Object ret = null;

        try {
            rs.absolute(row + 1);
            ret = rs.getObject(column + 1);
        } catch(SQLException e) {
            e.printStackTrace();
            ret = e;
        }

        return ret;
    }
}
