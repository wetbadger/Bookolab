package com.bookolab.restservice.repository;

import com.bookolab.restservice.model.Page;
import com.bookolab.restservice.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByFirstWord(Word firstWord);
    Optional<Page> findByLastWord(Word lastWord);

    @Modifying
    @Query(value = "TRUNCATE TABLE page RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateAndResetSequence();

    /**
     * 🚀 Blazing fast recursive search: Resolves exactly which Page ID owns the given Word ID
     */
    @Query(value = "WITH RECURSIVE word_chain AS ( " +
            "  SELECT id, next_word_id, 1 as absolute_position " +
            "  FROM word " +
            "  WHERE id NOT IN (SELECT next_word_id FROM word WHERE next_word_id IS NOT NULL) " +
            "  UNION ALL " +
            "  SELECT w.id, w.next_word_id, wc.absolute_position + 1 " +
            "  FROM word w " +
            "  INNER JOIN word_chain wc ON w.id = wc.next_word_id " +
            "), " +
            "target_word AS ( " +
            "  SELECT absolute_position FROM word_chain WHERE id = :wordId " +
            "), " +
            "page_bounds AS ( " +
            "  SELECT p.id as page_id, wc_first.absolute_position as start_pos, wc_last.absolute_position as end_pos " +
            "  FROM page p " +
            "  JOIN word_chain wc_first ON p.first_word_id = wc_first.id " +
            "  JOIN word_chain wc_last ON p.last_word_id = wc_last.id " +
            ") " +
            "SELECT page_id FROM page_bounds, target_word " +
            "WHERE target_word.absolute_position BETWEEN page_bounds.start_pos AND page_bounds.end_pos LIMIT 1",
            nativeQuery = true)
    Optional<Long> findPageIdByWordId(@Param("wordId") Long wordId);
}