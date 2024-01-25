package pl.interia.omnibus.vr.flashcard;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.google.vr.sdk.base.Eye;

import org.parceler.Parcel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import icepick.Icepick;
import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.model.socketio.learning.model.LWSProgress;
import pl.interia.omnibus.vr.VrCore;
import pl.interia.omnibus.vr.flashcard.model.CacheableFlashcard;
import pl.interia.omnibus.vr.flashcard.model.FlashcardSetState;
import pl.interia.omnibus.vr.flashcard.object3d.FlashcardModelInstance;
import pl.interia.omnibus.vr.flashcard.object3d.FlashcardSummaryDialogModelInstance;
import pl.interia.omnibus.vr.flashcard.object3d.SwitcherModelInstance;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance;
import pl.interia.omnibus.vr.object3d.room.RoomModelInstance;
import timber.log.Timber;

public class FlashcardsVrCoreImp extends VrCore {
    @State(FragmentData.FragmentDataBundler.class)
    protected VRFlashcardsFragmentData fragmentData;
    private FlashcardModelInstance flashcard3d;
    private SwitcherModelInstance switcher;
    private FlashcardSetState flashcardState;
    private DialogModelInstance dialog;
    private FlashcardSummaryDialogModelInstance summaryDialog;
    private Cacheable current;
    private boolean isEnd = false;
    private List<Flashcard> flashcardsToSolve;
    private boolean isRequestSummaryError = false;

    public FlashcardsVrCoreImp(Bundle savedState, VRFlashcardsFragmentData cs) {
        super();
        Icepick.restoreInstanceState(this, savedState);
        if (fragmentData == null) {
            this.fragmentData = cs;
        }
        Timber.d("fragmentData %s", fragmentData);
    }

    private void setFlashcardSides(CacheableFlashcard flashcard) {
        Schedulers.io().scheduleDirect(() -> {
            Bitmap qImage = flashcard.getQuestionImg();
            Bitmap aImage = flashcard.getAnswerImg();
            String qText = flashcard.getData().getQuestion().getBody();
            String aText = flashcard.getData().getAnswer().getBody();
            if (fragmentData.isReverseMode) {
                flashcard3d.prepare(aImage, aText, qImage, qText);
            } else {
                flashcard3d.prepare(qImage, qText, aImage, aText);
            }

            Gdx.app.postRunnable(() -> {
                flashcardState.purge();
                flashcard3d.show();
                switcher.show();
            });
        });
    }

    @Override
    protected void registerModels(AssetManager assets, Context ctx) {
        registerModel(new RoomModelInstance(assets));
        flashcard3d = new FlashcardModelInstance(assets, false);
        registerModel(flashcard3d);
        switcher = new SwitcherModelInstance(assets);
        registerModel(switcher);
        dialog = new DialogModelInstance(assets);
        registerModel(dialog);
        summaryDialog = new FlashcardSummaryDialogModelInstance(assets);
        registerModel(summaryDialog);
    }
    protected void onOpracowaniaServiceReady() {
        reset();
    }

    private void reset(){
        isEnd = false;
        isRequestSummaryError = false;
        flashcardsToSolve = fragmentData.progress.getFlashcardsToSolve(fragmentData.getFlashcardsAll());
        Collections.shuffle(flashcardsToSolve);
        flashcardState = new FlashcardSetState(flashcardsToSolve, fragmentData.index, service);
        moveNext();
    }

    private void moveNext() {
        current = flashcardState.next();
        fragmentData.index = flashcardState.getSolvingIndex();
    }

    @Override
    public void onClick(OmnibusModelInstance object) {
        if (object.equals(switcher)) {
            flashcard3d.flipToAnswer();
            switcher.hide();
        } else if (flashcard3d.isLearned(object)) {
            markFlashcardFinished(true);
        } else if (flashcard3d.isNotLearned(object)) {
            markFlashcardFinished(false);
        } else if (dialog.isButton1(object)) {
            if(isRequestSummaryError) {
                dialog.hide(this::doSolutionRequest);
            } else {
                dialog.hide(() -> current.cache());
            }
        } else if (dialog.isButton2(object) || summaryDialog.isButtonExit(object)) {
            exit();
        } else if (summaryDialog.isButtonRepeat(object)) {
            summaryDialog.hide(() -> {
                fragmentData.reset();
                LWSProgress p = new LWSProgress(fragmentData.flashcardsAll, summaryDialog.shouldRepeatUnlearned());
                fragmentData.setProgress(p);
                reset();
            });
        }
    }

    @Override
    protected void onDraw(Eye eye) {
        super.onDraw(eye);
        if (!isEnd && flashcardState != null) {
            if (current != null && current.getState() != CacheableFlashcard.State.FINISHED) {
                if (current.getState() == CacheableFlashcard.State.CACHED) {
                    setFlashcardSides((CacheableFlashcard) current);
                    current.setState(CacheableFlashcard.State.DRAWN);
                } else if (current.getState() == CacheableFlashcard.State.ERROR && !dialog.isHiding() && !dialog.isVisible()) {
                    dialog.show(DialogModelInstance.Type.IO_EXCEPTION);
                }
            } else if (flashcardState.isFinished()) {
                isEnd = true;
                doSolutionRequest();
            }
        }
    }

    private void markFlashcardFinished(boolean isLearned) {
        current.setState(CacheableFlashcard.State.FINISHED);
        flashcardsToSolve.get(flashcardState.getSolvingIndex()).setLearned(isLearned);
        if (isLearned) {
            flashcard3d.hideLeft(this::moveNext);
        } else {
            flashcard3d.hideRight(this::moveNext);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    private void doSolutionRequest(){
        addOnDisposeDisposable(service.sendFlashcardsSetSolution(fragmentData.getFlashcardSetId(), fragmentData.toSolution(), null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> summaryDialog.show(fragmentData.flashcardsAll), t -> {
                    isRequestSummaryError = true;
                    dialog.show(DialogModelInstance.Type.IO_EXCEPTION);
                }));
    }

    @Data
    @Parcel
    @NoArgsConstructor
    public static class VRFlashcardsFragmentData extends FlashcardFragmentData {

        public VRFlashcardsFragmentData(LinkedHashMap<Long, Flashcard> flashcards, LWSProgress progress, long flashcardSetId, boolean isReverseMode) {
            super(flashcards, progress, flashcardSetId, isReverseMode);

        }
    }
}
