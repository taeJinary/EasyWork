package com.taskflow.backend.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.SecurityFilterChain;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigClassStructureTest {

    @Test
    void JPA_설정은_Configuration과_감사기능을_활성화한다() {
        assertThat(JpaConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(JpaConfig.class.isAnnotationPresent(EnableJpaAuditing.class)).isTrue();
    }

    @Test
    void QueryDsl_설정은_JPAQueryFactory_빈을_제공한다() {
        assertThat(QueryDslConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(hasBeanMethodReturning(QueryDslConfig.class, JPAQueryFactory.class)).isTrue();
    }

    @Test
    void Redis_설정은_RedisTemplate_빈을_제공한다() {
        assertThat(RedisConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(hasBeanMethodReturning(RedisConfig.class, RedisTemplate.class)).isTrue();
    }

    @Test
    void Security_설정은_SecurityFilterChain_빈을_제공한다() {
        assertThat(SecurityConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(hasBeanMethodReturning(SecurityConfig.class, SecurityFilterChain.class)).isTrue();
    }

    private boolean hasBeanMethodReturning(Class<?> targetClass, Class<?> returnType) {
        return Arrays.stream(targetClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .anyMatch(returnType::isAssignableFrom);
    }
}
