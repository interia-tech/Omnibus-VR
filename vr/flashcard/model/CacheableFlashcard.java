package pl.interia.omnibus.vr.flashcard.model;

import android.graphics.Bitmap;
import android.util.Pair;

import io.reactivex.Single;
import lombok.Getter;
import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.vr.model.Cacheable;

public class CacheableFlashcard extends Cacheable<Flashcard> {

    @Getter
    private Bitmap questionImg;
    @Getter
    private Bitmap answerImg;

    public CacheableFlashcard(Flashcard flashcard, OpracowaniaService service) {
        super(flashcard, service);
    }

    protected void doCaching(){
        state = State.CACHING;
        Single.zip(getUrl(data.getQuestion().getImageId()), getUrl(data.getAnswer().getImageId()),
                (a, b) -> new Pair<>(a.get(), b.get()))
                .subscribe(p -> {
                    questionImg = p.first;
                    answerImg = p.second;
                    state = State.CACHED;
                }, t -> {
                    state = State.ERROR;
                    cause = t;
                });
    }

    public void clearCache() {
        questionImg = null;
        answerImg = null;
    }
}
