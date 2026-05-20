# metro-altroutes — Metro Addon

![Minecraft](https://img.shields.io/badge/Minecraft-1.21-blue)
![Paper](https://img.shields.io/badge/Paper-1.21-brightgreen)
![Folia](https://img.shields.io/badge/Folia-%E2%9C%93-supported)

metro-altroutes is a simple addon for Metro. It helps manage line status, show suspension notices, recommend alternate routes, and block boarding on paused lines.

---

## What it does

- Set a line to normal, suspended, or maintenance
- Automatically block boarding on suspended lines
- Show a custom message when a line is paused
- Recommend alternate lines when needed
- Cache line status to reduce server load
- Work on both Paper and Folia

---

## Who should use it

- Servers that run subway or rail systems
- Servers that need clear line suspension notices
- Servers that want alternate route support
- Servers already using Metro

---

## Dependencies

- Paper 1.20.4 or newer
- Folia
- Metro 1.1.7 or newer

Metro source repository:

https://github.com/CubeX-MC/Metro

Metro Maven coordinates:

`org.cubexmc:metro:[1.1.7,)`

Note: Metro is not available on Maven Central. It is recommended to build Metro locally or use an existing Metro.jar.

---

## Install

1. Make sure Metro is installed on your server.
2. Put `metro-altroutes.jar` in the `plugins/` folder.
3. Restart the server or reload the plugin.
4. Use `/m` commands to manage lines.

---

## Commands

| Command | Description |
|---------|-------------|
| `line setstatus <id> <status>` | Set the line status |
| `line setsuspensionmsg <id> <message>` | Set the suspension notice |
| `line setaltroute <id> <altId>` | Set an alternate route |
| `line clearaltroute <id>` | Clear the alternate route |
| `line status <id>` | View the line status |
| `line info <id>` | View line details |
| `line list` | List all lines |
| `reload` | Reload cache |

### Status values

| Value | Meaning | Boardable |
|-------|---------|-----------|
| `normal` | Line is running | Yes |
| `suspended` | Line is paused | No |
| `maintenance` | Line is in maintenance | Yes |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `metroaltroutes.admin` | Manage line status | OP |
| `metroaltroutes.use` | View line info | Everyone |

---

## Examples

```bash
# Suspend line 1
/m line setstatus line1 suspended

# Set a suspension notice
/m line setsuspensionmsg line1 "&cLine 1 is paused, estimated recovery in 2 hours."

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

## Project structure

```
src/main/java/top/chenray/metroaltroutes/
├── MetroAltroutes.java       # Plugin entry point
├── cache/
│   └── RouteCache.java       # Line status cache
├── commands/
│   └── LineCommand.java      # Command handling
└── listeners/
    └── BoardingListener.java # Boarding block and alternate route logic
```
