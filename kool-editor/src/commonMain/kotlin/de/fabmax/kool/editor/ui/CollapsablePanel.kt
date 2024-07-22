package de.fabmax.kool.editor.ui

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color

fun UiScope.collapsablePanel(
    title: String,
    imageIcon: IconProvider? = null,
    headerContent: (RowScope.() -> Unit)? = null,
    titleWidth: Dimension = Grow.Std,
    startExpanded: Boolean = true,
    onCollapseChanged: ((Boolean) -> Unit)? = null,
    block: ColumnScope.() -> Any?
) = Column(
    Grow.Std,
    scopeName = title
) {
    var isExpanded by remember(startExpanded)
    var isHovered by remember(false)

    Row(width = Grow.Std, height = sizes.lineHeightLarge) {
        modifier
            .backgroundColor(if (isHovered) colors.hoverBg else null)
            .onEnter { isHovered = true }
            .onExit { isHovered = false }
            .onClick {
                isExpanded = !isExpanded
                onCollapseChanged?.invoke(isExpanded)
            }

        Arrow (if (isExpanded) 90f else 0f, isHoverable = false) {
            modifier
                .margin(start = sizes.gap, end = if (imageIcon == null) sizes.gap else sizes.smallGap)
                .alignY(AlignmentY.Center)
        }
        imageIcon?.let {
            Image {
                modifier
                    .margin(end = sizes.smallGap)
                    .alignY(AlignmentY.Center)
                    .iconImage(it, UiColors.titleText)
            }
        }
        Text(title) {
            modifier
                .width(titleWidth)
                .alignY(AlignmentY.Center)
        }
        headerContent?.invoke(this)
    }
    if (isExpanded) {
        Column(width = Grow.Std) {
            modifier
                .padding(bottom = sizes.smallGap)
                .backgroundColor(colors.backgroundVariant)
            block()
        }
    }
}

inline fun UiScope.collapsablePanelLvl2(
    title: String,
    noinline headerContent: (RowScope.() -> Unit)? = null,
    titleWidth: Dimension = Grow.Std,
    startExpanded: Boolean = true,
    indicatorColor: Color = colors.secondaryVariant,
    isAlwaysShowIndicator: Boolean = false,
    noinline onCollapseChanged: ((Boolean) -> Unit)? = null,
    block: ColumnScope.() -> Any?
) = Column(
    Grow.Std,
    scopeName = title
) {
    var isExpanded by remember(startExpanded)
    var isHovered by remember(false)

    val bgColor = colors.hoverBg.withAlpha(0.15f)
    val bgColorHovered = colors.hoverBg

    Row(width = Grow.Std, height = sizes.lineHeightMedium) {
        modifier
            .backgroundColor(if (isHovered) bgColorHovered else bgColor)
            .onEnter { isHovered = true }
            .onExit { isHovered = false }
            .onClick {
                isExpanded = !isExpanded
                onCollapseChanged?.invoke(isExpanded)
            }

        Box(height = Grow.Std) {
            if (isExpanded || isAlwaysShowIndicator) {
                Box(width = sizes.smallGap, height = Grow.Std) {
                    modifier.backgroundColor(indicatorColor)
                }
            }
            Arrow (if (isExpanded) 90f else 0f, isHoverable = false) {
                modifier
                    .colors(colors.secondary)
                    .margin(start = sizes.editorPanelMarginStart, end = sizes.gap)
                    .alignY(AlignmentY.Center)
            }
        }

        Text(title) {
            modifier
                .width(titleWidth)
                .alignY(AlignmentY.Center)
        }
        headerContent?.invoke(this)
    }
    if (isExpanded) {
        Box(width = Grow.Std) {
            Box(width = sizes.smallGap * 0.5f, height = Grow.Std) {
                modifier.backgroundColor(indicatorColor)
            }
            Column(width = Grow.Std) {
                block()
            }
        }
    }
}