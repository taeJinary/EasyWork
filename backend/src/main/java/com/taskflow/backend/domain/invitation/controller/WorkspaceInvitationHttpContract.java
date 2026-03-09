package com.taskflow.backend.domain.invitation.controller;

public final class WorkspaceInvitationHttpContract {

    public static final String WORKSPACE_INVITATIONS_PATH = "/workspaces/{workspaceId}/invitations";
    public static final String MY_INVITATIONS_PATH = "/workspace-invitations/me";
    public static final String ACCEPT_PATH = "/workspace-invitations/{invitationId}/accept";
    public static final String REJECT_PATH = "/workspace-invitations/{invitationId}/reject";
    public static final String CANCEL_PATH = "/workspaces/{workspaceId}/invitations/{invitationId}/cancel";

    private WorkspaceInvitationHttpContract() {
    }

    public static String workspaceInvitationsPath(Long workspaceId) {
        return "/workspaces/" + workspaceId + "/invitations";
    }

    public static String acceptPath(Long invitationId) {
        return "/workspace-invitations/" + invitationId + "/accept";
    }

    public static String rejectPath(Long invitationId) {
        return "/workspace-invitations/" + invitationId + "/reject";
    }

    public static String cancelPath(Long workspaceId, Long invitationId) {
        return workspaceInvitationsPath(workspaceId) + "/" + invitationId + "/cancel";
    }
}
