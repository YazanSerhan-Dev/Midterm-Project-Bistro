# MQTT Configuration (Bistro System)

This folder documents the MQTT setup used by the Bistro system for **real-time,
event-driven communication** between the backend server and IoT devices
(e.g. the Smart Queue Display).

MQTT is used instead of HTTP polling to reduce network usage and latency,
and to better match real-world IoT architectures.

---

## 1) Overview

The MQTT architecture consists of:

- **Bistro Backend (Java)**  
  Publishes queue and reservation statistics as JSON messages.

- **MQTT Broker (Mosquitto)**  
  Acts as a message hub between publishers and subscribers.

- **ESP32 Smart Queue Display**  
  Subscribes to topics and updates the physical display in real time.

---

## 2) Broker

- Broker: **Eclipse Mosquitto**
- Default port: `1883`
- Transport: TCP
- Deployment: Local Linux service (or same machine as backend)

The broker itself is **NOT** part of this repository.
Only configuration examples are provided.

---

## 3) MQTT Topics

### Published Topics

| Topic | Description |
|------|------------|
| `bistro/reservations/today` | Current day reservation statistics |

---

## 4) Message Format

Messages are sent as **JSON** payloads.

Example:

```json
{
  "today": 12,
  "served": 7
}
