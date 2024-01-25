package pl.interia.omnibus.vr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.CardBoardAndroidApplication;
import com.badlogic.gdx.backends.android.CardBoardApplicationListener;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import io.reactivex.schedulers.Schedulers;
import pl.interia.omnibus.BuildConfig;
import pl.interia.omnibus.R;
import pl.interia.omnibus.container.FragmentData;
import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.model.OpracowaniaServiceInstallationResultEvent;
import pl.interia.omnibus.traffic.Traffic;
import pl.interia.omnibus.traffic.TrafficScreen;
import pl.interia.omnibus.utils.DeviceUtils;
import pl.interia.omnibus.vr.flashcard.FlashcardsLWSVrCoreImp;
import pl.interia.omnibus.vr.flashcard.FlashcardsVrCoreImp;
import pl.interia.omnibus.vr.olympiad.OlympiadVrCoreImp;
import pl.interia.omnibus.vr.quiz.QuizVrCoreImp;
import timber.log.Timber;

public class VrActivity extends CardBoardAndroidApplication implements CardBoardApplicationListener {
	private static final String CORE_TYPE = "coreType";
	private static final String CORE_DATA = "coreData";
	private VrCore core;

	public static void start(Context ctx, FragmentData data, VrCore.Type type, OpracowaniaService service) {
		if(DeviceUtils.isDeviceSupportVR(ctx)) {
			Intent i = new Intent(ctx, VrActivity.class);
			i.putExtra(CORE_TYPE, type);
			i.putExtra(CORE_DATA, Parcels.wrap(data));
			ctx.startActivity(i);
			service.sendVRUsedActivity()
					.subscribeOn(Schedulers.io())
					.subscribe(r -> {}, t -> Timber.w(t.getMessage()));
		} else {
			Toast.makeText(ctx, R.string.vr_not_supported, Toast.LENGTH_SHORT).show();
			Answers.getInstance().logCustom(new CustomEvent("Device not support VR mode"));
		}
	}

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		Timber.d("LIFECYCLE ANDROID-onCreate");
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(this, config);
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = 1;
		getWindow().setAttributes(lp);
		VrCore.Type type = (VrCore.Type)getIntent().getSerializableExtra(CORE_TYPE);
		if(type == null || type.equals(VrCore.Type.FLASHCARDS)){
            core = new FlashcardsVrCoreImp(savedInstanceState, Parcels.unwrap(getIntent().getParcelableExtra(CORE_DATA)));
		}else if(type.equals(VrCore.Type.QUIZZES)){
			core = new QuizVrCoreImp(savedInstanceState, Parcels.unwrap(getIntent().getParcelableExtra(CORE_DATA)));
		}else if(type.equals(VrCore.Type.OLYMPIADS)){
			core = new OlympiadVrCoreImp(savedInstanceState, Parcels.unwrap(getIntent().getParcelableExtra(CORE_DATA)));
		}else if(type.equals(VrCore.Type.FLASHCARDS_LWS)){
			core = new FlashcardsLWSVrCoreImp(savedInstanceState, Parcels.unwrap(getIntent().getParcelableExtra(CORE_DATA)));
		}
		EventBus.getDefault().register(this);

		Traffic.getInstance().notifyCreate(this);
		Traffic.getInstance().onScreenView(TrafficScreen.VR);
	}

	@Override
	public void create() {
		Timber.d("LIFECYCLE GDX-create");
		throwableWrapper(()->core.create(this));
	}

	@Override
	public void onDrawEye(Eye eye) {
		throwableWrapper(()->core.onDrawEye(eye));
	}

	@Override
	public void dispose() {
		Timber.d("LIFECYCLE GDX-dispose");
		throwableWrapper(()->core.dispose());
	}


	@Override
	public void resize(int width, int height) {
		Timber.d("LIFECYCLE GDX-resize");
		throwableWrapper(()->core.resize(width, height));
	}

	@Override
	public void resume() {
		// do nothing now
		Timber.d("LIFECYCLE GDX-resume");
	}
	@Override
	public void pause() {
		// do nothing now
		Timber.d("LIFECYCLE GDX-pause");
	}
	@Override
	public void render() {
		// do nothing now
	}
	@Override
	public void onFinishFrame(Viewport viewport) {
		// do nothing now
	}
	@Override
	public void onRendererShutdown() {
		// do nothing now
	}
	@Override
	public void onNewFrame(HeadTransform headTransform) {
		throwableWrapper(()->core.onNewFrame(headTransform));
	}

	private void throwableWrapper(Runnable run){
		if(BuildConfig.DEBUG){
			try {
				run.run();
			}catch(Exception e){
				Timber.e(e);
			}
		}else{
			run.run();
		}
	}

	@Override
	protected void onPause() {
		Timber.d("LIFECYCLE ANDROID-onPause");
		Traffic.getInstance().notifyPause(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Timber.d("LIFECYCLE ANDROID-onResume");
		Traffic.getInstance().notifyResume(this);
		super.onResume();
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(OpracowaniaServiceInstallationResultEvent event) {
		if (event.getService() != null) {
			core.notifyOpracowaniaServiceReady(event.getService());
		}else{
			throw new IllegalArgumentException(event.getCause());

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		core.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		Timber.d("LIFECYCLE ANDROID-onDestroy");
		super.onDestroy();
		EventBus.getDefault().unregister(this);
		Gdx.app.exit();
		Gdx.app = null;
		Gdx.graphics = null;
		Gdx.audio = null;
		Gdx.input = null;
		Gdx.files = null;
		Gdx.net = null;
		Gdx.gl = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		core.onDestroy();
	}
}
