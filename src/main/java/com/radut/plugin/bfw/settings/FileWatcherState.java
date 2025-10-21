package com.radut.plugin.bfw.settings;

public class FileWatcherState {
    private boolean enabled = true;
    private boolean isInSource = true;
    private boolean isInTestSource = true;
    private boolean isInContent = false;
    private boolean autoReloadEnabled = true;
    private boolean autoRebuildEnabled = true;
    private int debounceDelayMs = 500;
    private String pathRegexFilters = "";
    private String ignoredRegexFilters = "";


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInSource() {
        return isInSource;
    }

    public void setInSource(final boolean inSource) {
        isInSource = inSource;
    }

    public boolean isInTestSource() {
        return isInTestSource;
    }

    public void setInTestSource(final boolean inTestSource) {
        isInTestSource = inTestSource;
    }

    public boolean isInContent() {
        return isInContent;
    }

    public void setInContent(final boolean inContent) {
        isInContent = inContent;
    }

    public boolean isAutoReloadEnabled() {
        return autoReloadEnabled;
    }

    public void setAutoReloadEnabled(final boolean autoReloadEnabled) {
        this.autoReloadEnabled = autoReloadEnabled;
    }

    public boolean isAutoRebuildEnabled() {
        return autoRebuildEnabled;
    }

    public void setAutoRebuildEnabled(final boolean autoRebuildEnabled) {
        this.autoRebuildEnabled = autoRebuildEnabled;
    }

    public int getDebounceDelayMs() {
        return debounceDelayMs;
    }

    public void setDebounceDelayMs(final int debounceDelayMs) {
        this.debounceDelayMs = debounceDelayMs;
    }

    public String getPathRegexFilters() {
        return pathRegexFilters;
    }

    public void setPathRegexFilters(final String pathRegexFilters) {
        this.pathRegexFilters = pathRegexFilters;
    }

    public String getIgnoredRegexFilters() {
        return ignoredRegexFilters;
    }

    public void setIgnoredRegexFilters(final String ignoredRegexFilters) {
        this.ignoredRegexFilters = ignoredRegexFilters;
    }
}