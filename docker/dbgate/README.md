# DbGate Configuration

This directory contains the DbGate database administration tool configuration.

## Files

- `connections.jsonl` - Pre-configured database connections in JSONL format

## Connections Configuration

The `connections.jsonl` file contains pre-configured connections for all infrastructure services:

### Format

Each line is a JSON object with the following structure:

```json
{
  "_id": "unique-connection-id",
  "engine": "database-engine@plugin-name",
  "server": "hostname-or-service-name",
  "port": 5432,
  "user": "username",
  "password": "password",
  "database": "database-name",
  "displayName": "Human Readable Name"
}
```

### Pre-configured Connections

1. **PostgreSQL - IAM Service**
   - ID: `postgres-iam`
   - Server: `foundation-postgres-iam:5432`
   - Database: `iam`

2. **PostgreSQL - Billing Service**
   - ID: `postgres-billing`
   - Server: `foundation-postgres-billing:5432`
   - Database: `billing`

3. **PostgreSQL - Audit Service**
   - ID: `postgres-audit`
   - Server: `foundation-postgres-audit:5432`
   - Database: `audit`

4. **Redis - Cache**
   - ID: `redis-master`
   - Server: `foundation-redis:6379`

5. **RabbitMQ - Message Queue**
   - ID: `rabbitmq-management`
   - Server: `foundation-rabbitmq:15672`

6. **MinIO - S3 Object Storage**
   - ID: `minio-s3`
   - Server: `foundation-minio:9000`

## Adding Custom Connections

To add a custom connection, append a new line to `connections.jsonl`:

```json
{
  "_id": "my-custom-db",
  "engine": "postgres@dbgate-plugin-postgres",
  "server": "my-server",
  "port": 5432,
  "user": "myuser",
  "password": "mypass",
  "database": "mydb",
  "displayName": "My Custom Database"
}
```

### Supported Engines

- `postgres@dbgate-plugin-postgres` - PostgreSQL
- `mysql@dbgate-plugin-mysql` - MySQL
- `mariadb@dbgate-plugin-mariadb` - MariaDB
- `mongo@dbgate-plugin-mongo` - MongoDB
- `redis@dbgate-plugin-redis` - Redis
- `rabbitmq@dbgate-plugin-rabbitmq` - RabbitMQ
- `s3@dbgate-plugin-s3` - S3-compatible storage

## Updating Credentials

Credentials in this file should match the environment variables in your `.env` file:

```bash
# PostgreSQL
IAM_DB_USERNAME=svc_iam_dba
IAM_DB_PASSWORD=svc_iam_dba

# RabbitMQ
RABBITMQ_USERNAME=svc_platform_rmq
RABBITMQ_PASSWORD=svc_platform_rmq

# MinIO
MINIO_ROOT_USER=iqkv
MINIO_ROOT_PASSWORD=iqkv_password
```

After updating credentials:

1. Update `.env` file
2. Update `connections.jsonl`
3. Restart the demo stack:
   ```bash
   docker compose -f compose.demo.yaml restart foundation-dbgate
   ```

## Security Notes

⚠️ **Important**: This configuration file contains plaintext passwords and is intended for local development only.

- Do not commit real production credentials to version control
- Use environment variables or secrets management for production
- Consider using `.gitignore` for sensitive connection files

## Troubleshooting

### Connection fails with "Unknown host"

Ensure the service name matches exactly what's in `compose.demo.yaml`:

- `foundation-postgres-iam` (not `postgres-iam`)
- `foundation-redis` (not `redis`)

### Authentication errors

1. Verify credentials in `.env` match `connections.jsonl`
2. Check service logs: `docker logs foundation-postgres-iam`
3. Restart DbGate: `docker restart foundation-dbgate`

### Connections not appearing in DbGate UI

1. Verify file is mounted: `docker exec foundation-dbgate cat /root/.dbgate/connections.jsonl`
2. Check JSON syntax (each line must be valid JSON)
3. Restart DbGate to reload connections

## More Information

See [docker/dbgate.md](../dbgate.md) in the project docker directory for complete DbGate documentation.
