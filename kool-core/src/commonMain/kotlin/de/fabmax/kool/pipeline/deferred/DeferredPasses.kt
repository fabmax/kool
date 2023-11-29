package de.fabmax.kool.pipeline.deferred

import de.fabmax.kool.pipeline.OffscreenRenderPass

class DeferredPasses(val materialPass: MaterialPass, val lightingPass: PbrLightingPass) {

    val onActivate = mutableListOf<() -> Unit>()
    val onDeactivate = mutableListOf<() -> Unit>()

    val extraPasses = mutableListOf<OffscreenRenderPass>()

    var isEnabled: Boolean
        get() = materialPass.isEnabled && lightingPass.isEnabled
        set(value) {
            materialPass.isEnabled = value
            lightingPass.isEnabled = value
            if (extraPasses.isNotEmpty()) {
                for (i in extraPasses.indices) {
                    extraPasses[i].isEnabled = value
                }
            }

            if (value) {
                for (i in onActivate.indices) {
                    onActivate[i]()
                }
            } else {
                for (i in onDeactivate.indices) {
                    onDeactivate[i]()
                }
            }
        }

    fun checkSize(viewportW: Int, viewportH: Int) {
        if (viewportW > 0 && viewportH > 0) {
            materialPass.setSize(viewportW, viewportH)
            lightingPass.setSize(viewportW, viewportH)
        }
    }
}