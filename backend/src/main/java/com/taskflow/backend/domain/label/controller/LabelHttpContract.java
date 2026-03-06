package com.taskflow.backend.domain.label.controller;

public final class LabelHttpContract {

    public static final String PROJECT_LABELS_PATH = "/projects/{projectId}/labels";
    public static final String LABEL_PATH = "/labels/{labelId}";

    private LabelHttpContract() {
    }

    public static String projectLabelsPath(Long projectId) {
        return "/projects/" + projectId + "/labels";
    }

    public static String labelPath(Long labelId) {
        return "/labels/" + labelId;
    }
}
