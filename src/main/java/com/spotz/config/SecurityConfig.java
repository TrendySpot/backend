package com.spotz.config;

import com.spotz.global.jwt.JwtAuthFilter;
import com.spotz.global.jwt.JwtProvider;
import com.spotz.global.oauth.CustomOAuth2UserService;
import com.spotz.global.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                                // ✅ 공개 API
                                .requestMatchers(HttpMethod.GET, "/api/spots/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/spots/*/reviews").permitAll()
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                        "/api-docs/**").permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers("/login/oauth2/code/**").permitAll()

                                // ✅ 관리자 전용
                                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                                // ❌ [기존 코드 주석 처리] 인증 필요 설정을 잠시 막습니다.
                                .anyRequest().authenticated()

                        // ⭕ [개발용 임시 추가] 로그인 안 해도 모든 API 주소에 접근할 수 있게 엽니다.
//                    .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // 💡 [프론트 개발용 임시] JWT 필터 꺼둠 → 토큰 없이 API 호출 가능
                // 💡 프론트 개발 완료 후 아래 주석을 풀고 이 줄을 지울 것!
                // ❌ [개발 완료 후 주석 해제]
                .addFilterBefore(new JwtAuthFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",  // 💡 프론트 개발 서버
                "https://spotz.co.kr"    // ✅ 운영 도메인
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
