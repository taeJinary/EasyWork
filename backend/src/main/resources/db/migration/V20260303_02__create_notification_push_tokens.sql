create table if not exists notification_push_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    token varchar(512) not null,
    platform varchar(20) not null,
    is_active bit(1) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_notification_push_tokens_token unique (token),
    constraint fk_notification_push_tokens_user
        foreign key (user_id) references users (id)
);

create index idx_notification_push_tokens_user_active
    on notification_push_tokens (user_id, is_active);
