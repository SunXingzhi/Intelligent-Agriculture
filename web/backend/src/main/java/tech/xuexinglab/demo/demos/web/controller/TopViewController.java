package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.web.bind.annotation.*;
import tech.xuexinglab.demo.demos.web.entity.TopViewData;
import tech.xuexinglab.demo.demos.web.service.TopViewService;
import java.util.List;

@RestController
@RequestMapping("/api/top-view")
public class TopViewController {

    private final TopViewService service;

    public TopViewController(TopViewService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public TopViewData add(@RequestBody TopViewData data) {
        return service.addData(data);
    }

    @GetMapping("/all")
    public List<TopViewData> getAll() {
        return service.getAll();
    }
}