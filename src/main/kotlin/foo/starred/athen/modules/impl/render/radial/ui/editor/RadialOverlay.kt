package foo.starred.athen.modules.impl.render.radial.ui.editor

import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.utils.RadialRenderState
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.primitives.base.impl.IPrimitiveElement
import foo.starred.kontext.abstraction.text.measurer.MinecraftFontMeasurer
import foo.starred.kontext.abstraction.text.renderer.MinecraftFontRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor

class RadialOverlay(private val panel: IPrimitiveElement<*>) : IPrimitiveElement<RadialOverlay>() {
    override var x: Float = 0f
    override var y: Float = 0f
    override var width: Float = 0f
    override var height: Float = 0f
    override var color: CascadeGeometricColor = CascadeGeometricColor.WHITE

    override fun draw(graphics: GuiGraphicsExtractor) {
        val preview = RadialEditor.preview()
        if (preview.wheel.isEmpty()) return

        val centerX = panel.x.toInt() + panel.width.toInt() / 2
        val centerY = panel.y.toInt() + panel.height.toInt() / 2
        val count = maxOf(3, preview.wheel.size)
        val outer = RadialEditor.outerLayout(preview)

        val dx = mouseX - centerX
        val dy = mouseY - centerY
        val centerHovered = dx * dx + dy * dy < 144f

        var mainHit = -1
        var outerHit = -1

        if (!centerHovered && dx * dx + dy * dy >= 225f) {
            if (preview.main in preview.wheel.indices) {
                outerHit = when (RadialMenu.type) {
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
                        outer.map { it.first },
                        false,
                        RadialMenu.thickness,
                    )
                    else -> -1
                }
            }

            if (outerHit < 0) {
                mainHit = RadialRenderState.hit(mouseX, mouseY, centerX, centerY, count, RadialMenu.radius1, RadialMenu.radius2)
            }
        }

        for (index in preview.wheel.indices) {
            val (slotX, slotY) = RadialRenderState.anchor(centerX, centerY, count, RadialMenu.radius1, RadialMenu.radius2, index)
            graphics.item(preview.wheel[index].item, slotX - 8, slotY - 8)
        }

        if (RadialMenu.type == 1 && preview.main in preview.wheel.indices) {
            for (index in preview.wheel[preview.main].sub.indices) {
                val (slotX, slotY) = RadialRenderState.nested(centerX, centerY, count, RadialMenu.radius2, preview.main, index, RadialMenu.thickness)
                graphics.item(preview.wheel[preview.main].sub[index].item, slotX - 8, slotY - 8)
            }
        }

        if (RadialMenu.type == 2) {
            for ((position, slot) in outer) {
                val (slotX, slotY) = RadialRenderState.ring(centerX, centerY, count, RadialMenu.radius2, position, RadialMenu.thickness)
                graphics.item(slot.item, slotX - 8, slotY - 8)
            }
        }

        val canGoBack = preview.outer >= 0 || preview.wheelPath.isNotEmpty()
        val centerLabel = if (canGoBack) "←" else "✕"
        MinecraftFontRenderer.extract(
            graphics,
            centerLabel,
            centerX - MinecraftFontMeasurer.width(centerLabel) / 2,
            centerY - MinecraftFontMeasurer.height / 2,
            false,
            if (centerHovered) MochaColorScheme.Lavender.argb else MochaColorScheme.Subtext0.argb,
        )

        val label = if (centerHovered) {
            if (canGoBack) "Back" else "Exit"
        } else if (outerHit >= 0) {
            preview.wheel.getOrNull(preview.main)?.sub?.getOrNull(outerHit)?.name
        } else {
            preview.wheel.getOrNull(mainHit)?.name
        }

        if (label != null && panel.hovered) {
            val width = MinecraftFontMeasurer.width(label)
            val labelX = mouseX.toInt() + 12
            val labelY = mouseY.toInt() - 4

            graphics.rectangle(labelX - 5f, labelY - 5f, width + 10f, MinecraftFontMeasurer.height + 10f, MochaColorScheme.Base.argb)
            graphics.hollowRectangle(labelX - 5f, labelY - 5f, width + 10f, MinecraftFontMeasurer.height + 10f, 1f, MochaColorScheme.Lavender.argb, inset = false)
            MinecraftFontRenderer.extract(graphics, label, labelX, labelY, false, MochaColorScheme.Text.argb)
        }
    }
}
