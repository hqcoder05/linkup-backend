create table if not exists saved_posts (
    user_id bigint not null references users(id) on delete cascade,
    post_id bigint not null references posts(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, post_id)
);

create index if not exists idx_saved_posts_user_created
    on saved_posts(user_id, created_at desc);
