package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
        "/",
        "/home",
        "/new-home",
        "/user-login",
        "/login",
        "/guide",
        "/guide/{path:[^\\.]*}",
        "/collections",
        "/community",
        "/intelligence",
        "/intelligence/{path:[^\\.]*}",
        "/ai-creation",
        "/transformation",
        "/transformation/{path:[^\\.]*}",
        "/about",
        "/about/{path:[^\\.]*}",
        "/ceramics",
        "/ceramics/{path:[^\\.]*}",
        "/ceramics/{path1:[^\\.]*}/{path2:[^\\.]*}",
        "/ceramics/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}"
    })
    public String frontend() {
        return "forward:/index.html";
    }
}
