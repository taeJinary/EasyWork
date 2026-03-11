alter table users
    add column email_verified_at datetime(6) null after status;

update users
set email_verified_at = coalesce(created_at, now(6))
where email_verified_at is null;

create table if not exists email_verification_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    token_hash varchar(64) not null,
    expires_at datetime(6) not null,
    consumed_at datetime(6) null,
    revoked_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_email_verification_tokens_hash unique (token_hash),
    constraint fk_email_verification_tokens_user
        foreign key (user_id) references users (id)
);

create index idx_email_verification_tokens_user_active
    on email_verification_tokens (user_id, consumed_at, revoked_at, expires_at);
