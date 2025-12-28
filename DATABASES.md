# Databases




# In Docker

For local test, I choose to run my databases in Docker, using kubernetes.

For the configuration, I've created separate YAML files for each database (MongoDB, Neo4j, and Redis) that define their services and stateful sets. 
These files are designed to be easily managed and scaled within a Kubernetes environment. 
The databases are configured to use persistent volumes for data storage, ensuring that data is not lost even if the containers are restarted or the pods are deleted.

Here are the steps to run the databases in Docker:

## 1. Check if Kubernetes is Running

Using Docker Desktop:
1. Open the Docker Desktop Dashboard.
2. Look in the bottom-left corner (or top right). You should see the Kubernetes icon.
   - Green: It is running.
   - Grey/Red: It is stopped.
3. To start it: Go to Settings (Gear Icon) -> Kubernetes and ensure "Enable Kubernetes" is checked. Click Apply & Restart.
4. Wait for the status to turn green.


## 2. Check Your `kubectl` Context

If your cluster is running but `kubectl` is looking at an old or wrong cluster, you need to switch contexts.
1. List available contexts:
   ```bash
   kubectl config get-contexts
   ```
2. Set the desired context (`docker-desktop`):
   ```bash
   kubectl config use-context <context_name>
   ```
3. Verify connection:
   ```bash
   kubectl get nodes
   ```
   

## 3. Apply the configuration

```bash
kubectl apply -f secrets.yaml
kubectl apply -f mongodb.yaml
kubectl apply -f neo4j.yaml
kubectl apply -f redis.yaml
```


## 4. Initialize MongoDB Replica Set (Crucial)

Unlike Docker Compose where we used healthcheck trick, in Kubernetes, you must run the initiation command manually once after the pod starts.
1. Wait for the pod to be running
   ```bash
   kubectl get pods
   # Wait until mongodb-0 is "Running"
   ```
2. Execute the init command inside the pod:
   ```bash
   kubectl exec -it mongodb-0 -- mongosh --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'mongodb-0.mongo:27017'}]})"
   ```


## 5. Access Database from Local Machine

To access the database running inside kubernetes from your local machine,
you must create a tunnel using `kubectl port-forward`:
```bash
kubectl port-forward service/mongo 27017:27017
kubectl port-forward service/neo4j 7474:7474 7687:7687
kubectl port-forward service/redis 6379:6379
```
This will forward the database port to your local machine, allowing you to connect to it using:
- mongoDB: `localhost:27017`
- Neo4j: `localhost:7474`
- Redis: `localhost:6379`


Or running the script: `tunnels`
```bash
chmod +x tunnels.sh
./tunnels.sh
```

And the uri for connecting to:
- MongoDB:
   ```properties
   # Use this updated URI
   app.mongo-uri=mongodb://root:admin123@localhost:27017/booknet?replicaSet=rs0&directConnection=true&authSource=admin
   ```
- Neo4j:
   ```properties
   # Note: Use bolt://, not neo4j:// (neo4j:// attempts routing/clustering which fails locally)
   spring.neo4j.uri=bolt://localhost:7687
   spring.neo4j.authentication.username=neo4j
   spring.neo4j.authentication.password=admin123
   ```
- Redis:
   ```properties
   spring.redis.host=localhost
   spring.redis.port=6379
   ```

**Note On Connection Strings**: In your Java Spring Boot application (if it's also deployed in Kubernetes),
your connection strings will change to use the internal Service DNS:
- **MongoDB**: `mongodb://root:admin123@mongodb-0.mongo:27017/booknet?replicaSet=rs0`
- **Neo4j**: `neo4j://neo4j:password@neo4j:7687` (Service name)
- **Redis**: `redis` (Service name)


If mongodb replicat set doesn't initialize correctly at the beginning, run this command to reinitialize it:
```bash
kubectl exec -it mongodb-0 -- mongosh --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'mongodb-0.mongo:27017'}]})"
```
And for create a mongo-user in replicat:
```bash
kubectl exec -it mongodb-0 -- mongosh admin
db.createUser({
  user: "root",
  pwd: "admin123",
  roles: [ { role: "root", db: "admin" } ]
})
```



## The Official Kubernetes Dashboard

This is a web-based UI that runs inside your cluster. You access it via your browser.

### Step 1: Install the Dashboard

Run this command to deploy the dashboard services:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
```

### Step 2: Create an Admin User (Critical)

By default, the dashboard is locked. You need to create a "ServiceAccount" to log in.
Save this as `dashboard-admin.yaml` file

Apply it:
```bash
kubectl apply -f dashboard-admin.yaml
```

### Step 3: Get the Access Token

You need a token to log in. Generate it with this command:
```bash
kubectl -n kubernetes-dashboard create token admin-user
```

Copy the long string starting with `ey...`
Example: `eyJhbGciOiJSUzI1NiIsImtpZCI6Im1zXzJZYnI1Vm1GVGxtT3NsazlqVUwzOTZaeG11bUFBRU01SUYwN1RGY1EifQ.eyJhdWQiOlsiaHR0cHM6Ly9rdWJlcm5ldGVzLmRlZmF1bHQuc3ZjLmNsdXN0ZXIubG9jYWwiXSwiZXhwIjoxNzY2OTU4NjUzLCJpYXQiOjE3NjY5NTUwNTMsImlzcyI6Imh0dHBzOi8va3ViZXJuZXRlcy5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsIiwianRpIjoiODU1OTFiZmItMDE1MS00MzNjLWJiMjYtMTYwZTNiYWQ1YTdhIiwia3ViZXJuZXRlcy5pbyI6eyJuYW1lc3BhY2UiOiJrdWJlcm5ldGVzLWRhc2hib2FyZCIsInNlcnZpY2VhY2NvdW50Ijp7Im5hbWUiOiJhZG1pbi11c2VyIiwidWlkIjoiNGU0M2Y1YWUtYTE0YS00ZTZmLWI3NTUtZTIzNGJhOWYwZjhhIn19LCJuYmYiOjE3NjY5NTUwNTMsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlcm5ldGVzLWRhc2hib2FyZDphZG1pbi11c2VyIn0.U37q084jdK2wSx8ejZKDLErYGvmU9HwsaU_-xzGNuWCgD3C2i8FhTJorZMVdHUwJbmVEGRTqss8cVokxZUDYV7ddTZZaZqEVSFXISUvKWU3OWaQx4J5mA-sTJwrLY_vdnNvQtPho_1VOGBNDtK_KZEiXNyaxrE9trn3m119Z9371YLFXpJS2et-RI_uPSgERwlkxJyb3tNkb0mCGrbuXzSuEQAc3yvH0pcs1udp0OoTK9YTA9P8okH2aoy-IuChTsVFV1ma3gEISwVIAk07O_T8qNNbuy2O2mkkeiYFJKFXnG8BmEMAY9pm9O5MfGA6gHDJXGXRwtlNBhd4hzZazvA`

### Step 4: Access the Dashboard

1. Start the proxy server:
   ```bash
   kubectl proxy
   ```
2. Open this link in your browser: `http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/`
3. Select **Token** and paste the string you copied in Step 3.

