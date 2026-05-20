# metro-altroutes — Metro Addon

[![Metro](https://img.shields.io/badge/Metro-%E2%86%91%20GitHub-blue?style=flat-square)](https://github.com/CubeX-MC/Metro)

> **Line status management · Suspension announcements · Alternate route suggestions · Ride blocking**

---

## About Metro

This plugin is an addon for [Metro](https://github.com/CubeX-MC/Metro), a high-performance Minecraft rail transit plugin supporting Paper 1.20+ and Folia.

🔗 Metro GitHub: https://github.com/CubeX-MC/Metro

---

## Features

| Feature | Description |
|---------|-------------|
| 🚦 **Line Status** | Set lines to normal / suspended / maintenance |
| 🚫 **Ride Blocking** | Automatically block boarding on suspended lines |
| 📢 **Suspension Message** | Show custom messages when boarding is blocked |
| 🔀 **Alternate Routes** | Recommend alternate lines automatically |
| ⚡ **Cache Optimisation** | Loads on startup, refreshes on demand (TTL 30s), reduced main-thread overhead |
| 🧵 **Folia Compatible** | Auto-detects Paper or Folia runtime |

---

## Dependencies

- **Paper** 1.20.4+ or **Folia**
- **Metro** (https://github.com/CubeX-MC/Metro)

---

## Build

> Metro API dependency is automatically resolved via [JitPack](https://jitpack.io) — no manual setup needed.

```bash
mvn clean package
```

The built plugin will be at `target/metro-altroutes.jar`. Place it in your server's `plugins/` folder.

---

## Commands

All commands use the `/m` prefix in-game.

| Command | Description |
|---------|-------------|
| `line setstatus <id> <status>` | Set line operational status |
| `line setsuspensionmsg <id> <message>` | Set suspension announcement (supports § colour codes) |
| `line setaltroute <id> <altId>` | Set an alternate route |
| `line clearaltroute <id>` | Clear alternate routes |
| `line status <id>` | View line status |
| `line info <id>` | View full line snapshot |
| `line list` | List all lines with status |
| `reload` | Reload cache (hot reload) |

### Status Values

| Value | Effect | Boardable |
|-------|--------|-----------|
| `normal` | Normal operation (default) | ✅ Yes |
| `suspended` | Service suspended | ❌ No (blocked + notified) |
| `maintenance` | Under maintenance | ✅ Yes |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `metroaltroutes.admin` | Manage line operational status | OP |
| `metroaltroutes.use` | View line information | Everyone |

---

## Examples

```bash
# Suspend line 1
/m line setstatus line1 suspended

# Set suspension message
/m line setsuspensionmsg line1 "&cLine 1 is under maintenance, estimated recovery in 2 hours."

# Set line 2 as an alternate route
/m line setaltroute line1 line2

# View line status
/m line status line1

# List all lines
/m line list

# Resume normal operation
/m line setstatus line1 normal

# Clear alternate routes
/m line clearaltroute line1
```

---

## Workflow

1. Admin sets a line to `suspended`
2. Cache auto-updates within 30 seconds
3. When a player tries to board, **boarding is blocked**
4. The blocked player sees:
   - 📢 Suspension message (if set)
   - 🔀 Alternate route suggestion (if set)
5. When restored to `normal`, boarding resumes normally

---

## Project Structure

```
src/main/java/top/chenray/metroaltroutes/
├── MetroAltroutes.java       # Main class (API hook, task scheduling)
├── cache/
│   └── RouteCache.java       # Data cache (performance)
├── commands/
│   └── LineCommand.java      # /m line command executor
└── listeners/
    └── BoardingListener.java # Ride blocker & route recommender
```
