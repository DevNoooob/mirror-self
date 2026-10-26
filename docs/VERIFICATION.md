# 工程验证记录（2026-10-13）

执行环境：提供 Java 21 和 Python 3.13，但无 Maven、Docker、PostgreSQL Server，且无法连接 Maven 仓库。

| 检查 | 结果 | 方法 |
|---|---|---|
| 纯 JDK 领域代码编译与断言 | 通过 | `bash scripts/smoke-domain.sh` |
| XML/JSON 格式与 Schema 表结构完整性 | 通过 | `python3 scripts/validate_structure.py` |
| Spring Boot 完整 `mvn test` | **未执行** | 当前环境缺少 Maven / 依赖解析网络 |
| Testcontainers PostgreSQL + pgvector 集成 | **未执行** | 当前环境无 Docker |
| HTTP 实际请求 / Docker Compose 拉起 | **未执行** | 当前环境无 Docker |
| 完整中文语义记忆抽取 / LLM 验证 | 不在 V0.1 范围 | 当前仅保守规则 + 摘录式检索 |

注意：源码按官方 Spring Boot 4.0.8 / Spring AI 2.0.1 / pgvector 0.8.6 兼容文档选型，但**没有宣称整个工程在本环境已成功构建**。请在开发机执行：

```bash
mvn test
mvn -Dtest=PostgresMemoryIT test  # 需要 Docker，观察是否跳过
cp .env.example .env              # 设置两个强随机密钥
docker compose up --build -d
curl http://127.0.0.1:8080/api/v1/health
```

若实际构建/迁移失败，以 Maven/Flyway 日志为准定位；不要将静态结构检查视为端到端测试通过。
