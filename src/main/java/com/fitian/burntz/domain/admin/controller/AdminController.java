package com.fitian.burntz.domain.admin.controller;


import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminAccount adminAccount;

  // 제거된 엔드포인트: GET /api/v1/admin/account
  // 서버 시작 시 생성된 admin token/password를 로그에 출력하는 용도였으나,
  // 인증 없이 누구나 호출 가능하고 CloudWatch 로그를 통해 admin 계정 노출 위험이 있어 제거.
  // admin 계정 정보는 서버 시작 로그(AdminAccountConfig)에서 확인.

  @PostMapping("/login")
  public ApiResponse<Boolean> login(HttpServletRequest request) {

    Boolean isValid = adminAccount.validateAccount(request);

    return ApiResponse.success(isValid, "관리자 로그인 검증 완료");

  }

}
