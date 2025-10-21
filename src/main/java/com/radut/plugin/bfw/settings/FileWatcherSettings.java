package com.radut.plugin.bfw.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "FileWatcherSettings",
    storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class FileWatcherSettings implements PersistentStateComponent<FileWatcherState> {

    private FileWatcherState state = new FileWatcherState();

    public static FileWatcherSettings getInstance(@NotNull Project project) {
        return project.getService(FileWatcherSettings.class);
    }

    @Nullable
    @Override
    public FileWatcherState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull FileWatcherState state) {
        this.state = state;
    }


    public boolean isEnabled() {
        return state.isEnabled();
    }

    public void setEnabled(final boolean enabled) {
        state.setEnabled(enabled);
    }

    public boolean isInSource() {
        return state.isInSource();
    }

    public void setInSource(final boolean inSource) {
        state.setInSource(inSource);
    }

    public boolean isInTestSource() {
        return state.isInTestSource();
    }

    public void setInTestSource(final boolean inTestSource) {
        state.setInTestSource(inTestSource);
    }

    public boolean isInContent() {
        return state.isInContent();
    }

    public void setInContent(final boolean inContent) {
        state.setInContent(inContent);
    }

    public String getPathRegexFilters() {
        return state.getPathRegexFilters();
    }

    public void setIgnoredRegexFilters(final String ignoredRegexFilters) {
        state.setIgnoredRegexFilters(ignoredRegexFilters);
    }

    public boolean isAutoReloadEnabled() {
        return state.isAutoReloadEnabled();
    }

    public void setAutoRebuildEnabled(final boolean autoRebuildEnabled) {
        state.setAutoRebuildEnabled(autoRebuildEnabled);
    }

    public void setAutoReloadEnabled(final boolean autoReloadEnabled) {
        state.setAutoReloadEnabled(autoReloadEnabled);
    }

    public void setDebounceDelayMs(final int debounceDelayMs) {
        state.setDebounceDelayMs(debounceDelayMs);
    }

    public int getDebounceDelayMs() {
        return state.getDebounceDelayMs();
    }

    public boolean isAutoRebuildEnabled() {
        return state.isAutoRebuildEnabled();
    }

    public void setPathRegexFilters(final String pathRegexFilters) {
        state.setPathRegexFilters(pathRegexFilters);
    }

    public String getIgnoredRegexFilters() {
        return state.getIgnoredRegexFilters();
    }
}
