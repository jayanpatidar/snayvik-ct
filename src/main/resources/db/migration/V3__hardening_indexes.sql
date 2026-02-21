create index if not exists idx_policy_violations_user_type_created
    on policy_violations(user_id, type, created_at);

create index if not exists idx_policy_violations_resolved_created
    on policy_violations(resolved, created_at);

create index if not exists idx_time_sessions_user_start
    on time_sessions(user_id, start_time);

create index if not exists idx_time_sessions_user_task
    on time_sessions(user_id, task_key);

create index if not exists idx_webhook_events_source_status_received
    on webhook_events(source, status, received_at);
