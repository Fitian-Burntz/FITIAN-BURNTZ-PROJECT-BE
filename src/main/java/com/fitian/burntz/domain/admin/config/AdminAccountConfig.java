package com.fitian.burntz.domain.admin.config;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AdminAccountConfig {

  private SecretKey secretKey;

  @Bean
  public AdminAccount generateAdminAccount() {

    secretKey();
    String token = createToken();
    String password = generateSecurePassword();

    AdminAccount adminAccount = AdminAccount.builder()
        .token(token)
        .password(password)
        .secretKey(secretKey)
        .build();

    log.info(" ");
    log.info("================== Admin Account Info =================");
    log.info("= [ Admin Token: " + token + " ]");
    log.info("= [ Admin Password: " + password + " ]");
    log.info("======================================================");
    log.info(" ");

    return adminAccount;

  }

  private void secretKey() {

    String jwtSecretKey = generateHmacSHA512Key();

    byte[] keyBytes = jwtSecretKey.getBytes();

    this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
  }

  private String generateHmacSHA512Key() {
    return UUID.randomUUID().toString().replace("-", "") +
        UUID.randomUUID().toString().replace("-", "");
  }

  private String createToken() {
    Date now = new Date();
    Date expiryDate = new Date(new Date().getTime() + 3155760000000L);

    return Jwts.builder()
        .addClaims(Map.of("role", "ROLE_ADMIN"))
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS512)
        .compact();
  }

  private String generateSecurePassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder(10);

    for (int i = 0; i < 10; i++) {
      password.append(chars.charAt(random.nextInt(chars.length())));
    }

    return password.toString();
  }



}
