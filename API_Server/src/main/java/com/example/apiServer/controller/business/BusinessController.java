package com.example.apiServer.controller.business;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.apiServer.service.business.BusinessService;

@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @GetMapping("/get-all")
    public List<Map<String, String>> getProducts() {
        return service.readFile();
    }

    @GetMapping("/ct90")
    public List<Map<String, Object>> getCT90() {
        return service.getCT90();
    }

    @GetMapping("/ct70y")
    public List<Map<String, Object>> getCT70Y() {
        return service.getCT70Y();
    }

    @GetMapping("/ct90-history/{maVT}")
    public List<Map<String, Object>> getCT90History(@PathVariable String maVT) {
        return service.getCT90History(maVT);
    }

    @GetMapping("/ct70y-history/{maVT}")
    public List<Map<String, Object>> getCT70YHistory(@PathVariable String maVT) {
        return service.getCT70YHistory(maVT);
    }
}
