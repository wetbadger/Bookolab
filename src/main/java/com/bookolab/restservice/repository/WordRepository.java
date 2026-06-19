package com.bookolab.restservice.repository;

import com.bookolab.restservice.model.Word;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByNextWord(Word nextWord);
    Optional<Word> findByLocalId(String localId);

    @Query("SELECT w FROM Word w LEFT JOIN FETCH w.author WHERE w.id = :wordId")
    Optional<Word> findByIdWithAuthor(@Param("wordId") Long wordId);
}