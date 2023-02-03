package com.example.trainslide

import kotlin.math.*

object Util {
    /**
     * Creates a random float between an upper and lower bound
     */
    fun random(lower: Float, upper: Float) : Float {
        return ((Math.random() * (upper-lower))+lower ).toFloat()
    }
    fun calcDistance(xdistance : Float, ydistance : Float) : Float {
        return sqrt(xdistance*xdistance+ydistance*ydistance)
    }
}