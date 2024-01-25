package pl.interia.omnibus.vr.model;

import android.graphics.Bitmap;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.pojo.ImageSize;
import pl.interia.omnibus.utils.Optional;
import pl.interia.omnibus.utils.PicassoUtils;

@RequiredArgsConstructor
public abstract class Cacheable<T> {
    public enum State{
        CREATED,
        CACHING,
        ERROR,
        CACHED,
        DRAWN,
        FINISHED
    }

    @Getter
    @Setter
    protected State state = Cacheable.State.CREATED;
    @Getter
    protected Throwable cause;
    @Getter
    protected final T data;
    protected final OpracowaniaService service;

    public final void cache(){
        AndroidSchedulers.mainThread().scheduleDirect(() -> {
            if(state == State.CACHING){
                return;
            }
            doCaching();
        });
    }

    protected abstract void doCaching();
    public abstract void clearCache();

    protected final Single<Optional<Bitmap>> getUrl(long imageId){
        if(imageId != 0){
            return service.getImageUrl(imageId, ImageSize.W450XH)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(s -> PicassoUtils.load(s.get()));
        }
        return Single.just(new Optional<>(null));
    }

}