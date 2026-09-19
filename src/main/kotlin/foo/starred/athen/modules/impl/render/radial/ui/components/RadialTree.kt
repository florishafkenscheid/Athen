package foo.starred.athen.modules.impl.render.radial.ui.components

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.athen.api.storage.ResourceAPI
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.editor.RadialEditor
import foo.starred.cascade.constraints.impl.data.PositionAlignment
import foo.starred.cascade.constraints.impl.position.AlignPositionConstraint
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.events.impl.MouseEvent
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.ImagePrimitive.Companion.image
import foo.starred.cascade.primitives.impl.ItemPrimitive.Companion.item
import foo.starred.cascade.primitives.impl.RoundedRectanglePrimitive.Companion.roundedRectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive
import foo.starred.cascade.primitives.impl.TextPrimitive.Companion.text
import foo.starred.cascade.wrappers.text.impl.CascadeTextWrapper
import foo.starred.kontext.api.format.extension.component.literal

class RadialTree(private val side: ScrollablePrimitive) {
    fun fn() {
        side.children.clear()
        val y = slots(RadialEditor.working, emptyList(), 0, 4)

        roundedRectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(95f, 0f), FixedSizeConstraint(0, 22))
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
            offset = CascadeGeometricOffset(0f, y)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.argb)
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            on<MouseEvent.Press> {
                cancel()
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                RadialEditor.addRoot()
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Mantle.argb)
            }

            attach(side)
            adopt(text {
                wrapper = CascadeTextWrapper
                text = "+ Slot".literal()
                textSize = 9.5f
                color = CascadeGeometricColor(MochaColorScheme.Green.argb)
                position = CenterPositionConstraint()
            })
        }

        container {
            size = FixedSizeConstraint(10, 4)
            position = AlignPositionConstraint(PositionAlignment.CENTER, PositionAlignment.START)
            offset = CascadeGeometricOffset(0f, y + 24)
            interact = false
            attach(side)
        }
    }

    private fun slots(slots: List<RadialSlot>, parentPath: List<Int>, depth: Int, startY: Int): Int {
        var y = startY

        for ((index, slot) in slots.withIndex()) {
            val path = parentPath + index
            val selected = path == RadialEditor.selectedPath
            val expanded = slot !in RadialEditor.collapsed
            val width = maxOf(70f, 95f - depth * 5.5f)
            val height = if (depth == 0) 22 else 20
            val xOffset = if (depth == 0) 0 else -4
            val alignment = if (depth == 0) PositionAlignment.CENTER else PositionAlignment.END

            roundedRectangle {
                size = MixedSizeConstraint(PercentSizeConstraint(width, 0f), FixedSizeConstraint(0, height))
                position = AlignPositionConstraint(alignment, PositionAlignment.START)
                offset = CascadeGeometricOffset(xOffset, y)
                color = CascadeGeometricColor(if (selected) MochaColorScheme.Surface1.argb else MochaColorScheme.Mantle.argb)
                radius = CascadeGeometricRadius(4f)

                effect(OutlineEffect {
                    color = CascadeGeometricColor(if (selected) MochaColorScheme.Lavender.argb else MochaColorScheme.Surface0.argb)
                })

                attach(side)

                on<MouseEvent.Press> {
                    cancel()
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                    RadialEditor.commit()
                    RadialEditor.reload(path)
                }

                on<MouseEvent.Move.Enter> {
                    if (path == RadialEditor.selectedPath) return@on
                    color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
                }

                on<MouseEvent.Move.Exit> {
                    color = CascadeGeometricColor(if (path == RadialEditor.selectedPath) MochaColorScheme.Surface1.argb else MochaColorScheme.Mantle.argb)
                }

                if (slot.sub.isNotEmpty()) {
                    adopt(roundedRectangle {
                        size = FixedSizeConstraint(10, 12)
                        position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                        offset = CascadeGeometricOffset(2f, 0f)
                        color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
                        radius = CascadeGeometricRadius(2.5f)

                        on<MouseEvent.Press> {
                            cancel()
                            if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                            RadialEditor.commit()

                            if (!RadialEditor.collapsed.remove(slot)) {
                                RadialEditor.collapsed.add(slot)
                                if (RadialEditor.selectedPath.size > path.size && RadialEditor.selectedPath.take(path.size) == path) {
                                    return@on RadialEditor.reload(path)
                                }
                            }

                            this@RadialTree.fn()
                        }

                        adopt(image {
                            location = ResourceAPI.identify("textures/gui/chevron.png")
                            rotation = if (expanded) 180f else 90f
                            size = FixedSizeConstraint(5f, 5f)
                            color = CascadeGeometricColor(MochaColorScheme.Subtext0.argb)
                            position = CenterPositionConstraint()
                            interact = false
                        })
                    })
                }

                adopt(item {
                    item = slot.item
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(14f, 0f)
                })

                adopt(text {
                    wrapper = CascadeTextWrapper
                    text = CascadeFonts.sans.truncate(slot.name.ifBlank { "..." }, if (depth == 0) 9.5f else 9f, if (depth == 0) 46f else 34f).literal()
                    textSize = if (depth == 0) 9.5f else 9f
                    color = CascadeGeometricColor(if (selected) MochaColorScheme.Text.argb else MochaColorScheme.Subtext0.argb)
                    position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                    offset = CascadeGeometricOffset(32f, 0f)
                })

                if (selected) adopt(actions(path))
            }

            y += height + 2

            if (expanded && slot.sub.isNotEmpty()) {
                y = slots(slot.sub, path, depth + 1, y)
            }

            if (expanded && (slot.sub.isNotEmpty() || selected) && RadialEditor.canAddChild(path)) {
                y = addChild(path, depth + 1, y)
            }
        }

        return y
    }

    private fun actions(path: List<Int>) = container {
        size = FixedSizeConstraint(40, 14)
        position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.CENTER)
        offset = CascadeGeometricOffset(-4f, 0f)

        adopt(actionButton(PositionAlignment.START, 0f, null, MochaColorScheme.Text.argb) {
            RadialEditor.move(path, -1)
        })

        adopt(actionButton(PositionAlignment.CENTER, 180f, null, MochaColorScheme.Text.argb) {
            RadialEditor.move(path, 1)
        })

        adopt(actionButton(PositionAlignment.END, null, "×", MochaColorScheme.Red.argb) {
            RadialEditor.delete(path)
        })
    }

    private fun actionButton(
        alignment: PositionAlignment,
        rotation: Float?,
        label: String?,
        contentColor: Int,
        action: () -> Unit,
    ) = roundedRectangle {
        size = FixedSizeConstraint(12, 12)
        position = AlignPositionConstraint(alignment, PositionAlignment.CENTER)
        color = CascadeGeometricColor(MochaColorScheme.Surface1.argb)
        radius = CascadeGeometricRadius(2.5f)

        effect(OutlineEffect {
            color = CascadeGeometricColor(MochaColorScheme.Overlay0.argb)
        })

        on<MouseEvent.Press> {
            cancel()
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
            action()
        }

        if (rotation != null) {
            adopt(image {
                location = ResourceAPI.identify("textures/gui/chevron.png")
                this.rotation = rotation
                size = FixedSizeConstraint(6f, 6f)
                color = CascadeGeometricColor(contentColor)
                position = CenterPositionConstraint()
                interact = false
            })
        } else if (label != null) {
            adopt(text {
                wrapper = CascadeTextWrapper
                text = label.literal()
                textSize = 8.5f
                color = CascadeGeometricColor(contentColor)
                position = CenterPositionConstraint()
            })
        }
    }

    private fun addChild(path: List<Int>, depth: Int, y: Int): Int {
        val width = maxOf(70f, 95f - depth * 5.5f)

        roundedRectangle {
            size = MixedSizeConstraint(PercentSizeConstraint(width, 0f), FixedSizeConstraint(0, 18))
            position = AlignPositionConstraint(PositionAlignment.END, PositionAlignment.START)
            offset = CascadeGeometricOffset(-4f, y)
            color = CascadeGeometricColor(MochaColorScheme.Mantle.argb)
            radius = CascadeGeometricRadius(4f)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(side)

            on<MouseEvent.Press> {
                cancel()
                if (button != InputConstants.MOUSE_BUTTON_LEFT) return@on
                RadialEditor.addChild(path)
            }

            on<MouseEvent.Move.Enter> {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            }

            on<MouseEvent.Move.Exit> {
                color = CascadeGeometricColor(MochaColorScheme.Mantle.argb)
            }

            adopt(text {
                wrapper = CascadeTextWrapper
                text = "+ Sub".literal()
                textSize = 8.5f
                color = CascadeGeometricColor(MochaColorScheme.Green.argb)
                position = AlignPositionConstraint(PositionAlignment.START, PositionAlignment.CENTER)
                offset = CascadeGeometricOffset(6f, 0f)
            })
        }

        return y + 20
    }
}
