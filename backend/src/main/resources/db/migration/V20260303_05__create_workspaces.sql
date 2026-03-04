create table if not exists workspaces (
    id bigint not null auto_increment,
    owner_id bigint not null,
    name varchar(100) not null,
    description varchar(500) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    key idx_workspaces_owner_id (owner_id)
);

create table if not exists workspace_members (
    id bigint not null auto_increment,
    workspace_id bigint not null,
    user_id bigint not null,
    role varchar(20) not null,
    joined_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_workspace_members_workspace_user (workspace_id, user_id),
    key idx_workspace_members_user_id (user_id)
);
