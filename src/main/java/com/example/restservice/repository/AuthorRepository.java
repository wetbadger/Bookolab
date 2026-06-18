package com.example.restservice.repository;

import com.example.restservice.model.Author;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends CrudRepository<Author, Long> {
    Optional<Author> findAuthorByUsername(String username);
    boolean existsByUsername(String username);
}
