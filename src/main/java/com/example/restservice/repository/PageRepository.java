package com.example.restservice.repository;

import com.example.restservice.model.Page;
import com.example.restservice.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByFirstWord(Word firstWord);
    Optional<Page> findByLastWord(Word lastWord);
    @Modifying
    @Query(value = "TRUNCATE TABLE page RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateAndResetSequence();
}