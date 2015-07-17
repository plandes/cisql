package com.zensol.rsgui;

import javax.swing.JFrame;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetFrame extends JFrame {
    private ResultSetPanel panel;

    public ResultSetFrame() {
        this(true);
    }

    public ResultSetFrame(boolean hasQueryBox) {
        super("SQL Results");
        init(hasQueryBox);
    }

    private void init(boolean hasQueryBox) {
        panel = new ResultSetPanel(hasQueryBox);
        setContentPane(panel);
    }

    public ResultSetPanel getResultSetPanel() {
        return panel;
    }

    public void dispose() {
        super.dispose();
        panel.dispose();
    }
}

