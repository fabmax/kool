package de.fabmax.kool.demo.helloworld

import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.DemoScene
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.MdColor

class HelloUiDemo : DemoScene("Hello UI") {
    override fun Scene.setupMainScene(ctx: KoolContext) {
        setupUiScene(clearScreen = true)

        +Panel(colors = Colors.singleColorLight(MdColor.LIGHT_GREEN)) {
            modifier
                .size(400.dp, 300.dp)
                .align(AlignmentX.Center, AlignmentY.Center)
                .background(RoundRectBackground(colors.background, 16.dp))

            val clickCount = weakRememberState(0)
            Button("Click me!") {
                modifier
                    .alignX(AlignmentX.Center)
                    .margin(sizes.largeGap * 4f)
                    .padding(horizontal = sizes.largeGap, vertical = sizes.gap)
                    .font(sizes.largeText)
                    .onClick { clickCount.set(clickCount.value + 1) }
            }
            Text("Button clicked ${clickCount.use()} times") {
                modifier
                    .alignX(AlignmentX.Center)
            }
        }
    }
}