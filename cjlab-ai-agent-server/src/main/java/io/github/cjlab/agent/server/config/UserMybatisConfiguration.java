package io.github.cjlab.agent.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "io.github.cjlab.agent.user.persistence.mapper",
        "io.github.cjlab.agent.memory.persistence.mapper",
        "io.github.cjlab.agent.rag.persistence.mapper"
})
public class UserMybatisConfiguration {
}
