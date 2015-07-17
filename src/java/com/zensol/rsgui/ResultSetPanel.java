package com.zensol.rsgui;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ResultSetPanel extends JPanel {

    private final static int QUERY_PANEL_HEIGHT = 140;
    private final static int SCREEN_HEIGHT_FUDGE = 100;
    private final static int SCREEN_WIDTH_FUDGE = 20;

    private Connection connection;
    private Statement stmt;
    private ResultSet resultSet;
    private boolean hasQueryBox;

    private Action sendAction;
    private PackingColumnWidthJTable table;
    private TableModel model;
    private JTextArea queryBox;
    private JComponent mainPane;
    private JScrollPane tablePane;

    public ResultSetPanel(boolean hasQueryBox) {
        sendAction = new SendAction();
        this.hasQueryBox = hasQueryBox;
        init();
    }

    private void init() {
        JPanel root = this;

        root.setLayout(new BorderLayout());
        table = new PackingColumnWidthJTable();
        table.setColumnSelectionAllowed(true);
        queryBox = new JTextArea();
        tablePane = new JScrollPane(table);

        if (hasQueryBox) {
            mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                      tablePane, getTopPane());
        } else {
            mainPane = tablePane;
        }

        root.add(mainPane);
    }

    private JComponent getTopPane() {
        JPanel root = new JPanel();
        GridBagConstraints c = new GridBagConstraints(
            0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.NORTH,
            GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0);

        root.setLayout(new GridBagLayout());

        c.weightx = 0;
        c.weighty = 1;
        root.add(new JButton(sendAction), c);

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.gridx++;
        c.weightx = 1;
        root.add(new JScrollPane(queryBox), c);

        return root;
    }

    public Dimension getPreferredSize() {
        Dimension screenSize = java.awt.Toolkit.
            getDefaultToolkit().getScreenSize();
        int width = table.getPreferredSize().width + 4;
        int height;

        if (hasQueryBox) {
            height = table.getPreferredSize().height + QUERY_PANEL_HEIGHT;
        } else {
            height = table.getPreferredSize().height+ 20;
        }

        // use 100 for windows task bar, OSX menu bar, etc
        height = Math.min(screenSize.height - SCREEN_HEIGHT_FUDGE, height);
        width = Math.min(screenSize.width - SCREEN_WIDTH_FUDGE, width);


        return new Dimension(width, height);
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    protected final void executeQuery() {
        try {
            executeQuery(queryBox.getText());
        } catch(SQLException e) {
            e.printStackTrace();
            clearTable();
            JOptionPane.showMessageDialog(
                null, e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE); 
        }
    }

    protected void clearTable() {
        table.setModel(new javax.swing.table.AbstractTableModel() {
            public int getRowCount() { return 0; }
            public int getColumnCount() { return 0; }
            public Object getValueAt(int row, int column) { return ""; }
        });
    }           

    public void executeQuery(String query) throws SQLException {
        executeQuery(query, false);
    }

    public void executeQuery(String query, boolean bestLayout)
        throws SQLException {

        dispose();
        stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                          ResultSet.CONCUR_READ_ONLY);
        resultSet = stmt.executeQuery(query);
        displayResults(
            query, new ScrollableRSTableModel(resultSet), bestLayout);
    }

    public void displayResults(ResultSet rs) throws SQLException {
        displayResults("", rs);
    }

    public void displayResults(ResultSet rs, boolean bestLayout) throws SQLException {
        displayResults("", rs, bestLayout);
    }

    public void displayResults(String query, ResultSet rs) throws SQLException {
        displayResults(query, rs, false);
    }

    public void displayResults(String query, ResultSet rs, boolean bestLayout)
        throws SQLException {
        displayResults(query, new CachedRSTableModel(rs), bestLayout);
    }

    protected void displayResults(String query, TableModel model,
                                  boolean bestLayout)
        throws SQLException {

        queryBox.setText(query);
        if (bestLayout) model = new BestGuessPresTableModel(model);
        this.model = model;
        try {
            javax.swing.SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        table.setModel(ResultSetPanel.this.model);
                        if (mainPane instanceof JSplitPane) {
                            JSplitPane sp = (JSplitPane)mainPane;
                            Dimension screenSize = java.awt.Toolkit.
                                getDefaultToolkit().getScreenSize();
                            int height = table.getPreferredSize().height + 20;
                            height = Math.min(
                                screenSize.height -
                                QUERY_PANEL_HEIGHT - SCREEN_HEIGHT_FUDGE,
                                height);
                            sp.setDividerLocation(height);
                        }
                    }});
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public final void dispose() {
        try { if (resultSet != null) resultSet.close(); }
        catch(SQLException e) { e.printStackTrace(); }
        resultSet = null;

        try { if (stmt != null) stmt.close(); }
        catch(SQLException e) { e.printStackTrace(); }
        stmt = null;
    }

    protected class SendAction extends AbstractAction {
        public SendAction() {
            super("Send");
        }

        public void actionPerformed(ActionEvent e) {
            executeQuery();
        }
    }
}
