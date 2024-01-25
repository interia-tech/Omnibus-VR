package pl.interia.omnibus.vr.quiz.model;

import android.graphics.Bitmap;

import lombok.Getter;
import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.api.pojo.Question;
import pl.interia.omnibus.vr.model.Cacheable;

public class CachableQuestion extends Cacheable<Question> {
    @Getter
    private Bitmap questionImg;
    @Getter
    private long creationTime;

    public CachableQuestion(Question data, OpracowaniaService service) {
        super(data, service);
        creationTime = System.currentTimeMillis();
    }

    protected void doCaching(){
        state = State.CACHING;
        getUrl(data.getImageId())
                .subscribe(p -> {
                    questionImg = p.get();
                    state = State.CACHED;
                }, t -> {
                    state = State.ERROR;
                    cause = t;
                });
    }

    public void clearCache() {
        questionImg = null;
    }
}
