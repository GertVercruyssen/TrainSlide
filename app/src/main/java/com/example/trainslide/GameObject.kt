package com.example.trainslide

import com.google.android.filament.*
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.utils.*

/**
 * represents a mesh that can animate and move in the scene
 * inherit from this class and implement its behavior
 */
open class GameObject(private var asset: FilamentAsset, private var scene: Scene, private var static: Boolean=true) {
    var scale = 0.0f
    var position: Float3 = Float3(0.0f, 0.0f, 0.0f)
    var rotation: Float3 = Float3(0.0f, 0.0f, 0.0f)
    private var currentanimation :Int? = null
    private var animationTimer : Double = 0.0 //time current animation is playing
    private var previousFrametime : Double = 0.0

    fun step(time: Double, tm:TransformManager) {
        if(!static) {
            var dTime = time - previousFrametime
            previousFrametime = time
            animationTimer += dTime

            performBehavior(dTime)

            var matscale = scale(Float3(scale, scale, scale))
            var matrotation = rotation(rotation)
            var mattranslation = translation(position)
            tm.setTransform(
                tm.getInstance(asset.root),
                transpose(matscale.times(mattranslation).times(matrotation)).toFloatArray()
            )

            currentanimation?.let { it1 ->
                asset.animator.applyAnimation(it1, animationTimer.toFloat())
                asset.animator.updateBoneMatrices()
            }
        }
    }

    //abstract function for inheriting class. Default behavior is to do nothing
    open fun performBehavior(dTime: Double) {
        /**
         * to perform an animation:
         * animationTimer = 0.0
         * currentAnimationIndex = 4 //number of specific animation
         * animationMaxTimer = animator.getAnimationDuration(currentAnimationIndex)
         * if (animationTimer > animationMaxTimer)
         *      animationTimer = animationMaxTimer //freeze at the end OR do something else
         */
    }

    fun setIsRendering(value: Boolean) {
        if(value)
            scene.addEntities(asset.entities)
        else {
            for (i in asset.entities)
                scene.removeEntity(i)
        }
    }

    fun destroy(engine: Engine){
        engine.destroyEntity(asset.root)
    }
}