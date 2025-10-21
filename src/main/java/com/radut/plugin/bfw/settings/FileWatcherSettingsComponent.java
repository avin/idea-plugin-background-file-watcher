package com.radut.plugin.bfw.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class FileWatcherSettingsComponent {

    private final JPanel mainPanel;
    private final JPanel settingsPanel;
    private final JButton startButton = new JButton("Start Watching");
    private final JButton stopButton = new JButton("Stop Watching");
    private final JBLabel statusLabel = new JBLabel("Status: Unknown");
    private final JBCheckBox isInContent = new JBCheckBox("Include files in project content");
    private final JBCheckBox isInSource = new JBCheckBox("Include source files");
    private final JBCheckBox isInTestSource = new JBCheckBox("Include test source files");
    private final JBCheckBox autoReloadEnabled = new JBCheckBox("Enable automatic reload from disk");
    private final JBCheckBox autoRebuildEnabled = new JBCheckBox("Enable automatic rebuild after reload");
    private final JBTextField debounceDelayField = new JBTextField();
    private final JBTextArea pathRegexFiltersArea = new JBTextArea();
    private final JBTextArea ignoredRegexFiltersArea = new JBTextArea();

    // Labels that should be grayed out when disabled
    private final JBLabel configureFilesLabel = new JBLabel("<html>Configure which files should trigger auto-reload and rebuild:</html>");
    private final JBLabel includedRegexHeaderLabel = new JBLabel("<html><b>Included Path Regex Filters(Valid for Content Only)</b></html>");
    private final JBLabel includedRegexDescLabel = new JBLabel("<html>Enter regex patterns (one per line) to INCLUDE file paths. Files matching at least one pattern will be watched:<br/>Example: .*\\.java$ (matches all Java files) <br/>Empty means matches all files</html>");
    private final JBLabel ignoredRegexHeaderLabel = new JBLabel("<html><b>Ignored Path Regex Filters(Valid for Any location)</b></html>");
    private final JBLabel ignoredRegexDescLabel = new JBLabel("<html>Enter regex patterns (one per line) to IGNORE file paths. Files matching any pattern will be ignored:<br/>Example: .*\\.log$ (ignores all log files)<br/>Empty means that no file from sources will be filtered</html>");
    private final JBLabel actionsHeaderLabel = new JBLabel("<html><b>Actions Configuration</b></html>");
    private final JBLabel debounceDelayLabel = new JBLabel("Debounce delay (milliseconds):");

    // Store scroll panes for enable/disable
    private JScrollPane includedScrollPane;
    private JScrollPane ignoredScrollPane;

    public FileWatcherSettingsComponent() {
        debounceDelayField.setColumns(6);
        pathRegexFiltersArea.setRows(5);
        pathRegexFiltersArea.setLineWrap(false);
        ignoredRegexFiltersArea.setRows(5);
        ignoredRegexFiltersArea.setLineWrap(false);

        includedScrollPane = new JScrollPane(pathRegexFiltersArea);
        includedScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        ignoredScrollPane = new JScrollPane(ignoredRegexFiltersArea);
        ignoredScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Add listeners to enable/disable regex fields based on checkbox states
        isInSource.addItemListener(e -> updateIgnoreRegexFieldState());
        isInTestSource.addItemListener(e -> updateIgnoreRegexFieldState());
        isInContent.addItemListener(e -> updateIncludedRegexFieldState());

        // Create donation link
        LinkLabel<String> donateLink = new LinkLabel<>("Donate", null, (aSource, aLinkData) -> {
            BrowserUtil.browse("https://www.paypal.com/donate/?hosted_button_id=C9U54KULFG48C");
        });
        donateLink.setToolTipText("Support the development of this plugin");

        // Create a panel for the header with donate link
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JBLabel("<html><b>File Watching Configuration</b></html>"), BorderLayout.WEST);
        headerPanel.add(donateLink, BorderLayout.EAST);

        // Create a panel for the control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);

        // Create a separate panel for all settings (to enable/disable as a group)
        settingsPanel = FormBuilder.createFormBuilder()

                .addComponent(actionsHeaderLabel, 0)
                .addVerticalGap(10)
                .addComponent(autoReloadEnabled, 1)
                .addTooltip("Automatically reload files from disk when changes are detected")
                .addComponent(autoRebuildEnabled, 1)
                .addTooltip("Automatically rebuild the project after reloading files")
                .addVerticalGap(10)
                .addLabeledComponent(debounceDelayLabel, debounceDelayField, 1)
                .addTooltip("Wait this many milliseconds after the last change before triggering reload/rebuild")

                .addComponent(configureFilesLabel, 0)
                .addVerticalGap(5)
                .addComponent(isInSource, 1)
                .addTooltip("Include files in source directories (src/main/java, etc.)")
                .addComponent(isInTestSource, 1)
                .addTooltip("Include files in test source directories (src/test/java, etc.)")
                .addComponent(isInContent, 1)
                .addTooltip("Include files that are part of the project content")
                .addVerticalGap(15)
                .addComponent(includedRegexHeaderLabel, 0)
                .addVerticalGap(5)
                .addComponent(includedRegexDescLabel, 0)
                .addVerticalGap(5)
                .addComponent(includedScrollPane, 3)
                .addVerticalGap(15)
                .addComponent(ignoredRegexHeaderLabel, 0)
                .addVerticalGap(5)
                .addComponent(ignoredRegexDescLabel, 0)
                .addVerticalGap(5)
                .addComponent(ignoredScrollPane, 3)
                .addVerticalGap(15)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // Create main panel with header, controls, and settings
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(headerPanel, 0)
                .addVerticalGap(10)
                .addComponent(new JBLabel("<html><b>File Watcher Control</b></html>"), 0)
                .addVerticalGap(5)
                .addComponent(controlPanel, 0)
                .addVerticalGap(15)
                .addComponent(settingsPanel, 0)
                .getPanel();

        // Set initial state of regex fields
        updateIncludedRegexFieldState();
        updateIgnoreRegexFieldState();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public boolean isInContent() {
        return isInContent.isSelected();
    }

    public void setIsInContent(boolean value) {
        isInContent.setSelected(value);
    }

    public boolean isInSource() {
        return isInSource.isSelected();
    }

    public void setIsInSource(boolean value) {
        isInSource.setSelected(value);
    }

    public boolean isInTestSource() {
        return isInTestSource.isSelected();
    }

    public void setIsInTestSource(boolean value) {
        isInTestSource.setSelected(value);
    }

    public boolean isAutoReloadEnabled() {
        return autoReloadEnabled.isSelected();
    }

    public void setAutoReloadEnabled(boolean value) {
        autoReloadEnabled.setSelected(value);
    }

    public boolean isAutoRebuildEnabled() {
        return autoRebuildEnabled.isSelected();
    }

    public void setAutoRebuildEnabled(boolean value) {
        autoRebuildEnabled.setSelected(value);
    }

    public int getDebounceDelayMs() {
        try {
            return Integer.parseInt(debounceDelayField.getText());
        } catch (NumberFormatException e) {
            return 500; // default
        }
    }

    public void setDebounceDelayMs(int value) {
        debounceDelayField.setText(String.valueOf(value));
    }

    public String getPathRegexFilters() {
        return pathRegexFiltersArea.getText();
    }

    public void setPathRegexFilters(String value) {
        pathRegexFiltersArea.setText(value != null ? value : "");
    }

    public String getIgnoredRegexFilters() {
        return ignoredRegexFiltersArea.getText();
    }

    public void setIgnoredRegexFilters(String value) {
        ignoredRegexFiltersArea.setText(value != null ? value : "");
    }

    public JButton getStartButton() {
        return startButton;
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public void updateControlStatus(boolean isRunning) {
        if (isRunning) {
            statusLabel.setText("Status: Running");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            enableAllSettings(true);
        } else {
            statusLabel.setText("Status: Stopped");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            enableAllSettings(false);
        }
        updateIncludedRegexFieldState();
        updateIgnoreRegexFieldState();
    }

    private void enableAllSettings(boolean enabled) {
        // Enable/disable the entire settings panel at once
        // This grays out all components within the panel (checkboxes, fields, labels)
        settingsPanel.setEnabled(enabled);

        // Recursively enable/disable all child components
        setEnabledRecursive(settingsPanel, enabled);
    }

    private void setEnabledRecursive(java.awt.Container container, boolean enabled) {
        for (java.awt.Component component : container.getComponents()) {
            component.setEnabled(enabled);
            if (component instanceof java.awt.Container) {
                setEnabledRecursive((java.awt.Container) component, enabled);
            }
        }
    }

    /**
     * Updates the enabled state of Included Regex fields.
     * Enabled only when isInContent is checked.
     */
    private void updateIncludedRegexFieldState() {
        boolean enabled = isInContent.isSelected();
        includedRegexHeaderLabel.setEnabled(enabled);
        includedRegexDescLabel.setEnabled(enabled);
        pathRegexFiltersArea.setEnabled(enabled);
        includedScrollPane.setEnabled(enabled);
    }

    /**
     * Updates the enabled state of Ignore Regex fields.
     * Enabled only when at least one of isInSource or isInTestSource is checked.
     */
    private void updateIgnoreRegexFieldState() {
        boolean enabled = isInSource.isSelected() || isInTestSource.isSelected();
        ignoredRegexHeaderLabel.setEnabled(enabled);
        ignoredRegexDescLabel.setEnabled(enabled);
        ignoredRegexFiltersArea.setEnabled(enabled);
        ignoredScrollPane.setEnabled(enabled);
    }
}
