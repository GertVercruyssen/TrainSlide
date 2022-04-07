package com.example.filtut

import com.google.android.filament.Engine
import com.google.android.filament.Scene
import com.google.android.filament.gltfio.FilamentAsset

object GameLogic {
    private var assets: MutableMap<String,GameObject> = mutableMapOf()
    lateinit var engine: Engine
    var tilt :Float = 0.0f

    /**
     * Add specific game objects here, tied to the asset name
     */
    fun addGameObject(id:String, asset: FilamentAsset, scene: Scene, isRendering:Boolean = true) {
        if(id.contains("train"))
            assets[id] = Train(asset,scene,true)
        else
            assets[id] = GameObject(asset,scene)

        assets[id]?.setIsRendering(isRendering)
    }
    fun step(totalTime: Double) {
        doGameLogic()
        for (gameObject in assets.values)
            gameObject.step(totalTime, engine.transformManager)
    }

    private fun doGameLogic() {
        //Write game logic here, performed before each individual object's logic
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