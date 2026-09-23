-- Settings for the whole site, changed on the administration page. Exactly one row.
create table site_settings (
    id                  smallint primary key default 1 check (id = 1),
    invitations_per_day integer  not null check (invitations_per_day between 1 and 1000)
);
insert into site_settings (invitations_per_day) values (20);

-- One row per invitation email sent, for the daily limit. Kept when the invitation is
-- cancelled, so inviting and cancelling cannot get round the limit.
create table invitation_emails (
    id        bigint generated always as identity primary key,
    sender_id bigint      not null references users (id) on delete cascade,
    sent_at   timestamptz not null
);
create index invitation_emails_sender_idx on invitation_emails (sender_id, sent_at);
