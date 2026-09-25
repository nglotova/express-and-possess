-- A picture that could not be taken from a wish's link is tried again a few times: shops such
-- as Amazon sometimes answer a server with a robot check instead of the product page.
alter table expressions add column picture_attempts integer not null default 0;
alter table expressions add column picture_tried_at timestamptz;

-- Wishes whose link gave no picture so far get their first retry.
update expressions set picture_attempts = 1, picture_tried_at = now()
where picture_url is null and picture_link is not null;
