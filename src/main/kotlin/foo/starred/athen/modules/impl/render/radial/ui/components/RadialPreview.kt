package foo.starred.athen.modules.impl.render.radial.ui.components

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialOverlay
import foo.starred.athen.modules.impl.render.radial.utils.RadialRenderState
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FillSizeConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.primitives.impl.ContainerPrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.RenderStatePrimitive.Companion.renderState

class RadialPreview(main: ContainerPrimitive) {
    var panel: RectanglePrimitive
        private set

    init {
        panel = rectangle {
            size = FixedSizeConstraint(320, 320)
            position = FixedPositionConstraint(586, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                cancel()

                val preview = RadialEditor.preview()
                if (preview.wheel.isEmpty()) return@on

                val centerX = self.x.toInt() + self.width.toInt() / 2
                val centerY = self.y.toInt() + self.height.toInt() / 2
                val dx = x.toFloat() - centerX
                val dy = y.toFloat() - centerY

                if (dx * dx + dy * dy < 144f) {
                    RadialEditor.commit()
                    RadialEditor.previewBack(preview)
                    return@on
                }

                val hit = hit(x.toFloat(), y.toFloat(), centerX, centerY, preview)
                RadialEditor.commit()

                when {
                    hit.outer >= 0 -> RadialEditor.selectPreviewOuter(preview, hit.outer)
                    hit.main >= 0 -> RadialEditor.selectPreviewMain(preview, hit.main)
                }
            }

            attach(main)
        }

        renderState {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(panel)

            provider = provider@{ graphics ->
                val preview = RadialEditor.preview()
                if (preview.wheel.isEmpty()) return@provider null

                val centerX = panel.x.toInt() + panel.width.toInt() / 2
                val centerY = panel.y.toInt() + panel.height.toInt() / 2
                val count = maxOf(3, preview.wheel.size)
                val hit = hit(mouseX, mouseY, centerX, centerY, preview)
                val nested = if (RadialMenu.type == 1) preview.wheel.getOrNull(preview.main)?.sub ?: emptyList() else emptyList()
                val outer = RadialEditor.outerLayout(preview)
                val highlightedOuter = if (hit.outer >= 0) hit.outer else preview.outer
                val ring = if (RadialMenu.type == 2) outer.getOrNull(highlightedOuter)?.first ?: -1 else -1
                val highlightedMain = if (RadialMenu.type == 0 && hit.main < 0) preview.main else hit.main

                RadialRenderState(
                    graphics,
                    centerX,
                    centerY,
                    count,
                    nested,
                    outer,
                    highlightedMain,
                    highlightedOuter,
                    if (RadialMenu.type == 0) -1 else preview.main,
                    ring,
                )
            }

            ascend = true
        }

        RadialOverlay(panel).apply {
            size = FillSizeConstraint()
            position = FixedPositionConstraint(0, 0)
            interact = false
            attach(panel)
        }
    }

    private fun hit(
        mouseX: Float,
        mouseY: Float,
        centerX: Int,
        centerY: Int,
        preview: RadialEditor.PreviewState,
    ): Hit {
        val dx = mouseX - centerX
        val dy = mouseY - centerY
        if (dx * dx + dy * dy < 225f) return Hit()

        val count = maxOf(3, preview.wheel.size)
        var main = -1
        var outer = -1

        if (preview.main in preview.wheel.indices) {
            outer = when (RadialMenu.type) {
                1 -> RadialRenderState.hitNested(
                    mouseX,
                    mouseY,
                    centerX,
                    centerY,
                    count,
                    RadialMenu.radius2,
                    preview.main,
                    preview.wheel[preview.main].sub.size,
                    false,
                    RadialMenu.thickness,
                )
                2 -> RadialRenderState.hitRing(
                    mouseX,
                    mouseY,
                    centerX,
                    centerY,
                    count,
                    RadialMenu.radius2,
                    RadialEditor.outerLayout(preview).map { it.first },
                    false,
                    RadialMenu.thickness,
                )
                else -> -1
            }
        }

        if (outer < 0) {
            main = RadialRenderState.hit(mouseX, mouseY, centerX, centerY, count, RadialMenu.radius1, RadialMenu.radius2)
        }

        return Hit(main, outer)
    }

    private data class Hit(val main: Int = -1, val outer: Int = -1)
}
