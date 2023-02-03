package com.example.trainslide

import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentAsset
import kotlin.math.abs

/**
 * controls all logic to be performed by the game
 * movement of objects, changes in lighting, etc...
 */
object GameLogic {
    private var assets: MutableMap<String,GameObject> = mutableMapOf()
    lateinit var engine: Engine
    private lateinit var viewMatrix: FloatArray
    var tilt :Float = 0.0f

    fun initMembers(
        engine: Engine,
    ) {
        this.engine = engine
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
        var newViewMatrix = GameScene.camera.getViewMatrix(null)
        newViewMatrix[0] += Util.random(strength*-1,strength) + viewMatrix[0]
        newViewMatrix[4] += Util.random(strength*-1,strength) + viewMatrix[4]
        newViewMatrix[8] += Util.random(strength*-1,strength) + viewMatrix[8]
        GameScene.camera.setModelMatrix(viewMatrix)
    }

    /**
     * Tell the gamelogic that all the objects are inserted and can be modified for initial positions, etc
     */
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

    /**
     * Add specific game objects here
     * The id will determine what kind of behavior it gets
     * If new types of objects are made, add them here
     */
    fun addGameObject(id:String, asset: FilamentAsset, isRendering:Boolean = true) {
        if(id.contains("train",true))
            assets[id] = Train(asset,GameScene.scene)
        else
            assets[id] = GameObject(asset,GameScene.scene)

        assets[id]?.setIsRendering(isRendering)
    }
}