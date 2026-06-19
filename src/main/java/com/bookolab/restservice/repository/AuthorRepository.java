package com.bookolab.restservice.repository;

import com.bookolab.restservice.model.Author;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends CrudRepository<Author, Long> {
    Optional<Author> findAuthorByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
