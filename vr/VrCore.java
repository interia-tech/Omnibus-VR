package pl.interia.omnibus.vr;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.android.CardboardCamera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import pl.interia.omnibus.model.OpracowaniaService;
import pl.interia.omnibus.vr.hud.Hud;
import pl.interia.omnibus.vr.object3d.OmnibusModelInstance;
import pl.interia.omnibus.vr.utils.Fonts;

public abstract class VrCore implements Hud.OnClickListener{
    private static boolean IS_CAM_CONTROLLER_ENABLED = false;
    private CardboardCamera eyeCamera;
    private CardboardCamera headCamera;
    private Environment environment;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 300f;
    private ModelBatch modelBatch;
    private AssetManager assets;
    protected boolean isLoaded = false;
    private boolean isInited = false;
    protected boolean isServiceReady = false;
    private Hud hud;
    private Array<OmnibusModelInstance> allModels = new Array<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private CameraInputController camController;
    private static final Vector3 CAM_POSITION =     new Vector3(0,1f, 1f);
    private Vector3 camCenterNear =  new Vector3(0,1f, 0);
    private Vector3 camCenterFar =   new Vector3(0,1f, 1);
    private Vector3 leftEyeDrawPoint;
    private Vector3 rightEyeDrawPoint;
    private Vector3 leftEyeRayPoint;
    private HeadTransform headTransform;
    protected Activity activity;
    protected OpracowaniaService service;

    public enum Type{
        FLASHCARDS,
        QUIZZES,
        OLYMPIADS,
        FLASHCARDS_LWS
    }


    public final void notifyOpracowaniaServiceReady(OpracowaniaService service){
        this.service = service;
        if(!isInited){
            onOpracowaniaServiceReady();
            isServiceReady = true;
            isInited = true;
        }
    }

    protected void addOnDisposeDisposable(Disposable d){
        disposables.add(d);
    }

    public void create(Activity activity) {
        this.activity = activity;
        Fonts.init(activity);
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0, -1f, -0.4f));
        eyeCamera = new CardboardCamera();
        eyeCamera.position.set(CAM_POSITION);
        eyeCamera.near = Z_NEAR;
        eyeCamera.far = Z_FAR;
        eyeCamera.update();
        headCamera = new CardboardCamera();
        headCamera.position.set(CAM_POSITION);
        headCamera.near = Z_NEAR;
        headCamera.far = Z_FAR;
        headCamera.update();
        if(IS_CAM_CONTROLLER_ENABLED) {
            camController = new CameraInputController(eyeCamera);
            Gdx.input.setInputProcessor(camController);
        }
        assets = new AssetManager();
        registerModels(assets, activity);
        hud = new Hud(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), assets, this);
    }

    protected final void registerModel(OmnibusModelInstance model){
        allModels.add(model);
    }

    protected void doneLoading() {
        for(OmnibusModelInstance i : allModels){
            i.notifyDoneLoading(assets);
        }
        hud.notifyDoneLoading(assets);
        hud.setModels(allModels);
        isLoaded = true;
    }

    private boolean isLoaded(){
        if(!isLoaded && assets.update()){
            doneLoading();
        }
        return isLoaded;
    }

    void onNewFrame(HeadTransform ht) {
        Matrix4 m = new Matrix4(ht.getHeadView());
        headCamera.setEyeViewAdjustMatrix(m);
        headCamera.update();
        if(headTransform == null) {
            calculateWorldReferencePoints();
        }
        headTransform = ht;
    }

    final void onDrawEye(Eye eye) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        // Apply the eye transformation to the camera.
        Matrix4 m = new Matrix4(eye.getEyeView());
        eyeCamera.setEyeViewAdjustMatrix(m);
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        eyeCamera.setEyeProjection(new Matrix4(perspective));
        eyeCamera.update();
        if(leftEyeDrawPoint == null || rightEyeDrawPoint == null){
            calculateEyeCenter(eye);
            return;
        }
        if(isLoaded()) {
            if(IS_CAM_CONTROLLER_ENABLED) {
                camController.update();
            }
            onDraw(eye);
        }
    }

    protected void onDraw(Eye eye){
        for (OmnibusModelInstance i : allModels) {
            i.updateAnimations(Gdx.graphics.getDeltaTime() * 1.0f);
        }

        modelBatch.begin(eyeCamera);
        for (OmnibusModelInstance i : allModels) {
            i.render(modelBatch, i.isUseEnvironment() ? environment : null);
        }
        modelBatch.end();
        hud.setDrawPoint(eye.getType() == Eye.Type.LEFT ? leftEyeDrawPoint : rightEyeDrawPoint);
        if(eye.getType() == Eye.Type.LEFT) {
            hud.updateFocusedObject(eyeCamera, leftEyeRayPoint);
        }
        hud.draw();
    }

    public final void dispose() {
        disposables.dispose();
        modelBatch.dispose();
        for (OmnibusModelInstance i : allModels) {
            i.dispose();
        }
        allModels.clear();
        assets.dispose();
        hud.dispose();
    }

    final void resize(int width, int height) {
        leftEyeDrawPoint = null;
        rightEyeDrawPoint = null;
        headTransform = null;
        if(hud != null) {
            hud.resize(width, height);
        }
    }

    private void calculateEyeCenter(Eye eye){
       Vector3 center = eyeCamera.project(new Vector3(camCenterNear));
        if(eye.getType() == Eye.Type.LEFT) {
            leftEyeDrawPoint = center;
            center = eyeCamera.project(new Vector3(camCenterFar));
            eyeCamera.unproject(center);
            Vector3 rayCenterNear = eyeCamera.project(center);
            center = eyeCamera.project(new Vector3(camCenterNear));
            eyeCamera.unproject(center);
            Vector3 rayCenterFar = eyeCamera.project(center);
            leftEyeRayPoint = new Vector3(rayCenterNear.x - (rayCenterNear.x - rayCenterFar.x) / 2,
                    rayCenterNear.y - (rayCenterNear.y - rayCenterFar.y) / 2, 0);
        }else{
            rightEyeDrawPoint = center;
        }
    }

    private void calculateWorldReferencePoints(){
        Vector3 center = new Vector3(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        camCenterNear = headCamera.unproject(new Vector3(center));
        center.z = 1;
        camCenterFar = headCamera.unproject(new Vector3(center));
    }

    public void onSaveInstanceState(Bundle outState){

    }

    public final void exit(){
        activity.finish();
        Gdx.app.exit();
    }


    public void onDestroy() {

    }

    protected abstract void onOpracowaniaServiceReady();
    protected abstract void registerModels(AssetManager assets, Context ctx);
}
