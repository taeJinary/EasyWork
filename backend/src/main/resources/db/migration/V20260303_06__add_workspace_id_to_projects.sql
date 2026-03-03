alter table projects
    add column workspace_id bigint null;

create index idx_projects_workspace_id
    on projects (workspace_id);
