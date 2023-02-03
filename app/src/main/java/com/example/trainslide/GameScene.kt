package com.example.trainslide

import android.content.res.AssetManager
import com.example.trainslide.GameLogic.engine
import com.google.android.filament.*
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.utils.KtxLoader
import java.nio.ByteBuffer

/**
 * Wrapper for the scene, lights, camera
 * Handles creation of the gameobjects and adds them to the scene
 */
object GameScene {
    lateinit var scene: Scene
    lateinit var view: View
    lateinit var camera: Camera
    lateinit var level: Level

    lateinit var assetLoader: AssetLoader //filament assets
    lateinit var assets: AssetManager //android assets
    lateinit var resourceLoader: ResourceLoader //engine synchronized loader

    // Filament entity representing a renderable object
    @Entity
    var light = 0

    fun initMembers(
        engine: Engine,
        view: View,
        assets: AssetManager,
        resourceLoader: ResourceLoader
    ) {
        this.scene = engine.createScene()
        this.view = view
        this.assets = assets
        this.assetLoader = AssetLoader(engine, MaterialProvider(engine), EntityManager.get())
        this.resourceLoader = resourceLoader
        loadLevel("Trains")
        setupScene()
        loadEnvironment(level.getParamsforSpecific("environment")["filename"]!!)
        loadObjects()
    }

    fun loadLevel(filename: String) {
        val bytes = readAsset("levels/${filename}.txt")
        level = Level(String(bytes.array()))
    }

    private fun loadObjects() {
        var objects = level.getParamsfor("object")
        for(objectdata in objects.values) {
            loadGlb(objectdata["objectname"]!!, objectdata["filename"]!!, objectdata["static"]!!.toBoolean())
        }
        GameLogic.doSetupObjects()
    }

    private fun setupScene() {
        val cameraParams = level.getParamsforSpecific("camera")
        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera = engine.createCamera().apply { setExposure(cameraParams["aperture"]!!.toFloat(), cameraParams["shutterSpeed"]!!.toFloat(), cameraParams["sensitivity"]!!.toFloat()) }

        view.camera = camera
        view.scene = scene

        // We now need a light, let's create a directional light
        light = EntityManager.get().create()
        val lightParams = level.getParamsforSpecific("light")

        // Create a color from a temperature (D65)
        val (r, g, b) = Colors.cct(lightParams["temperature"]!!.toFloat())
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            // Intensity of the sun in lux on a clear day
            .intensity(lightParams["intensity"]!!.toFloat())
            // The direction is normalized on our behalf
            .direction(lightParams["directionx"]!!.toFloat(), lightParams["directiony"]!!.toFloat(), lightParams["directionz"]!!.toFloat())
            .castShadows(lightParams["shadows"]!!.toBoolean())
            .build(engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        //Point the camera at the origin, slightly raised
        GameScene.camera.lookAt(-5.0,20.0,5.0,0.0,0.0,0.0,0.0,1.0,0.0)

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
    /**
     * Loads a monolithic binary glTF and populates the Filament scene.
     */
    private fun loadGlb(objectname: String, filename: String, static: Boolean=true) : FilamentAsset {
        val asset : FilamentAsset?
        val buffer = readAsset("models/${filename}.glb")

        asset = assetLoader.createAssetFromBinary(buffer)
        asset?.let {
            resourceLoader.loadResources(asset)
            asset.releaseSourceData()
            GameLogic.addGameObject(objectname ,asset, static)
        }
        return asset!!
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }
}