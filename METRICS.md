# Book Net API




## Metrics

### Prometheus

To access to prometheus go to http://localhost:9090/:
1. Open the URL
2. Click **Status** > **Targets** in the top menu
3. You should see `spring-boot-api` listed
    - **State**: `UP` (Green) means it is successfully reading data from your API
    - **State**: `DOWN` (Red) means it cannot reach your API container

### Grafana

URL: http://localhost:3000/

Login Credentials:
- Username: `admin`
- Password: `admin` (We defined this in `docker-compose.yml`)

How to see your data:
1. **Login** using the credentials above
2. **Add Data Source**:
    - Go to **Connections (Or Configuration)** -> **Data Sources**
    - Click **Add data source** -> Select **Prometheus**
    - **Connection URL**: `http://prometheus:9090` (Use the container name, not localhost)
    - Click **Save & Test**. You should see a green checkmark
3. **Import Dashboard**:
    - Go to **Dashboards** -> **Import**
    - Enter ID: **4701** (Standard Spring Boot Dashboard) and click **Load**
    - Select your "Prometheus" data source at the bottom
    - Click **Import**

#### Visualize in Grafana

The standard Spring Boot dashboard (4701) covers basic JVM/Tomcat stuff, but for databases, you often need specific dashboards or custom queries.

##### Import Specific Dashboards

Here are popular Community Dashboard IDs you can import (Dashboards -> Import):
- 13107 (Neo4j)
- 2583 (MongoDB) or 11378 (Spring Boot 2.1 System Monitor - includes Mongo)

##### Create a Custom "Database Health" Panel

To see everything in one place, create a new Dashboard in Grafana and add these queries:

- `mongodb_driver_pool_checkedout{application="booknet-api"}`
- `neo4j_driver_connections_active{application="booknet-api"}`
- `redis_commons_pool2_num_active{name="main-pool", application="booknet-api"}`
- `rate(mongodb_driver_commands_seconds_count{command="find"}[1m])`
- `rate(mongodb_driver_commands_seconds_count{command=~"insert|update|delete"}[1m])`
- `rate(redis_ops_seconds_count[1m])`. Legend: `{{cmd}}`
- `rate(redis_ops_seconds_sum[1m]) / rate(redis_ops_seconds_count[1m])`
- `rate(mongodb_driver_commands_seconds_count[1m])`. Legend: `{{command}}`
- `rate(neo4j_ops_seconds_count[1m])`. Legend: `{{query}}`

### Raw API Metrics (Actuator)

URL: http://localhost:8080/actuator/prometheus





