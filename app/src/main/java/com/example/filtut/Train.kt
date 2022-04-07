package com.example.filtut

import com.google.android.filament.Scene
import com.google.android.filament.gltfio.FilamentAsset

/**
 * Contains the behavior of train objects (sliding left and right and controlling the door animations)
 * Direction true = right
 */
class Train(private var asset: FilamentAsset, private var scene: Scene, direction: Boolean) : GameObject(asset,scene) {
    var speed : Float = 0.0f
    init {
        if(direction)
            rotation.y = Math.PI.toFloat()/2*-1
    }

    override fun performBehavior(dTime: Double) {
        val tilt = GameLogic.tilt
        speed += tilt
        position.x += speed
    }
}