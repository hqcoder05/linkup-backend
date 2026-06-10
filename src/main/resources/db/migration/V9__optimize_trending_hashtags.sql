create index if not exists idx_posts_created_at
    on posts(created_at desc);

create index if not exists idx_post_hashtags_hashtag_post
    on post_hashtags(hashtag_id, post_id);
