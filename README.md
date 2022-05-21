## mysql2postgresql-jdbc-agent

- 使用`java agent`无侵入的方式使用`postgresql`替换`mysql`. 参考 `https://github.com/wuwen5/ojdbc-mysql2oracle` 移植了`postgresql`版本
- `nacos`基于`2.1.0` 测试
- `xxljob`基于`2.3.0` 测试

### nacos 配置

### xxl-job 配置

## jar 启动方式

- 配置java agent

## docker

```
docker build -t mysql2postgresql-jdbc-agent:1.0.0 .
docker tag mysql2postgresql-jdbc-agent:1.0.0 221.214.10.82:81/mysql2postgresql-jdbc-agent/mysql2postgresql-jdbc-agent:1.0.0
docker push 221.214.10.82:81/mysql2postgresql-jdbc-agent/mysql2postgresql-jdbc-agent:1.0.0
```

## k8s

- initContainers