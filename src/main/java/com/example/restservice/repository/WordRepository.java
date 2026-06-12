package com.example.restservice.repository;

import com.example.restservice.model.Word;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByNextWord(Word nextWord);
    Optional<Word> findByLocalId(String localId);
}