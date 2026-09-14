# Build plan

Source of truth for scope: `Express & Possess - spec v3 draft.html` in `~/Natasha Work Search`
(a copy lives in `docs/spec-v3.html`). This file says in what order the spec gets built and
where each piece stops so it can be reviewed.

## Shape of the repository

```
express-and-possess/
  pom.xml                 parent: Java 21, Spring Boot 3, shared dependency versions
  api/                    Spring Boot application (Java): auth, groups, expressions, comments,
                          notifications, outbox. One deployable.
  notifier/               Spring Boot application (Kotlin): consumes Kafka, delivers email and
                          later Telegram.
  web/                    React + TypeScript + Vite, mobile-first PWA.
  docker-compose.yml      postgres, kafka, mailpit, api, notifier, web
  docs/                   spec, decisions, architecture diagram
  .github/workflows/      build and test on push
```

Packages inside `api` follow the spec's domains: `auth`, `groups`, `expressions`, `comments`,
`notifications`, `outbox`, plus `common` for shared plumbing. Each domain owns its entities,
repository, service and controller. Nothing reaches into another domain's repository; it calls
the other domain's service.

## Conventions

- Java 21 language level (`maven.compiler.release=21`), built with whatever JDK 21+ is
  installed. Kotlin only in `notifier`.
- Maven, one parent POM. Maven is on the résumé and already installed.
- PostgreSQL only, no H2. Tests run against a real PostgreSQL in Testcontainers.
- Flyway migrations are the schema. JPA never creates or alters tables.
- Every status transition is a method on the expression service with a test. The
  transition table in the spec is the test list.
- Commits happen when Natasha says so, at the end of each milestone, in her name.
- No code is written that the spec does not ask for.

## Milestones

Each milestone ends with `mvn verify` green and a short written summary of what to review.

### M0. Skeleton

Parent POM, `api` module that boots, Flyway V1 with the full schema from the spec, a
Testcontainers smoke test that runs the migration, Docker Compose with postgres and mailpit,
README stub, CI workflow.
Review: the schema. It is the one artefact that is expensive to change later.

### M1. Accounts

Register, login with a session cookie, logout, password reset by emailed token (Mailpit
locally), profile name change, `ADMIN` role on the user. Spring Security configuration.
Review: the security configuration and the password reset flow.

### M2. Groups and membership

Create and edit a group, invite by email (existing account joins at once, new address gets
a one-time link), share link on and off, remove, leave, hand over admin, close, archive,
delete. Group status New/Working computed. Membership rules from spec section 7.
Review: the invitation flow and what happens to a removed member's expressions.
Built 2026-09-14. Two rules from spec section 7 wait for the domains they depend on:
releasing a removed member's implementations (M3, needs the expression service) and the
in-app notification when an existing account is added (M4). Both are listed in the M3 and
M4 scope below.

### M3. Expressions and comments

Create, edit, delete, picture upload, the full lifecycle: Take Care as one conditional
update, Release, Provided, Got it. Incognito everywhere including comments. The concurrency
test: twenty threads press Take Care, one wins, nineteen get 409. Also from M2: when a member
is removed or leaves, any expression they are implementing is released; the My Groups
attention flags and the "has created any expression" mark on Group users.
Review: the state machine and the concurrency test. This is the interview material.

### M4. Notifications

`outbox_events` written in the same transaction as every change; a publisher relays to Kafka
topic `expression-events`; in-app notifications table and the bell endpoint with unread
count; the Kotlin `notifier` consumes the topic and sends email through Mailpit. The
flashing `!` and warning icon on My Groups are computed from the same data. Also from M2:
the in-app notification when an existing account is added to a group.
Review: the outbox publisher and the README paragraph that explains why Kafka is here.
Built 2026-09-14. One change to the spec, section 9: Incognito is chosen when Take Care is
pressed (a checkbox next to the button), not afterwards, so the "took care" notification can
say "Someone" from the start. The implementer can still change it later. In-app rows are
written in the change's transaction; only the external channels go through Kafka. One topic,
`notifications`, carries both expression and group events.

### M5. Web app

React pages in the order of the wireframes: login and register, My Groups, create and edit
group, group activity, expression, group users and a user's expressions, profile,
notifications bell. Router-based navigation with one parent link per page. PWA manifest.
Review: on a phone, against the wireframes.
Built 2026-09-14. Verified in a phone-sized browser: register, My Groups, group activity,
expression page. Two fixes came out of that run and are in the API: the error dispatch is
permitted in the security config, and `GET /api/auth/csrf` lets the web app obtain the CSRF
cookie before its first write. Not built on purpose: the Open Graph preview (left for later
in the spec).

### M6. Administration page

Users, groups including archived, force an expression's status with a reason.
Built 2026-09-14. The first administrator is whoever registers with an address listed in
`APP_ADMIN_EMAILS`; after that, administrators promote each other on the page. The role is
read into the session at login, so a promotion or revocation takes effect at the next login.
Forcing a status back to Expressed clears the implementer; any other status needs one.

### M7. Demo and deployment

Seed data with a fictional family, "Log in as Alice / Bob / Carol" buttons on the demo
build, nightly reset job, Compose file for a small server, README with architecture diagram
and a 30-second GIF. Résumé line updated to link the demo.

## What is needed from Natasha

- Docker Desktop running whenever tests run (Testcontainers needs it).
- A decision on hosting before M7 (a small VPS is assumed).
- A GitHub repository name when the code is ready to push.
