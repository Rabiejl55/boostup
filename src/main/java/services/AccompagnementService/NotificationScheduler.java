package services.AccompagnementService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private final SessionNotificationService notificationService;

    public NotificationScheduler(SessionNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long initialDelay = 0; // commence immédiatement
        long period = 24; // vérifie toutes les 24 heures

        scheduler.scheduleAtFixedRate(() -> notificationService.checkNotifications(),
                initialDelay, period, TimeUnit.HOURS);
    }
}