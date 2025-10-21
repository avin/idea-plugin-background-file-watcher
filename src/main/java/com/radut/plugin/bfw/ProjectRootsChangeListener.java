package com.radut.plugin.bfw;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for changes to project structure (source roots, content roots, etc.)
 * and automatically restarts file watchers when changes are detected.
 */
public class ProjectRootsChangeListener implements ModuleRootListener {
    private static final Logger LOG = Logger.getInstance(ProjectRootsChangeListener.class);

    @Override
    public void rootsChanged(@NotNull ModuleRootEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        LOG.info("Project structure changed, restarting watchers for: " + project.getName());

        FileWatcherService service = project.getService(FileWatcherService.class);
        if (service != null && service.isRunning()) {
            service.restartWatchers();
        }
    }
}
