package de.fabmax.kool.util.ao

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.shadermodel.*
import de.fabmax.kool.pipeline.shading.ModeledShader
import de.fabmax.kool.scene.Camera
import de.fabmax.kool.scene.Group
import de.fabmax.kool.scene.mesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.createUint8Buffer
import de.fabmax.kool.util.deferred.DeferredMrtPass
import kotlin.math.*
import kotlin.random.Random

class AmbientOcclusionPass(screenCam: Camera, val aoSetup: AoSetup, width: Int, height: Int) :
        OffscreenRenderPass2d(Group(), renderPassConfig {
            name = "AmbientOcclusionPass"
            setSize(width, height)
            clearDepthTexture()
            addColorTexture(TexFormat.RGBA)
        }) {

    var radius = 1f
    var strength = 1.25f
    var power = 1.5f
    var bias = 0.05f
    var kernelSz = 32
        set(value) {
            field = value
            setKernelSize(value)
        }

    private var aoUniforms: AoUniforms? = null
    private var aoNode: AoNode? = null

    private val noiseTex = makeNoiseTexture()

    init {
        clearColor = Color.WHITE

        (drawNode as Group).apply {
            +mesh(listOf(Attribute.POSITIONS, Attribute.TEXTURE_COORDS)) {
                generate {
                    rect {
                        size.set(1f, 1f)
                        mirrorTexCoordsY()
                    }
                }

                val model = ShaderModel("AoPass").apply {
                    val ifScreenPos: StageInterfaceNode
                    vertexStage {
                        ifScreenPos = stageInterfaceNode("ifTexCoords", attrTexCoords().output)
                        positionOutput = fullScreenQuadPositionNode(attrTexCoords().output).outQuadPos
                    }
                    fragmentStage {
                        val noiseTex = texture2dNode("noiseTex")
                        val aoUnis = addNode(AoUniforms(screenCam, true, stage))

                        val depthTex: Texture2dNode
                        val depthComponent: String
                        val origin: ShaderNodeIoVar
                        val normal: ShaderNodeIoVar

                        if (aoSetup.isDeferred) {
                            depthTex = texture2dNode("positionTex")
                            depthComponent = "z"
                            origin = splitNode(texture2dSamplerNode(depthTex, ifScreenPos.output).outColor, "xyz").output
                            normal = splitNode(texture2dSamplerNode(texture2dNode("normalTex"), ifScreenPos.output).outColor, "xyz").output

                        } else {
                            depthTex = texture2dNode("normalDepthTex")
                            depthComponent = "a"
                            val normalDepth = texture2dSamplerNode(depthTex, ifScreenPos.output).outColor
                            normal = splitNode(normalDepth, "xyz").output

                            val unProj = addNode(UnprojectPosNode(aoUnis, stage))
                            unProj.inDepth = splitNode(normalDepth, "a").output
                            unProj.inScreenPos = ifScreenPos.output
                            origin = unProj.outPosition
                        }

                        val aoNd = addNode(AoNode(aoUnis, noiseTex, depthTex, depthComponent, stage))
                        aoNd.inScreenPos = ifScreenPos.output
                        aoNd.inOrigin = origin
                        aoNd.inNormal = normal
                        colorOutput(aoNd.outColor)
                    }
                }
                shader = ModeledShader(model).apply {
                    onPipelineCreated += { _, _, _ ->
                        if (aoSetup.isForward) {
                            model.findNode<Texture2dNode>("normalDepthTex")!!.sampler.texture = aoSetup.linearDepthPass?.colorTexture
                        } else {
                            model.findNode<Texture2dNode>("positionTex")!!.sampler.texture = aoSetup.mrtPass?.positionAo
                            model.findNode<Texture2dNode>("normalTex")!!.sampler.texture = aoSetup.mrtPass?.normalRoughness
                        }
                        model.findNode<Texture2dNode>("noiseTex")!!.sampler.texture = noiseTex
                        aoUniforms = model.findNode("aoUniforms")
                        aoNode = model.findNode("aoNode")
                        setKernelSize(kernelSz)
                    }
                }
            }
        }
    }

    private fun setKernelSize(nKernels: Int) {
        val n = min(nKernels, MAX_KERNEL_SIZE)
        aoNode?.apply {
            val scales = (0 until n)
                    .map { lerp(0.1f, 1f, (it.toFloat() / n).pow(2)) }
                    .shuffled(Random(17))

            for (i in 0 until n) {
                val xi = hammersley(i, n)
                val phi = 2f * PI.toFloat() * xi.x
                val cosTheta = sqrt((1f - xi.y))
                val sinTheta = sqrt(1f - cosTheta * cosTheta)

                val k = MutableVec3f(
                        sinTheta * cos(phi),
                        sinTheta * sin(phi),
                        cosTheta
                )
                aoUniforms.uKernel.value[i] = k.norm().scale(scales[i])
            }
            aoUniforms.uKernelN.value = n
        }
    }

    private fun radicalInverse(pBits: Int): Float {
        var bits = pBits.toLong()
        bits = (bits shl 16) or (bits shr 16)
        bits = ((bits and 0x55555555) shl 1) or ((bits and 0xAAAAAAAA) shr 1)
        bits = ((bits and 0x33333333) shl 2) or ((bits and 0xCCCCCCCC) shr 2)
        bits = ((bits and 0x0F0F0F0F) shl 4) or ((bits and 0xF0F0F0F0) shr 4)
        bits = ((bits and 0x00FF00FF) shl 8) or ((bits and 0xFF00FF00) shr 8)
        return bits.toFloat() * 2.3283064365386963e-10f // / 0x100000000
    }

    private fun hammersley(i: Int, n: Int): Vec2f {
        return Vec2f(i.toFloat() / n.toFloat(), radicalInverse(i))
    }

    private fun lerp(a: Float, b: Float, f: Float): Float {
        return a + f * (b - a)
    }

    private fun makeNoiseTexture(): Texture2d {
        val buf = createUint8Buffer(4 * 16)
        val rotAngles = (0 until 16).map { PI.toFloat() * it / 8 }.shuffled()

        for (i in 0 until 16) {
            val ang = rotAngles[i]
            val x = cos(ang)
            val y = sin(ang)
            buf[i*4+0] = ((x * 0.5f + 0.5f) * 255).toInt().toByte()
            buf[i*4+1] = ((y * 0.5f + 0.5f) * 255).toInt().toByte()
            buf[i*4+2] = 0
            buf[i*4+3] = 1
        }

        val data = TextureData2d(buf, 4, 4, TexFormat.RGBA)
        val texProps = TextureProps(TexFormat.RGBA, AddressMode.REPEAT, AddressMode.REPEAT,
                minFilter = FilterMethod.NEAREST, magFilter = FilterMethod.NEAREST,
                mipMapping = false, maxAnisotropy = 1)
        return Texture2d(texProps, "ao_noise_tex") { data }
    }

    override fun dispose(ctx: KoolContext) {
        drawNode.dispose(ctx)
        noiseTex.dispose()
        super.dispose(ctx)
    }

    private inner class AoUniforms(val cam: Camera, val withInvProj: Boolean, graph: ShaderGraph) : ShaderNode("aoUniforms", graph) {
        val uKernel = Uniform3fv("uKernel", MAX_KERNEL_SIZE)
        val uKernelN = Uniform1i(32, "uKernelN")
        val uProj = UniformMat4f("uProj")
        val uInvProj = UniformMat4f("uInvProj")
        val uNoiseScale = Uniform2f("uNoiseScale")
        val uRadius = Uniform1f("uRadius")
        val uStrength = Uniform1f("uStrength")
        val uPower = Uniform1f("uPower")
        val uBias = Uniform1f("uBias")

        override fun setup(shaderGraph: ShaderGraph) {
            super.setup(shaderGraph)
            shaderGraph.descriptorSet.apply {
                uniformBuffer(name, shaderGraph.stage) {
                    +{ uKernel }
                    +{ uProj }
                    if (withInvProj) +{ uInvProj }
                    +{ uNoiseScale }
                    +{ uRadius }
                    +{ uStrength }
                    +{ uPower }
                    +{ uBias }
                    +{ uKernelN }

                    onUpdate = { _, _ ->
                        uProj.value.set(cam.proj)
                        uNoiseScale.value.set(width / 4f, height / 4f)
                        uRadius.value = radius
                        uStrength.value = strength
                        uPower.value = power
                        uBias.value = bias

                        if (withInvProj) {
                            uInvProj.value.set(cam.invProj)
                        }
                    }
                }
            }
        }
    }

    private class UnprojectPosNode(val aoUniforms: AoUniforms, graph: ShaderGraph) : ShaderNode("unprojectPos", graph) {
        var inDepth = ShaderNodeIoVar(ModelVar1fConst(1f))
        var inScreenPos = ShaderNodeIoVar(ModelVar2fConst(Vec2f.ZERO))

        val outPosition = ShaderNodeIoVar(ModelVar3f("${name}_outPos"), this)

        override fun setup(shaderGraph: ShaderGraph) {
            super.setup(shaderGraph)
            dependsOn(inDepth, inScreenPos)
        }

        override fun generateCode(generator: CodeGenerator) {
            generator.appendMain("""
                vec4 projPos = vec4(${inScreenPos.ref2f()} * 2.0 - vec2(1.0), 1.0, 1.0);
                vec4 viewPos = ${aoUniforms.uInvProj} * projPos;
                ${outPosition.declare()} = viewPos.xyz / viewPos.w;
                $outPosition *= (${inDepth.ref1f()} / $outPosition.z);
            """)
        }
    }

    private inner class AoNode(val aoUniforms: AoUniforms, val noiseTex: Texture2dNode, val depthTex: Texture2dNode, val depthComponent: String, graph: ShaderGraph) : ShaderNode("aoNode", graph) {
        var inScreenPos = ShaderNodeIoVar(ModelVar2fConst(Vec2f.ZERO))
        var inOrigin = ShaderNodeIoVar(ModelVar3fConst(Vec3f.ZERO))
        var inNormal = ShaderNodeIoVar(ModelVar3fConst(Vec3f.Y_AXIS))

        val outColor = ShaderNodeIoVar(ModelVar4f("colorOut"), this)

        override fun setup(shaderGraph: ShaderGraph) {
            dependsOn(noiseTex)
            dependsOn(inScreenPos, inOrigin)
            super.setup(shaderGraph)
        }

        override fun generateCode(generator: CodeGenerator) {
            generator.appendMain("""
                if ($inOrigin.z > 0.0) {
                    discard;
                }
                
                // compute kernel rotation
                vec2 noiseCoord = ${inScreenPos.ref2f()} * ${aoUniforms.uNoiseScale};
                vec3 rotVec = ${generator.sampleTexture2d(noiseTex.name, "noiseCoord")}.xyz * 2.0 - 1.0;
                vec3 tangent = normalize(rotVec - ${inNormal.ref3f()} * dot(rotVec, ${inNormal.ref3f()}));
                vec3 bitangent = cross(${inNormal.ref3f()}, tangent);
                mat3 tbn = mat3(tangent, bitangent, ${inNormal.ref3f()});
                
                float occlusion = 0.0;
                float bias = ${aoUniforms.uBias} * ${aoUniforms.uRadius};
                for (int i = 0; i < ${aoUniforms.uKernelN}; i++) {
                    vec3 kernel = tbn * ${aoUniforms.uKernel}[i];
                    vec3 samplePos = $inOrigin + kernel * ${aoUniforms.uRadius};
                    
                    vec4 sampleProj = ${aoUniforms.uProj} * vec4(samplePos, 1.0);
                    sampleProj.xyz /= sampleProj.w;
                    sampleProj.xy = sampleProj.xy * 0.5 + 0.5;
                    
                    if (sampleProj.x > 0.0 && sampleProj.x < 1.0 && sampleProj.y > 0.0 && sampleProj.y < 1.0 && sampleProj.z > 0.0) {
                        float sampleDepth = ${generator.sampleTexture2d(depthTex.name, "sampleProj.xy")}.$depthComponent;
                        float rangeCheck = 1.0 - smoothstep(0.0, 1.0, abs($inOrigin.z - sampleDepth) / (4.0 * ${aoUniforms.uRadius}));
                        float occlusionInc = clamp((sampleDepth - (samplePos.z + bias)) * 10.0, 0.0, 1.0);
                        occlusion += occlusionInc * rangeCheck;
                    }
                }
                occlusion /= float(${aoUniforms.uKernelN});
                float occlFac = pow(clamp(1.0 - occlusion * ${aoUniforms.uStrength}, 0.0, 1.0), ${aoUniforms.uPower});
                
                ${outColor.declare()} = vec4(occlFac, 0.0, 0.0, 1.0);
            """)
        }
    }

    companion object {
        const val MAX_KERNEL_SIZE = 128
    }
}

class AoSetup private constructor(val mrtPass: DeferredMrtPass?, val linearDepthPass: NormalLinearDepthMapPass?) {
    val isDeferred: Boolean
        get() = mrtPass != null
    val isForward: Boolean
        get() = linearDepthPass != null

    companion object {
        fun deferred(mrtPass: DeferredMrtPass) = AoSetup(mrtPass, null)
        fun forward(linearDepthPass: NormalLinearDepthMapPass) = AoSetup(null, linearDepthPass)
    }
}
