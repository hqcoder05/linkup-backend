-- LinkUp Instagram-like social model migration.
-- Review on staging before production. This script preserves existing users/posts/media where possible.

alter table users
    add column if not exists private_account boolean not null default false;

-- Convert two-way connections to one-way follows.
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

do $$
begin
    if to_regclass('public.connections') is not null then
        insert into follows (follower_id, following_id, status, created_at, updated_at, approved_at)
        select requester_id, addressee_id, 'ACCEPTED', created_at, updated_at, coalesce(responded_at, updated_at, created_at)
        from connections
        where status = 'ACCEPTED'
        on conflict do nothing;
    end if;
end $$;

drop table if exists connections;

alter table media
    add column if not exists post_id bigint references posts(id) on delete cascade,
    add column if not exists story_id bigint,
    add column if not exists thumbnail_url varchar(700),
    add column if not exists position integer not null default 0,
    add column if not exists width integer,
    add column if not exists height integer;

-- Migrate legacy single image/video columns into carousel media rows.
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'posts' and column_name = 'image_url'
    ) then
        insert into media (user_id, post_id, url, thumbnail_url, type, position, original_filename, file_size, created_at)
        select p.user_id, p.id, p.image_url, p.image_url, 'image', 0, null, 0, p.created_at
        from posts p
        where p.image_url is not null
          and not exists (select 1 from media m where m.post_id = p.id and m.url = p.image_url);
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'posts' and column_name = 'video_url'
    ) then
        insert into media (user_id, post_id, url, thumbnail_url, type, position, original_filename, file_size, created_at)
        select p.user_id, p.id, p.video_url, null, 'video', 1, null, 0, p.created_at
        from posts p
        where p.video_url is not null
          and not exists (select 1 from media m where m.post_id = p.id and m.url = p.video_url);
    end if;
end $$;

alter table posts
    drop column if exists image_url,
    drop column if exists video_url;

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

create table if not exists stories (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    caption varchar(700),
    expires_at timestamptz not null,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

do $$
begin
    if not exists (
        select 1 from information_schema.table_constraints
        where constraint_schema = 'public'
          and table_name = 'media'
          and constraint_name = 'fk_media_story'
    ) then
        alter table media
            add constraint fk_media_story foreign key (story_id) references stories(id) on delete cascade;
    end if;
end $$;

alter table messages
    add column if not exists shared_post_id bigint references posts(id) on delete set null,
    add column if not exists shared_story_id bigint references stories(id) on delete set null,
    add column if not exists disappearing boolean not null default false,
    add column if not exists expires_at timestamptz;

drop table if exists resumes;

create index if not exists idx_follows_following_status on follows(following_id, status);
create index if not exists idx_follows_follower_status on follows(follower_id, status);
create index if not exists idx_media_post_position on media(post_id, position);
create index if not exists idx_stories_active_expires on stories(active, expires_at);
create index if not exists idx_post_tags_user on post_tags(tagged_user_id);
create index if not exists idx_hashtags_name on hashtags(name);
