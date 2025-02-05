package com.goodcol.muses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodcol.muses.entity.OauthAuthorization;
import com.goodcol.muses.entity.OauthTestUser;
import com.goodcol.muses.repository.AuthorizationRepository;
import com.goodcol.muses.repository.ClientRepository;
import com.goodcol.muses.repository.UserRepository;
import com.goodcol.muses.service.DefaultRegisteredClientRepositoryImpl;
import com.goodcol.muses.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ClientRepository clientRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private AuthorizationRepository authorizationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    PasswordEncoder bCryptPasswordEncoder;

    /*
    /connect/register
    {
        "client_id": "postman-oidc",
        "client_id_issued_at": "",
        "client_name": "postman-oidc",
        "client_secret": "postman-oidc",
        "client_secret_expires_at": "",
        "token_endpoint_auth_method": "client_secret_basic",
        "scope": ["openid","profile","client.create","message.read","niubi666","client.read","message.write"],
        "grant_types": ["refresh_token","client_credentials","authorization_code"],
        "redirect_uris": ["http://127.0.0.1:8555/hahahaha","http://127.0.0.1:8555/test/test1"],
        "response_types": "",
        "id_token_signed_response_alg": "",
        "jwks_uri": "",
        "token_endpoint_auth_signing_alg": ""
    }
     */

    /**
     * 模拟向数据库注册信息
     */
    @Test
    void contextLoads() {
        //先删除数据库数据再新增，每次手动删除，烦死了
        jdbcTemplate.update("delete from oauth_test_user where 1=1");
        jdbcTemplate.update("delete from oauth_client where 1=1");
        jdbcTemplate.update("delete from oauth_authorization where 1=1");
        jdbcTemplate.update("delete from oauth_authorization_consent where 1=1");

        /*
         * 内置的 OIDC 客户端注册端点,非客户端
         *  https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationRequest
         * org.springframework.security.oauth2.server.authorization.oidc.web.OidcClientRegistrationEndpointFilter
         * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
         */
        String clientId = "messaging-client";
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                //                .clientSecret("{noop}secret")
                .clientSecret(bCryptPasswordEncoder.encode("secret"))
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(null)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                //支持的授权类型
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                //可以配置多个重定向地址
                .redirectUri("http://127.0.0.1:8555/test/test1")
                .redirectUri("http://127.0.0.1:8555/hahahaha")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
                .scope("niubi666")
                // OIDC 注册客户端端点、读取客户端端点必须要的scope，但是仅能包含其一个，
                // 同时你 获取code的那个URL必须去掉 scope 里面的 openid，否则会自动添加到scope，即使你只选了 client.create
                // 那最后的scope还是会包含openid，导致注册端点的scope校验过不去
                .scope("client.create")
                .scope("client.read")
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(true)
                                .requireProofKey(false)
                                .build()
                ).tokenSettings(
                        TokenSettings.builder()
                                //token有效期
                                .accessTokenTimeToLive(Duration.ofMinutes(30))
                                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                                .refreshTokenTimeToLive(Duration.ofDays(30))
                                .build())
                .build();

        RegisteredClientRepository registeredClientRepository
                = new DefaultRegisteredClientRepositoryImpl(clientRepository);
        RegisteredClient byClientId = registeredClientRepository.findByClientId(clientId);
        if (byClientId != null) {
            throw new RuntimeException("客户端ID" + clientId + "已存在！");
        }
        registeredClientRepository.save(registeredClient);
        System.out.println("新建注册的客户端信息成功======================================");

        String username = "zhangsan";
        OauthTestUser testUser = new OauthTestUser();
        testUser.setUsername(username);
        //        testUser.setPassword("{noop}123123");
        testUser.setPassword(bCryptPasswordEncoder.encode("123123"));
        testUser.setAuthCodes("authCodeOne,authCodeTwo");

        Optional<OauthTestUser> zhangsan = userRepository.findUserByUsername(username);
        if (zhangsan.isPresent()) {
            throw new RuntimeException("用户zhangsan已存在！");
        }
        userRepository.save(testUser);
        System.out.println("新建用户 " + username + "----123123 成功======================================");

    }

    /**
     * NimbusJwtDecoder 校验token是否有效的方式
     */
    @Test
    public void nimbusJwt() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:8555/oauth2/jwks").build();
        org.springframework.security.oauth2.jwt.Jwt jwt = decoder.decode("eyJraWQiOiJrdXNjaHp6cCIsImFsZyI6IlJTMjU2In0" +
                ".eyJzdWIiOiJ6aGFuZ3NhbiIsImF1ZCI6Im1lc3NhZ2luZy1jbGllbnQiLCJuYmYiOjE2NzI3MTUxODgsInNjb3BlIjpbIm9wZW5pZCIsIm1lc3NhZ2UucmVhZCIsIm1lc3NhZ2Uud3JpdGUiXSwiaXNzIjoiaHR0cDovLzEyNy4wLjAuMTo4NTU1IiwiaWt1biI6IuiUoeW-kOWdpCIsImV4cCI6MTY3MjcxNjk4OCwiaWt1bm5ubm5uIjoi5bCP6buR5a2QIiwiaWF0IjoxNjcyNzE1MTg4fQ.qvgEX0iF42ds3BoHUSGG0-ZhseWpDuoE7mDehXIxGonUXJIulB2UW0DlPUS4HLbx7v8EjYwbJwcZrL_XurcON-cF9wTSjuFJWHBtNSPK7juIsl9YX6ReALiQuPjdTVG_vLh5Hmz_hJzT3MsE0ZYhEalXFV1cSyYvgnbYQDuwX8How0Lz0rrTLxF4bTXWaEjza_ROpFRhr6Y8ha8XP32XsniGduGQOd6lJxubJ7DBiQSvIOUyy0TRYX6oa76pCTXHouyqXVWML-y_fUgBkGfREZg1ZaDzs1-AY7vfTvQi-FtDcFUSVzJxdao4WgeIUAKBCY7c8cAAF5mlkXYP0xup_7ZLYP_dI8gvNXvZ8Myb_0tqoc-3X2eyoSVVeU1OIZ1JEL2Kn7kewnagLY741GhXHAbl1oPVkeNcBUszdqq2A9TMQGg73rCI6fpcQuMx0EzKAvjCFdozYvOpm8WD8n1TCWonisPl_f5-NiAcMUa__D99PIIYfAXgfBJHOR4xuaNZqxDp70e7H6y_3Z-_mEtL6hxmnWLDNIBXn6UPZy_mE0TjuGHsb71Nfx7tga2ej9E5gEHRqFlE6Tw09BY21jAex6Vbv83m0_DOZdI28kRLYLL3qMeg8rntxwQWU23NN8coA9-k0RqTorJ5ce4uCAWFXlY0-ccZhpar51XTbUocx70");
        Map<String, Object> claims = jwt.getClaims();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            System.out.println(entry.getKey() + "----" + entry.getValue());
        }
    }


    @Test
    void test() {

        Optional<OauthAuthorization> byAccessTokenValue = authorizationRepository.findByAccessTokenValue(
                "eyJraWQiOiI0MTdhMTA0Mi04ODVlLTQwY2ItYjJlMS1hNDc5OTU0ZjdkN2MiLCJhbGciOiJSUzI1NiJ9" +
                        ".eyJzdWIiOiJ1c2VyMSIsImF1ZCI6Im1lc3NhZ2luZy1jbGllbnQiLCJuYmYiOjE2NzExNTc3MDcsImlzcyI6Imh0dHA6Ly8xMjcuMC4wLjE6ODU1NSIsImFjY2Vzc3p6cHp6cHp6cCI6ImFjY2Vzc3p6cHp6cHp6cCIsIndhbmd3dSI6Indhbmd3dXdhbmd3dXdhbmd3dSIsImV4cCI6MTY3MTE1OTUwNywiaWF0IjoxNjcxMTU3NzA3fQ.g91krNKTD0B0CaxJ1LII80y9LNAaY0LohVAbCPLgSIBExadb0HElxaRmMPOxu9VToiQqhRA7ebXi5XueNkQfy5cEQXzRRnZ-ACmBoWFRosKy-kDaK0lPq16lhsVXsPt36LGkiJO08RKbwx_o089UTudaybcqkM-tOpwqoPJa52R0CoNI3KMhXPlasbWMUHdX7Vb40BSiu7J8f9VdXYkQqHwR5XZFLm905PGPP2Vg56V0P8Vu_iJIJKEjYR0stw1Wf_-Cz7YCg11nh-nFdCo-uxyX2QTM5F2t34FiwAS_OrmSwJLQzviGZ-L_ywOOb9D4kr6KwwSRwxt498kLkXhRAA");

        Map<String, Object> map = null;
        try {
            map = objectMapper.readValue(objectMapper.writeValueAsString(byAccessTokenValue.get()),
                    new TypeReference<>() {
                    });
            System.out.println(map.get("oidcIdTokenClaims"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void jjwt() {
        Jws<Claims> claimsJws = JwtUtils.parseJWT(JwtUtils.JWT_SEPARATOR + "eyJhbGciOiJIUzI1NiJ9" +
                        ".eyJzdWIiOiJsb2dpbiIsImp0aSI6ImNjZDRjOTgyLWVjNzEtNGM3My04Y2VhLTQ3Mjc1YWJkNTljNyIsImlzcyI6Ik9BdXRoMiIsImlhdCI6MTY3MzM0MjQ1MCwiZXhwIjoxNjczMzQ0MjUwLCJsb2dpbklkIjoiemhhbmdzYW4iLCJmb3JjZVJlc2V0UHdkIjoiJDJhJDEwJDZrc2dNY0ZJU3pjQloyeXBobUh6OS5wOFZpcFhCMzg5VnFqbXNLWFVVTDRsemUwS1BhdjJpIiwic2NvcGUiOlsib3BlbmlkIiwibml1Ymk2NjYiXX0.d4Zw2X2DN8JJAlmuREUJyOIfE9lUX3JHoUQ4gZJyHI8",
                "thealgorithm-specificsigningkeytousetodigitallysigntheJWT");

        System.out.println(claimsJws.getBody());
    }

}
