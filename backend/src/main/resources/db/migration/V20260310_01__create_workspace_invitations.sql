create table if not exists workspace_invitations (
    id bigint not null auto_increment,
    workspace_id bigint not null,
    inviter_user_id bigint not null,
    invitee_user_id bigint not null,
    role varchar(20) not null,
    status varchar(20) not null,
    responded_at datetime(6) null,
    expires_at datetime(6) not null,
    pending_key varchar(100) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_workspace_invitation_pending_key unique (pending_key),
    constraint fk_workspace_invitations_workspace
        foreign key (workspace_id) references workspaces (id),
    constraint fk_workspace_invitations_inviter
        foreign key (inviter_user_id) references users (id),
    constraint fk_workspace_invitations_invitee
        foreign key (invitee_user_id) references users (id)
);

create index idx_workspace_invitations_invitee_status_created
    on workspace_invitations (invitee_user_id, status, created_at desc);

create index idx_workspace_invitations_invitee_created
    on workspace_invitations (invitee_user_id, created_at desc);
