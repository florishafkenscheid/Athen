@file:Suppress("ObjectPrivatePropertyName", "Unused")

package foo.starred.athen.modules.impl.render.radial.ui.editor

import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.modules.impl.render.radial.RadialMenu
import foo.starred.athen.modules.impl.render.radial.actions.IAction
import foo.starred.athen.modules.impl.render.radial.data.RadialSlot
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialForm
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialHeader
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialPreview
import foo.starred.athen.modules.impl.render.radial.ui.components.RadialTree
import foo.starred.cascade.constraints.impl.position.CenterPositionConstraint
import foo.starred.cascade.constraints.impl.position.FixedPositionConstraint
import foo.starred.cascade.constraints.impl.size.FixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.MixedSizeConstraint
import foo.starred.cascade.constraints.impl.size.PercentSizeConstraint
import foo.starred.cascade.effects.impl.OutlineEffect
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricResolution
import foo.starred.cascade.primitives.impl.ContainerPrimitive.Companion.container
import foo.starred.cascade.primitives.impl.RectanglePrimitive.Companion.rectangle
import foo.starred.cascade.primitives.impl.ScrollablePrimitive.Companion.scrollable
import foo.starred.cascade.screen.CascadeScreen

object RadialEditor : CascadeScreen("Radial Menu Editor [Athen]", CascadeGeometricResolution.FHD.of(2f)) {
    private var head: RadialHeader
    private var tree: RadialTree
    private var form: RadialForm
    private var view: RadialPreview
    val working = mutableListOf<RadialSlot>()
    val collapsed = mutableSetOf<RadialSlot>()

    var selectedPath: List<Int> = emptyList()
        private set

    var editing = false
    var type = 0

    val slot: RadialSlot?
        get() = slot(selectedPath)

    val names: List<String>
        get() {
            return RadialMenu.configs.keys.toList()
        }

    init {
        val main = container {
            size = FixedSizeConstraint(906, 320)
            position = CenterPositionConstraint()
            attach(scene)
        }

        val side = rectangle {
            size = FixedSizeConstraint(130, 320)
            position = FixedPositionConstraint(0, 0)
            color = CascadeGeometricColor(MochaColorScheme.Base.argb)

            effect(OutlineEffect {
                color = CascadeGeometricColor(MochaColorScheme.Surface0.argb)
            })

            attach(main)
        }

        head = RadialHeader(side)

        tree = RadialTree(scrollable {
            size = MixedSizeConstraint(PercentSizeConstraint(100f, 0f), FixedSizeConstraint(0, 298))
            position = FixedPositionConstraint(0, 20)
            attach(side)
        })

        form = RadialForm(container {
            size = FixedSizeConstraint(444, 320)
            position = FixedPositionConstraint(136, 0)
            attach(main)
        })

        view = RadialPreview(main)
    }

    override fun init() {
        super.init()

        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        reload(listOf(0))
    }

    override fun removed() {
        save()
        super.removed()
    }

    fun reload(path: List<Int> = selectedPath) {
        selectedPath = normalize(path)
        unfocus()

        val s = slot ?: run {
            form.name.value = ""
            form.item.value = ""
            form.value.value = ""
            form.texture.value = ""
            head.fn()
            tree.fn()
            form.fn()
            return
        }

        form.name.value = s.name
        form.item.value = s.itemId
        type = s.action.id
        form.value.value = s.action.serializable
        form.texture.value = s.text ?: ""

        head.fn()
        tree.fn()
        form.fn()
    }

    fun slot(path: List<Int>): RadialSlot? {
        var current: List<RadialSlot> = working
        var result: RadialSlot? = null

        for (index in path) {
            result = current.getOrNull(index) ?: return null
            current = result.sub
        }

        return result
    }

    fun children(path: List<Int>): List<RadialSlot> {
        return if (path.isEmpty()) working else slot(path)?.sub ?: emptyList()
    }

    fun canAddChild(path: List<Int>): Boolean {
        val parent = slot(path) ?: return false
        val max = when (RadialMenu.type) {
            1 -> if (path.size == 1) 5 else 0
            2 -> children(path.dropLast(1)).size
            else -> Int.MAX_VALUE
        }

        return parent.sub.size < max
    }

    fun addChild(path: List<Int>) {
        commit()
        val parent = slot(path) ?: return
        if (!canAddChild(path)) return

        parent.sub += RadialSlot("New Sub")
        collapsed.remove(parent)
        reload(path + parent.sub.lastIndex)
    }

    fun addRoot() {
        commit()
        working.add(RadialSlot("New Slot"))
        reload(listOf(working.lastIndex))
    }

    fun move(path: List<Int>, offset: Int) {
        commit()
        val index = path.lastOrNull() ?: return
        val parentPath = path.dropLast(1)
        val siblings = children(parentPath).toMutableList()
        val target = index + offset
        if (index !in siblings.indices || target !in siblings.indices) return

        val slot = siblings[index]
        siblings[index] = siblings[target]
        siblings[target] = slot
        replaceChildren(parentPath, siblings)
        reload(parentPath + target)
    }

    fun delete(path: List<Int>) {
        commit()
        val index = path.lastOrNull() ?: return
        val parentPath = path.dropLast(1)
        val siblings = children(parentPath).toMutableList()
        if (index !in siblings.indices) return

        collapsed.remove(siblings.removeAt(index))
        replaceChildren(parentPath, siblings)
        reload(if (siblings.isEmpty()) parentPath else parentPath + index.coerceAtMost(siblings.lastIndex))
    }

    fun preview(): PreviewState {
        val path = selectedPath
        if (path.isEmpty()) return PreviewState(emptyList(), working, -1, -1)

        return when (RadialMenu.type) {
            0 -> PreviewState(path.dropLast(1), children(path.dropLast(1)), path.last(), -1)
            2 -> {
                if (path.size % 2 == 0) {
                    val wheelPath = path.dropLast(2)
                    PreviewState(wheelPath, children(wheelPath), path[path.lastIndex - 1], path.last())
                } else {
                    val wheelPath = path.dropLast(1)
                    PreviewState(wheelPath, children(wheelPath), path.last(), -1)
                }
            }
            else -> PreviewState(emptyList(), working, path.first(), path.getOrNull(1) ?: -1)
        }
    }

    fun outerLayout(preview: PreviewState = preview()): List<Pair<Int, RadialSlot>> {
        if (RadialMenu.type != 2) return emptyList()
        val sub = preview.wheel.getOrNull(preview.main)?.sub ?: return emptyList()
        val count = sub.size
        val sectors = maxOf(3, preview.wheel.size)

        return List(count) { index ->
            ((preview.main - count / 2 + index + sectors * 2) % sectors) to sub[index]
        }
    }

    fun selectPreviewMain(preview: PreviewState, index: Int) {
        val path = preview.wheelPath + index
        val selected = slot(path)
        reload(if (RadialMenu.type == 0 && selected?.sub?.isNotEmpty() == true) path + 0 else path)
    }

    fun selectPreviewOuter(preview: PreviewState, index: Int) {
        val path = preview.wheelPath + preview.main + index
        val selected = slot(path)
        reload(if (RadialMenu.type != 1 && selected?.sub?.isNotEmpty() == true) path + 0 else path)
    }

    fun previewBack(preview: PreviewState = preview()) {
        val path = when {
            preview.outer >= 0 -> selectedPath.dropLast(1)
            preview.wheelPath.isNotEmpty() -> preview.wheelPath
            else -> return
        }
        reload(path)
    }

    fun commit() {
        val s = slot ?: return
        s.name = form.name.value
        s.itemId = form.item.value
        s.text = form.texture.value.ifBlank { null }
        s.action = IAction.create(type, form.value.value)
    }

    fun sync() {
        commit()
        tree.fn()
    }

    fun unfocus() {
        scene.focused = null
        editing = false
    }

    fun rename() {
        val n = head.title.value.trim()
        if (n.isBlank() || n == RadialMenu.active || n in names) {
            editing = false
            head.fn()
            return
        }

        RadialMenu.rename(RadialMenu.active, n)
        editing = false
        head.fn()
    }

    fun save() {
        commit()
        RadialMenu.slots.clear()
        RadialMenu.slots.addAll(working)
        RadialMenu.save()
    }

    fun switch(name: String) {
        RadialMenu.load(name)
        working.clear()
        working.addAll(RadialMenu.slots)
        collapsed.clear()
        reload(listOf(0))
    }

    private fun normalize(path: List<Int>): List<Int> {
        if (working.isEmpty()) return emptyList()

        val normalized = mutableListOf<Int>()
        var current: List<RadialSlot> = working
        val requested = path.ifEmpty { listOf(0) }

        for (index in requested) {
            if (current.isEmpty()) break
            val safe = index.coerceIn(0, current.lastIndex)
            normalized.add(safe)
            current = current[safe].sub
        }

        return normalized
    }

    private fun replaceChildren(path: List<Int>, children: List<RadialSlot>) {
        if (path.isEmpty()) {
            working.clear()
            working.addAll(children)
            return
        }

        slot(path)?.sub = children
    }

    data class PreviewState(
        val wheelPath: List<Int>,
        val wheel: List<RadialSlot>,
        val main: Int,
        val outer: Int,
    )
}
