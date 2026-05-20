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
- PlaceholderAPI (optional, for placeholder support)

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
| `line setaltroute <id> <altId> [priority]` | Set an alternate route (optional priority) |
| `line clearaltroute <id> [altId]` | Clear alternate route(s) (single or all) |
| `line setautoresume <id> <minutes>` | Set auto-recovery countdown |
| `line cancelautoresume <id>` | Cancel auto-recovery |
| `line setschedule <id> <start>-<end>` | Set maintenance window (HH:mm-HH:mm) |
| `line clearschedule <id>` | Clear maintenance window |
| `line stats <id>` | View line operation statistics |
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

## PlaceholderAPI support

If PlaceholderAPI is installed on your server, use these placeholders (replace `<line>` with the actual line ID):

| Placeholder | Returns |
|-------------|---------|
| `%metroaltroutes_line_status_<line>%` | Line status: `normal` / `suspended` / `maintenance` |
| `%metroaltroutes_line_status_display_<line>%` | Coloured status display text |
| `%metroaltroutes_line_boardable_<line>%` | Whether boardable: `true` / `false` |
| `%metroaltroutes_line_suspended_<line>%` | Whether suspended: `yes` / `no` |
| `%metroaltroutes_line_suspension_msg_<line>%` | Suspension notice text |
| `%metroaltroutes_line_altroutes_<line>%` | Alternate routes (comma-separated) |
| `%metroaltroutes_line_altroute_count_<line>%` | Number of alternate routes |
| `%metroaltroutes_line_name_<line>%` | Line name |
| `%metroaltroutes_line_terminus_<line>%` | Line terminus |
| `%metroaltroutes_line_stop_count_<line>%` | Number of stops |
| `%metroaltroutes_line_owner_<line>%` | Line owner |
| `%metroaltroutes_line_color_<line>%` | Line colour code |
| `%metroaltroutes_line_stats_suspend_<line>%` | Suspend count |
| `%metroaltroutes_line_stats_intercept_<line>%` | Intercept count |
| `%metroaltroutes_line_stats_alt_recommend_<line>%` | Alt-route recommendation count |
| `%metroaltroutes_line_autoresume_<line>%` | Auto-resume minutes left (0=not set) |
| `%metroaltroutes_line_schedule_<line>%` | Maintenance window (e.g. `02:00-04:00`) |

Example scoreboard or plugin usage:

```yaml
%metroaltroutes_line_status_display_line1%
%metroaltroutes_line_altroutes_line1%
```

---

## Examples

```bash
# Suspend line 1
/m line setstatus line1 suspended

# Set a suspension notice
/m line setsuspensionmsg line1 "&cLine 1 is paused, estimated recovery in 2 hours."

# Set line 2 as an alternate route with priority
/m line setaltroute line1 line2 50

# View line status
/m line status line1

# List all lines
/m line list

# View line statistics
/m line stats line1

# Set auto-recovery in 30 minutes
/m line setautoresume line1 30

# Schedule a maintenance window (02:00-04:00 daily)
/m line setschedule line1 02:00-04:00

# Clear maintenance window
/m line clearschedule line1

# Resume normal operation
/m line setstatus line1 normal

# Remove a specific alternate route
/m line clearaltroute line1 line2
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
├── data/
│   └── LineDataManager.java  # Data management: alt-route priorities, auto-recovery, schedules, statistics
├── hooks/
│   └── PlaceholderHook.java  # PlaceholderAPI expansion exposing line data
└── listeners/
    └── BoardingListener.java # Boarding block and alternate route logic
```
