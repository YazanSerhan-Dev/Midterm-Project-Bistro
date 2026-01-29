# Linux Deployment (Bistro Headless Server)

This folder documents how we run the **Bistro server** on Linux in a production-like way:
- Server runs as a **systemd service**
- Automatically restarts if it crashes
- Loads DB config from `server.properties` (kept **out of GitHub**)

---

## 1) Prerequisites
- Ubuntu 22.04+ (or similar)
- Java installed (example: OpenJDK 25+)
- (Optional) MySQL on Linux OR remote MySQL reachable
- Network time sync enabled (important for time-based jobs)

---

## 2) Recommended folder layout on the Linux machine

We recommend storing the jar in:
- `/opt/bistro/ServerHeadless.jar`

And storing secrets in:
- `/opt/bistro/server.properties`  ✅ (NOT committed)

---

## 3) Install systemd service

### Step A — Copy the service file
Copy:
`deployment/linux/systemd/bistro.service`

to:
`/etc/systemd/system/bistro.service`

### Step B — Reload systemd and start the service
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now bistro
