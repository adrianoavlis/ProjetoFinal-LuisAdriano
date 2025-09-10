package br.com.shop2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "index"; // busca index.jsp em /WEB-INF/views/
    }

    @GetMapping("/ui")
public String ui() {
    return "importar"; // resolve /WEB-INF/views/importar.jsp
}
}
