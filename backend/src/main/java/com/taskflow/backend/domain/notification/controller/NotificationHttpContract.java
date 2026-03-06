package com.taskflow.backend.domain.notification.controller;

public final class NotificationHttpContract {

    public static final String BASE_PATH = "/notifications";
    public static final String UNREAD_COUNT_PATH = "/unread-count";
    public static final String READ_PATH = "/{notificationId}/read";
    public static final String READ_ALL_PATH = "/read-all";

    private NotificationHttpContract() {
    }

    public static String unreadCountPath() {
        return BASE_PATH + UNREAD_COUNT_PATH;
    }

    public static String readPath(Long notificationId) {
        return BASE_PATH + "/" + notificationId + "/read";
    }

    public static String readAllPath() {
        return BASE_PATH + READ_ALL_PATH;
    }
}
