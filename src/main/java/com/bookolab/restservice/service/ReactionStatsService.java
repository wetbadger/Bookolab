package com.bookolab.restservice.service;

import com.bookolab.restservice.dto.UserReactionStats;
import com.bookolab.restservice.enums.ReactionType;
import com.bookolab.restservice.model.Author;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.ReactionRepository;
import com.bookolab.restservice.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionStatsService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void broadcastWordAuthorReactionStats(Long wordId) {
        try {
            // Load the word with its author
            Word word = wordRepository.findByIdWithAuthor(wordId)
                    .orElseThrow(() -> new RuntimeException("Word not found with id: " + wordId));

            Author author = word.getAuthor();
            if (author == null) {
                return;
            }

            // Count reactions RECEIVED on ALL words by this author
            // This counts how many times OTHER people reacted to THIS author's words
            long totalLikesReceived = reactionRepository.countReactionsReceivedByAuthor(author.getId(), ReactionType.LIKE);
            long totalDislikesReceived = reactionRepository.countReactionsReceivedByAuthor(author.getId(), ReactionType.DISLIKE);

            UserReactionStats stats = new UserReactionStats(totalLikesReceived, totalDislikesReceived);

            // Send to the word author's private queue
            messagingTemplate.convertAndSendToUser(
                    author.getUsername(),
                    "/queue/reaction-stats",
                    stats
            );

            System.out.println("📊 Author " + author.getUsername() +
                    " received stats - Likes: " + totalLikesReceived +
                    ", Dislikes: " + totalDislikesReceived);

        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast user reaction stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = true)
    public UserReactionStats getUserReactionStats(String username) {
        Author author = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found"));

        // Count reactions RECEIVED by this author on all their words
        long totalLikesReceived = reactionRepository.countReactionsReceivedByAuthor(author.getId(), ReactionType.LIKE);
        long totalDislikesReceived = reactionRepository.countReactionsReceivedByAuthor(author.getId(), ReactionType.DISLIKE);

        return new UserReactionStats(totalLikesReceived, totalDislikesReceived);
    }
}