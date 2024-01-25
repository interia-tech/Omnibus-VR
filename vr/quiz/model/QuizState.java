package pl.interia.omnibus.vr.quiz.model;

import java.util.List;

import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.api.pojo.Question;
import pl.interia.omnibus.vr.model.Cacheable;
import pl.interia.omnibus.vr.model.ItemsState;

public class QuizState extends ItemsState<Question> {

    public QuizState(List<Question> f, int startQuestionIndex, OpracowaniaService service) {
        super(f, startQuestionIndex, service);
    }

    @Override
    protected Cacheable buildCacheable(Question data) {
        return new CachableQuestion(data, service);
    }
}
