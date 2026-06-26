package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.web.bind.annotation.*;
import tech.xuexinglab.demo.demos.web.entity.FrontViewData;
import tech.xuexinglab.demo.demos.web.service.FrontViewService;

import java.util.List;

@RestController
@RequestMapping("/api/front-history")
public class FrontViewController {

    private final FrontViewService frontViewService;

    public FrontViewController(FrontViewService frontViewService) {
        this.frontViewService = frontViewService;
    }

    @GetMapping("/all")
    public List<FrontViewData> getAll() {
        return frontViewService.getAll();
    }

    @GetMapping("/{id}")
    public FrontViewData getById(@PathVariable Long id) {
        return frontViewService.getById(id);
    }
}