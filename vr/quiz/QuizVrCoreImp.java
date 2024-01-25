package pl.interia.omnibus.vr.quiz;


import android.content.Context;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.google.vr.sdk.base.Eye;

import org.parceler.Parcel;

import java.util.Collections;

import icepick.Icepick;
import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.api.QuizSolution;
import pl.interia.omnibus.model.api.pojo.QuestionAnswerPair;
import pl.interia.omnibus.model.api.pojo.quiz.Quiz;
import pl.interia.omnibus.utils.Utils;
import pl.interia.omnibus.vr.VrCore;
import pl.interia.omnibus.vr.flashcard.model.CacheableFlashcard;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.object3d.dialog.DialogModelInstance;
import pl.interia.omnibus.vr.object3d.room.RoomModelInstance;
import pl.interia.omnibus.vr.quiz.model.CachableQuestion;
import pl.interia.omnibus.vr.quiz.model.QuizState;
import pl.interia.omnibus.vr.quiz.object3d.QuizButtonModelInstance;
import pl.interia.omnibus.vr.quiz.object3d.QuizQuestionModelInstance;
import pl.interia.omnibus.vr.quiz.object3d.QuizSummaryDialogModelInstance;
import timber.log.Timber;

public class QuizVrCoreImp extends VrCore {
    private QuizState quizState;
    private DialogModelInstance dialog;
    private QuizQuestionModelInstance quizQuestion3d;
    private QuizSummaryDialogModelInstance summaryDialog;
    private boolean isEnd = false;
    private Cacheable current;
    @State(FragmentData.FragmentDataBundler.class)
    protected VRQuizFragmentData fragmentData;
    private boolean isRequestSummaryError = false;

    public QuizVrCoreImp(Bundle savedState, VRQuizFragmentData cs) {
        super();
        Icepick.restoreInstanceState(this, savedState);
        if(fragmentData == null){
            this.fragmentData = cs;
        }
        Timber.d( "fragmentData %s", fragmentData);
    }

    protected void onOpracowaniaServiceReady(){
        reset();
    }

    private void reset(){
        isEnd = false;
        isRequestSummaryError = false;
        Collections.shuffle(fragmentData.quiz.getQuestions());
        quizState = new QuizState(fragmentData.quiz.getQuestions(), fragmentData.index, service);
        moveNext();
    }

    private void setQuestion(CachableQuestion question){
        Schedulers.io().scheduleDirect(() -> {
            quizQuestion3d.prepare(question.getQuestionImg(), question.getData(), fragmentData.index, fragmentData.quiz.getQuestions().size());
            Gdx.app.postRunnable(() -> {
                quizState.purge();
                if(!quizQuestion3d.isShowed()) {
                    quizQuestion3d.show();
                }else {
                    quizQuestion3d.flipToFront();
                }
            });
        });
    }

    @Override
    protected void registerModels(AssetManager assets, Context ctx) {
        registerModel(new RoomModelInstance(assets));
        quizQuestion3d = new QuizQuestionModelInstance(assets, ctx);
        registerModel(quizQuestion3d);
        dialog = new DialogModelInstance(assets);
        registerModel(dialog);
        summaryDialog = new QuizSummaryDialogModelInstance(assets, ctx);
        registerModel(summaryDialog);
    }

    @Override
    public void onClick(OmnibusModelInstance object) {
        if(quizQuestion3d.isAnswerClick(object)) {
            long answerId = ((QuizButtonModelInstance)object).getAnswer().getId();
            fragmentData.solution.registerAnswer(new QuestionAnswerPair(fragmentData.quiz.getQuestions().get(fragmentData.index).getId(), answerId));
            current.setState(CacheableFlashcard.State.FINISHED);
            if (quizState.isNextFinished()) {
                moveNext();
            }else {
                quizQuestion3d.flipToBack(this::moveNext);
            }
        } else if (summaryDialog.isButtonRepeat(object)){
            summaryDialog.hide(() -> {
                fragmentData.reset();
                reset();
            });
        } else if(dialog.isButton1(object)){
            if(isRequestSummaryError) {
                dialog.hide(this::doSolutionRequest);
            } else {
                dialog.hide(() -> current.cache());
            }
        } else if (dialog.isButton2(object) || summaryDialog.isButtonExit(object)){
            exit();
        }
    }

    @Override
    protected void onDraw(Eye eye) {
        super.onDraw(eye);
        if(!isEnd && quizState != null) {
            if (current != null && current.getState() != CachableQuestion.State.FINISHED) {
                if(current.getState() == CachableQuestion.State.CACHED) {
                    setQuestion((CachableQuestion)current);
                    current.setState(CachableQuestion.State.DRAWN);
                }else if(current.getState() == CachableQuestion.State.ERROR && shouldFireHideToDialogShowAnim()){
                    quizQuestion3d.hide(()-> dialog.show(DialogModelInstance.Type.IO_EXCEPTION));
                }
            } else if (quizState.isFinished()) {
                isEnd = true;
                quizQuestion3d.hide(this::doSolutionRequest);
            }
        }
    }

    private boolean shouldFireHideToDialogShowAnim() {
        return !quizQuestion3d.isHiding() && !dialog.isHiding() && !dialog.isVisible();
    }

    private void moveNext(){
        current = quizState.next();
        fragmentData.index = quizState.getSolvingIndex();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    private void doSolutionRequest(){
        QuizSolution solution = fragmentData.toSolution();
        addOnDisposeDisposable(service.sendQuizSolution(fragmentData.getQuizId(), solution)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> summaryDialog.show(new QuizSummaryDialogModelInstance.Solution(solution.getDuration(), r.getData())), t -> {
                    isRequestSummaryError = true;
                    dialog.show(DialogModelInstance.Type.IO_EXCEPTION);
                }));
    }

    @Data
    @Parcel
    @NoArgsConstructor
    public static class VRQuizFragmentData implements FragmentData {
        long quizId;
        long solvingStartTime;
        Quiz quiz;
        int index;
        QuizSolution solution;

        public VRQuizFragmentData(long quizId, Quiz quiz) {
            this.quizId = quizId;
            this.quiz = quiz;
            reset();
        }

        private QuizSolution toSolution(){
            solution.setDuration(Utils.getCurrentUnixTime() - solvingStartTime);
            return solution;
        }

        void reset(){
            index = 0;
            solvingStartTime = Utils.getCurrentUnixTime();
            solution = new QuizSolution();
        }
    }
}
