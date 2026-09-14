# Deploying

Two instances of the same stack on one small server: the private family one and the
public demo. Caddy in front routes by host name and manages the certificates.

Tested against Docker Compose v2. A 2 vCPU / 4 GB server is enough for both instances;
Kafka is the hungriest part.

## 1. Server

Any Linux box with Docker and a public IP. On a fresh Ubuntu:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

Log out and in again.

## 2. DNS

Three A records to the server's IP, for example:

| Name | Purpose |
|---|---|
| `wishes.example.com` | the family instance |
| `demo.example.com` | the public demo |
| `mail.demo.example.com` | the demo's Mailpit inbox |

## 3. Code and settings

```bash
git clone <repository> express-and-possess
cd express-and-possess/deploy
cp family.env.example family.env
cp demo.env.example demo.env
```

Edit both files. `KAFKA_CLUSTER_ID` is any 22-character base64 string; `DB_PASSWORD` is any
string. The family instance needs a real SMTP relay in `MAIL_HOST`, `MAIL_PORT` and
`MAIL_FROM`. Put the host names from step 2 into `caddy/Caddyfile`.

## 4. Start

```bash
docker compose -p family --env-file family.env -f compose.yml up -d --build
docker compose -p demo   --env-file demo.env   -f compose.yml up -d --build
docker compose -p caddy  -f caddy/compose.yml up -d
```

The first build takes a few minutes. Then open `https://demo.example.com`: the login page
shows the three "Log in as" buttons and the family is already seeded.

## 5. The first family administrator

Register on the family instance with an address listed in `ADMIN_EMAILS`. That account
gets the administration page; it can promote others there.

## Updating

```bash
git pull
docker compose -p family --env-file family.env -f compose.yml up -d --build
docker compose -p demo   --env-file demo.env   -f compose.yml up -d --build
```

Flyway migrates the database on startup.

## What the demo does on its own

- Seeds Alice, Bob and Carol and their group on startup.
- Wipes everything and seeds again every night at 03:00 server time
  (`app.demo.reset-cron` in the API's configuration).
- Sends its emails to Mailpit, readable at `mail.demo.example.com`.

## Backup

The family instance's data is the `family_postgres-data` and `family_uploads` volumes.

```bash
docker compose -p family exec postgres pg_dump -U expresspossess expresspossess | gzip > backup-$(date +%F).sql.gz
```
