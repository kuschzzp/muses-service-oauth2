package com.goodcol.muses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodcol.muses.entity.OauthAuthorization;
import com.goodcol.muses.repository.AuthorizationRepository;
import com.goodcol.muses.repository.ClientRepository;
import com.goodcol.muses.service.MysqlRegisteredClientRepositoryImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
class MusesServiceOauth2ApplicationTests {

    @Resource
    private JdbcOperations jdbcOperations;

    @Resource
    private ClientRepository clientRepository;

    @Resource
    private AuthorizationRepository authorizationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test() {

        Optional<OauthAuthorization> byAccessTokenValue = authorizationRepository.findByAccessTokenValue(
                "eyJraWQiOiI0MTdhMTA0Mi04ODVlLTQwY2ItYjJlMS1hNDc5OTU0ZjdkN2MiLCJhbGciOiJSUzI1NiJ9" +
                        ".eyJzdWIiOiJ1c2VyMSIsImF1ZCI6Im1lc3NhZ2luZy1jbGllbnQiLCJuYmYiOjE2NzExNTc3MDcsImlzcyI6Imh0dHA6Ly8xMjcuMC4wLjE6ODU1NSIsImFjY2Vzc3p6cHp6cHp6cCI6ImFjY2Vzc3p6cHp6cHp6cCIsIndhbmd3dSI6Indhbmd3dXdhbmd3dXdhbmd3dSIsImV4cCI6MTY3MTE1OTUwNywiaWF0IjoxNjcxMTU3NzA3fQ.g91krNKTD0B0CaxJ1LII80y9LNAaY0LohVAbCPLgSIBExadb0HElxaRmMPOxu9VToiQqhRA7ebXi5XueNkQfy5cEQXzRRnZ-ACmBoWFRosKy-kDaK0lPq16lhsVXsPt36LGkiJO08RKbwx_o089UTudaybcqkM-tOpwqoPJa52R0CoNI3KMhXPlasbWMUHdX7Vb40BSiu7J8f9VdXYkQqHwR5XZFLm905PGPP2Vg56V0P8Vu_iJIJKEjYR0stw1Wf_-Cz7YCg11nh-nFdCo-uxyX2QTM5F2t34FiwAS_OrmSwJLQzviGZ-L_ywOOb9D4kr6KwwSRwxt498kLkXhRAA");

        Map<String, Object> map = null;
        try {
            map = objectMapper.readValue(objectMapper.writeValueAsString(byAccessTokenValue.get()),
                    new TypeReference<Map<String, Object>>() {
                    });
            System.out.println(map.get("oidcIdTokenClaims"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void contextLoads() {
        //        System.out.println(jdbcOperations.queryForList("select * from oauth_authorization_consent"));
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("messaging-client")
                .clientSecret("{noop}secret")
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(null)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                //可以配置多个重定向地址
                .redirectUri("http://127.0.0.1:8998/test/test1")
                .redirectUri("http://127.0.0.1:8998/hahahaha")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(true)
                                .requireProofKey(false)
                                .build()
                ).tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(30))
                                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                                .refreshTokenTimeToLive(Duration.ofDays(30))
                                .build())
                .build();

        RegisteredClientRepository registeredClientRepository
                = new MysqlRegisteredClientRepositoryImpl(clientRepository);
        RegisteredClient byClientId = registeredClientRepository.findByClientId("messaging-client");
        if (byClientId != null) {
            throw new RuntimeException("客户端ID已存在！");
        }
        registeredClientRepository.save(registeredClient);

    }
}
