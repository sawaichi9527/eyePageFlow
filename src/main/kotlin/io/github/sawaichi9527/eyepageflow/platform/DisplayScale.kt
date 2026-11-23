package io.github.sawaichi9527.eyepageflow.platform

import java.awt.GraphicsConfiguration
import java.awt.geom.AffineTransform
import kotlin.math.abs

/**
 * 顯示縮放（Java2D uiScale）的純判斷邏輯。
 *
 * 顯示設定變更後，視窗會換到帶有新 `defaultTransform` 的 [GraphicsConfiguration]。
 * 重繪與重排由 UI 層負責；這裡只回答「比例是否真的變了」，避免每次視窗移動都重排。
 */
object DisplayScale {
    fun of(configuration: GraphicsConfiguration): Float = of(configuration.defaultTransform)

    fun of(transform: AffineTransform): Float = transform.scaleX.toFloat()

    /** `previous <= 0` 視為尚未取得比例，一律當成變更。 */
    fun changed(previous: Float, next: Float, tolerance: Float = 0.001f): Boolean =
        previous <= 0f || abs(previous - next) > tolerance
}
