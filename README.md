> ## distrib-proxy
> 2018-05-29 | Zachary Bai | 代理分配，依赖`spring-data-redis`，需自己实现`RedisTemplate` Bean 的实现

---
### Group
所有 `Group` 共享全部的 proxy

### Client
单个 `Group` 下的所有 `Client` 不能重复使用同一个 proxy