package de.fabmax.kool.modules.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.clamp
import de.fabmax.kool.scene.geometry.TextProps
import de.fabmax.kool.scene.ui.Font
import de.fabmax.kool.scene.ui.FontProps
import de.fabmax.kool.util.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

interface ComboBoxScope : UiScope {
    override val modifier: ComboBoxModifier
}

open class ComboBoxModifier(surface: UiSurface) : UiModifier(surface) {
    var font: FontProps by property { it.sizes.normalText }
    var items: List<String> by property(emptyList())
    var selectedIndex: Int by property(0)

    var textColor: Color by property { it.colors.onBackground }
    var textBackgroundColor: Color by property { it.colors.accentVariant.withAlpha(0.3f) }
    var textBackgroundHoverColor: Color by property { it.colors.accentVariant.withAlpha(0.45f) }
    var expanderColor: Color by property { it.colors.accentVariant }
    var expanderHoverColor: Color by property { it.colors.accent }
    var expanderArrowColor: Color by property { it.colors.onAccent }

    var onItemSelected: ((Int) -> Unit)? by property(null)
}

fun <T: ComboBoxModifier> T.font(font: FontProps): T { this.font = font; return this }
fun <T: ComboBoxModifier> T.textColor(color: Color): T { textColor = color; return this }
fun <T: ComboBoxModifier> T.items(items: List<String>): T { this.items = items; return this }
fun <T: ComboBoxModifier> T.selectedIndex(index: Int): T { this.selectedIndex = index; return this }
fun <T: ComboBoxModifier> T.onItemSelected(block: ((Int) -> Unit)?): T { this.onItemSelected = block; return this }

inline fun UiScope.ComboBox(block: ComboBoxScope.() -> Unit): ComboBoxScope {
    val comboBox = uiNode.createChild(ComboBoxNode::class, ComboBoxNode.factory)
    comboBox.modifier
        .padding(horizontal = sizes.gap, vertical = sizes.smallGap * 0.5f)
        .hoverListener(comboBox)
        .onClick(comboBox)
        .onWheelY {
            if (it.pointer.deltaScrollY > 0 && comboBox.modifier.selectedIndex > 0) {
                comboBox.modifier.onItemSelected?.invoke(comboBox.modifier.selectedIndex - 1)
            } else if (it.pointer.deltaScrollY < 0 && comboBox.modifier.selectedIndex < comboBox.modifier.items.lastIndex) {
                comboBox.modifier.onItemSelected?.invoke(comboBox.modifier.selectedIndex + 1)
            }
        }
    comboBox.block()

    if (comboBox.isExpanded.use()) {
        Popup(comboBox.leftPx, comboBox.bottomPx) {
            modifier.border(RectBorder(colors.accentVariant.withAlpha(0.5f), 1.dp))

            comboBox.modifier.items.forEachIndexed { i, item ->
                val hovered = comboBox.hoveredItem.use()
                Text(item) {
                    modifier
                        .width(Grow.Std)
                        .padding(horizontal = sizes.gap, vertical = sizes.smallGap * 0.5f)
                        .onEnter {
                            comboBox.hoveredItem.set(i)
                        }
                        .onExit {
                            comboBox.hoveredItem.set(-1)
                        }
                        .onClick {
                            comboBox.modifier.onItemSelected?.invoke(i)
                            comboBox.isExpanded.set(false)
                            comboBox.hoveredItem.set(-1)
                        }
                    if (i == hovered) {
                        modifier
                            .backgroundColor(comboBox.modifier.expanderHoverColor)
                            .textColor(comboBox.modifier.expanderArrowColor)
                    }
                }
            }
        }
    }

    return comboBox
}

open class ComboBoxNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), ComboBoxScope, Clickable, Hoverable {
    override val modifier = ComboBoxModifier(surface)

    private val textProps = TextProps(Font.DEFAULT_FONT)
    private val textCache = CachedText(this)

    private var isHovered = mutableStateOf(false)

    var isExpanded = mutableStateOf(false)
    var hoveredItem = mutableStateOf(-1)

    private val selectedText: String
        get() {
            return if (modifier.items.isEmpty()) {
                ""
            } else {
                modifier.items[modifier.selectedIndex.clamp(0, modifier.items.lastIndex)]
            }
        }

    override fun measureContentSize(ctx: KoolContext) {
        val font = surface.getFont(modifier.font, ctx)
        val textMetrics = textCache.getTextMetrics(selectedText, font, ctx)
        val modWidth = modifier.width
        val modHeight = modifier.height
        val measuredWidth = if (modWidth is Dp) {
            modWidth.px
        } else {
            textMetrics.width + paddingStartPx + paddingEndPx + sizes.largeGap.px * 1.5f
        }
        val measuredHeight = if (modHeight is Dp) modHeight.px else textMetrics.height + paddingTopPx + paddingBottomPx
        setContentSize(measuredWidth, measuredHeight)
    }

    override fun render(ctx: KoolContext) {
        val customClip = MutableVec4f()
        val expanderWidth = sizes.largeGap.px * 1.5f
        val expanderBorder = round(rightPx - expanderWidth)

        var textBgColor = modifier.textBackgroundColor
        var arrowBgColor = modifier.expanderColor
        if (isHovered.use()) {
            textBgColor = modifier.textBackgroundHoverColor
            arrowBgColor = modifier.expanderHoverColor
        }

        if (modifier.background == null) {
            // only set default button background if no custom one was configured
            modifier.background(UiRenderer {
                val cornerRadius = sizes.smallGap.px
                val draw = getUiPrimitives(UiSurface.LAYER_BACKGROUND)
                draw.localRoundRect(0f, 0f, widthPx, heightPx, cornerRadius, textBgColor)

                customClip.set(clipBoundsPx)
                customClip.x = max(clipLeftPx, expanderBorder)
                draw.roundRect(
                    rightPx - expanderWidth - cornerRadius, topPx,
                    expanderWidth + cornerRadius, heightPx,
                    cornerRadius, arrowBgColor, customClip)
            })
        }

        super.render(ctx)

        textProps.apply {
            font = surface.getFont(modifier.font, ctx)
            text = selectedText
            isYAxisUp = false
            val textMetrics = textCache.textMetrics
            val oriX = paddingStartPx
            val oriY = (heightPx - textMetrics.height) / 2f + textMetrics.yBaseline
            origin.set(leftPx + oriX, topPx + oriY, 0f)
        }
        customClip.set(clipBoundsPx)
        customClip.z = min(clipRightPx, expanderBorder)
        textCache.addTextGeometry(getTextBuilder(modifier.font, ctx).geometry, textProps, modifier.textColor, clip = customClip)

        getPlainBuilder().configured(modifier.expanderArrowColor) {
            val cx = widthPx - expanderWidth * 0.5f
            val cy = heightPx * 0.5f
            val d = sizes.smallGap.px * 1.25f
            val i0 = vertex { position.set(cx - d, round(cy - d * 0.5f), 0f) }
            val i1 = vertex { position.set(cx, cy + d * 0.7f, 0f) }
            val i2 = vertex { position.set(cx + d, round(cy - d * 0.5f), 0f) }
            addTriIndices(i0, i1, i2)
        }
    }

    override fun onEnter(ev: PointerEvent) {
        isHovered.set(true)
    }

    override fun onExit(ev: PointerEvent) {
        isHovered.set(false)
    }

    override fun onClick(ev: PointerEvent) {
        isExpanded.set(!isExpanded.value)
    }

    companion object {
        val factory: (UiNode, UiSurface) -> ComboBoxNode = { parent, surface -> ComboBoxNode(parent, surface) }
    }
}