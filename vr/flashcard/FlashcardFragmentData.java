package pl.interia.omnibus.vr.flashcard;

import android.os.SystemClock;

import org.parceler.Parcel;

import java.util.LinkedHashMap;

import lombok.Data;
import lombok.NoArgsConstructor;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.api.pojo.FlashcardAnswerPair;
import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.model.api.pojo.flashcardsset.FlashcardsSetSolution;
import pl.interia.omnibus.model.socketio.learning.model.LWSProgress;
import pl.interia.omnibus.utils.Utils;

@Data
@Parcel
@NoArgsConstructor
class FlashcardFragmentData implements FragmentData {
    LinkedHashMap<Long, Flashcard> flashcardsAll;
    LWSProgress progress;
    long solvingStartTime;
    int index;
    long flashcardSetId;
    boolean isReverseMode;

    FlashcardFragmentData(LinkedHashMap<Long, Flashcard> flashcardsAll, LWSProgress progress, long flashcardSetId, boolean isReverseMode) {
        this.flashcardsAll = flashcardsAll;
        this.progress = progress;
        this.flashcardSetId = flashcardSetId;
        this.isReverseMode = isReverseMode;
        reset();
    }

    FlashcardsSetSolution toSolution(){
        FlashcardsSetSolution s = new FlashcardsSetSolution();
        s.setDuration(Utils.getCurrentUnixTime() - solvingStartTime);
        FlashcardAnswerPair[] answers = new FlashcardAnswerPair[flashcardsAll.size()];
        int i = 0;
        for(Flashcard f : flashcardsAll.values()){
            FlashcardAnswerPair pair = new FlashcardAnswerPair();
            pair.setFlashcardId(f.id);
            pair.setLearned(f.isLearned);
            answers[i] = pair;
            i++;
        }
        s.setSolution(answers);
        return s;
    }

    void reset(){
        index = 0;
        solvingStartTime = Utils.getCurrentUnixTime();
    }

    public long calculateElapsedMillisecondsFrom() {
        return SystemClock.elapsedRealtime() - solvingStartTime;
    }
}
