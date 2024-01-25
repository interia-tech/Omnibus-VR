package pl.interia.omnibus.vr.flashcard;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.google.vr.sdk.base.Eye;

import org.parceler.Parcel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import icepick.Icepick;
import icepick.State;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.interia.omnibus.Constants;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.api.DataObject;
import pl.interia.omnibus.model.api.pojo.flashcardsset.Flashcard;
import pl.interia.omnibus.model.api.pojo.flashcardsset.FlashcardsSetSolutionSummary;
import pl.interia.omnibus.model.api.pojo.flashcardsset.LWSTeacherType;
import pl.interia.omnibus.model.pojo.ImageSize;
import pl.interia.omnibus.model.socketio.learning.model.LWSProgress;
import pl.interia.omnibus.model.socketio.learning.model.LWSUser;
import pl.interia.omnibus.model.socketio.learning.queuing.QueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.QueuingLWSClient;
import pl.interia.omnibus.model.socketio.learning.queuing.message.DisconnectQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.ErrorQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.LWSAnswerRequestQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.LWSProgressQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.LWSRestartQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.LWSRestartRequestQueueMessage;
import pl.interia.omnibus.model.socketio.learning.queuing.message.TrainingAbortQueueMessage;
import pl.interia.omnibus.utils.PicassoUtils;
import pl.interia.omnibus.vr.VrCore;
import pl.interia.omnibus.vr.flashcard.model.CacheableFlashcard;
import pl.interia.omnibus.vr.flashcard.model.FlashcardSetState;
import pl.interia.omnibus.vr.flashcard.object3d.FlashcardLWSSummaryDialogModelInstance;
import pl.interia.omnibus.vr.flashcard.object3d.FlashcardModelInstance;
import pl.interia.omnibus.vr.flashcard.object3d.LWSAvatarModelInstance;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance;
import pl.interia.omnibus.vr.object3d.room.RoomModelInstance;
import timber.log.Timber;

public class FlashcardsLWSVrCoreImp extends VrCore {
    @State(FragmentData.FragmentDataBundler.class)
    protected VRFlashcardsLWSFragmentData fragmentData;
    private FlashcardModelInstance flashcard3d;
    private FlashcardSetState flashcardState;
    private DialogModelInstance dialog;
    private FlashcardLWSSummaryDialogModelInstance summaryDialog;
    private LWSAvatarModelInstance avatar3d;
    private Cacheable current;
    private boolean isEnd = false;
    private List<Flashcard> flashcardsToSolve;
    private boolean isRequestSummaryError = false;
    public static QueuingLWSClient CLIENT;
    private boolean isReceivingEnabled = false;

    public FlashcardsLWSVrCoreImp(Bundle savedState, VRFlashcardsLWSFragmentData cs) {
        super();
        Icepick.restoreInstanceState(this, savedState);
        if (fragmentData == null) {
            this.fragmentData = cs;
        }
        Timber.d("fragmentData %s", fragmentData);
    }

    @Override
    protected void registerModels(AssetManager assets, Context ctx) {
        registerModel(new RoomModelInstance(assets));
        flashcard3d = new FlashcardModelInstance(assets, fragmentData.isStudent);
        registerModel(flashcard3d);
        dialog = new DialogModelInstance(assets);
        registerModel(dialog);
        summaryDialog = new FlashcardLWSSummaryDialogModelInstance(assets, fragmentData.isStudent);
        registerModel(summaryDialog);
        avatar3d = new LWSAvatarModelInstance(assets);
        registerModel(avatar3d);
    }

    protected void onOpracowaniaServiceReady() {
        start();
    }

    @Override
    protected void doneLoading() {
        super.doneLoading();
        start();
    }

    private void start(){
        Gdx.app.postRunnable(() -> {
            if(isLoaded && isServiceReady) {
                addOnDisposeDisposable(handleFriendAcquired(fragmentData.partner).subscribe(() -> {}, Timber::e));
                reset();
            }
        });
    }

    private Completable handleFriendAcquired(LWSUser partner){
        return service.getImageUrl(partner.getAvatarId(), ImageSize.W225XH)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(s -> PicassoUtils.load(s.get()))
                .observeOn(Schedulers.io())
                .flatMapCompletable(p -> {
                    if(p.isPresent()) {
                        avatar3d.prepare(p.get(), partner.getName());
                        Gdx.app.postRunnable(() -> avatar3d.show());
                        return Completable.complete();
                    } else {
                        return Completable.error(new Exception("Avatar image is null"));
                    }
                });
    }


    private void setFlashcardSides(CacheableFlashcard flashcard) {
        Schedulers.io().scheduleDirect(() -> {
            prepareFlashcardObject(flashcard);
            Gdx.app.postRunnable(() -> {
                flashcardState.purge();
                if(fragmentData.isStudent){
                    flashcard3d.show(() -> isReceivingEnabled = true);
                } else {
                    flashcard3d.show(() -> flashcard3d.flipToAnswer(() -> isReceivingEnabled = true));
                }
            });
        });
    }

    private void prepareFlashcardObject(CacheableFlashcard flashcard) {
        Bitmap qImage = flashcard.getQuestionImg();
        Bitmap aImage = flashcard.getAnswerImg();
        String qText = flashcard.getData().getQuestion().getBody();
        String aText = flashcard.getData().getAnswer().getBody();
        if(!fragmentData.isStudent) {
            flashcard3d.prepare(qImage, qText, aImage, aText);
        } else if(fragmentData.isReverseMode) {
            flashcard3d.prepare(aImage, aText, qImage, qText);
        } else {
            flashcard3d.prepare(qImage, qText, aImage, aText);
        }
    }

    private void reset(){
        fragmentData.reset();
        isEnd = false;
        isRequestSummaryError = false;
        flashcardsToSolve = fragmentData.progress.getFlashcardsToSolve(fragmentData.getFlashcardsAll());
        flashcardState = new FlashcardSetState(flashcardsToSolve, fragmentData.index, service);
        moveNext();
    }

    private void moveNext() {
        current = flashcardState.next();
        fragmentData.index = flashcardState.getSolvingIndex();
    }

    @Override
    public void onClick(OmnibusModelInstance object) {
        if (flashcard3d.isLearned(object)) {
            markFlashcardFinished(true);
        } else if (flashcard3d.isNotLearned(object)) {
            markFlashcardFinished(false);
        } else if (dialog.isButton1(object)) {
            if(dialog.getCurrentType() == DialogModelInstance.Type.IO_EXCEPTION) {
                hideDialog(isRequestSummaryError ? this::doSolutionRequest : () -> current.cache());
            } else {
                exit();
            }
        } else if (dialog.isButton2(object) || summaryDialog.isButtonDisconnect(object)) {
            isReceivingEnabled = false;
            exit();
        } else if (summaryDialog.isButtonRepeat(object)) {
            boolean shouldRepeatUnlearned = summaryDialog.shouldRepeatUnlearned(object);
            if(fragmentData.isAmIOwnerLWSRoom()) {
                CLIENT.sendTrainingRestart(fragmentData.roomId, new LWSProgress(fragmentData.getFlashcardsAll(), shouldRepeatUnlearned));
            } else {
                CLIENT.sendTrainingRestartRequest(fragmentData.roomId, shouldRepeatUnlearned);
            }
        }
    }

    @Override
    protected void onDraw(Eye eye) {
        super.onDraw(eye);
        if (flashcardState != null) {
            if(!isEnd) {
                if (current != null && current.getState() != CacheableFlashcard.State.FINISHED) {
                    handleFlashcardState();
                } else if (flashcardState.isFinished()) {
                    isEnd = true;
                    doSolutionRequest();
                }
            }
            if(isReceivingEnabled){
                handleNextMessage();
            }
        }
    }

    private void handleFlashcardState(){
        if (current.getState() == CacheableFlashcard.State.CACHED) {
            setFlashcardSides((CacheableFlashcard) current);
            current.setState(CacheableFlashcard.State.DRAWN);
        } else if (current.getState() == CacheableFlashcard.State.ERROR && !dialog.isHiding() && !dialog.isVisible()) {
            showDialog(DialogModelInstance.Type.IO_EXCEPTION);
        }
    }

    private void handleNextMessage() {
        QueueMessage m = CLIENT.nextMessage();
        if(m instanceof LWSProgressQueueMessage) {
            handleProgressMsg((LWSProgressQueueMessage) m);
        } else if (m instanceof LWSAnswerRequestQueueMessage) {
            handleAnswerRequestMsg((LWSAnswerRequestQueueMessage) m);
        } else if (m instanceof LWSRestartRequestQueueMessage) {
            LWSRestartRequestQueueMessage rrm = (LWSRestartRequestQueueMessage) m;
            if(fragmentData.isAmIOwnerLWSRoom()) {
                CLIENT.sendTrainingRestart(fragmentData.roomId, new LWSProgress(fragmentData.getFlashcardsAll(), rrm.isRepeatOnlyUnlearned()));
            }
        } else if (m instanceof LWSRestartQueueMessage) {
            handleRestartMsg((LWSRestartQueueMessage) m);
        } else if (m instanceof ErrorQueueMessage) {
            Timber.e(((ErrorQueueMessage) m).getCause());
        } else if (m instanceof DisconnectQueueMessage) {
            showExitingDialog(DialogModelInstance.Type.DISCONNECTED);
        } else if (m instanceof TrainingAbortQueueMessage) {
            showExitingDialog(DialogModelInstance.Type.LWS_PARTNER_LEAVE);
        }
    }

    private void handleRestartMsg(LWSRestartQueueMessage rm) {
        isReceivingEnabled = false;
        fragmentData.setProgress(rm.getProgress());
        long hideAnimationStartMs = SystemClock.elapsedRealtime();
        summaryDialog.hide(() -> {
            long leftMs = Constants.WAITING_TIME_FOR_PRE_GAME - (SystemClock.elapsedRealtime() - hideAnimationStartMs);
            AndroidSchedulers.mainThread().scheduleDirect(() -> Gdx.app.postRunnable(() -> {
                        if (fragmentData.isAmIOwnerLWSRoom()) {
                            fragmentData.getProgress().moveToFirstQuestion();
                            CLIENT.sendTrainingProgress(fragmentData.roomId, fragmentData.getProgress());
                            reset();
                        } else {
                            isReceivingEnabled = true;
                        }
                    }), leftMs, TimeUnit.MILLISECONDS);
        });
    }

    private void handleAnswerRequestMsg(LWSAnswerRequestQueueMessage arm) {
        isReceivingEnabled = false;
        Flashcard cur = ((CacheableFlashcard) current).getData();
        LWSProgress progress = fragmentData.progress;
        if(progress.getState() == LWSProgress.STARTED && cur.getId() == fragmentData.progress.getQuestion()) {
            cur.setLearned(arm.isLearned());
            if(fragmentData.index + 1 < flashcardsToSolve.size()) {
                fragmentData.getProgress().moveNext(cur, flashcardsToSolve.get(++fragmentData.index));
            } else {
                fragmentData.getProgress().moveToLastQuestion(cur, fragmentData.calculateElapsedMillisecondsFrom());
            }
            flashcard3d.flipToAnswer(() -> markFlashcardFinished(arm.isLearned()));
        }
    }

    private void handleProgressMsg(LWSProgressQueueMessage pm) {
        if(fragmentData.progress.getState() == LWSProgress.READY && pm.getProgress().getState() == LWSProgress.STARTED) {
            isReceivingEnabled = false;
            fragmentData.setProgress(pm.getProgress());
            reset();
        } else if(fragmentData.isStudent) {
            isReceivingEnabled = false;
            long lastFlashcardId = fragmentData.getProgress().getQuestion();
            fragmentData.setProgress(pm.getProgress());
            flashcard3d.flipToAnswer(() -> markFlashcardFinished(pm.getProgress().isLearned(lastFlashcardId)));
        }
    }

    private void markFlashcardFinished(boolean isLearned) {
        isReceivingEnabled = false;
        current.setState(CacheableFlashcard.State.FINISHED);
        Flashcard cur = flashcardsToSolve.get(flashcardState.getSolvingIndex());
        cur.setLearned(isLearned);
        if (isLearned) {
            flashcard3d.hideLeft(this::moveNext);
        } else {
            flashcard3d.hideRight(this::moveNext);
        }

        if (fragmentData.isAmIOwnerLWSRoom()) {
            CLIENT.sendTrainingProgress(fragmentData.roomId, fragmentData.getProgress());
        } else if (!fragmentData.isStudent && fragmentData.getProgress().getState() == LWSProgress.STARTED) {
            CLIENT.sendMarkAnswerCommand(fragmentData.roomId, cur.id, isLearned);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    private void doSolutionRequest(){
        isReceivingEnabled = false;
        if(fragmentData.isAmIOwnerLWSRoom()) {
            addOnDisposeDisposable(service.sendFlashcardsSetSolution(fragmentData.getFlashcardSetId(), fragmentData.toSolution(), fragmentData.partner.getId())
                    .map(DataObject::getData)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleSolutionResponse, this::handleSolutionRequestError));
        } else {
            showSummary();
        }
    }

    private void handleSolutionRequestError(Throwable cause) {
        Timber.d(cause);
        if (Gdx.app != null) {
            Gdx.app.postRunnable(() -> {
                isRequestSummaryError = true;
                showDialog(DialogModelInstance.Type.IO_EXCEPTION);
            });
        }
    }

    private void handleSolutionResponse(FlashcardsSetSolutionSummary res) {
        CLIENT.sendTrainingStatistics(fragmentData.roomId, res.getStatistics());
        addOnDisposeDisposable(
                service.sendFlashcardsSetLearning(fragmentData.getFlashcardSetId(),
                        fragmentData.partner.getId(),
                        fragmentData.roomId,
                        fragmentData.isStudent ? LWSTeacherType.INVITED : LWSTeacherType.OWNER)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(r -> showSummary(), this::handleSolutionRequestError)
        );
    }

    private void showSummary(){
        summaryDialog.show(fragmentData.flashcardsAll, () -> isReceivingEnabled = true);
    }

    private void showDialog(DialogModelInstance.Type type){
        isReceivingEnabled = false;
        dialog.show(type, () -> isReceivingEnabled = true);
    }

    private void showExitingDialog(DialogModelInstance.Type type) {
        isReceivingEnabled = false;
        flashcard3d.setShouldBeRendered(false);
        if(summaryDialog.isVisible()) {
            summaryDialog.hide(() -> dialog.show(type));
        } else if(dialog.isVisible()) {
            dialog.hide(() -> dialog.show(type));
        } else {
            dialog.show(type);
        }
    }

    private void hideDialog(Runnable runAfterEnd){
        isReceivingEnabled = false;
        dialog.hide(runAfterEnd);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CLIENT.destroy();
        CLIENT = null;
    }

    @Data
    @Parcel
    @NoArgsConstructor
    public static class VRFlashcardsLWSFragmentData extends FlashcardFragmentData {
        boolean isStudent;
        String roomId;
        LWSUser partner;
        boolean amIOwnerLWSRoom;

        public VRFlashcardsLWSFragmentData(LinkedHashMap<Long, Flashcard> flashcards, LWSProgress progress, long flashcardSetId, boolean isStudent,
                                           String roomId, LWSUser partner, boolean isReverseMode, boolean amIOwnerLWSRoom) {
            super(flashcards, progress, flashcardSetId, isReverseMode);
            this.isStudent = isStudent;
            this.roomId = roomId;
            this.partner = partner;
            this.amIOwnerLWSRoom = amIOwnerLWSRoom;
        }
    }
}
