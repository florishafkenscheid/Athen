@file:Suppress("ObjectPrivatePropertyName", "Unchecked_cast")

package foo.starred.athen.modules.impl.general

import com.google.gson.Gson
import foo.starred.athen.annotations.impl.AthenModuleScope
import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.enums.MessagePrefixType
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.api.network.http.AthenAPI
import foo.starred.athen.api.network.http.WebAPI.request
import foo.starred.athen.config.ConfigManager.update
import foo.starred.athen.config.dsl.impl.category.ConfigCategory
import foo.starred.athen.config.theme.impl.catppuccin.MochaColorScheme
import foo.starred.athen.events.GuiEvent
import foo.starred.athen.modules.Module
import foo.starred.athen.utils.command
import foo.starred.kontext.abstraction.chat.color.MinecraftColors
import foo.starred.kontext.abstraction.client.impl.MinecraftClient
import foo.starred.kontext.api.EMPTY_COMPONENT
import foo.starred.kontext.api.format.extension.roman.parseRomanNumeral
import foo.starred.kontext.api.format.extension.string.stripped
import foo.starred.kontext.api.item.data.enchant.SkyBlockItemEnchant
import foo.starred.kontext.api.item.extension.get
import foo.starred.kontext.api.item.resolver.impl.SkyBlockEnchantKontextItemDataResolver
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.kontext.util.compress
import foo.starred.kontext.util.decompress
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.network.chat.FormattedText

@LoadableAthenModule
@AthenModuleScope(skyblock = true)
object ColoredEnchants : Module(
    "Colored enchants",
    "Custom colors for enchants!",
    ConfigCategory.GENERAL
) {
    private val l = listOf("Bold", "Italic", "Underline", "Strike-through")

    private val replaceRoman by config.switch("Replace roman", true)

    private val ultimate by config.group("Ultimate enchants")
    private val `ultimate$color` by ultimate.colorPicker("Ultimate color", MochaColorScheme.Mauve.argb)
    private val `ultimate$style` by ultimate.multiSelector("Ultimate style", l, listOf(0))

    private val max by config.group("Maxed enchants")
    private val `max$color` by max.colorPicker("Max color", MinecraftColors.RED.color)
    private val `max$style` by max.multiSelector("Max style", l)

    private val high by config.group("High-level enchants")
    private val `high$color` by high.colorPicker("High color", MinecraftColors.RED.color)
    private val `high$style` by high.multiSelector("High style", l)

    private val normal by config.group("Normal-level enchants")
    private val `normal$color` by normal.colorPicker("Normal color", MinecraftColors.BLUE.color)
    private val `normal$style` by normal.multiSelector("Normal style", l)

    private val bad by config.group("Bad-level enchants")
    private val `bad$color` by bad.colorPicker("Bad color", 0xFFAAAAAA)
    private val `bad$style` by bad.multiSelector("Bad style", l)

    private val List<Int>.bold: Boolean
        get() = 0 in this

    private val List<Int>.italic: Boolean
        get() = 1 in this

    private val List<Int>.underline: Boolean
        get() = 2 in this

    private val List<Int>.strike: Boolean
        get() = 3 in this

    private val enchantRegex = Regex("(?<enchant>[A-Za-z][A-Za-z' -]*) (?<level>[IVXLCDM]+)")
    private val enchants = mutableMapOf<String, Enchant>()

    init {
        AthenAPI.data("enchants.json").request {
            success<Map<String, Any>> { str ->
                for ((a, b) in str) {
                    val c = b as? Map<String, Map<String, Any?>> ?: continue
                    for ((d, e) in c) {
                        enchants[d] = Enchant(
                            e["loreName"] as String,
                            Enchant.category(a),
                            (e["maxLevel"] as Double).toInt(),
                            (e["goodLevel"] as Double).toInt()
                        )
                    }
                }
            }
        }

        command {
            "export" / "enchants" {
                val data = mapOf(
                    $$"ultimate$color" to `ultimate$color`,
                    $$"ultimate$style" to `ultimate$style`,
                    $$"max$color" to `max$color`,
                    $$"max$style" to `max$style`,
                    $$"high$color" to `high$color`,
                    $$"high$style" to `high$style`,
                    $$"normal$color" to `normal$color`,
                    $$"normal$style" to `normal$style`,
                    $$"bad$color" to `bad$color`,
                    $$"bad$style" to `bad$style`,
                    "replaceRoman" to replaceRoman
                )

                MinecraftClient.clipboard = Gson().toJson(data).compress()
                "Enchant config exported to clipboard!".mod()
            }

            "import" / "enchants" {
                val clipboard = MinecraftClient.clipboard
                if (clipboard.isEmpty()) return@invoke "No data found in clipboard!".mod(MessagePrefixType.ERROR)

                val map = Gson().fromJson<Map<String, Any>>(clipboard.decompress(), Map::class.java)

                for ((k, v) in map) {
                    when (k) {
                        $$"ultimate$color", $$"max$color", $$"high$color", $$"normal$color", $$"bad$color" -> update("$_key.$k", (v as Double).toInt())
                        $$"ultimate$style", $$"max$style", $$"high$style", $$"normal$style", $$"bad$style" -> update("$_key.$k", (v as List<Double>).map { it.toInt() })
                        "replaceRoman" -> update(k, v as Boolean)
                    }
                }

                "Enchant config imported successfully!".mod()
            }
        }
        on<GuiEvent.Tooltip.Update> (100) {
            if (enchants.isEmpty()) return@on
            if (item.get<SkyBlockEnchantKontextItemDataResolver, List<SkyBlockItemEnchant>>()?.isEmpty() != false) return@on

            color(tooltip)
        }
    }

    @JvmStatic
    fun colorChatHover(text: FormattedText): FormattedText {
        if (!enabled || enchants.isEmpty()) return text
        val component = text as? Component ?: return text
        val tooltip = lines(component)

        return if (color(tooltip)) ComponentUtils.formatList(tooltip, Component.literal("\n")) else text
    }

    private fun lines(component: Component): MutableList<Component> {
        val lines = mutableListOf(EMPTY_COMPONENT.copy())
        for (part in component.toFlatList()) {
            for ((index, text) in part.string.split('\n').withIndex()) {
                if (index > 0) lines.add(EMPTY_COMPONENT.copy())
                if (text.isNotEmpty()) lines.last().append(Component.literal(text).withStyle(part.style))
            }
        }

        return lines.toMutableList<Component>()
    }

    private fun color(tooltip: MutableList<Component>): Boolean {
        var found = false
        for (index in tooltip.indices) {
            val line = tooltip[index]
            val plain = line.stripped()

            if (found && plain.isEmpty()) break
            if ("◆" in plain) continue

            val parts = line.toFlatList()
            if (parts.firstOrNull()?.style?.color?.value == null) continue

            val result = EMPTY_COMPONENT.copy()
            for (part in parts) {
                val text = part.string
                val matches = enchantRegex.findAll(text).toList()

                fun keep(value: String) {
                    result.append(Component.literal(value).withStyle(part.style))
                }

                if (matches.isEmpty()) {
                    result.append(part)
                    continue
                }

                var cursor = 0
                for (match in matches) {
                    val start = match.range.first
                    val end = match.range.last + 1

                    if (start > cursor) {
                        keep(text.substring(cursor, start))
                    }

                    val key = match.groups["enchant"]?.value?.lowercase() ?: continue
                    val roman = match.groups["level"]?.value ?: continue
                    val level = roman.parseRomanNumeral() ?: continue

                    val enchant = enchants[key]
                    if (enchant == null) {
                        keep(text.substring(start, end))
                        cursor = end
                        continue
                    }

                    val comma = if (end < text.length && text[end] == ',') "," else ""

                    result.append("${enchant.style(level)}${enchant.name} ${if (replaceRoman) level else roman}$comma".parse())
                    cursor = end + comma.length
                    found = true
                }

                if (cursor < text.length) {
                    keep(text.substring(cursor))
                }
            }

            tooltip[index] = result
        }

        return found
    }

    private data class Enchant(
        val name: String,
        val category: Category,
        val max: Int,
        val good: Int
    ) {
        enum class Category {
            Stacking,
            Ultimate,
            Normal
        }

        fun style(int: Int): String {
            val (s, c) = when {
                category == Category.Ultimate -> `ultimate$style` to `ultimate$color`
                int >= max -> `max$style` to `max$color`
                int > good -> `high$style` to `high$color`
                int == good -> `normal$style` to `normal$color`
                else -> `bad$style` to `bad$color`
            }

            return buildString {
                if (s.bold) append("<bold>")
                if (s.italic) append("<italic>")
                if (s.underline) append("<underline>")
                if (s.strike) append("<strikethrough>")
                append("<$c>")
            }
        }

        companion object {
            fun category(str: String): Category = when (str.uppercase()) {
                "STACKING" -> Category.Stacking
                "ULTIMATE" -> Category.Ultimate
                else -> Category.Normal
            }
        }
    }
}
