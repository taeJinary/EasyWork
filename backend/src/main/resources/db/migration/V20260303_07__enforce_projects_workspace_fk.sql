insert into workspaces (
    owner_id,
    name,
    description,
    created_at,
    updated_at
)
select distinct
    p.owner_id,
    concat('Legacy Workspace #', p.owner_id),
    'Auto-generated workspace for legacy projects',
    now(6),
    now(6)
from projects p
where p.workspace_id is null
  and not exists (
      select 1
      from workspaces w
      where w.owner_id = p.owner_id
  );

update projects p
set p.workspace_id = (
    select w.id
    from workspaces w
    where w.owner_id = p.owner_id
    order by w.updated_at desc, w.id desc
    limit 1
)
where p.workspace_id is null;

insert into workspace_members (
    workspace_id,
    user_id,
    role,
    joined_at,
    updated_at
)
select distinct
    p.workspace_id,
    p.owner_id,
    'OWNER',
    now(6),
    now(6)
from projects p
left join workspace_members wm
  on wm.workspace_id = p.workspace_id
 and wm.user_id = p.owner_id
where p.workspace_id is not null
  and wm.id is null;

alter table projects
    modify column workspace_id bigint not null;

alter table projects
    add constraint fk_projects_workspace_id
    foreign key (workspace_id) references workspaces(id);
