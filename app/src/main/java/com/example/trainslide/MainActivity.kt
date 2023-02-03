package com.example.trainslide

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.*
import com.google.android.filament.utils.*


class MainActivity : AppCompatActivity() ,android.view.View.OnTouchListener, android.hardware.SensorEventListener {

    companion object {
        init { Utils.init()}
    }

    private val uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var accell: Sensor
    private lateinit var sensorManager: SensorManager

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    private lateinit var engine: Engine
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer
    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var gamescene: GameScene
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // Should be pretty obvious :)
    private lateinit var camera: Camera

    //swapchain
    private var swapChain: SwapChain? = null

    private lateinit var resourceLoader: ResourceLoader
    private lateinit var displayHelper: DisplayHelper

    //camera specific variables
    private val kNearPlane = 0.5
    private val kFarPlane = 10000.0
    private val kFovDegrees = 45.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this).apply { setContentView(this) }
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(surfaceView.context)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accell = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupFilament()
        surfaceView.setOnTouchListener(this)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        view = engine.createView()
        resourceLoader = ResourceLoader(engine)
        GameLogic.initMembers(engine)
        GameScene.initMembers(engine, view, assets , resourceLoader)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            //schedule the next frame
            choreographer.postFrameCallback(this)
            // This check guarantees that we have a swap chain
            if (!uiHelper.isReadyToRender) {
                return
            }

            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            GameLogic.step(seconds)

            //Allow the resource loader to finalize textures that have become ready.
            resourceLoader.asyncUpdateLoad()

            // If beginFrame() returns false you should skip the frame
            // This means you are sending frames too quickly to the GPU
            if (renderer.beginFrame(swapChain!!, currentTime)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
        sensorManager.registerListener(this,accell,SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameLogic.destroy()
        choreographer.removeFrameCallback(frameCallback)
        engine.destroyRenderer(renderer)
        engine.destroyEntity(GameScene.light)
        engine.destroyView(view)
        engine.destroyScene(GameScene.scene)
        resourceLoader.destroy()
        swapChain?.let { engine.destroySwapChain(it) }
        engine.flushAndWait()
        swapChain = null
        uiHelper.detach()
    }

    /**
     * Handles a [MotionEvent] to enable one-finger orbit, two-finger pan, and pinch-to-zoom.
     */
    @SuppressWarnings("ClickableViewAccessibility")
    override fun onTouch(view: android.view.View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                GameLogic.touch(event.getX(0), event.getY(0))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                GameLogic.move(event.getX(0), event.getY(0))
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSensorChanged(event: SensorEvent) {
        GameLogic.tilt = event.values[1]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //do nothing
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            view.viewport = Viewport(0, 0, width, height)
            val aspect = width.toDouble() / height.toDouble()
            GameScene.camera.setProjection(kFovDegrees, aspect, kNearPlane, kFarPlane, Camera.Fov.VERTICAL)
        }
    }
}