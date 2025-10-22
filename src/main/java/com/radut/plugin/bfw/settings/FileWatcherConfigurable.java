package com.radut.plugin.bfw.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.radut.plugin.bfw.FileWatcherService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileWatcherConfigurable implements Configurable {

    private final Project project;
    private FileWatcherSettingsComponent settingsComponent;
    private FileWatcherService.StateChangeListener stateChangeListener;

    public FileWatcherConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @NlsContexts.ConfigurableName
    @Override
    public String getDisplayName() {
        return "Background File Watcher";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new FileWatcherSettingsComponent();

        // Set up button listeners
        FileWatcherService service = project.getService(FileWatcherService.class);

        settingsComponent.getStartButton().addActionListener(e -> {
            if (service != null) {
                service.startWatching();
            }
        });

        settingsComponent.getStopButton().addActionListener(e -> {
            if (service != null) {
                service.stopWatching();
            }
        });

        // Register state change listener
        if (service != null) {
            stateChangeListener = this::updateButtonStates;
            service.addStateChangeListener(stateChangeListener);
        }

        // Update initial button states
        updateButtonStates();

        return settingsComponent.getPanel();
    }

    private void updateButtonStates() {
        FileWatcherService service = project.getService(FileWatcherService.class);
        if (service != null && settingsComponent != null) {
            settingsComponent.updateControlStatus(service.isRunning());
        }
    }

    /**
     * Validates regex patterns in a filter string. Eliminates code duplication between included and ignored regex
     * validation.
     *
     * @param filterString Multi-line string of regex patterns to validate
     * @param filterType   Type of filter for error messages ("included" or "ignored")
     * @throws ConfigurationException if any pattern is invalid
     */
    private void validateRegexPatterns(String filterString, String filterType) throws ConfigurationException {
        if (filterString != null && !filterString.trim().isEmpty()) {
            String[] patterns = filterString.split("\n");
            for (int i = 0; i < patterns.length; i++) {
                String pattern = patterns[i].trim();
                if (!pattern.isEmpty()) {
                    try {
                        Pattern.compile(pattern);
                    } catch (PatternSyntaxException e) {
                        throw new ConfigurationException(
                                "Invalid " + filterType + " regex pattern on line " + (i + 1) +
                                ": " + pattern + "\nError: " + e.getMessage()
                        );
                    }
                }
            }
        }
    }

    @Override
    public boolean isModified() {
        FileWatcherSettings settings = FileWatcherSettings.getInstance(project);
        FileWatcherState state = settings.getState();

        return
                settingsComponent.isInContent() != state.isInContent() ||
                settingsComponent.isInSource() != state.isInSource() ||
                settingsComponent.isInTestSource() != state.isInTestSource() ||
                settingsComponent.isAutoReloadEnabled() != state.isAutoReloadEnabled() ||
                settingsComponent.isAutoRebuildEnabled() != state.isAutoRebuildEnabled() ||
                settingsComponent.getDebounceDelayMs() != state.getDebounceDelayMs() ||
                !settingsComponent.getPathRegexFilters().equals(state.getPathRegexFilters()) ||
                !settingsComponent.getIgnoredRegexFilters().equals(state.getIgnoredRegexFilters());
    }

    @Override
    public void apply() throws ConfigurationException {
        // Validate regex patterns using utility method
        validateRegexPatterns(settingsComponent.getPathRegexFilters(), "included");
        validateRegexPatterns(settingsComponent.getIgnoredRegexFilters(), "ignored");

        FileWatcherSettings settings = FileWatcherSettings.getInstance(project);

        // Check if settings that affect watched directories are changing
        boolean watchSettingsChanged =
                settingsComponent.isInContent() != settings.isInContent() ||
                settingsComponent.isInSource() != settings.isInSource() ||
                settingsComponent.isInTestSource() != settings.isInTestSource() ||
                settingsComponent.getDebounceDelayMs() != settings.getDebounceDelayMs();

        settings.setAutoReloadEnabled(settingsComponent.isAutoReloadEnabled());
        settings.setAutoRebuildEnabled(settingsComponent.isAutoRebuildEnabled());
        settings.setDebounceDelayMs(settingsComponent.getDebounceDelayMs());
        settings.setInSource(settingsComponent.isInSource());
        settings.setInTestSource(settingsComponent.isInTestSource());
        settings.setInContent(settingsComponent.isInContent());
        settings.setPathRegexFilters(settingsComponent.getPathRegexFilters());
        settings.setIgnoredRegexFilters(settingsComponent.getIgnoredRegexFilters());

        // Restart watchers if watch-related settings changed
        if (watchSettingsChanged) {
            FileWatcherService service = project.getService(FileWatcherService.class);
            if (service != null && service.isRunning()) {
                service.restartWatchers();
            }
        }
    }

    @Override
    public void reset() {
        FileWatcherSettings settings = FileWatcherSettings.getInstance(project);
        FileWatcherState state = settings.getState();

        settingsComponent.setAutoReloadEnabled(state.isAutoReloadEnabled());
        settingsComponent.setAutoRebuildEnabled(state.isAutoRebuildEnabled());
        settingsComponent.setDebounceDelayMs(state.getDebounceDelayMs());
        settingsComponent.setIsInSource(state.isInSource());
        settingsComponent.setIsInTestSource(state.isInTestSource());
        settingsComponent.setIsInContent(state.isInContent());
        settingsComponent.setPathRegexFilters(state.getPathRegexFilters());
        settingsComponent.setIgnoredRegexFilters(state.getIgnoredRegexFilters());

        // Update button states based on current service status
        updateButtonStates();
    }

    @Override
    public void disposeUIResources() {
        // Unregister state change listener
        if (stateChangeListener != null) {
            FileWatcherService service = project.getService(FileWatcherService.class);
            if (service != null) {
                service.removeStateChangeListener(stateChangeListener);
            }
            stateChangeListener = null;
        }
        settingsComponent = null;
    }
}
