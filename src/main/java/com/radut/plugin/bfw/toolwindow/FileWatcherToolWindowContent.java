package com.radut.plugin.bfw.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.radut.plugin.bfw.FileWatcherService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileWatcherToolWindowContent {
    private final Project project;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final DefaultTableModel eventsTableModel;
    private final JBTable eventsTable;
    private static final int MAX_ROWS = 1_000;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final JButton startButton = new JButton("Start Watching");
    private final JButton stopButton = new JButton("Stop Watching");
    private final JBLabel statusLabel = new JBLabel("Status: Unknown");
    private FileWatcherService.StateChangeListener stateChangeListener;

    public FileWatcherToolWindowContent(Project project) {
        this.project = project;

        // Create single table model for all events
        eventsTableModel = new DefaultTableModel(new String[]{"Timestamp", "Event Type", "Trigger", "Matched Rule", "File Path"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2) { //Trigger
                    return Boolean.class;
                }
                return String.class;
            }
        };

        eventsTable = new JBTable(eventsTableModel);
        eventsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        eventsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        eventsTable.getColumnModel().getColumn(0).setMaxWidth(230);
        eventsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        eventsTable.getColumnModel().getColumn(1).setMaxWidth(150);
        eventsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        eventsTable.getColumnModel().getColumn(2).setMaxWidth(150);
        eventsTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        eventsTable.getColumnModel().getColumn(3).setMaxWidth(350);

        // Create buttons
        JButton clearButton = new JButton("Clear Events");
        clearButton.addActionListener(e -> clear());

        JButton scrollToBottomButton = new JButton("Scroll to Bottom");
        scrollToBottomButton.addActionListener(e -> scrollToBottom());

        // Set up action listeners for control buttons
        FileWatcherService service = project.getService(FileWatcherService.class);

        startButton.addActionListener(e -> {
            if (service != null) {
                service.startWatching();
            }
        });

        stopButton.addActionListener(e -> {
            if (service != null) {
                service.stopWatching();
            }
        });

        // Register state change listener to automatically update control status
        if (service != null) {
            stateChangeListener = () -> updateControlStatus();
            service.addStateChangeListener(stateChangeListener);
        }

        // Initialize button states
        updateControlStatus();

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JBLabel("File Watcher Events - Project: " + project.getName()), BorderLayout.WEST);

        // Create control panel for start/stop buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);
        headerPanel.add(controlPanel, BorderLayout.CENTER);

        // Create button panel for utility buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(scrollToBottomButton);
        buttonPanel.add(clearButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(new JBScrollPane(eventsTable), BorderLayout.CENTER);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }


    public void addEvent(boolean isTrigger, String eventType, String matchedRule, String filePath) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = dateFormat.format(new Date());
            // Add row at the end (bottom) for newest events
            eventsTableModel.addRow(new Object[]{timestamp, eventType, isTrigger, matchedRule, filePath});

            // Limit rows to prevent memory issues - remove from the top (oldest)
            while (eventsTableModel.getRowCount() > MAX_ROWS) {
                eventsTableModel.removeRow(0);
            }

            // Scroll to the last row (most recent event at bottom)
            scrollToBottom();
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            eventsTableModel.setRowCount(0);
        });
    }

    private void scrollToBottom() {
        if (eventsTable.getRowCount() > 0) {
            int lastRow = eventsTable.getRowCount() - 1;
            eventsTable.scrollRectToVisible(eventsTable.getCellRect(lastRow, 0, true));
        }
    }

    public void updateControlStatus() {
        SwingUtilities.invokeLater(() -> {
            FileWatcherService service = project.getService(FileWatcherService.class);
            if (service != null) {
                boolean isRunning = service.isRunning();
                if (isRunning) {
                    statusLabel.setText("Status: Running");
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } else {
                    statusLabel.setText("Status: Stopped");
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                }
            }
        });
    }

    /**
     * Dispose method to clean up the state change listener. Should be called when the tool window is being disposed.
     */
    public void dispose() {
        if (stateChangeListener != null) {
            FileWatcherService service = project.getService(FileWatcherService.class);
            if (service != null) {
                service.removeStateChangeListener(stateChangeListener);
            }
            stateChangeListener = null;
        }
    }
}

