create table if not exists task_attachment_cleanup_jobs (
    id bigint not null auto_increment,
    attachment_id bigint not null,
    storage_path varchar(500) not null,
    retry_count int not null,
    next_retry_at datetime(6) not null,
    last_error_message varchar(500) null,
    completed_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
);

create index idx_attachment_cleanup_jobs_next_retry_at
    on task_attachment_cleanup_jobs (next_retry_at);

create index idx_attachment_cleanup_jobs_storage_path_completed_at
    on task_attachment_cleanup_jobs (storage_path, completed_at);
