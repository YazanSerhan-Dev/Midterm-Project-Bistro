## MQTT Topics

- `bistro/reservations/today`

## Payload (JSON)
```json
{
  "today": 12,
  "served": 7
}


### `deployment/mqtt/mosquitto/mosquitto.conf.example`
```conf
# Default port
listener 1883

# Allow only local connections by default (safe)
bind_address 127.0.0.1

# If you ever need LAN access, comment bind_address and use firewall rules.
# bind_address 0.0.0.0

allow_anonymous true
persistence true
persistence_location /var/lib/mosquitto/
log_dest syslog
