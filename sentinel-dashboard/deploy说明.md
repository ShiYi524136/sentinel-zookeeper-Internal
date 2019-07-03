# Sentinel 控制台 --生产打包部署说明

## 1. 编译和启动

### 1.1 如何编译

使用如下命令将代码打包成一个 fat jar:

```bash
mvn clean package
```

### 1.2 部署包说明



### 1.2 如何启动

本地启动命令，添加以下JVM环境变量：
```
-Dcsp.sentinel.dashboard.server=localhost:8181 -Dproject.name=sentinel-dashboard
```

使用如下命令启动编译后的控制台：

```bash
java -Dserver.port=8080 \
-Dcsp.sentinel.dashboard.server=localhost:8080 \
-Dproject.name=sentinel-dashboard \
-jar target/sentinel-dashboard.jar
```

上述命令中我们指定几个 JVM 参数，其中 `-Dserver.port=8080` 用于指定 Spring Boot 启动端口为 `8080`，其余几个是 Sentinel 客户端的参数。
为便于演示，我们对控制台本身加入了流量控制功能，具体做法是引入 `CommonFilter` 这个 Sentinel 拦截器。上述 JVM 参数的含义是：

| 参数 | 作用 |
|--------|--------|
|`Dcsp.sentinel.dashboard.server=localhost:8080`|向 Sentinel 客户端指定控制台的地址|
|`-Dproject.name=sentinel-dashboard`|向 Sentinel 指定本程序名称|


