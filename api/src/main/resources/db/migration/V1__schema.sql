-- Express & Possess, schema version 1.
-- Statuses are stored as text with a check constraint rather than PostgreSQL enums,
-- so a new value is one migration line and JPA maps them as plain strings.

create table users (
    id            bigint generated always as identity primary key,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    name          varchar(100) not null,
    role          varchar(20)  not null default 'MEMBER',
    enabled       boolean      not null default true,
    email_enabled boolean      not null default true,
    created_at    timestamptz  not null default now(),
    constraint users_role_check check (role in ('MEMBER', 'ADMIN'))
);
-- Email is the identifier and is compared case-insensitively.
create unique index users_email_lower_idx on users (lower(email));

create table password_reset_tokens (
    id         bigint generated always as identity primary key,
    user_id    bigint      not null references users (id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at    timestamptz
);

-- New and Working are computed from whether the group has expressions; only
-- Closed and Archived are stored, so an active group is simply ACTIVE.
create table groups (
    id          bigint generated always as identity primary key,
    name        varchar(100) not null,
    owner_id    bigint       not null references users (id),
    status      varchar(20)  not null default 'ACTIVE',
    share_token varchar(64)  unique,
    created_at  timestamptz  not null default now(),
    constraint groups_status_check check (status in ('ACTIVE', 'CLOSED', 'ARCHIVED'))
);

create table group_members (
    group_id  bigint      not null references groups (id) on delete cascade,
    user_id   bigint      not null references users (id) on delete cascade,
    role      varchar(20) not null default 'MEMBER',
    joined_at timestamptz not null default now(),
    primary key (group_id, user_id),
    constraint group_members_role_check check (role in ('ADMIN', 'MEMBER'))
);
create index group_members_user_idx on group_members (user_id);

create table group_invitations (
    id          bigint generated always as identity primary key,
    group_id    bigint       not null references groups (id) on delete cascade,
    email       varchar(254) not null,
    token_hash  varchar(64)  not null unique,
    invited_by  bigint       not null references users (id),
    created_at  timestamptz  not null default now(),
    expires_at  timestamptz  not null,
    accepted_at timestamptz
);
create index group_invitations_group_idx on group_invitations (group_id);

create table expressions (
    id             bigint generated always as identity primary key,
    group_id       bigint      not null references groups (id) on delete cascade,
    creator_id     bigint      not null references users (id),
    implementer_id bigint      references users (id),
    status         varchar(20) not null default 'EXPRESSED',
    description    text        not null,
    picture_url    varchar(500),
    wanted_by      date,
    providing_by   date,
    incognito      boolean     not null default false,
    version        bigint      not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    constraint expressions_status_check
        check (status in ('EXPRESSED', 'IN_PROCESS', 'PROVIDED', 'IN_POSSESSION')),
    -- An expression has an implementer exactly when it has left EXPRESSED.
    constraint expressions_implementer_matches_status
        check ((status = 'EXPRESSED') = (implementer_id is null)),
    constraint expressions_creator_not_implementer
        check (implementer_id is null or implementer_id <> creator_id)
);
create index expressions_group_status_idx on expressions (group_id, status);
create index expressions_creator_idx on expressions (creator_id);
create index expressions_implementer_idx on expressions (implementer_id);

create table comments (
    id            bigint generated always as identity primary key,
    expression_id bigint      not null references expressions (id) on delete cascade,
    author_id     bigint      references users (id),
    body          text        not null,
    system_note   boolean     not null default false,
    created_at    timestamptz not null default now()
);
create index comments_expression_idx on comments (expression_id, created_at);

create table notifications (
    id            bigint generated always as identity primary key,
    user_id       bigint       not null references users (id) on delete cascade,
    type          varchar(40)  not null,
    group_id      bigint       references groups (id) on delete cascade,
    expression_id bigint       references expressions (id) on delete cascade,
    message       varchar(500) not null,
    read_at       timestamptz,
    created_at    timestamptz  not null default now()
);
create index notifications_user_unread_idx on notifications (user_id) where read_at is null;

-- Written in the same transaction as the change it describes; relayed to Kafka
-- by a publisher that marks published_at. See docs/plan.md, milestone M4.
create table outbox_events (
    id             bigint generated always as identity primary key,
    aggregate_type varchar(40) not null,
    aggregate_id   bigint      not null,
    event_type     varchar(40) not null,
    payload        jsonb       not null,
    created_at     timestamptz not null default now(),
    published_at   timestamptz
);
create index outbox_events_unpublished_idx on outbox_events (created_at) where published_at is null;
