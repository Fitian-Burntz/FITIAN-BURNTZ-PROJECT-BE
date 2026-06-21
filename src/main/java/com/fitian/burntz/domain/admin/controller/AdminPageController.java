package com.fitian.burntz.domain.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/api/v1/admin")
public class AdminPageController {

  @GetMapping("/login")
  public String loginPage() {
    log.info("Admin Login Page Accessed");
    return "admin/admin-login";
  }

  @GetMapping("/lobby")
  public String lobbyPage() {
    log.info("lobby Page Accessed");
    return "admin/admin-lobby";
  }

  @GetMapping("/error")
  public String errorPage() {
    return "admin/admin-error";
  }

  @GetMapping("/box-management")
  public String boxManagementPage() {
    log.info("Admin Box Management Page Accessed");
    return "admin/boxes/admin-box-management";
  }

  @GetMapping("/growth")
  public String growthPage() {
    log.info("Admin Growth Page Accessed");
    return "admin/growth/admin-growth";
  }

}
