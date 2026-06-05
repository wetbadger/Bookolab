package com.example.restservice.controller;

import com.example.restservice.model.Word;
import com.example.restservice.repository.CourseRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
// Spring Boot 4 natively supports the 'version' property for clean routing
@RequestMapping(value = "/api/words")
@NullMarked // Spring Boot 4 standard for compile-time null safety
public class WordController {

    private final CourseRepository courseRepository;

    // Constructor injection is the recommended best practice
    public WordController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // GET all courses
    @GetMapping
    public List<Word> getAllCourses() {
        return courseRepository.findAll();
    }

    // GET a single course by ID
    @GetMapping("/{id}")
    public Word getCourseById(@PathVariable Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // POST create a new course
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Word createCourse(@RequestBody Word course) {
        return courseRepository.save(course);
    }

    // PUT update an existing course
    @PutMapping("/{id}")
    public Word updateCourse(@PathVariable Long id, @RequestBody Word courseDetails) {
        return courseRepository.findById(id)
                .map(existingCourse -> {
                    existingCourse.setContent(courseDetails.getContent());
                    return courseRepository.save(existingCourse);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // DELETE a course
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        courseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}