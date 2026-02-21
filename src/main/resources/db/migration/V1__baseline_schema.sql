create table if not exists board_mappings (
    prefix varchar(16) primary key,
    board_id varchar(64) not null unique,
    board_name varchar(255) not null
);

create table if not exists tasks (
    task_key varchar(64) primary key,
    prefix varchar(16) not null references board_mappings(prefix),
    pulse_id varchar(64) not null,
    board_id varchar(64) not null,
    status varchar(64),
    started_at timestamptz,
    first_commit_at timestamptz,
    merged_at timestamptz,
    completed_at timestamptz,
    drift_score numeric(10, 2) default 0,
    risk_score numeric(10, 2) default 0
);

create index if not exists idx_tasks_prefix on tasks(prefix);
create index if not exists idx_tasks_merged_at on tasks(merged_at);

create table if not exists pull_requests (
    id bigserial primary key,
    task_key varchar(64) references tasks(task_key),
    repository varchar(255) not null,
    pr_number integer not null,
    opened_at timestamptz,
    merged_at timestamptz,
    reopen_count integer not null default 0
);

create unique index if not exists uq_pull_requests_repo_number on pull_requests(repository, pr_number);
create index if not exists idx_pull_requests_task_key on pull_requests(task_key);

create table if not exists commits (
    id bigserial primary key,
    task_key varchar(64) references tasks(task_key),
    repository varchar(255) not null,
    commit_hash varchar(64) not null,
    author varchar(255),
    committed_at timestamptz
);

create unique index if not exists uq_commits_repo_hash on commits(repository, commit_hash);
create index if not exists idx_commits_task_key on commits(task_key);

create table if not exists task_metrics (
    task_key varchar(64) primary key references tasks(task_key),
    lead_time_seconds bigint,
    cycle_time_seconds bigint,
    commit_count integer not null default 0,
    rework_rate numeric(10, 4) default 0,
    integrity_score numeric(10, 2) default 0,
    drift_score numeric(10, 2) default 0,
    risk_score numeric(10, 2) default 0
);

create table if not exists daily_snapshots (
    snapshot_date date not null,
    prefix varchar(16) not null,
    avg_lead_time bigint,
    avg_risk_score numeric(10, 2),
    drift_rate numeric(10, 4),
    rework_rate numeric(10, 4),
    primary key (snapshot_date, prefix)
);

create index if not exists idx_daily_snapshots_prefix on daily_snapshots(prefix);

create table if not exists policy_violations (
    id bigserial primary key,
    task_key varchar(64) references tasks(task_key),
    user_id varchar(64),
    type varchar(128) not null,
    severity varchar(32) not null,
    message text not null,
    created_at timestamptz not null default now(),
    resolved boolean not null default false,
    resolved_by varchar(64),
    resolved_reason text,
    resolved_at timestamptz
);

create index if not exists idx_policy_violations_task_key on policy_violations(task_key);
create index if not exists idx_policy_violations_created_at on policy_violations(created_at);

create table if not exists threshold_policies (
    id bigserial primary key,
    violation_type varchar(128) not null,
    threshold_count integer not null,
    time_window_days integer not null,
    escalation_level varchar(32) not null,
    notify_email varchar(255),
    notify_slack varchar(255),
    active boolean not null default true,
    updated_by varchar(255) not null,
    updated_at timestamptz not null default now()
);

create table if not exists threshold_change_log (
    id bigserial primary key,
    threshold_id bigint not null references threshold_policies(id),
    old_value varchar(255),
    new_value varchar(255),
    changed_by varchar(255) not null,
    changed_at timestamptz not null default now()
);

create index if not exists idx_threshold_change_log_threshold_id on threshold_change_log(threshold_id);

create table if not exists webhook_events (
    id bigserial primary key,
    source varchar(32) not null,
    payload jsonb not null,
    dedupe_key varchar(255) not null,
    correlation_id varchar(64) not null,
    received_at timestamptz not null default now(),
    processed_at timestamptz,
    status varchar(32) not null default 'RECEIVED',
    error_message text
);

create unique index if not exists uq_webhook_events_source_dedupe on webhook_events(source, dedupe_key);
create index if not exists idx_webhook_events_received_at on webhook_events(received_at);
create index if not exists idx_webhook_events_status on webhook_events(status);

create table if not exists time_sessions (
    id bigserial primary key,
    task_key varchar(64) references tasks(task_key),
    user_id varchar(64) not null,
    start_time timestamptz not null,
    end_time timestamptz,
    duration_minutes integer,
    source varchar(32) not null,
    edit_reason text
);

create index if not exists idx_time_sessions_task_key on time_sessions(task_key);

create table if not exists users (
    id varchar(64) primary key,
    email varchar(255) not null unique,
    name varchar(255) not null,
    active boolean not null default true,
    deactivated_at timestamptz
);

create table if not exists skill_groups (
    id bigserial primary key,
    name varchar(255) not null unique
);

create table if not exists user_skill_groups (
    user_id varchar(64) not null references users(id),
    skill_group_id bigint not null references skill_groups(id),
    primary key (user_id, skill_group_id)
);

create table if not exists access_audit_logs (
    id bigserial primary key,
    user_id varchar(64) not null references users(id),
    system varchar(32) not null,
    action varchar(32) not null,
    target varchar(255) not null,
    status varchar(32) not null,
    performed_by varchar(64) not null,
    timestamp timestamptz not null default now()
);

create index if not exists idx_access_audit_logs_timestamp on access_audit_logs(timestamp);
