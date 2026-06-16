package com.example.restservice.service;

import com.example.restservice.enums.ReactionType;
import com.example.restservice.model.Author;
import com.example.restservice.model.Reaction;
import com.example.restservice.model.Word;
import com.example.restservice.repository.AuthorRepository;
import com.example.restservice.repository.ReactionRepository;
import com.example.restservice.repository.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final AuthorRepository authorRepository;
    private final WordRepository wordRepository;

    public ReactionService(ReactionRepository reactionRepository,
                           AuthorRepository authorRepository,
                           WordRepository wordRepository) {
        this.reactionRepository = reactionRepository;
        this.authorRepository = authorRepository;
        this.wordRepository = wordRepository;
    }

    @Transactional
    public String handleReactionAndReturnAction(Long authorId, Long wordId, ReactionType newReactionType) {

        var existing = reactionRepository.findByAuthorIdAndWordId(authorId, wordId);

        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            if (reaction.getReactionType() == newReactionType) {
                reactionRepository.delete(reaction);
                System.out.println("🗑️ DB Action: Removed reaction for Word " + wordId);
                return "REMOVED";
            } else {
                reaction.setReactionType(newReactionType);
                reactionRepository.save(reaction);
                System.out.println("🔄 DB Action: Changed reaction type to " + newReactionType + " for Word " + wordId);
                return "CHANGED";
            }
        } else {
            Author author = authorRepository.findById(authorId)
                    .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorId));
            Word word = wordRepository.getReferenceById(wordId);

            if (word.getAuthor() == author) {
                return "REJECTED";
            }

            Reaction reaction = new Reaction();
            reaction.setAuthor(author);
            reaction.setWord(word);
            reaction.setReactionType(newReactionType);

            reactionRepository.save(reaction);
            reactionRepository.flush();
            System.out.println("💾 DB Action: Created fresh " + newReactionType + " row for Word " + wordId);
            return "ADDED";
        }
    }
}
