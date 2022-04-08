package com.example.filtut

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.Scene
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.Float3
import kotlin.math.abs

object GameLogic {
    private var assets: MutableMap<String,GameObject> = mutableMapOf()
    lateinit var engine: Engine
    lateinit var camera: Camera
    private lateinit var viewMatrix: FloatArray
    var tilt :Float = 0.0f

    fun initMembers(engine: Engine,camera: Camera) {
        this.camera = camera
        this.engine = engine
        viewMatrix= GameLogic.camera.getViewMatrix(null)
    }

    /**
     * Add specific game objects here, tied to the asset name
     */
    fun addGameObject(id:String, asset: FilamentAsset, scene: Scene, isRendering:Boolean = true) {
        if(id.contains("train",true))
            assets[id] = Train(asset,scene)
        else
            assets[id] = GameObject(asset,scene)

        assets[id]?.setIsRendering(isRendering)
    }

    fun step(totalTime: Double) {
        //add camera shake when we are going fast
        val totalspeed =abs((assets["train1"] as Train).speed) + abs((assets["train2"] as Train).speed)
        if(totalspeed >30)
            shakeCamera(totalspeed-30*0.1f)

        for (gameObject in assets.values)
            gameObject.step(totalTime, engine.transformManager)
    }

    private fun shakeCamera(strength :Float) {
        var newViewMatrix = viewMatrix
        newViewMatrix[0] += Util.random(strength*-1,strength) + viewMatrix[0]
        newViewMatrix[4] += Util.random(strength*-1,strength) + viewMatrix[4]
        newViewMatrix[8] += Util.random(strength*-1,strength) + viewMatrix[8]
        camera.setModelMatrix(viewMatrix)
    }

    fun doSetupObjects() {
        assets["train1"]?.position?.z =1.0f
        assets["train1"]?.rotation?.y = Math.PI.toFloat()*-1
        assets["train2"]?.position?.z=-1.0f
    }

    fun getObjectByName(name: String): GameObject? {
        return assets[name]
    }

    fun destroy(){
        for (gameob in assets.values)
            gameob.destroy(engine)
        assets.clear()
    }

    fun touch(touchx : Float, touchy : Float) {
        //not yet implemented
    }

    fun move(touchx : Float, touchy : Float) {
        //not yet implemented
    }
}