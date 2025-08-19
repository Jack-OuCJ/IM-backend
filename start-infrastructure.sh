#!/bin/bash

echo "Starting IM Backend Services..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker first."
    exit 1
fi

# 启动基础设施
echo "Starting infrastructure services..."
docker-compose up -d

# 等待服务启动
echo "Waiting for services to start..."
sleep 30

# 检查Nacos是否启动
echo "Checking Nacos status..."
while ! curl -f http://localhost:8848/nacos/v1/ns/operator/metrics > /dev/null 2>&1; do
    echo "Waiting for Nacos to start..."
    sleep 5
done

# 检查MySQL是否启动
echo "Checking MySQL status..."
while ! docker exec mysql mysqladmin ping -h localhost --silent; do
    echo "Waiting for MySQL to start..."
    sleep 5
done

# 检查Redis是否启动
echo "Checking Redis status..."
while ! docker exec redis redis-cli ping > /dev/null 2>&1; do
    echo "Waiting for Redis to start..."
    sleep 5
done

echo "Infrastructure services started successfully!"
echo ""
echo "Service URLs:"
echo "- Nacos Console: http://localhost:8848/nacos (nacos/nacos)"
echo "- MySQL: localhost:3306 (root/root123456)"
echo "- Redis: localhost:6379"
echo "- Kafka: localhost:9092"
echo "- InfluxDB: http://localhost:8086"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin123456)"
echo "- Jaeger: http://localhost:16686"
echo ""
echo "Now you can start the Java services:"
echo "1. cd services/auth-service && mvn spring-boot:run"
echo "2. cd services/connection-service && mvn spring-boot:run"
echo "3. cd services/message-sequence-service && mvn spring-boot:run"
