alter table users
    add column if not exists phone_number varchar(40),
    add column if not exists date_of_birth date,
    add column if not exists email_notifications_enabled boolean not null default true,
    add column if not exists push_notifications_enabled boolean not null default true,
    add column if not exists autoplay_video_enabled boolean not null default true,
    add column if not exists content_visible_to_public boolean not null default true,
    add column if not exists search_indexing_enabled boolean not null default true,
    add column if not exists two_factor_enabled boolean not null default false,
    add column if not exists active boolean not null default true,
    add column if not exists deactivated_at timestamptz;

create index if not exists idx_refresh_tokens_user_active
    on refresh_tokens(user_id, revoked, expires_at);
