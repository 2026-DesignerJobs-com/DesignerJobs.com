# Deployment guide — building the jar & running as a service

How to build the backend into a runnable jar and run it as a Linux **systemd** service.

## 0. What you're deploying

One Spring Boot 3.2 process (JDK 17) that serves **both** the REST API and the static
frontend (`frontend/design3/`) on a single port (default **8080**). State lives in an
embedded **H2 file database** — there is no separate DB server to run.

## 1. Prerequisites

| where | needs |
|---|---|
| build host | JDK **17** + Maven 3.9+, and a JDK-17 Maven *toolchain* (`~/.m2/toolchains.xml`, see `backend/README.md`) |
| server | a JRE/JDK **17** (`java -version` → 17). Nothing else — H2 is embedded. |

## 2. Build the jar

From `backend/`:

```sh
mvn clean package
```

Output: **`backend/target/designerjobs-backend-1.0-SNAPSHOT.jar`** — an executable Spring Boot
"fat" jar (run it with `java -jar`).

- The build forces JDK 17 via the toolchains plugin; without `~/.m2/toolchains.xml` it fails
  fast with *"Cannot find matching toolchain"*.
- If a failing test blocks the build: `mvn clean package -DskipTests` (prefer fixing tests; only
  skip when you know why).
- Copy the jar to the server, e.g. `scp backend/target/designerjobs-backend-1.0-SNAPSHOT.jar user@server:/tmp/app.jar`.

## 3. Server layout — the relative-path gotcha (read this)

By default three paths are **relative to the process working directory**:

| what | default | property / override |
|---|---|---|
| frontend files | `../frontend/design3/` | `--app.frontend.path=…` |
| H2 database | `./data/projectdb` | `-Ddb.url=jdbc:h2:file:…` (JVM system property) |
| log file | `./logs/app.log` | `--logging.file.name=…` |

If you set `WorkingDirectory=/opt/designerjobs`, then `./data` and `./logs` resolve correctly
**inside** it — but `../frontend/design3` resolves to `/opt/frontend/design3` (one level up),
which is wrong. So **the frontend path must be set explicitly** (the service file below does this).

Recommended layout:

```
/opt/designerjobs/
├── app.jar
├── frontend/design3/      # copy the whole frontend/design3 here
├── data/                  # H2 db, created on first run
└── logs/
```

Create a service user and the directories:

```sh
sudo useradd --system --home /opt/designerjobs --shell /usr/sbin/nologin designerjobs
sudo mkdir -p /opt/designerjobs/{data,logs}
sudo cp /tmp/app.jar /opt/designerjobs/app.jar
sudo cp -r frontend/design3 /opt/designerjobs/frontend/design3   # (copy from a checkout)
sudo chown -R designerjobs:designerjobs /opt/designerjobs
```

## 4. Configuration

**Always override the JWT secret** — the built-in default is dev-only (and must be ≥ 32 chars).
Generate one:

```sh
openssl rand -base64 48
```

Put secrets in a root-owned env file `/etc/designerjobs.env` (`chmod 600`):

```sh
APP_JWT_SECRET=<paste the generated secret>
APP_CORS_ALLOWED_ORIGINS=https://your-domain.example
APP_LOG_LEVEL=INFO
```

These map to `app.jwt.secret`, `app.cors.allowed-origins`, and the log level via Spring's
relaxed binding. Token lifetime is `app.jwt.expiry-millis` (2 h) — override with
`--app.jwt.expiry-millis=…` if needed.

## 5. systemd service

`/etc/systemd/system/designerjobs.service`:

```ini
[Unit]
Description=DesignerJobs.com (Spring Boot)
After=network.target

[Service]
User=designerjobs
Group=designerjobs
WorkingDirectory=/opt/designerjobs
EnvironmentFile=/etc/designerjobs.env
ExecStart=/usr/bin/java -jar /opt/designerjobs/app.jar \
  --server.port=8080 \
  --app.frontend.path=/opt/designerjobs/frontend/design3/
# (DB -> ./data and logs -> ./logs resolve under WorkingDirectory automatically.
#  To put the DB elsewhere add, BEFORE -jar:  -Ddb.url=jdbc:h2:file:/abs/path/projectdb )
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
# light hardening
NoNewPrivileges=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/designerjobs

[Install]
WantedBy=multi-user.target
```

Enable and start:

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now designerjobs
sudo systemctl status designerjobs
journalctl -u designerjobs -f          # live logs
```

Verify: `curl -i http://localhost:8080/jobs` (200) and `curl -I http://localhost:8080/index.html` (200).

## 6. HTTPS / reverse proxy (recommended)

Keep the app on `127.0.0.1:8080` and put **nginx** (with a Let's Encrypt cert) in front:

```nginx
server {
    listen 443 ssl;
    server_name your-domain.example;
    # ssl_certificate / ssl_certificate_key ...

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Then set `APP_CORS_ALLOWED_ORIGINS=https://your-domain.example`. The frontend uses an iframe
shell (`X-Frame-Options: SAMEORIGIN`) and the JS hardcodes `http://localhost:8080` as the API
base — for a real domain you'll also need to update those fetch base URLs in `frontend/design3/`.

## 7. Operations

- **Logs:** `journalctl -u designerjobs` plus the file `/opt/designerjobs/logs/app.log`. Use
  `APP_LOG_LEVEL=INFO` in production (DEBUG is very noisy).
- **Health:** Actuator is on the classpath → `GET /actuator/health` (or just hit `/jobs`).
- **Update:** rebuild the jar → copy over `app.jar` → `sudo systemctl restart designerjobs`.
- **Backup:** the single file `data/projectdb.mv.db` *is* the whole database — back it up
  (stop the service first for a consistent copy).
- **Reset state:** `sudo systemctl stop designerjobs`, delete `data/projectdb.mv.db`, start again.

## 8. Production checklist

- [ ] `APP_JWT_SECRET` overridden with a strong ≥32-char value (not the dev default).
- [ ] Runs as the non-root `designerjobs` user.
- [ ] Behind HTTPS (reverse proxy).
- [ ] `APP_CORS_ALLOWED_ORIGINS` = your real origin only.
- [ ] `APP_LOG_LEVEL=INFO`; `logs/` and `/etc/designerjobs.env` have tight permissions.
- [ ] `data/projectdb.mv.db` is backed up.
