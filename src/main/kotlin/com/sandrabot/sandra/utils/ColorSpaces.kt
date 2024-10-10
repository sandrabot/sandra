/*
 * Copyright 2017-2024 Avery Carroll and Logan Devecka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandrabot.sandra.utils

import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Calculates the "true" average color of the [image] provided.
 * Colors are first converted from sRGB into CIELAB before being averaged, and finally returned to sRGB.
 * By performing the average calculation on CIELAB values, the result is perceived as more natural.
 *
 * [Read more about the CIELAB color space](https://en.wikipedia.org/wiki/CIELAB_color_space)
 */
fun findTrueAverageColor(image: BufferedImage): Color {
    val pixelCount = image.width * image.height
    val colorsL = ArrayList<Float>(pixelCount)
    val colorsA = ArrayList<Float>(pixelCount)
    val colorsB = ArrayList<Float>(pixelCount)
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val pixel = Color(image.getRGB(x, y))
            CIELabColorSpace.fromRGB(
                floatArrayOf(pixel.red.toFloat(), pixel.green.toFloat(), pixel.blue.toFloat())
            ).let { (l, a, b) ->
                colorsL += l
                colorsA += a
                colorsB += b
            }
        }
    }
    val averageL = colorsL.average().toFloat()
    val averageA = colorsA.average().toFloat()
    val averageB = colorsB.average().toFloat()
    return CIELabColorSpace.toRGB(
        floatArrayOf(averageL, averageA, averageB)
    ).let { (r, g, b) -> Color(r.roundToInt(), g.roundToInt(), b.roundToInt()) }
}


/**
 * Implementation was provided by "Chus" in this wonderful answer
 * [on Stackoverflow](https://stackoverflow.com/a/77687937/13274689).
 */
object SRGBColorSpace : ColorSpace(TYPE_RGB, 3) {
    override fun toRGB(colorValue: FloatArray) = colorValue

    override fun fromRGB(rgbValue: FloatArray) = rgbValue

    private fun fTo(t: Float): Float = if (t <= 0.04045f) t / 12.92f else ((t + 0.055f) / 1.055f).pow(2.4f)

    override fun toCIEXYZ(colorValue: FloatArray): FloatArray {
        // Also normalize RGB values here.
        val r = fTo(colorValue[0] / 255.0f)
        val g = fTo(colorValue[1] / 255.0f)
        val b = fTo(colorValue[2] / 255.0f)

        // Use D50 chromatically adapted matrix here as Photoshop does.
        val x = (0.4360747f * r) + (0.3850649f * g) + (0.1430804f * b)
        val y = (0.2225045f * r) + (0.7168786f * g) + (0.0606169f * b)
        val z = (0.0139322f * r) + (0.0971045f * g) + (0.7141733f * b)

        return floatArrayOf(x, y, z)
    }

    private fun fFrom(t: Float): Float = if (t > 0.0031308f) (1.055f * t.pow(1 / 2.4f)) - 0.055f else t * 12.92f

    override fun fromCIEXYZ(colorValue: FloatArray): FloatArray {
        val x = colorValue[0]
        val y = colorValue[1]
        val z = colorValue[2]

        // Use D50 chromatically adapted matrix as Photoshop does.
        val tR = (3.1338561f * x) + (-1.6168667f * y) + (-0.4906146f * z)
        val tG = (-0.9787684f * x) + (1.9161415f * y) + (0.0334540f * z)
        val tB = (0.0719453f * x) + (-0.2289914f * y) + (1.4052427f * z)

        val r = fFrom(tR) * 255.0f
        val g = fFrom(tG) * 255.0f
        val b = fFrom(tB) * 255.0f

        return floatArrayOf(r, g, b)
    }

    private fun readResolve(): Any = SRGBColorSpace
}


/**
 * Implementation was provided by "Chus" in this wonderful answer
 * [on Stackoverflow](https://stackoverflow.com/a/77687937/13274689).
 */
object CIELabColorSpace : ColorSpace(TYPE_Lab, 3) {
    // We use illuminant D50 "CIE 1931 2 Degree Standard Observer" as Photoshop does.
    // Values are normalized.
    private const val Xn = 0.964212f
    private const val Yn = 1.0f
    private const val Zn = 0.825188f
    private const val delta = 24.0f / 116.0f

    override fun toRGB(colorValue: FloatArray): FloatArray {
        return SRGBColorSpace.fromCIEXYZ(toCIEXYZ(colorValue))
    }

    override fun fromRGB(rgbValue: FloatArray): FloatArray {
        return fromCIEXYZ(SRGBColorSpace.toCIEXYZ(rgbValue))
    }

    private fun fTo(t: Float): Float = if (t > delta) t.pow(3.0f) else (t - (16.0f / 116.0f)) / 7.787f

    override fun toCIEXYZ(colorValue: FloatArray): FloatArray {
        val tY = (colorValue[0] + 16.0f) / 116.0f
        val tX = (colorValue[1] / 500.0f) + tY
        val tZ = tY - (colorValue[2] / 200.0f)

        val x = Xn * fTo(tX)
        val y = Yn * fTo(tY)
        val z = Zn * fTo(tZ)

        return floatArrayOf(x, y, z)
    }

    private fun fFrom(t: Float): Float = if (t > delta.pow(3.0f)) cbrt(t) else (7.787f * t) + (16.0f / 116.0f)

    override fun fromCIEXYZ(colorValue: FloatArray): FloatArray {
        val x = colorValue[0] / Xn
        val y = colorValue[1] / Yn
        val z = colorValue[2] / Zn

        val l = (116.0f * fFrom(y)) - 16.0f
        val a = 500.0f * (fFrom(x) - fFrom(y))
        val b = 200.0f * (fFrom(y) - fFrom(z))

        return floatArrayOf(l, a, b)
    }

    private fun readResolve(): Any = CIELabColorSpace
}
