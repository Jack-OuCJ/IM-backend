# Kafka Demo 项目

这个文件夹包含完整的IM后端服务源代码和Kafka数据存储。

## 项目结构

```
kafka-demo/
├── src/                    # 源代码目录
│   ├── main/
│   │   ├── java/
│   │   │   └── com/im/    # Java源码
│   │   └── resources/      # 配置文件
├── kafka1/data/           # Kafka节点1数据
├── kafka2/data/           # Kafka节点2数据
├── kafka3/data/           # Kafka节点3数据
└── README.md
```

## 启动说明

1. 启动Kafka集群（在上级目录执行）：
```bash
cd ..
docker-compose -f docker-kafka.yml up -d
```

2. 编译并运行应用：
```bash
cd kafka-demo
mvn clean package
java -jar target/im-backend-1.0.0.jar
```

## 访问Kafka UI

集群启动后，可以通过以下地址访问Kafka UI：
- URL: http://localhost:9090

## Kafka节点信息

- kafka1: localhost:9092
- kafka2: localhost:9094  
- kafka3: localhost:9096
