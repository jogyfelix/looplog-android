package com.jolabs.looplog.design_system.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.max

/**
 * A helper extension function to calculate the bounds of a RoundedPolygon.
 */
fun RoundedPolygon.getBounds() = calculateBounds().let {
    androidx.compose.ui.geometry.Rect(it[0], it[1], it[2], it[3])
}

/**
 * A Jetpack Compose Shape that renders a [RoundedPolygon].
 *
 * This class allows using RoundedPolygons from the androidx.graphics.shapes
 * library (like MaterialShapes.Fan) with modifiers like .clip() or .background().
 *
 * It handles scaling the normalized polygon to the component's size.
 *
 * @param polygon The [RoundedPolygon] to render.
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon
) : Shape {
    // We reuse this path object to avoid allocations on every recomposition.
    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Get the bounds of the normalized polygon (which is centered at (0,0)
        // with a radius of 1).
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)

        // Reset the path for this draw operation
        path.rewind()

        // Calculate the scale factor to make the polygon fill the
        // component's size.
        val scaleX = size.width / maxDimension
        val scaleY = size.height / maxDimension

        // Create an Android Path and Matrix for transformation
        val androidPath = android.graphics.Path()
        val matrix = android.graphics.Matrix()

        // Apply scaling and translation to center and fit the polygon
        // within the given size.
        matrix.apply {
            // Scale to fit
            postScale(scaleX, scaleY)

            // Translate to move the polygon's top-left to (0,0)
            // and then center it.
            postTranslate(
                -bounds.left * scaleX + (size.width - bounds.width * scaleX) / 2f,
                -bounds.top * scaleY + (size.height - bounds.height * scaleY) / 2f
            )
        }

        // Convert the RoundedPolygon to an Android Path
        polygon.toPath(androidPath)

        // Apply the matrix transformation
        androidPath.transform(matrix)

        // Convert to Compose Path and add to our reusable path
        path.addPath(androidPath.asComposePath())

        return Outline.Generic(path)
    }
}