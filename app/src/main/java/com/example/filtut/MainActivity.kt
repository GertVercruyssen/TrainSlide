package com.example.filtut

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
import java.nio.ByteBuffer


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
    private lateinit var scene: Scene
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // Should be pretty obvious :)
    private lateinit var camera: Camera

    //swapchain
    private var swapChain: SwapChain? = null
    // Filament entity representing a renderable object
    @Entity private var light = 0

    private lateinit var assetLoader: AssetLoader
    private lateinit var resourceLoader: ResourceLoader
    private lateinit var displayHelper: DisplayHelper

    //camera specific variables
    private val kNearPlane = 0.5
    private val kFarPlane = 10000.0
    private val kFovDegrees = 45.0
    private val kAperture = 16f
    private val kShutterSpeed = 1f / 125f
    private val kSensitivity = 100f

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
        setupScene()

        loadEnvironment("venetian_crossroads_2k")
        surfaceView.setOnTouchListener(this)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera().apply { setExposure(kAperture, kShutterSpeed, kSensitivity) }
        assetLoader = AssetLoader(engine, MaterialProvider(engine), EntityManager.get())
        resourceLoader = ResourceLoader(engine)
        GameLogic.engine = engine
    }

    private fun setupScene() {
        view.camera = camera
        view.scene = scene

        //Point the camera at the origin, slightly raised
        camera.lookAt(-5.0,20.0,5.0,0.0,0.0,0.0,0.0,1.0,0.0)

        // We now need a light, let's create a directional light
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                // Intensity of the sun in lux on a clear day
                .intensity(110_000.0f)
                // The direction is normalized on our behalf
                .direction(-0.753f, -1.0f, 0.890f)
                .castShadows(true)
                .build(engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        // Add renderable entities to the scene as they become ready.
        loadGlb("spider")
        loadGlb("floor")
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
        engine.destroyEntity(light)
        engine.destroyView(view)
        engine.destroyScene(scene)
        resourceLoader.destroy()
        swapChain?.let { engine.destroySwapChain(it) }
        engine.flushAndWait()
        swapChain = null
        uiHelper.detach()
    }

    /**
     * Loads a monolithic binary glTF and populates the Filament scene.
     */
    private fun loadGlb(name: String) : FilamentAsset {
        val asset : FilamentAsset?
        val buffer = readAsset("models/${name}.glb")

        asset = assetLoader.createAssetFromBinary(buffer)
        asset?.let {
            resourceLoader.loadResources(asset)
            asset.releaseSourceData()
            GameLogic.addGameObject(name ,asset, scene)
        }
        return asset!!
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        var buffer = readAsset("envs/$ibl/${ibl}_ibl.ktx")
        KtxLoader.createIndirectLight(engine, buffer).apply {
            intensity = 10_000f
            scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        buffer = readAsset("envs/$ibl/${ibl}_skybox.ktx")
        KtxLoader.createSkybox(engine, buffer).apply {
            scene.skybox = this
        }
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
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

    override fun onSensorChanged(event: SensorEvent)
    {
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
            camera.setProjection(kFovDegrees, aspect, kNearPlane, kFarPlane, Camera.Fov.VERTICAL)
        }
    }
}