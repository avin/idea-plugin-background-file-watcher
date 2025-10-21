package com.radut.plugin.bfw.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.IconUtil;
import com.radut.plugin.bfw.FileWatcherService;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ToggleFileWatcherAction extends AnAction {

    // Track which projects have registered listeners (using WeakHashMap for automatic cleanup)
    private final Set<Project> registeredProjects = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        FileWatcherService service = project.getService(FileWatcherService.class);
        if (service == null) {
            return;
        }
        // Toggle the file watcher
        // The state change notification will automatically update the tool window and icon
        service.toggleWatchingState();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        FileWatcherService service = project.getService(FileWatcherService.class);
        if (service == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Register listener for this project if not already registered
        if (!registeredProjects.contains(project)) {
            service.addStateChangeListener(() -> updateIcon(service));
            registeredProjects.add(project);
        }

        e.getPresentation().setEnabled(true);

        // Update the text and icon based on the current state
        updatePresentationState(e.getPresentation(), service);
    }

    private void updateIcon(FileWatcherService service) {

        updatePresentationState(getTemplatePresentation(), service);
    }

    private void updatePresentationState(com.intellij.openapi.actionSystem.Presentation presentation, FileWatcherService service) {
        if (service.isRunning()) {
            presentation.setText("Disable File Watching");
            presentation.setDescription("Stop watching files for changes");
            // Blue eye icon when running
            presentation.setIcon(IconUtil.colorize(AllIcons.Actions.Show, JBColor.BLUE));
        } else {
            presentation.setText("Enable File Watching");
            presentation.setDescription("Start watching files for changes");
            // Gray eye icon when stopped
            presentation.setIcon(IconUtil.colorize(AllIcons.Actions.Show, JBColor.GRAY));
        }
    }
}
