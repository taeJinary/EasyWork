create table if not exists invitation_email_retry_jobs (
    id bigint not null auto_increment,
    invitation_id bigint not null,
    invitee_email varchar(320) not null,
    project_name varchar(100) not null,
    inviter_nickname varchar(50) not null,
    role varchar(20) not null,
    retry_count int not null default 0,
    next_retry_at datetime(6) not null,
    last_error_message varchar(500) null,
    completed_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    index idx_invitation_email_retry_jobs_next_retry_at (next_retry_at),
    index idx_invitation_email_retry_jobs_invitation_id (invitation_id)
);
