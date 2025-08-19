#!/bin/bash

echo "========================================="
echo "IM Backend System Test Script"
echo "========================================="

# 检查服务是否运行
check_service() {
    local service_name=$1
    local port=$2
    local path=$3
    
    echo "Checking ${service_name}..."
    if curl -f "http://localhost:${port}${path}" > /dev/null 2>&1; then
        echo "✅ ${service_name} is running on port ${port}"
        return 0
    else
        echo "❌ ${service_name} is not responding on port ${port}"
        return 1
    fi
}

# 测试认证服务
test_auth_service() {
    echo ""
    echo "Testing Auth Service..."
    
    # 测试登录
    response=$(curl -s -X POST http://localhost:8081/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{
            "username": "testuser1",
            "password": "123456"
        }')
    
    if echo "$response" | grep -q "jwtToken"; then
        echo "✅ Login test passed"
        # 提取JWT Token
        jwt_token=$(echo "$response" | grep -o '"jwtToken":"[^"]*"' | cut -d'"' -f4)
        echo "JWT Token: ${jwt_token:0:50}..."
        
        # 测试Token验证
        verify_response=$(curl -s -X GET http://localhost:8081/api/auth/verify \
            -H "Authorization: Bearer $jwt_token")
        
        if echo "$verify_response" | grep -q "Token valid"; then
            echo "✅ Token verification test passed"
        else
            echo "❌ Token verification test failed"
        fi
    else
        echo "❌ Login test failed"
        echo "Response: $response"
    fi
}

# 测试连接服务
test_connection_service() {
    echo ""
    echo "Testing Connection Service..."
    
    # 测试连接统计
    response=$(curl -s http://localhost:8082/api/connection/stats)
    
    if echo "$response" | grep -q "localConnections"; then
        echo "✅ Connection stats test passed"
        echo "Stats: $response"
    else
        echo "❌ Connection stats test failed"
        echo "Response: $response"
    fi
    
    # 测试用户在线状态
    response=$(curl -s http://localhost:8082/api/connection/online/user001)
    
    if echo "$response" | grep -q "code"; then
        echo "✅ User online status test passed"
        echo "User online status: $response"
    else
        echo "❌ User online status test failed"
        echo "Response: $response"
    fi
}

# 测试序列号服务
test_sequence_service() {
    echo ""
    echo "Testing Message Sequence Service..."
    
    # 测试获取C2C序列号
    response=$(curl -s "http://localhost:8083/api/sequence/c2c?fromUserId=user001&toUserId=user002")
    
    if echo "$response" | grep -q "code"; then
        echo "✅ C2C sequence test passed"
        echo "C2C sequence: $response"
    else
        echo "❌ C2C sequence test failed"
        echo "Response: $response"
    fi
    
    # 测试获取群聊序列号
    response=$(curl -s "http://localhost:8083/api/sequence/group?groupId=group001")
    
    if echo "$response" | grep -q "code"; then
        echo "✅ Group sequence test passed"
        echo "Group sequence: $response"
    else
        echo "❌ Group sequence test failed"
        echo "Response: $response"
    fi
}

# 测试基础设施
test_infrastructure() {
    echo ""
    echo "Testing Infrastructure..."
    
    # 检查Nacos
    if curl -f http://localhost:8848/nacos/v1/ns/operator/metrics > /dev/null 2>&1; then
        echo "✅ Nacos is running"
    else
        echo "❌ Nacos is not running"
    fi
    
    # 检查Redis
    if docker exec redis redis-cli ping > /dev/null 2>&1; then
        echo "✅ Redis is running"
    else
        echo "❌ Redis is not running"
    fi
    
    # 检查MySQL
    if docker exec mysql mysqladmin ping -h localhost --silent > /dev/null 2>&1; then
        echo "✅ MySQL is running"
    else
        echo "❌ MySQL is not running"
    fi
    
    # 检查InfluxDB
    if curl -f http://localhost:8086/health > /dev/null 2>&1; then
        echo "✅ InfluxDB is running"
    else
        echo "❌ InfluxDB is not running"
    fi
}

# 主测试流程
main() {
    echo "Starting system tests..."
    
    # 检查基础设施
    test_infrastructure
    
    # 检查服务状态
    echo ""
    echo "Checking service health..."
    check_service "Auth Service" 8081 "/actuator/health"
    check_service "Connection Service" 8082 "/actuator/health"
    check_service "Sequence Service" 8083 "/actuator/health"
    
    # 等待服务完全启动
    echo ""
    echo "Waiting for services to be fully ready..."
    sleep 5
    
    # 执行功能测试
    test_auth_service
    test_connection_service
    test_sequence_service
    
    echo ""
    echo "========================================="
    echo "Test completed!"
    echo "========================================="
    echo ""
    echo "WebSocket test:"
    echo "Open your browser and go to: http://localhost:8082"
    echo "Or use WebSocket client to connect: ws://localhost:8082/ws/im?userId=user001"
    echo ""
    echo "API Documentation:"
    echo "- Auth Service: http://localhost:8081/doc.html"
    echo "- Connection Service: http://localhost:8082/doc.html" 
    echo "- Sequence Service: http://localhost:8083/doc.html"
    echo ""
    echo "Monitoring:"
    echo "- Grafana: http://localhost:3000 (admin/admin123456)"
    echo "- Prometheus: http://localhost:9090"
    echo "- Jaeger: http://localhost:16686"
}

# 运行测试
main
