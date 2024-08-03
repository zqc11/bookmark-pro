package org.bookmark.pro.listeners.app;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.bookmark.pro.base.BaseExportService;
import org.bookmark.pro.constants.BookmarkProIcon;
import org.bookmark.pro.context.BookmarkRunService;
import org.bookmark.pro.utils.BookmarkNoticeUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupScheduler implements BaseExportService {

    private final ScheduledExecutorService scheduler;

    public BackupScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        // 默认12个小时备份一次
        long backupInterval = 12;
        try {
            backupInterval = Integer.parseInt(BookmarkRunService.getBookmarkSettings().getBackUpTime()); // 获取备份间隔，时间为小时
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 设置初始延迟时间为备份间隔时间
        scheduler.scheduleAtFixedRate(this::performBackupForAllProjects, backupInterval, backupInterval, TimeUnit.HOURS);
    }

    private void performBackupForAllProjects() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (project != null) {
                performBackup(project);
            }
        }
    }

    private void performBackup(Project project) {
        if (BookmarkRunService.getBookmarkSettings().getAutoBackup()) {
            File autoBackupFile = getAutoBackupRootPath(project);
            String fileName = project.getName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss")) + ".json";
            BookmarkRunService.getPersistenceService(project).exportBookmark(project, autoBackupFile.getPath() + File.separator + fileName);
        }
    }

    /**
     * 发送导出通知
     *
     * @param project    项目
     * @param projectDir 项目根目录
     */
    private void sendExportNotice(Project project, String projectDir) {
        AnAction openExportFile = new NotificationAction(BookmarkProIcon.EYE_SIGN + "ViewFile") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(new File(projectDir));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        BookmarkNoticeUtil.projectNotice(project, String.format("Export bookmark success. Output file directory: [%s]", projectDir), openExportFile);
    }
}