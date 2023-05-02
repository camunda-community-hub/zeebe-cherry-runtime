package io.camunda.cherry.web;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@EnableAutoConfiguration
public class HomeController {

  @GetMapping("/")
  String home() {
    return "index.html";
  }

  @GetMapping("/workers")
  String workers() {
    return "workers";
  }

  @GetMapping("/dashboard")
  String dashboard() {
    return "dashboard";
  }

  @GetMapping("/worker")
  String worker() {
    return "worker";
  }
}
