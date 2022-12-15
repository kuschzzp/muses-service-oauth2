package com.goodcol.muses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <pre>
 *
 * <a href="https://github.com/spring-projects/spring-authorization-server">https://github.com/spring-projects/spring-authorization-server</a>
 *
 * <a href="https://docs.spring.io/spring-authorization-server/docs/current/reference/html/index.html">https://docs.spring.io/spring-authorization-server/docs/current/reference/html/index.html</a>
 *
 * <a href="https://jwt.ms/">https://jwt.ms/</a>
 *
 * <a href="https://blog.51cto.com/u_14558366/3135455">https://blog.51cto.com/u_14558366/3135455</a>
 *
 * <a href="https://blog.csdn.net/dghkgjlh/article/details/121665795">https://blog.csdn.net/dghkgjlh/article/details/121665795</a>
 *
 * <a href="https://docs.spring.io/spring-authorization-server/docs/current/reference/html/configuration-model.html#default-configuration">https://docs.spring.io/spring-authorization-server/docs/current/reference/html/configuration-model.html#default-configuration</a>
 *
 * <a href="https://docs.spring.io/spring-authorization-server/docs/current/reference/html/guides/how-to-jpa.html#define-data-model">https://docs.spring.io/spring-authorization-server/docs/current/reference/html/guides/how-to-jpa.html#define-data-model</a>
 *
 * <a href="https://docs.spring.io/spring-authorization-server/docs/current/reference/html/protocol-endpoints.html#oidc-user-info-endpoint">https://docs.spring.io/spring-authorization-server/docs/current/reference/html/protocol-endpoints.html#oidc-user-info-endpoint</a>
 * </pre>
 *
 * @author Mr.kusch
 * @date 2022/11/10 16:37
 */
@SpringBootApplication
public class MusesServiceOauth2Application {

    public static void main(String[] args) {
        SpringApplication.run(MusesServiceOauth2Application.class, args);
    }
}