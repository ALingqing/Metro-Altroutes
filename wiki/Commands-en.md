# Command Reference

All metro-altroutes commands use the `/m` alias.

---

## Line Status Management

| Command | Description |
| :--- | :--- |
| `/m line setstatus <lineId> <status>` | Set line operating status |

**Status values:**

| Value | Effect | Boardable |
| :--- | :--- | :--- |
| `normal` | Normal operation | ✅ Yes |
| `suspended` | Suspended | ❌ No |
| `maintenance` | In maintenance | ✅ Yes |

---

## Suspension Message

| Command | Description |
| :--- | :--- |
| `/m line setsuspensionmsg <lineId> <message>` | Set the suspension notice text |

Supports `&` colour codes.

---

## Alternate Route Management

| Command | Description |
| :--- | :--- |
| `/m line setaltroute <lineId> <altId> [priority]` | Add an alternate route (lower number = higher priority) |
| `/m line clearaltroute <lineId> [altId]` | Remove alternate route(s) (omit altId to clear all) |

Priority is optional, defaults to `100`. Alternate routes are recommended in ascending priority order.

---

## Auto Recovery

| Command | Description |
| :--- | :--- |
| `/m line setautoresume <lineId> <minutes>` | Set auto-recovery countdown. Line resumes automatically when timer expires |
| `/m line cancelautoresume <lineId>` | Cancel auto-recovery |

---

## Scheduled Maintenance

| Command | Description |
| :--- | :--- |
| `/m line setschedule <lineId> <start>-<end>` | Set a daily maintenance window (HH:mm-HH:mm) |
| `/m line clearschedule <lineId>` | Clear the scheduled maintenance window |

The line will suspend automatically when the window starts and resume when it ends.

---

## Queries

| Command | Description |
| :--- | :--- |
| `/m line stats <lineId>` | View line operation statistics |
| `/m line status <lineId>` | View current line status |
| `/m line info <lineId>` | View detailed line snapshot |
| `/m line list` | List all lines on the server |

---

## System Management

| Command | Description |
| :--- | :--- |
| `/m reload` | Reload cache and plugin config (requires `metroaltroutes.admin`) |

---

## Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `metroaltroutes.admin` | Manage line status and notices | OP |
| `metroaltroutes.use` | View line status and info | Everyone |

---

## PlaceholderAPI Placeholders

| Placeholder | Returns |
| :--- | :--- |
| `%metroaltroutes_line_status_<line>%` | Line status |
| `%metroaltroutes_line_status_display_<line>%` | Coloured status display |
| `%metroaltroutes_line_boardable_<line>%` | Boardable (true/false) |
| `%metroaltroutes_line_suspended_<line>%` | Suspended (yes/no) |
| `%metroaltroutes_line_suspension_msg_<line>%` | Suspension notice |
| `%metroaltroutes_line_altroutes_<line>%` | Alternate route list |
| `%metroaltroutes_line_altroute_count_<line>%` | Alternate route count |
| `%metroaltroutes_line_name_<line>%` | Line name |
| `%metroaltroutes_line_terminus_<line>%` | Terminus |
| `%metroaltroutes_line_stop_count_<line>%` | Stop count |
| `%metroaltroutes_line_owner_<line>%` | Owner |
| `%metroaltroutes_line_color_<line>%` | Colour |
| `%metroaltroutes_line_stats_suspend_<line>%` | Suspend count |
| `%metroaltroutes_line_stats_intercept_<line>%` | Intercept count |
| `%metroaltroutes_line_stats_alt_recommend_<line>%` | Alt-recommend count |
| `%metroaltroutes_line_autoresume_<line>%` | Auto-resume minutes left |
| `%metroaltroutes_line_schedule_<line>%` | Maintenance window |
