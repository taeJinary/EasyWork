create table if not exists notification_push_retry_jobs (
    id bigint not null auto_increment,
    notification_id bigint not null,
    push_token_id bigint not null,
    retry_count int not null,
    next_retry_at datetime(6) not null,
    last_error_message varchar(500),
    completed_at datetime(6),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_notification_push_retry_jobs_open (notification_id, push_token_id, completed_at),
    key idx_notification_push_retry_jobs_pending (completed_at, next_retry_at, id),
    key idx_notification_push_retry_jobs_notification_id (notification_id),
    key idx_notification_push_retry_jobs_push_token_id (push_token_id)
);
