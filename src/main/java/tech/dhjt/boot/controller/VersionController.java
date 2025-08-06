package tech.dhjt.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    @GetMapping(value = "/test", version = "1.0")
    public String getVersion1() {
        return String.format("1.0.0 config: %s",  12) ;
    }

    @GetMapping(value = "/test", version = "2.0")
    public String getVersion2() {
        return "2.0.0";
    }

    @GetMapping(value = "/test/{version}")
    public String getVersion3(@PathVariable("version") String version) {
        return "3.0.0";
    }

}
