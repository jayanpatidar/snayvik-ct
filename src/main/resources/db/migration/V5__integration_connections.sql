create table if not exists integration_connections (
    id bigserial primary key,
    system_name varchar(32) not null unique,
    active boolean not null default false,
    settings_json text not null default '{}',
    secret_ciphertext text,
    secret_nonce varchar(128),
    secret_updated_at timestamptz,
    updated_by varchar(128) not null default 'system',
    updated_at timestamptz not null default now()
);

create index if not exists idx_integration_connections_updated_at
    on integration_connections(updated_at);

create table if not exists repo_mappings (
    id bigserial primary key,
    repository varchar(255) not null unique,
    allowed_prefixes varchar(1024),
    enabled boolean not null default true,
    updated_by varchar(128) not null default 'system',
    updated_at timestamptz not null default now()
);

create index if not exists idx_repo_mappings_enabled on repo_mappings(enabled);
