package com.example.restservice.repository;

import com.example.restservice.enums.ReactionType;
import com.example.restservice.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByAuthorIdAndWordId(Long authorId, Long wordId);
    long countByWordIdAndReactionType(Long wordId, ReactionType reactionType);
    boolean existsByAuthorIdAndWordIdAndReactionType(Long authorId, Long wordId, ReactionType reactionType);
    /**
     * Fetches reaction aggregates for all words on a specific page in a single database round-trip.
     * Returns a list of maps where each map contains: 'wordId', 'type', and 'cnt'
     */
    @Query(value = "SELECT r.word.id as wordId, r.reactionType as type, COUNT(r) as cnt " +
            "FROM Reaction r " +
            "WHERE r.word.id IN :wordIds " +
            "GROUP BY r.word.id, r.reactionType")
    List<Map<String, Object>> getReactionCountsForWords(@Param("wordIds") List<Long> wordIds);

    /**
     * Finds all word IDs that a specific user has reacted to with a given reaction type.
     */
    @Query("SELECT r.word.id FROM Reaction r WHERE r.author.id = :authorId AND r.word.id IN :wordIds AND r.reactionType = :reactionType")
    List<Long> findWordIdsReactedByUser(
            @Param("authorId") Long authorId,
            @Param("wordIds") List<Long> wordIds,
            @Param("reactionType") ReactionType reactionType
    );
}
