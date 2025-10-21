package com.radut.plugin.bfw;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.radut.plugin.bfw.settings.FileWatcherSettings;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectOpenListener implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(ProjectOpenListener.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.info("Project opened: " + project.getName());

        // Check if file watching is enabled in settings
        FileWatcherSettings settings = FileWatcherSettings.getInstance(project);
        if (!settings.isEnabled()) {
            LOG.info("File watching is disabled in settings for project: " + project.getName());
            return Unit.INSTANCE;
        }

        // Get the FileWatcherService and start watching
        FileWatcherService service = project.getService(FileWatcherService.class);
        if (service != null) {
            service.startWatching();
            LOG.info("File watcher started for project: " + project.getName());
        } else {
            LOG.error("Could not get FileWatcherService for project: " + project.getName());
        }

        return Unit.INSTANCE;
    }
}

