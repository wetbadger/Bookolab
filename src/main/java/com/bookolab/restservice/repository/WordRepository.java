package com.bookolab.restservice.repository;

import com.bookolab.restservice.model.Word;

import java.util.List;
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

    /*
    The head of the chain is the word that no other word points at. Single query instead of
    scanning findByNextWord for every word in the database during repagination.
    */
    @Query("SELECT w FROM Word w WHERE w.id NOT IN (SELECT x.nextWord.id FROM Word x WHERE x.nextWord IS NOT NULL)")
    List<Word> findChainHeads();
}