package carobnifrulas.web_tasks.card;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardService {

    private final CardRepository cards;

    public CardService(CardRepository cards) {
        this.cards = cards;
    }

    public List<Card> findByList(Long listId) {
        return cards.findByListIdAndArchivedAtIsNullOrderByPositionAsc(listId);
    }

    public List<Card> findAssignedTo(Long userId) {
        return cards.findByAssignedToAndArchivedAtIsNullOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public void assignToMe(Long cardId, Long myUserId) {
        Card c = cards.findById(cardId).orElseThrow();
        if (c.getAssignedTo() != null) throw new IllegalStateException("Task je već preuzet.");
        c.setAssignedTo(myUserId);
        cards.save(c);
    }

    @Transactional
    public void unassign(Long cardId, Long myUserId) {
        Card c = cards.findById(cardId).orElseThrow();
        if (c.getAssignedTo() == null) return;
        if (!c.getAssignedTo().equals(myUserId)) throw new IllegalStateException("Nije tvoj task.");
        c.setAssignedTo(null);
        cards.save(c);
    }

    @Transactional
    public void moveToList(Long cardId, Long targetListId) {
        Card c = cards.findById(cardId).orElseThrow();
        c.setListId(targetListId);
        cards.save(c);
    }
}