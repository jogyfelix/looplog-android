package com.jolabs.looplog.habit.utils

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape

private val habitShapeMap = mutableMapOf<Any, RoundedPolygon>()
private val shapeWrapperCache = mutableMapOf<Any, Shape>()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val allShapes = listOf(
    MaterialShapes.Circle, MaterialShapes.Square, MaterialShapes.Slanted,
    MaterialShapes.Arch, MaterialShapes.SemiCircle, MaterialShapes.Oval,
    MaterialShapes.Pill, MaterialShapes.Triangle, MaterialShapes.Arrow,
    MaterialShapes.Fan, MaterialShapes.Diamond, MaterialShapes.ClamShell,
    MaterialShapes.Pentagon, MaterialShapes.Gem, MaterialShapes.Sunny,
    MaterialShapes.VerySunny, MaterialShapes.Cookie4Sided, MaterialShapes.Cookie6Sided,
    MaterialShapes.Cookie7Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided,
    MaterialShapes.Clover4Leaf, MaterialShapes.Clover8Leaf, MaterialShapes.Burst,
    MaterialShapes.SoftBurst, MaterialShapes.Boom, MaterialShapes.SoftBoom,
    MaterialShapes.Flower, MaterialShapes.Puffy, MaterialShapes.PuffyDiamond,
    MaterialShapes.Ghostish, MaterialShapes.PixelCircle, MaterialShapes.PixelTriangle,
    MaterialShapes.Bun, MaterialShapes.Heart
)

/**
 * Returns a deterministic RoundedPolygon for a given ID.
 * The shape is cached to ensure consistency across the app's lifetime.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun getHabitShape(id: Any): RoundedPolygon {
    return habitShapeMap.getOrPut(id) {
        allShapes.random()
    }
}

/**
 * Returns a cached Compose Shape wrapper for a RoundedPolygon based on a given ID.
 */
fun getHabitShapeWrapper(id: Any): Shape {
    return shapeWrapperCache.getOrPut(id) {
        val polygon = getHabitShape(id)
        RoundedPolygonShape(polygon)
    }
}
