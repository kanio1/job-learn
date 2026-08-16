# Triage labels

Canonical roles live as the `Status:` line on local markdown tickets (not GitHub labels).

| Role | `Status:` value | Meaning |
|---|---|---|
| needs-triage | `needs-triage` | Maintainer needs to evaluate |
| needs-info | `needs-info` | Waiting on more information |
| ready-for-agent | `ready-for-agent` | Fully specified; agent can implement |
| ready-for-human | `ready-for-human` | Needs a human (Keycloak, secrets, judgement) |
| wontfix | `wontfix` | Will not be actioned |

Every triaged item also has exactly one `Category: bug` or `Category: enhancement`.
