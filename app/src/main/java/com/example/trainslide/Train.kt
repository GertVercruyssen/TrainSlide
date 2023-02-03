package com.example.trainslide

import com.google.android.filament.Scene
import com.google.android.filament.gltfio.FilamentAsset
import kotlin.math.abs

/**
 * Contains the behavior of train objects (sliding left and right and controlling the door animations)
 * Direction true = right
 */

class Train(private var asset: FilamentAsset, private var scene: Scene) : GameObject(asset,scene) {
    var speed : Float = 0.0f
    init {
    }

    override fun performBehavior(dTime: Double) {
        val tilt = GameLogic.tilt
        speed += tilt/10
        if(abs(speed) > tilt*tilt) //clamp speed to tilt^2
            speed = tilt*tilt* if(speed>0) 1 else -1
        position.x += speed

        if(abs(position.x) > 30)
            position.x = position.x*-1
    }
}