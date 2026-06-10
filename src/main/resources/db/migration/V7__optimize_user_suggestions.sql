create index if not exists idx_profiles_user_location
    on profiles(user_id, lower(location))
    where location is not null;

create index if not exists idx_posts_user_id
    on posts(user_id);

create index if not exists idx_post_hashtags_post_hashtag
    on post_hashtags(post_id, hashtag_id);

create index if not exists idx_post_hashtags_hashtag_post
    on post_hashtags(hashtag_id, post_id);
