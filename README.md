---
title: Transport Management System
emoji: 🚛
colorFrom: blue
colorTo: indigo
sdk: docker
pinned: false
app_port: 8080
---

# Transport Management System

Backend system for managing loads, transporters, bids, and bookings.

## Deployment

This application is configured for deployment on Hugging Face Spaces using Docker.

### Environment Variables

You must configure the following Secrets in your Hugging Face Space settings:

- `DB_HOST`: The hostname of your Supabase database.
- `DB_PORT`: The port (usually 5432 or 6543).
- `DB_NAME`: The database name (e.g., postgres).
- `DB_USERNAME`: Your Supabase database user (e.g., postgres).
- `DB_PASSWORD`: Your Supabase database password.
