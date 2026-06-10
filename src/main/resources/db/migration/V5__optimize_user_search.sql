create extension if not exists pg_trgm;

create index if not exists idx_users_full_name_trgm
    on users using gin (full_name gin_trgm_ops);

create index if not exists idx_users_email_trgm
    on users using gin (email gin_trgm_ops);

create index if not exists idx_follows_follower_following_status
    on follows(follower_id, following_id, status);

create index if not exists idx_follows_following_follower_status
    on follows(following_id, follower_id, status);
