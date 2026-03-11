create table email_verification_retry_jobs (
    id bigint not null auto_increment,
    user_id bigint not null,
    open_key varchar(64) null,
    retry_count int not null,
    next_retry_at datetime(6) not null,
    last_error_message varchar(500) null,
    completed_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_email_verification_retry_jobs_open_key (open_key),
    key idx_email_verification_retry_jobs_pending (completed_at, next_retry_at, id),
    key idx_email_verification_retry_jobs_updated_at (updated_at),
    constraint fk_email_verification_retry_jobs_user_id
        foreign key (user_id) references users(id)
);
