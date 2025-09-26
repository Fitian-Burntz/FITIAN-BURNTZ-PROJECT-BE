package com.fitian.burntz.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/admin")
public class AdminPageController {

  @GetMapping("/login")
  public String loginPage() {
    return "/admin/admin-login";
  }

  @GetMapping("/lobby")
  public String lobbyPage() {
    return "/admin/admin-lobby";
  }

  @GetMapping("/error")
  public String errorPage() {
    return "/admin/admin-error";
  }

}
