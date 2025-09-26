package com.fitian.burntz.domain.admin.controller;


import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminAccount adminAccount;

  @PostMapping("/login")
  public ApiResponse<Boolean> login(HttpServletRequest request) {

    Boolean isValid = adminAccount.validateAccount(request);

    return ApiResponse.success(isValid, "관리자 로그인 검증 완료");

  }

  @GetMapping("/account")
  public void getAdminAccount() {

    String token = adminAccount.getToken();
    String password = adminAccount.getPassword();


    log.info(" ");
    log.info("================== Admin Account Info =================");
    log.info("= [ Admin Token: " + token + " ]");
    log.info("= [ Admin Password: " + password + " ]");
    log.info("======================================================");
    log.info(" ");
  }

}
