package pl.interia.omnibus.vr.model;

import java.util.List;

import lombok.Getter;
import pl.interia.omnibus.model.OpracowaniaService;
import timber.log.Timber;

public abstract class ItemsState<T> {
    protected final OpracowaniaService service;
    private Cacheable[] cacheables;
    @Getter
    private int solvingIndex;
    @Getter
    private boolean isFinished;

    protected abstract Cacheable buildCacheable(T data);

    public ItemsState(List<T> f, int startQuestionIndex, OpracowaniaService service) {
        this.service = service;
        cacheables = new Cacheable[f.size()];
        for(int i = 0; i < f.size(); i++){
            cacheables[i] = buildCacheable(f.get(i));
        }
        if(startQuestionIndex == f.size()){
            solvingIndex = startQuestionIndex;
            isFinished = true;
        }else {
            solvingIndex = startQuestionIndex - 1;
            isFinished = false;
            cacheables[startQuestionIndex].cache();
            Timber.d("ItemsState cache %s", 0);
        }
    }

    public Cacheable next(){
        if(isFinished){
            return null;
        }
        solvingIndex++;
        if(solvingIndex >= cacheables.length){
            isFinished = true;
            return null;
        }
        cacheNext();
        return cacheables[solvingIndex];
    }

    public boolean isNextFinished(){
        return solvingIndex + 1 >= cacheables.length;
    }

    private void cacheNext() {
        if(solvingIndex + 1 < cacheables.length) {
            Timber.d("ItemsState cache %s", solvingIndex + 1);
            cacheables[solvingIndex + 1].cache();
        }
    }

    public void purge() {
        Timber.d("ItemsState purge %s", solvingIndex);
        cacheables[solvingIndex].clearCache();
    }
}
