# DbGate - Database Administration Tool

DbGate is a web-based database administration tool included in the demo stack that provides unified access to all infrastructure services.

## 🎯 What DbGate Provides

DbGate gives you web-based access to:

- **PostgreSQL Databases**: IAM, Billing, and Audit services
- **Redis Cache**: Session and cache storage
- **RabbitMQ**: Message queue management
- **MinIO S3**: Object storage browser

## 🚀 Quick Start

### Start the Demo Stack with DbGate

```bash
# Start the entire demo stack (includes DbGate)
docker compose -f compose.demo.yaml up -d

# Or on Windows PowerShell
docker compose -f compose.demo.yaml up -d
```

### Access DbGate Web UI

DbGate runs internally on the `foundation-network` and is accessible via:

```bash
# Port forward to your local machine
docker exec -it foundation-dbgate sh

# Or configure Nginx to expose it (recommended)
```

**Recommended**: Add DbGate to your Nginx configuration to access it via browser.

## 📋 Pre-configured Connections

DbGate automatically includes connections for:

### PostgreSQL Databases

| Connection                   | Server                      | Port | Database | Username        |
| ---------------------------- | --------------------------- | ---- | -------- | --------------- |
| PostgreSQL - IAM Service     | foundation-postgres-iam     | 5432 | iam      | svc_iam_dba     |
| PostgreSQL - Billing Service | foundation-postgres-billing | 5432 | billing  | svc_billing_dba |
| PostgreSQL - Audit Service   | foundation-postgres-audit   | 5432 | audit    | svc_audit_dba   |

### Redis Cache

| Connection    | Server           | Port |
| ------------- | ---------------- | ---- |
| Redis - Cache | foundation-redis | 6379 |

### RabbitMQ

| Connection               | Server              | Port  | Username         |
| ------------------------ | ------------------- | ----- | ---------------- |
| RabbitMQ - Message Queue | foundation-rabbitmq | 15672 | svc_platform_rmq |

### MinIO S3

| Connection                | Server           | Port | Access Key |
| ------------------------- | ---------------- | ---- | ---------- |
| MinIO - S3 Object Storage | foundation-minio | 9000 | iqkv       |

## 🔧 Configuration

### Environment Variables

Configure credentials via `.env` file:

```bash
# PostgreSQL
IAM_DB_USERNAME=svc_iam_dba
IAM_DB_PASSWORD=svc_iam_dba
BILLING_DB_USERNAME=svc_billing_dba
BILLING_DB_PASSWORD=svc_billing_dba
AUDIT_DB_USERNAME=svc_audit_dba
AUDIT_DB_PASSWORD=svc_audit_dba

# RabbitMQ
RABBITMQ_USERNAME=svc_platform_rmq
RABBITMQ_PASSWORD=svc_platform_rmq

# MinIO
MINIO_ROOT_USER=iqkv
MINIO_ROOT_PASSWORD=iqkv_password
```

### Custom Connections

Edit `docker/dbgate/connections.jsonl` to add custom database connections:

```json
{
  "_id": "custom-db",
  "engine": "postgres@dbgate-plugin-postgres",
  "server": "hostname",
  "port": 5432,
  "user": "username",
  "password": "password",
  "database": "dbname",
  "displayName": "Custom Database"
}
```

Supported engines:

- `postgres@dbgate-plugin-postgres` - PostgreSQL
- `redis@dbgate-plugin-redis` - Redis
- `rabbitmq@dbgate-plugin-rabbitmq` - RabbitMQ
- `s3@dbgate-plugin-s3` - S3-compatible storage
- `mysql@dbgate-plugin-mysql` - MySQL
- `mariadb@dbgate-plugin-mariadb` - MariaDB
- `mongo@dbgate-plugin-mongo` - MongoDB

## 💡 Common Tasks

### Browse PostgreSQL Tables

1. Open DbGate web interface
2. Select "PostgreSQL - IAM Service" (or other service)
3. Navigate through schemas and tables
4. Query, export, or import data

### View Redis Keys

1. Select "Redis - Cache"
2. Browse keys organized by pattern
3. View, edit, or delete key values

### Monitor RabbitMQ Queues

1. Select "RabbitMQ - Message Queue"
2. View queues, exchanges, and bindings
3. Monitor message rates and stats

### Browse S3 Buckets

1. Select "MinIO - S3 Object Storage"
2. Browse buckets and objects
3. Upload, download, or delete files

## 🔐 Security Notes

- **Development Only**: DbGate configuration is intended for local development
- **Credentials**: Update default passwords in production environments
- **Network**: DbGate only accessible within `foundation-network` by default
- **Logins**: Authentication is enabled (`LOGINS=1`)

## 📦 Docker Compose Configuration

```yaml
foundation-dbgate:
  image: dbgate/dbgate:5.3.3
  container_name: foundation-dbgate
  environment:
    WEB_ROOT: /
    LOGINS: "1"
  expose:
    - "3000"
  volumes:
    - ./docker/dbgate/connections.jsonl:/root/.dbgate/connections.jsonl:ro
    - foundation_dbgate_data:/root/.dbgate
  networks:
    - foundation-network
```

## 🛠️ Troubleshooting

### DbGate won't start

```bash
# Check logs
docker logs foundation-dbgate

# Verify connections file exists
ls docker/dbgate/connections.jsonl

# Restart DbGate
docker restart foundation-dbgate
```

### Can't connect to databases

```bash
# Verify database services are healthy
docker ps --filter name=foundation-postgres
docker ps --filter name=foundation-redis
docker ps --filter name=foundation-rabbitmq
docker ps --filter name=foundation-minio

# Check network connectivity
docker exec foundation-dbgate ping foundation-postgres-iam
```

### Reset DbGate data

```bash
# Stop and remove DbGate
docker compose -f compose.demo.yaml stop foundation-dbgate
docker compose -f compose.demo.yaml rm -f foundation-dbgate

# Remove volume
docker volume rm foundation_dbgate_data

# Restart
docker compose -f compose.demo.yaml up -d foundation-dbgate
```

## 📚 Additional Resources

- [DbGate Official Documentation](https://dbgate.org/docs/)
- [DbGate GitHub Repository](https://github.com/dbgate/dbgate)
- [Supported Database Engines](https://dbgate.org/docs/databases.html)

## 🎓 Tips

1. **Query History**: DbGate saves your SQL query history
2. **Export Data**: Export tables as CSV, JSON, or SQL
3. **Import Data**: Import data from various formats
4. **Schema Compare**: Compare database schemas
5. **ER Diagrams**: Visualize database relationships
6. **Dark Mode**: Available in settings for easier viewing

---

For issues or questions, refer to the main [README.md](README.md) or open an issue in the repository.
