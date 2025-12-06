# Running PostgreSQL with Docker

## Quick Start

1. **Start PostgreSQL container:**
   ```bash
   docker-compose up -d
   ```

2. **Check if PostgreSQL is running:**
   ```bash
   docker-compose ps
   ```

3. **View logs:**
   ```bash
   docker-compose logs postgres
   ```

4. **Stop PostgreSQL:**
   ```bash
   docker-compose down
   ```

5. **Stop and remove all data:**
   ```bash
   docker-compose down -v
   ```

## Database Details

- **Host:** localhost
- **Port:** 5432
- **Database:** transport_db
- **Username:** postgres
- **Password:** postgres

The database `transport_db` is automatically created when the container starts.

## Verify Connection

You can verify PostgreSQL is running by checking the container:
```bash
docker ps
```

Or connect using psql (if you have PostgreSQL client):
```bash
docker exec -it transport_postgres psql -U postgres -d transport_db
```

## Troubleshooting

If port 5432 is already in use:
1. Change the port in `docker-compose.yml` (e.g., `"5433:5432"`)
2. Update `application.properties` to use the new port

