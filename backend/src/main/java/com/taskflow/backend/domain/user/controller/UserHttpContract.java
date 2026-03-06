package com.taskflow.backend.domain.user.controller;

public final class UserHttpContract {

    public static final String BASE_PATH = "/users";
    public static final String ME_PATH = "/me";
    public static final String ME_PASSWORD_PATH = "/me/password";

    private UserHttpContract() {
    }

    public static String mePath() {
        return BASE_PATH + ME_PATH;
    }

    public static String mePasswordPath() {
        return BASE_PATH + ME_PASSWORD_PATH;
    }
}
