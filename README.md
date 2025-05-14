# Keycloak을 활용한 Client Credentials Grant + JWKS 검증 + OpenFeign M2M 호출 모의 테스트

</br>

아래에 **PG → AppCard → CardIssuer → Bank** 의 4개 Spring Boot 애플리케이션을 모의로 만들어, 각 `/hello` 엔드포인트가

```
hello keycloak! i'm pg-server
hello keycloak! i'm appcard-server
…
```

를 반환하도록 하고, 각 호출마다 Keycloak에서 발급받은 Access Token(Client Credentials Grant·JWKS 검증)을 사용하도록 설정하는 전체 구조를 모의 테스트합니다.

---

</br>

## 1. Keycloak 설정

1. **Realm** 생성 (예: `my-realm`)
2. **Clients** 4개 등록

   * `pg-server`, `appcard-server`, `cardissuer-server`, `bank-server`
   * **Access Type**: `confidential`
   * **Service Accounts Enabled**: ON
   * **Standard Flow Enabled**: OFF
   * **Direct Access Grants Enabled**: OFF
3. 각 클라이언트의 **Credentials** 탭에서 `Client Secret` 확보
4. **Client Scopes** → 역할 매핑

   * 예: `pg-server` → Optional Scope에 `pg-to-appcard` 추가
   * `pg-to-appcard` Scope엔 `appcard-server`의 `hello` API 접근용 Role(예: `hello:access`) 매핑
   * 마찬가지로 `appcard-to-cardissuer`, `issuer-to-bank` Scope 생성·매핑

> M2M 통신용 Client → Scope → Role 구조
>
> ```
> pg-server --[pg-to-appcard]--> appcard-server:hello
> appcard-server --[appcard-to-cardissuer]--> cardissuer-server:hello
> …
> ```

</br>

---

</br>

## 2. 공통 Gradle/Maven 의존성

```gradle
plugins {
    id 'org.springframework.boot' version '3.0.0'
    id 'io.spring.dependency-management' version '1.1.0'
    id 'java'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencyManagement {
    // Spring Cloud OpenFeign 의존성 관리 (선택)
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:2022.0.3"
    }
}

dependencies {
    // 웹서버
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // OAuth2 Client (Client Credentials Grant)
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    // Resource Server (JWT/JWKS 검증)
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    // OpenFeign
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

```

</br>

---

</br>

## 3. 각 서버별 `application.yml` 예시 (PG 서버)

```yaml
server:
  port: 8080

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # JWKS 엔드포인트
          jwk-set-uri: https://<KEYCLOAK_HOST>/realms/my-realm/protocol/openid-connect/certs
      client:
        registration:
          pg-server:
            client-id: pg-server
            client-secret: <PG_SECRET>
            authorization-grant-type: client_credentials
            scope: pg-to-appcard
        provider:
          keycloak:
            token-uri: https://<KEYCLOAK_HOST>/realms/my-realm/protocol/openid-connect/token

feign:
  client:
    config:
      pg-to-appcard:
        # AppCard 서버 호출용 Feign 클라이언트 설정
        url: http://localhost:8081
```

* **resource-server.jwt** 로 JWKS 기반 서명 검증
* **oauth2.client.registration.pg-server** 으로 Client Credentials Grant 요청
* **feign.client.config.pg-to-appcard.url** 로 AppCard 서버 지정

</br>

> 다른 서버들(`appcard-server`, `cardissuer-server`, `bank-server`)도 `server.port`, `registration.*`, `scope`만 바꿔(`appcard-to-cardissuer`, …), `feign.client.config` 에 다음 서버 호스트만 바꾸면 동일하게 적용됩니다.

</br>

---

</br>

## 4. Security & Feign 설정 (공통)

```java
@Configuration
@EnableFeignClients
public class SecurityConfig {

  // OAuth2AuthorizedClientManager 빈
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clients,
      OAuth2AuthorizedClientRepository authClients) {

    var provider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build();

    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, authClients);
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }

  // Feign 요청 시 Bearer Token 자동 첨부
  @Bean
  public RequestInterceptor oauth2FeignInterceptor(OAuth2AuthorizedClientManager manager) {
    return requestTemplate -> {
      var authRequest = OAuth2AuthorizeRequest.withClientRegistrationId("pg-server")
        .principal("pg-server")
        .build();
      var client = manager.authorize(authRequest);
      String token = client.getAccessToken().getTokenValue();
      requestTemplate.header("Authorization", "Bearer " + token);
    };
  }

  // 모든 /hello 엔드포인트 인증 필요
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(authz -> authz
        .requestMatchers("/hello").authenticated()
        .anyRequest().permitAll()
      )
      .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
    return http.build();
  }
}
```

* **OAuth2AuthorizedClientManager**: Client Credentials Grant 처리
* **RequestInterceptor**: Feign 호출 시 자동으로 `pg-server` 토큰 첨부
* **SecurityFilterChain**: `/hello` 요청은 JWT 검증 후 통과

다른 서버들도 `withClientRegistrationId("appcard-server")`, `...("cardissuer-server")`, `...("bank-server")` 로 각각 Interceptor를 복제합니다.

</br>

---

</br>

## 5. Controller & Feign Client (PG 서버 예)

```java
@RestController
public class HelloController {

  private final AppCardClient appCardClient;

  public HelloController(AppCardClient appCardClient) {
    this.appCardClient = appCardClient;
  }

  @GetMapping("/hello")
  public String hello() {
    // 로컬 메시지 출력 + AppCard 호출 예시
    String downstream = appCardClient.hello();
    return "hello keycloak! i'm pg-server\n-> " + downstream;
  }
}

@FeignClient(name = "pg-to-appcard", configuration = SecurityConfig.class)
public interface AppCardClient {
  @GetMapping("/hello")
  String hello();
}
```

* `/hello` 호출 시 “hello keycloak! i'm pg-server” + AppCard 서버의 응답을 덧붙여 보여줌
* AppCard 호출에도 Feign Interceptor가 `pg-to-appcard` 토큰을 붙여 줍니다.

</br>

---

</br>

## 6. 나머지 서버들 구성

* **AppCard 서버(포트 8081)**

  * `client.registration.appcard-server.scope: appcard-to-cardissuer`
  * Feign Client→CardIssuer, `/hello` 반환 `"hello keycloak! i'm appcard-server"`
* **CardIssuer 서버(8082)**

  * `client.registration.cardissuer-server.scope: issuer-to-bank`
  * Feign Client→Bank, `/hello` 반환 `"hello keycloak! i'm cardissuer-server"`
* **Bank 서버(8083)**

  * `client.registration.bank-server.scope:` (필요없으면 공란)
  * `/hello` 반환 `"hello keycloak! i'm bank-server"`

모두 **SecurityConfig**, **application.yml** 패턴을 복사·수정만 하면 됩니다.

</br>

---

</br>

### ✅ 테스트 순서

1. **Keycloak**: 모든 Client & Scope & Role 매핑 완료
2. **은행 서버**부터 순차적 기동:

   ```bash
   cd bank-server && ./mvnw spring-boot:run
   cd cardissuer-server && ./mvnw spring-boot:run
   …
   ```
3. **PG 서버**에 `/hello` 호출

   ```bash
   curl http://localhost:8080/hello
   ```

   * PG→AppCard→CardIssuer→Bank 로직이 실행되며
   * 최종적으로

     ```
     hello keycloak! i'm pg-server
     -> hello keycloak! i'm appcard-server
     -> hello keycloak! i'm cardissuer-server
     -> hello keycloak! i'm bank-server
     ```

     와 같이 연결되어 출력되면 성공!

