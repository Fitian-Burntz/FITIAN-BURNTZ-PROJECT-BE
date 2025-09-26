package com.fitian.burntz.domain.admin.dto;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Getter
@Setter
@Slf4j
public class AdminAccount {

  private final String token;
  private final String password;
  private final SecretKey secretKey;

  public boolean validateAccount(HttpServletRequest request) {

    String token = request.getHeader("A_Token");
    String password = request.getHeader("A_Password");

    try {
      Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .setAllowedClockSkewSeconds(60)
          .build()
          .parseClaimsJws(token);

      if (!this.password.equals(password)) {
        log.error("[Admin] 비밀번호가 일치하지 않음.");
        return false;
      }

      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("[Admin] 토큰이 유효하지 않음.");
      return false;
    }
  }







}
