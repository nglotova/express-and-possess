# Deploying

Two instances of the same stack on one server:

| Address | Instance |
|---|---|
| `expresspossess.glotov.ca` | The main site. Anyone can register; groups are private to their members. |
| `demo.expresspossess.glotov.ca` | The public demo: "Log in as Alice / Bob / Carol", wiped and reseeded every night. |
| `mail.demo.expresspossess.glotov.ca` | The demo's Mailpit inbox, where the demo's emails can be read. |

The two instances have separate databases, so the demo's nightly wipe never touches the main site.
Caddy sits in front of both, routes by host name, and gets the HTTPS certificates itself.

The steps below use Oracle Cloud's Always Free tier for the server, GoDaddy for DNS, and Brevo's
free plan for email. Any Linux server with Docker works the same way from step 4 on.

## 1. The server (Oracle Cloud, Always Free)

1. Create an Oracle Cloud account. The home region can't be changed later; pick one close to the
   users, such as Toronto or Montreal.
2. **Upgrade the account to Pay As You Go** (Billing, Upgrade and Manage Payment). Oracle stops and
   reclaims Always Free servers it considers idle over 7 days, and a site like this one is idle most
   of the time. Pay As You Go accounts are exempt, and nothing is charged while usage stays within
   the Always Free limits.
3. Create an instance (Compute, Instances, Create instance):
   - Image: Ubuntu 24.04.
   - Shape: Ampere `VM.Standard.A1.Flex`, 2 OCPUs, 12 GB memory. That is the whole Always Free
     allowance and enough for both instances.
   - Networking: keep the defaults, with a public IPv4 address.
   - SSH keys: let Oracle generate a key pair and download the private key.
4. Note the instance's public IP address.
5. Open ports 80 and 443 in Oracle's firewall: the instance's subnet, its default security list,
   Add ingress rules. Source `0.0.0.0/0`, TCP, destination port `80`; then the same for `443`.

Log in from the Mac, using the downloaded key:

```bash
chmod 600 ~/Downloads/ssh-key.key
ssh -i ~/Downloads/ssh-key.key ubuntu@<server IP>
```

Oracle's Ubuntu image also blocks everything but SSH inside the server. On the server:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

Install Docker:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

Log out and in again.

## 2. DNS (GoDaddy)

In GoDaddy: My Products, `glotov.ca`, DNS, Add New Record. Three records, each of type **A**,
value the server's IP address, TTL 1 hour:

| Name |
|---|
| `expresspossess` |
| `demo.expresspossess` |
| `mail.demo.expresspossess` |

These only add new names. The existing records for the domain's website and mailboxes stay as they
are.

## 3. Email (Brevo, free plan)

The main site sends invitations, password resets and notifications. The free plan allows 300 emails
a day.

The emails come from `no-reply@notify.glotov.ca`, a subdomain used only by this site. Anyone can
register and send invitations, and if someone misused that, mail services would grow suspicious of
the sending domain. A subdomain of its own keeps that apart from the mailboxes at `glotov.ca`. The
administration page also limits how many invitation emails one member can send a day (20 to start
with; site administrators have no limit).

1. Create a Brevo account.
2. Senders, Domains and Dedicated IPs, Domains, Add a domain: `notify.glotov.ca`. Brevo lists a few
   TXT and CNAME records; add each one in GoDaddy's DNS page as in step 2, then press Verify in
   Brevo. GoDaddy asks only for the part of each name before `glotov.ca`: a record Brevo names
   `something.notify.glotov.ca` is entered as `something.notify`. Each name should contain `notify`;
   the existing records for the mailboxes at `glotov.ca` stay as they are.
3. SMTP and API, SMTP tab: note the SMTP login and generate an SMTP key. They go into `main.env` in
   the next step.

The demo sends no real email: its messages go to its own Mailpit inbox.

## 4. Code and settings

On the server:

```bash
git clone https://github.com/<account>/express-and-possess.git
cd express-and-possess/deploy
cp main.env.example main.env
cp demo.env.example demo.env
```

Edit both files (`nano main.env`):

- `DB_PASSWORD`: any long random string, different in each file. `openssl rand -hex 24` makes one.
- `ADMIN_EMAILS`: the address of the site's administrator.
- `MAIL_USERNAME` and `MAIL_PASSWORD`: the Brevo SMTP login and key from step 3.
- `KAFKA_CLUSTER_ID` can stay as it is.

The host names in `caddy/Caddyfile` already match step 2.

## 5. Start

```bash
docker compose -p main --env-file main.env -f compose.yml up -d --build
docker compose -p demo --env-file demo.env -f compose.yml up -d --build
docker compose -p caddy -f caddy/compose.yml up -d
```

The first build compiles everything on the server and takes several minutes. Then:

- `https://demo.expresspossess.glotov.ca` shows the three "Log in as" buttons, with the demo family
  already seeded.
- `https://expresspossess.glotov.ca` shows the login page of the main site.

## 6. The administrator

Register on the main site with the address in `ADMIN_EMAILS`. That account gets the administration
page (the ⚙ in the top bar) and can make others administrators there.

## 7. On a phone

Open `https://expresspossess.glotov.ca` in the phone's browser. On an iPhone: Share, Add to Home
Screen. On Android, Chrome offers Install app. The site then opens from its own icon, full screen.

## Updating

```bash
cd express-and-possess
git pull
cd deploy
docker compose -p main --env-file main.env -f compose.yml up -d --build
docker compose -p demo --env-file demo.env -f compose.yml up -d --build
```

Flyway migrates the database on startup.

## What the demo does on its own

- Seeds Alice, Bob and Carol and their group on startup.
- Wipes everything and seeds again every night at 03:00 server time
  (`app.demo.reset-cron` in the API's configuration).
- Sends its emails to Mailpit, readable at `mail.demo.expresspossess.glotov.ca`.

## Backup

The main site's data is in the `main_postgres-data` and `main_uploads` volumes.

```bash
docker compose -p main exec postgres pg_dump -U expresspossess expresspossess | gzip > backup-$(date +%F).sql.gz
```
