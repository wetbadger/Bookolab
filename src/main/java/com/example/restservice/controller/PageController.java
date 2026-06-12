package com.example.restservice.controller;

import com.example.restservice.dto.BoundedPageResponse;
import com.example.restservice.dto.PageResponseDto;
import com.example.restservice.model.Page;
import com.example.restservice.service.PageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origin}", allowCredentials = "true")
@RequestMapping(value = "/api/pages")
@NullMarked
public class PageController {

    private final PageService pageService;

    // Constructor injection
    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping
    public List<Page> getAllPages() {
        return pageService.getAllPages();
    }

    @GetMapping("/flat/{id}")
    public PageResponseDto getFlatPage(@PathVariable Long id) {
        return pageService.getFlatPage(id);
    }

    @GetMapping("/{id}")
    public BoundedPageResponse getPage(@PathVariable Long id) {
        return pageService.getBoundedPage(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Page createPage(@RequestBody Page page) {
        return pageService.createPage(page);
    }

    @PutMapping("/{id}")
    public Page updatePage(@PathVariable Long id, @RequestBody Page pageDetails) {
        return pageService.updatePage(id, pageDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        pageService.deletePage(id);
        return ResponseEntity.noContent().build();
    }
}