create table if not exists story_views (
    id bigserial primary key,
    viewer_id bigint not null references users(id) on delete cascade,
    story_id bigint not null references stories(id) on delete cascade,
    created_at timestamptz not null default now(),
    unique (viewer_id, story_id)
);

create index if not exists idx_story_views_viewer_story
    on story_views(viewer_id, story_id);

create index if not exists idx_story_views_story
    on story_views(story_id);
