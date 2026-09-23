-- Messages members send to the site administrators through Contact us.
create table contact_messages (
    id         bigint generated always as identity primary key,
    sender_id  bigint        not null references users (id) on delete cascade,
    topic      varchar(20)   not null check (topic in ('PROBLEM', 'SUGGESTION', 'OTHER')),
    body       varchar(2000) not null,
    page       varchar(500),
    created_at timestamptz   not null
);
create index contact_messages_sender_idx on contact_messages (sender_id, created_at);
