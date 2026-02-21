create table if not exists auth_users (
    id bigserial primary key,
    username varchar(128) not null unique,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_auth_users_active on auth_users(active);
