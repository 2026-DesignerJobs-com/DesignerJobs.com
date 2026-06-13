# Setting environment variables (for the Pexels key & friends)

The "Design Inspiration" section on the profile page needs a **Pexels API key**. The key is
**not** in the repo — you supply it through the `PEXELS_API_KEY` environment variable. Without
it, `/api/design-inspiration` just returns 401 and that one section stays empty; the rest of the
app runs fine.

> **Never paste the key into the code or commit it.** It only goes in an environment variable.

## 1. Get a key (once)
Sign in at <https://www.pexels.com/api/> → "Your API Key" → copy it.

## 2. Set `PEXELS_API_KEY`

### IntelliJ IDEA (how most of us run it)
1. **Run ▸ Edit Configurations…**
2. Pick the Spring Boot run config (the one that starts `Main`/`mvn spring-boot:run`).
3. In **Environment variables**, click the little icon and add:
   ```
   PEXELS_API_KEY=your-key-here
   ```
4. **Apply** → run as usual. (It's stored in *your* IDE config, not in git.)

### macOS / Linux terminal
```sh
export PEXELS_API_KEY=your-key-here
cd backend && mvn spring-boot:run
```
To make it permanent, add that `export …` line to `~/.zshrc` (macOS) or `~/.bashrc` (Linux) and open a new terminal.

### Windows
- **PowerShell:** `$env:PEXELS_API_KEY="your-key-here"` then `mvn spring-boot:run` (same window).
- **cmd:** `set PEXELS_API_KEY=your-key-here` then `mvn spring-boot:run`.
- Permanent: *Edit the system environment variables* → **Environment Variables…** → New → `PEXELS_API_KEY`.

### systemd (server / production)
When the app runs as a systemd service (see `reviewsANDguides/DEPLOYMENT.md`), don't put the
secret in the `.service` file (it's world-readable). Put it in the service's **env file**,
root-owned and locked down:
```sh
sudo sh -c 'echo "PEXELS_API_KEY=your-key-here" >> /etc/designerjobs.env'
sudo chmod 600 /etc/designerjobs.env
sudo systemctl restart designerjobs
```
The unit loads it via `EnvironmentFile=/etc/designerjobs.env`. (No `daemon-reload` needed for an
env-file change — just `restart`.)

## 3. Check it worked
With the app running on `:8080`:
```sh
curl "http://localhost:8080/api/design-inspiration?query=branding"
```
- JSON with `photos` → ✅ key is picked up.
- `401` / empty → the env var isn't set in the process you started the app from (re-check the run config / terminal).

## Other env vars (same idea)
The app reads everything overridable through `${VAR:default}` in `backend/src/main/resources/application.properties`:

| variable | what it's for |
|---|---|
| `PEXELS_API_KEY` | Pexels "design inspiration" key (this guide) |
| `APP_JWT_SECRET` | JWT signing key — must be ≥ 32 chars; override outside dev |
| `APP_CORS_ALLOWED_ORIGINS` | comma-separated CORS origins |
| `APP_LOG_LEVEL` | our log level (`INFO` for quiet, `DEBUG` default) |

Set any of them exactly the same way. For server/systemd deployment, put them in the
`EnvironmentFile` described in `reviewsANDguides/DEPLOYMENT.md`.
