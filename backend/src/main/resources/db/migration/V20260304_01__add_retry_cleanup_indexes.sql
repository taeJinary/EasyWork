create index idx_attachment_cleanup_jobs_completed_updated_id
    on task_attachment_cleanup_jobs (completed_at, updated_at, id);

create index idx_invitation_email_retry_jobs_completed_updated_id
    on invitation_email_retry_jobs (completed_at, updated_at, id);

create index idx_notification_push_retry_jobs_completed_updated_id
    on notification_push_retry_jobs (completed_at, updated_at, id);
