alter table notifications
    add column if not exists target_id varchar(120),
    add column if not exists last_interactor_id bigint references users(id) on delete set null,
    add column if not exists interaction_count integer not null default 1;

create index if not exists idx_notifications_aggregation
    on notifications(user_id, type, target_id, read);
