alter table posts
    add column if not exists likes_count integer not null default 0,
    add column if not exists comments_count integer not null default 0;

update posts p
set likes_count = counts.likes_count
from (
    select post_id, count(*)::integer as likes_count
    from likes
    where post_id is not null
    group by post_id
) counts
where p.id = counts.post_id;

update posts p
set comments_count = counts.comments_count
from (
    select post_id, count(*)::integer as comments_count
    from comments
    group by post_id
) counts
where p.id = counts.post_id;

create index if not exists idx_posts_engagement_rank
    on posts(likes_count desc, comments_count desc, created_at desc);

create index if not exists idx_likes_user_post_author
    on likes(user_id, post_id);

create index if not exists idx_comments_user_post
    on comments(user_id, post_id);
