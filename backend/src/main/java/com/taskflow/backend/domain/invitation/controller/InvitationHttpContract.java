package com.taskflow.backend.domain.invitation.controller;

public final class InvitationHttpContract {

    public static final String PROJECT_INVITATIONS_PATH = "/projects/{projectId}/invitations";
    public static final String MY_INVITATIONS_PATH = "/invitations/me";
    public static final String ACCEPT_PATH = "/invitations/{invitationId}/accept";
    public static final String REJECT_PATH = "/invitations/{invitationId}/reject";
    public static final String CANCEL_PATH = "/projects/{projectId}/invitations/{invitationId}/cancel";

    private InvitationHttpContract() {
    }

    public static String projectInvitationsPath(Long projectId) {
        return "/projects/" + projectId + "/invitations";
    }

    public static String acceptPath(Long invitationId) {
        return "/invitations/" + invitationId + "/accept";
    }

    public static String rejectPath(Long invitationId) {
        return "/invitations/" + invitationId + "/reject";
    }

    public static String cancelPath(Long projectId, Long invitationId) {
        return projectInvitationsPath(projectId) + "/" + invitationId + "/cancel";
    }
}
