package pl.interia.omnibus.vr.flashcard.model;

import java.util.List;

import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.model.ItemsState;

public class FlashcardSetState extends ItemsState<Flashcard> {

    public FlashcardSetState(List<Flashcard> f, int startQuestionIndex, OpracowaniaService service) {
        super(f, startQuestionIndex, service);
    }

    @Override
    protected Cacheable buildCacheable(Flashcard data) {
        return new CacheableFlashcard(data, service);
    }
}
