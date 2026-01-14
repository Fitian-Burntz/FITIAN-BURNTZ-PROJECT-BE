package com.fitian.burntz.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.cofig
 * @fileName : SwaggerConfig
 * @date : 2025-09-04
 * @description : Swagger 관련 설정 파일
 * http://localhost:8080/swagger-ui/index.html
 */

@Configuration
public class SwaggerConfig {
    // API 메타정보
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Burntz API")
                        .version("v1.0")
                        .description("Burntz API DOC"));
    }

    // JWT 보안 설정 커스터마이저
    private OpenApiCustomizer jwtSecurityCustomizer() {
        return openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("jwt token"))
                .getComponents()
                .addSecuritySchemes("jwt token", new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .in(SecurityScheme.In.HEADER)
                        .bearerFormat("JWT")
                        .scheme("bearer"));
    }

    // 인증 API 그룹
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("🔐 인증 API")
                .pathsToMatch("/api/v1/auth/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 멤버 API 그룹
    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("👤 멤버 API")
                .pathsToMatch("/api/v1/member/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 멤버 리스트 API 그룹
    @Bean
    public GroupedOpenApi memberListApi() {
        return GroupedOpenApi.builder()
                .group("📝 멤버리스트 API")
                .pathsToMatch("/api/v1/member-list/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 박스 API 그룹
    @Bean
    public GroupedOpenApi boxApi() {
        return GroupedOpenApi.builder()
            .group("📦 박스 API")
            .pathsToMatch("/api/v1/boxes/**")
                //wod랑 record경로 제외
                .pathsToExclude(
                        "/api/v1/boxes/**/wods",     
                        "/api/v1/boxes/**/wods/**"
                )
            .addOpenApiCustomizer(jwtSecurityCustomizer())
            .build();
    }

    // 클래스 API 그룹
    @Bean
    public GroupedOpenApi classesApi() {
        return GroupedOpenApi.builder()
                .group("🕘 클래스 API")
                .pathsToMatch("/api/v1/classes/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 채널 API 그룹
    @Bean
    public GroupedOpenApi channelApi() {
        return GroupedOpenApi.builder()
                .group("💬 채널 API")
                .pathsToMatch("/api/v1/channels/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    //Wod API 그룹
    @Bean
    public GroupedOpenApi wodApi() {
        return GroupedOpenApi.builder()
                .group("\uD83C\uDFCB\uFE0F WOD API")
                .pathsToMatch(
                        "/api/v1/boxes/*/wods",
                        "/api/v1/boxes/*/wods/*"
                )
                .pathsToExclude("/api/v1/boxes/**/wods/**/records/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // Record API 그룹 (records 하위만 포함)
    @Bean
    public GroupedOpenApi recordApi() {
        return GroupedOpenApi.builder()
                .group("📝 Record API")
                .pathsToMatch(
                        "/api/v1/boxes/*/wods/*/records",
                        "/api/v1/boxes/*/wods/*/records/*"
                )
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 채널 API 그룹
    @Bean
    public GroupedOpenApi membershipApi() {
        return GroupedOpenApi.builder()
                .group("🪪 멤버십 API")
                .pathsToMatch("/api/v1/boxPk/{boxPk}/membership/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

    // 알람 API 그룹
    @Bean
    public GroupedOpenApi oauthApi() {
        return GroupedOpenApi.builder()
                .group("⏰ 알람 로그인 API")
                .pathsToMatch("/api/v1/alarm/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }


    // 결제 API 그룹
    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("💰 결제 API")
                .pathsToMatch("/api/v1/payments/**")
                .addOpenApiCustomizer(jwtSecurityCustomizer())
                .build();
    }

}
