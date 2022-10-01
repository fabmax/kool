package de.fabmax.kool.demo

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.toString

object UiSizes {
    val hGap: Dp get() = Settings.uiSize.value.sizes.gap * 1.25f
    val vGap: Dp get() = Settings.uiSize.value.sizes.gap

    val baseElemSize: Dp get() = Settings.uiSize.value.sizes.gap * 4f
    val menuWidth: Dp get() = baseElemSize * 7f
}

fun UiScope.MenuRow(vGap: Dp = UiSizes.vGap, block: UiScope.() -> Unit) {
    Row(width = Grow.Std) {
        modifier.margin(horizontal = UiSizes.hGap, vertical = vGap)
        block()
    }
}

fun UiScope.MenuSlider(
    value: Float,
    min: Float,
    max: Float,
    txtFormat: (Float) -> String = { it.toString(2) },
    txtWidth: Dp = UiSizes.baseElemSize,
    onChangeEnd: ((Float) -> Unit)? = null,
    onChange: (Float) -> Unit
) {
    Slider(value, min, max) {
        modifier
            .width(Grow.Std)
            .alignY(AlignmentY.Center)
            .margin(horizontal = sizes.gap)
            .onChange(onChange)
        modifier.onChangeEnd = onChangeEnd
    }
    if (txtWidth.value > 0f) {
        Text(txtFormat(value)) {
            labelStyle()
            modifier.width(txtWidth).textAlignX(AlignmentX.End)
        }
    }
}

fun UiScope.MenuSlider2(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    txtFormat: (Float) -> String = { it.toString(2) },
    onChange: (Float) -> Unit
) {
    MenuRow {
        Text(label) { labelStyle(Grow.Std) }
        Text(txtFormat(value)) { labelStyle() }
    }
    MenuRow {
        Slider(value, min, max) {
            modifier
                .width(Grow.Std)
                .alignY(AlignmentY.Center)
                .onChange(onChange)
        }
    }
}

fun UiScope.LabeledSwitch(label: String, toggleState: MutableStateValue<Boolean>) {
    Text(label) {
        labelStyle(Grow.Std)
        modifier.onClick { toggleState.toggle() }
    }
    Switch(toggleState.use()) {
        modifier
            .alignY(AlignmentY.Center)
            .onToggle { toggleState.set(it) }
    }
}

fun TextScope.sectionTitleStyle() {
    modifier
        .width(Grow.Std)
        .margin(vertical = UiSizes.hGap)    // hGap is intentional, since we want a little more spacing around titles
        .textColor(colors.accent)
        .backgroundColor(colors.accentVariant.withAlpha(0.2f))
        .font(sizes.largeText)
        .textAlignX(AlignmentX.Center)
}

fun TextScope.labelStyle(width: Dimension = WrapContent) {
    modifier
        .width(width)
        .align(yAlignment = AlignmentY.Center)
}