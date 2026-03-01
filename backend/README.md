# Daifu Manage Backend

## Prerequisites

- JDK 17+
- Maven 3.9+
- MySQL 8.0+

## Quick start

1. Create database:

```sql
CREATE DATABASE daifu_manage DEFAULT CHARACTER SET utf8mb4;
```

2. Update datasource in `src/main/resources/application.yml`.
3. Run:

```bash
mvn spring-boot:run
```

4. Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## Initial admin

- username: `admin`
- password hash is provided in SQL initialization script

Replace it with your own secure password before production.


