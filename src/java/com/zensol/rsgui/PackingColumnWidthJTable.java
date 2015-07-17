package com.zensol.rsgui;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class PackingColumnWidthJTable extends JTable {

    private static final int WIDTH_MAX = 500;

    private int margin;

    public PackingColumnWidthJTable() {
        this(5);
    }

    public PackingColumnWidthJTable(int margin) {
        // Disable auto resizing
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.margin = margin;
    }

    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        packColumns();
    }

    public void packColumns() {
        for (int c = 0; c < getColumnCount(); c++) {
            packColumn(this, c, margin);
        }
    }
    
    // Sets the preferred width of the visible column specified by
    // vColIndex. The column will be just wide enough to show the column head
    // and the widest cell in the column.  margin pixels are added to the left
    // and right (resulting in an additional width of 2*margin pixels).
    protected void packColumn(JTable table, int vColIndex, int margin) {
        TableModel model = table.getModel();
        DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width = 0;
    
        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(
            table, col.getHeaderValue(), false, false, 0, 0);
        width = comp.getPreferredSize().width;
    
        // Get maximum width of column data
        for (int r=0; r<table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(
                table, table.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }
    
        // Add margin
        width += 2*margin;

        // make sure our columns don't kill usage
        width = Math.min(width, WIDTH_MAX);
    
        // Set the width
        col.setPreferredWidth(width);
    }
}
