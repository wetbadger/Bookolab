package com.example.restservice.controller;

import com.example.restservice.model.Page;
import com.example.restservice.repository.PageRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
// Spring Boot 4 natively supports the 'version' property for clean routing
@RequestMapping(value = "/api/pages")
@NullMarked // Spring Boot 4 standard for compile-time null safety
public class PageController {

    private final PageRepository pageRepository;

    // Constructor injection is the recommended best practice
    public PageController(PageRepository repo) {
        this.pageRepository = repo;
    }

    // GET all pages
    @GetMapping
    public List<Page> getAllCourses() {
        return pageRepository.findAll();
    }

    // GET a single page by ID
    @GetMapping("/{id}")
    public Page getPageById(@PathVariable Long id) {
        return pageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // POST create a new page
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Page createPage(@RequestBody Page page) {
        return pageRepository.save(page);
    }

    // PUT update an existing page
    @PutMapping("/{id}")
    public Page updatePage(@PathVariable Long id, @RequestBody Page pageDetails) {
        return pageRepository.findById(id)
                .map(existingPage -> {
                    existingPage.setFirstWord(pageDetails.getFirstWord());
                    
                    return pageRepository.save(existingPage);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // DELETE a page
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (!pageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}