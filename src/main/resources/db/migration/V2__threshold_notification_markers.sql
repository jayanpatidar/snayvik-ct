create table if not exists threshold_notification_markers (
    id bigserial primary key,
    threshold_id bigint not null references threshold_policies(id),
    user_id varchar(64) not null,
    window_start date not null,
    last_notified_at timestamptz not null default now()
);

create unique index if not exists uq_threshold_marker on threshold_notification_markers(threshold_id, user_id, window_start);
create index if not exists idx_threshold_marker_notified_at on threshold_notification_markers(last_notified_at);
