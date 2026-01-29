# 🍽️ Bistro – Restaurant Management System

Bistro is a full-stack **restaurant management system** developed as a **team-based academic project**, designed to simulate real-world restaurant operations using modern software engineering practices.

The project focuses on **client–server architecture**, **concurrency**, **database management**, and **clean software design**.

---

## 📌 Project Overview

The Bistro system enables restaurants to manage their daily operations efficiently, including reservations, waiting lists, table allocation, and billing.

This project was developed as part of a **Software Engineering degree**, emphasizing real-world constraints and production-style architecture.

---

## 🧩 System Architecture

### 1️⃣ Client Application (JavaFX)
- Used by customers, subscribers, and staff
- Create and manage reservations
- Join and leave waiting lists
- Terminal-based check-in
- Bill payment flow
- Communicates with the server over TCP

### 2️⃣ Server Application (Java + OCSF)
- Central business logic and validation
- Handles concurrent client requests
- Manages reservations, waiting lists, tables, and bills
- Time-based reminder logic

### 3️⃣ Database (MySQL)
- Persistent data storage
- Uses connection pooling
- Stores users, reservations, tables, bills, activities, and waiting lists

---

## 🛠️ Technologies Used

- **Java 25**
- **JavaFX**
- **MySQL 8**
- **OCSF (Object Client–Server Framework)**
- **Kryo Serialization**
- **Linux (Deployment Environment)**

---

## 📦 Core Features

### 🔐 User Management
- Customers and subscribers
- Terminal access without login
- Subscriber identification via terminal

### 📅 Reservations
- Create, cancel, and check-in reservations
- Validation based on restaurant opening hours
- Concurrency-safe reservation handling

### ⏳ Waiting List
- Join and leave waiting list
- Automatic table assignment
- Status flow: WAITING → ASSIGNED → CANCELED
- Notifications when a table becomes available

### 🪑 Table Management
- Track table availability
- Prevent conflicts with upcoming reservations
- Dynamic assignment logic

### 💳 Billing
- Bill creation after check-in
- Track session duration
- Time-based reminders
- Payment and checkout flow

---

## 🧠 Software Engineering Principles

- Separation of concerns (Client / Server / DAO / DTO)
- Thread-safe server logic
- Connection pooling for database access
- Status-driven business logic
- Modular and maintainable architecture

---

## 📁 Project Structure (High-Level)

## Deployment & Infrastructure (Personal Extension)

In addition to the academic requirements of the project, I personally extended
the system to explore production-like deployment and real-time communication
concepts.

These extensions were developed as part of a **personal initiative** and were
not required by the course.

The extended features include:
- Deploying the backend server on **Linux in headless mode**
- Running the server as a **systemd service**, enabling:
  - automatic startup on boot
  - automatic restart on failure
- Externalizing runtime configuration (e.g. database credentials) and keeping
  sensitive data out of version control
- Publishing real-time system statistics using **MQTT** to support external
  IoT devices (Smart Queue Display)

Detailed documentation for these extensions can be found here:

- 📁 `deployment/linux` – Headless Linux deployment using systemd  
- 📁 `deployment/mqtt` – MQTT setup, topics, and broker configuration

