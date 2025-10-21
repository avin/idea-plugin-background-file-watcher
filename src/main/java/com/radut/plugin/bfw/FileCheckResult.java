package com.radut.plugin.bfw;


public class FileCheckResult {
    private final boolean shouldProcess;
    private final String matchedRule;
    private final String details;

    public FileCheckResult(boolean shouldProcess, String matchedRule, String details) {
        this.shouldProcess = shouldProcess;
        this.matchedRule = matchedRule;
        this.details = details;
    }


    public boolean isShouldProcess() {
        return shouldProcess;
    }

    public String getMatchedRule() {
        return matchedRule;
    }

    public String getDetails() {
        return details;
    }

}