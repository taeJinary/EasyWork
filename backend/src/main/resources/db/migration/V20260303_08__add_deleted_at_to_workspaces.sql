alter table workspaces
    add column deleted_at datetime(6) null;

create index idx_workspaces_deleted_at
    on workspaces (deleted_at);
