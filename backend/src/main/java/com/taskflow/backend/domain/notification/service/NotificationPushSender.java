package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;

public interface NotificationPushSender {

    void send(String token, PushPlatform platform, String title, String body);
}
