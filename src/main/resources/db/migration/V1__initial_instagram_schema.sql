create table if not exists users (
    id bigserial primary key,
    email varchar(200) not null unique,
    full_name varchar(120) not null,
    avatar_url varchar(500),
    private_account boolean not null default false,
    password_hash varchar(255) not null,
    role varchar(30) not null default 'USER',
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

create table if not exists profiles (
    id bigserial primary key,
    user_id bigint not null unique references users(id) on delete cascade,
    nickname varchar(120),
    bio varchar(1000),
    headline varchar(120),
    location varchar(200),
    website_url varchar(500),
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

create table if not exists posts (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    caption varchar(2000),
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

create table if not exists follows (
    follower_id bigint not null references users(id) on delete cascade,
    following_id bigint not null references users(id) on delete cascade,
    status varchar(30) not null default 'ACCEPTED',
    created_at timestamptz not null default now(),
    updated_at timestamptz,
    approved_at timestamptz,
    primary key (follower_id, following_id),
    constraint ck_follows_no_self check (follower_id <> following_id)
);

create table if not exists stories (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    caption varchar(700),
    expires_at timestamptz not null,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists media (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    post_id bigint references posts(id) on delete cascade,
    story_id bigint references stories(id) on delete cascade,
    url varchar(700) not null,
    thumbnail_url varchar(700),
    type varchar(50) not null,
    position integer not null default 0,
    width integer,
    height integer,
    original_filename varchar(200),
    file_size bigint not null default 0,
    provider_public_id varchar(120),
    created_at timestamptz not null default now()
);

create table if not exists comments (
    id bigserial primary key,
    post_id bigint not null references posts(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    content varchar(1000) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

create table if not exists likes (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    post_id bigint references posts(id) on delete cascade,
    comment_id bigint references comments(id) on delete cascade,
    created_at timestamptz not null default now()
);

create unique index if not exists uk_likes_user_post
    on likes(user_id, post_id)
    where post_id is not null;

create unique index if not exists uk_likes_user_comment
    on likes(user_id, comment_id)
    where comment_id is not null;

create table if not exists hashtags (
    id bigserial primary key,
    name varchar(120) not null unique,
    created_at timestamptz not null default now()
);

create table if not exists post_hashtags (
    id bigserial primary key,
    post_id bigint not null references posts(id) on delete cascade,
    hashtag_id bigint not null references hashtags(id) on delete cascade,
    unique (post_id, hashtag_id)
);

create table if not exists post_tags (
    id bigserial primary key,
    post_id bigint not null references posts(id) on delete cascade,
    tagged_user_id bigint not null references users(id) on delete cascade,
    media_position integer not null default 0,
    x double precision,
    y double precision,
    created_at timestamptz not null default now()
);

create table if not exists conversations (
    id bigserial primary key,
    name varchar(200),
    group_conversation boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

create table if not exists conversation_members (
    conversation_id bigint not null references conversations(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    joined_at timestamptz not null default now(),
    last_read_at timestamptz,
    primary key (conversation_id, user_id)
);

create table if not exists messages (
    id bigserial primary key,
    conversation_id bigint not null references conversations(id) on delete cascade,
    sender_id bigint not null references users(id) on delete cascade,
    content varchar(5000),
    attachment_url varchar(700),
    shared_post_id bigint references posts(id) on delete set null,
    shared_story_id bigint references stories(id) on delete set null,
    disappearing boolean not null default false,
    expires_at timestamptz,
    deleted boolean not null default false,
    created_at timestamptz not null default now()
);

create table if not exists notifications (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    type varchar(80) not null,
    title varchar(200) not null,
    content varchar(1000) not null,
    url varchar(700),
    read boolean not null default false,
    created_at timestamptz not null default now()
);

create table if not exists refresh_tokens (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    token varchar(700) not null unique,
    expires_at timestamptz not null,
    revoked boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists idx_posts_user_created on posts(user_id, created_at desc);
create index if not exists idx_follows_following_status on follows(following_id, status);
create index if not exists idx_follows_follower_status on follows(follower_id, status);
create index if not exists idx_media_post_position on media(post_id, position);
create index if not exists idx_stories_active_expires on stories(active, expires_at);
create index if not exists idx_post_tags_user on post_tags(tagged_user_id);
create index if not exists idx_hashtags_name on hashtags(name);
create index if not exists idx_messages_conversation_created on messages(conversation_id, created_at desc);
