#!/bin/bash

# Function to kill background processes when you press Ctrl+C
cleanup() {
    echo ""
    echo "Stopping all tunnels..."
    kill $MONGO_PID $NEO4J_PID $REDIS_PID 2>/dev/null
    echo "Done. Bye!"
    exit
}

# Trap the SIGINT signal (Ctrl+C) to run the cleanup function
trap cleanup SIGINT

echo "=================================================="
echo "🔌 Starting Kubernetes Tunnels for Local Dev..."
echo "=================================================="

# 1. MongoDB
echo "Starting MongoDB tunnel (27017)..."
kubectl port-forward service/mongo 27017:27017 > /dev/null 2>&1 &
MONGO_PID=$!

# 2. Neo4j (Browser + Bolt)
echo "Starting Neo4j tunnel (7474, 7687)..."
kubectl port-forward service/neo4j 7474:7474 7687:7687 > /dev/null 2>&1 &
NEO4J_PID=$!

# 3. Redis
echo "Starting Redis tunnel (6379)..."
kubectl port-forward service/redis 6379:6379 > /dev/null 2>&1 &
REDIS_PID=$!

echo "=================================================="
echo "✅ Tunnels active!"
echo "   MongoDB: localhost:27017"
echo "   Neo4j:   localhost:7474 (Browser) / :7687 (Bolt)"
echo "   Redis:   localhost:6379"
echo ""
echo "Press Ctrl+C to stop everything."
echo "=================================================="

# Wait indefinitely keeps the script running
wait