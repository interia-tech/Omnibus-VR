package pl.interia.omnibus.vr.olympiad;


import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.google.vr.sdk.base.Eye;

import org.parceler.Parcel;

import icepick.Icepick;
import icepick.State;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.api.ApiException;
import pl.interia.omnibus.model.api.ApiResponse;
import pl.interia.omnibus.model.api.pojo.CompletionStatus;
import pl.interia.omnibus.model.api.pojo.olympiad.OlympiadAnswerResult;
import pl.interia.omnibus.model.api.pojo.olympiad.OlympiadQuestion;
import pl.interia.omnibus.model.api.pojo.olympiad.OlympiadState;
import pl.interia.omnibus.vr.VrCore;
import pl.interia.omnibus.vr.flashcard.model.CacheableFlashcard;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.object3d.ButtonModelInstance;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance;
import pl.interia.omnibus.vr.object3d.room.RoomModelInstance;
import pl.interia.omnibus.vr.olympiad.object3d.OlympiadQuestionModelInstance;
import pl.interia.omnibus.vr.quiz.model.CachableQuestion;
import pl.interia.omnibus.vr.quiz.object3d.QuizButtonModelInstance;
import timber.log.Timber;

import static pl.interia.omnibus.model.api.ApiResponse.Result.OLYMPIAD_NOT_ACTIVE;
import static pl.interia.omnibus.model.api.ApiResponse.Result.OLYMPIC_QUESTION_NOT_ACTIVE;

public class OlympiadVrCoreImp extends VrCore {
    private DialogModelInstance dialog;
    private OlympiadQuestionModelInstance olympiadQuestion3d;
    private ButtonModelInstance start;
    private boolean isEnd = false;
    private CachableQuestion current;
    @State(FragmentData.FragmentDataBundler.class)
    protected VROlympiadFragmentData coreState;
    private Request errorRequest;
    private int currentQuestionIndex;
    private int finalQuestionIndex;
    private long questionId;
    private int points;
    private CompletionStatus state;

    public OlympiadVrCoreImp(Bundle savedState, VROlympiadFragmentData cs) {
        super();
        Icepick.restoreInstanceState(this, savedState);
        if(coreState == null){
            this.coreState = cs;
        }
        Timber.d( "fragmentData %s", coreState);
    }

    protected void onOpracowaniaServiceReady(){
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
                Schedulers.io().scheduleDirect(() -> {
                    start.prepare(ButtonModelInstance.Type.START);
                    Gdx.app.postRunnable(() -> start.show("Button|show"));
                });
            }
        });
    }

    private void handleQuestionResponse(OlympiadQuestion data) {
        Timber.d("Question response end at %s", System.currentTimeMillis());
        current = new CachableQuestion(data, service);
        current.cache();
    }

    private void setQuestion(CachableQuestion question){
        Schedulers.io().scheduleDirect(() -> {
            olympiadQuestion3d.setQuestion(question.getQuestionImg(), question.getData(), currentQuestionIndex, finalQuestionIndex, points);
            Gdx.app.postRunnable(() -> {
                question.clearCache();
                olympiadQuestion3d.showQuestion(question, () -> onAnswerClick(-1));
            });
        });
    }

    private void setScore(int score){
        Schedulers.io().scheduleDirect(() -> {
            olympiadQuestion3d.setScore(score);
            Gdx.app.postRunnable(() -> {
                Runnable runAfetr = () -> {
                    if(isLastQuestion()){
                        isEnd = true;
                        olympiadQuestion3d.showSummaryButton();
                    }else{
                        olympiadQuestion3d.showStopAndGoNextButtons();
                    }
                };
                if (olympiadQuestion3d.isShowed()) {
                    olympiadQuestion3d.flipToBack(runAfetr);
                }else{
                    olympiadQuestion3d.show(runAfetr, true);
                }
            });

        });
    }


    private void setSummary() {
        Schedulers.io().scheduleDirect(() -> {
            olympiadQuestion3d.setSummary(points);
            Gdx.app.postRunnable(() -> olympiadQuestion3d.flipToFront(() -> olympiadQuestion3d.showExityButton()));

        });
    }

    @Override
    protected void registerModels(AssetManager assets, Context ctx) {
        registerModel(new RoomModelInstance(assets));
        olympiadQuestion3d = new OlympiadQuestionModelInstance(assets, ctx);
        registerModel(olympiadQuestion3d);
        dialog = new DialogModelInstance(assets);
        registerModel(dialog);
        start = new ButtonModelInstance(assets);
        registerModel(start);
    }

    @Override
    public void onClick(OmnibusModelInstance object) {
        if(olympiadQuestion3d.isAnswerClick(object)) {
            long answerId = ((QuizButtonModelInstance)object).getAnswer().getId();
            onAnswerClick(answerId);
        } else if(olympiadQuestion3d.isSummaryClick(object)){
            olympiadQuestion3d.hideSummaryButtons(this::setSummary);
        } else if(olympiadQuestion3d.isStopClick(object) || olympiadQuestion3d.isExitClick(object)){
            exit();
        } else if(olympiadQuestion3d.isNextQuestionClick(object)){
            olympiadQuestion3d.hideStopAndGoNextButtons(this::doQuestionRequest);
        } else if(dialog.isButton1(object)){
            handleDialogButton1Click();
        } else if(dialog.isButton2(object)){
            handleDialogButton2Click();
        } else if (start.equals(object)){
            start.hide("Button|hide", this::doInitRequest);
        }
    }

    private void onAnswerClick(long answerId){
        current.setState(CacheableFlashcard.State.FINISHED);
        if(answerId != -1) {
            olympiadQuestion3d.hideAnswersButtons(() -> doAnswerRequest(answerId));
        } else {
            olympiadQuestion3d.hideAnswersButtons(() -> doStateRequest(Request.buildAnswerRequest(this, answerId)));
        }
    }

    @Override
    protected void onDraw(Eye eye) {
        super.onDraw(eye);
        if(!isEnd && current != null && current.getState() != CachableQuestion.State.FINISHED) {
            if(current.getState() == CachableQuestion.State.CACHED) {
                setQuestion(current);
                current.setState(CachableQuestion.State.DRAWN);
            }else if(current.getState() == CachableQuestion.State.ERROR && shouldFireHideToDialogShowAnim()){
                olympiadQuestion3d.hide(()-> dialog.show(DialogModelInstance.Type.IO_EXCEPTION));
            }
        }
    }

    private boolean shouldFireHideToDialogShowAnim() {
        return !olympiadQuestion3d.isHiding() && !dialog.isHiding() && !dialog.isVisible();
    }

    void handleDialogButton1Click(){
        if(dialog.getCurrentType() == DialogModelInstance.Type.IO_EXCEPTION){
            Runnable runAfterHide = null;
            if(current.getState() == Cacheable.State.ERROR) {
                runAfterHide = () -> current.cache();
            } else if(errorRequest != null) {
                runAfterHide = errorRequest.runnable;
            } else {
                Timber.e(new Exception("Bad app flow "+current.getState()+" "+ errorRequest));
            }
            dialog.hide(runAfterHide);
        }
    }

    void handleDialogButton2Click(){
        if(dialog.getCurrentType() == DialogModelInstance.Type.IO_EXCEPTION){
            exit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    private void handleOlympiadUnexpectedFinished(){
        Toast.makeText(activity, "Olimpiada już się zakonczyła", Toast.LENGTH_SHORT).show();
        exit();
    }

    private void handleRequestErrors(Throwable t, Request type){
        if(ApiException.getApiCode(t) == OLYMPIC_QUESTION_NOT_ACTIVE){
            doStateRequest(type);
        } else if(ApiException.getApiCode(t) == OLYMPIAD_NOT_ACTIVE){
            handleOlympiadUnexpectedFinished();
        } else {
            errorRequest = type;
            olympiadQuestion3d.hide(()-> dialog.show(DialogModelInstance.Type.IO_EXCEPTION));
        }
    }

    private static class Request {
        private static final int INIT_ID = 1;
        private static final int STATE_ID = 2;
        private static final int QUESTION_ID = 3;
        private static final int ANSWER_ID = 4;

        private Runnable runnable;
        private int id;

        private Request(int id, Runnable runnable){
            this.id = id;
            this.runnable = runnable;
        }

        static Request buildInitRequest(OlympiadVrCoreImp core){
            return new Request(INIT_ID, core::doInitRequest);
        }

        static Request buildStateRequest(OlympiadVrCoreImp core, Request request){
            return new Request(STATE_ID, () -> core.doStateRequest(request));
        }

        static Request buildQuestionRequest(OlympiadVrCoreImp core){
            return new Request(QUESTION_ID, core::doQuestionRequest);
        }

        static Request buildAnswerRequest(OlympiadVrCoreImp core, long answerId){
            return new Request(ANSWER_ID, () -> core.doAnswerRequest(answerId));
        }
    }


    private Single<ApiResponse<OlympiadState>> getStateRequest(long olympiadId) {
        return service.getOlympiadState(olympiadId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void doInitRequest(){
        addOnDisposeDisposable(getStateRequest(coreState.getOlympiadId())
                .subscribe(res -> {
                    if(res.getData().getState() == CompletionStatus.FINISHED){
                        handleOlympiadUnexpectedFinished();
                    }else {
                        OlympiadState s = res.getData();
                        updateState(s.getCurrentQuestionIndex(), s.getNumberOfQuestions(), s.getCurrentQuestionId(), s.getPoints(), s.getState());
                        doQuestionRequest();
                    }
                }, t -> handleRequestErrors(t, Request.buildInitRequest(this))));
    }

    private void doStateRequest(Request type){
        addOnDisposeDisposable(getStateRequest(coreState.getOlympiadId())
                .subscribe(res -> {
                    if(res.getData().getState() == CompletionStatus.FINISHED){
                        if(type.id != Request.STATE_ID) {
                            OlympiadState s = res.getData();
                            updateState(s.getCurrentQuestionIndex(), s.getNumberOfQuestions(), s.getCurrentQuestionId(), s.getPoints(), s.getState());
                            setScore(-1);
                        }else{
                            handleOlympiadUnexpectedFinished();
                        }
                    }else{
                        OlympiadState s = res.getData();
                        updateState(s.getCurrentQuestionIndex(), s.getNumberOfQuestions(), s.getCurrentQuestionId(), s.getPoints(), s.getState());
                        if(type.id == Request.QUESTION_ID){
                            doQuestionRequest();
                        } else {
                            setScore(-1);
                        }
                    }
                }, thr -> handleRequestErrors(thr, Request.buildStateRequest(this, type)))
        );
    }

    private void doQuestionRequest(){
        addOnDisposeDisposable(service.getOlympiadQuestion(coreState.getOlympiadId(), questionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> handleQuestionResponse(r.getData()), t -> handleRequestErrors(t, Request.buildQuestionRequest(this))));
    }

    private void doAnswerRequest(long answerId){
        addOnDisposeDisposable(service.sendOlympiadAnswer(coreState.getOlympiadId(), questionId, answerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> {
                    OlympiadAnswerResult o =  r.getData();
                    updateState(o.getCurrentQuestionIndex(), o.getNumberOfQuestions(), o.getCurrentQuestionId(), o.getPoints(), o.getState());
                    setScore(r.getData().getPointsForQuestion());
                }, t -> handleRequestErrors(t, Request.buildAnswerRequest(this, answerId))));
    }

    private void updateState(int currIndex, int maxIndex, long questionId, int currPoints, CompletionStatus state){
        this.currentQuestionIndex = currIndex - 1;
        this.finalQuestionIndex = maxIndex - 1;
        this.state = state;
        this.questionId = questionId;
        this.points = currPoints;
    }

    private boolean isLastQuestion(){
        return state == CompletionStatus.FINISHED;
    }

    @Data
    @Parcel
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VROlympiadFragmentData implements FragmentData {
        long olympiadId;
    }
}
