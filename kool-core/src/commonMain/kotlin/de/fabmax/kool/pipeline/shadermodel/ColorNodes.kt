package de.fabmax.kool.pipeline.shadermodel

import de.fabmax.kool.util.Color


class ColorAlphaNode(graph: ShaderGraph) : ShaderNode("colorAlphaNode_${graph.nextNodeId}", graph) {
    var inColor = ShaderNodeIoVar(ModelVar4fConst(Color.MAGENTA), null)
    var inAlpha = ShaderNodeIoVar(ModelVar1fConst(1f), null)
    val outAlphaColor = ShaderNodeIoVar(ModelVar4f("${name}_outColor"), this)

    override fun setup(shaderGraph: ShaderGraph) {
        super.setup(shaderGraph)
        dependsOn(inColor, inAlpha)
    }

    override fun generateCode(generator: CodeGenerator) {
        generator.appendMain("""
            ${outAlphaColor.declare()} = vec4(${inColor.ref3f()}, ${inColor.ref4f()}.a * ${inAlpha.ref1f()});
            """)
    }
}

class PremultiplyColorNode(graph: ShaderGraph) : ShaderNode("colorPreMult_${graph.nextNodeId}", graph) {
    var inColor = ShaderNodeIoVar(ModelVar4fConst(Color.MAGENTA))
    val outColor = ShaderNodeIoVar(ModelVar4f("${name}_outColor"), this)

    override fun setup(shaderGraph: ShaderGraph) {
        super.setup(shaderGraph)
        dependsOn(inColor)
    }

    override fun generateCode(generator: CodeGenerator) {
        generator.appendMain("${outColor.declare()} = vec4(${inColor.ref3f()} * ${inColor.ref4f()}.a, ${inColor.ref4f()}.a);")
    }
}

class GammaNode(graph: ShaderGraph) : ShaderNode("gamma_${graph.nextNodeId}", graph) {
    var inColor = ShaderNodeIoVar(ModelVar4fConst(Color.MAGENTA))
    var inGamma = ShaderNodeIoVar(ModelVar1fConst(1f / 2.2f))
    val outColor = ShaderNodeIoVar(ModelVar4f("${name}_outColor"), this)

    override fun setup(shaderGraph: ShaderGraph) {
        super.setup(shaderGraph)
        dependsOn(inColor, inGamma)
    }

    override fun generateCode(generator: CodeGenerator) {
        generator.appendMain("${outColor.declare()} = vec4(pow(${inColor.ref3f()}, vec3(1.0/${inGamma.ref1f()})), ${inColor.ref4f()}.a);")
    }
}

class HdrToLdrNode(graph: ShaderGraph) : ShaderNode("hdrToLdr_${graph.nextNodeId}", graph) {
    var inColor = ShaderNodeIoVar(ModelVar4fConst(Color.MAGENTA))
    var inGamma = ShaderNodeIoVar(ModelVar1fConst(2.2f))
    val outColor = ShaderNodeIoVar(ModelVar4f("${name}_outColor"), this)

    override fun setup(shaderGraph: ShaderGraph) {
        super.setup(shaderGraph)
        dependsOn(inColor, inGamma)
    }

    private fun generateUncharted2(generator: CodeGenerator) {
        generator.appendFunction("uncharted2", """
            vec3 uncharted2Tonemap_func(vec3 x) {
                float A = 0.15;     // shoulder strength
                float B = 0.50;     // linear strength
                float C = 0.10;     // linear angle
                float D = 0.20;     // toe strength
                float E = 0.02;     // toe numerator
                float F = 0.30;     // toe denominator  --> E/F = toe angle
                return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
            }
            
            vec3 uncharted2Tonemap(vec3 rgbLinear) {
                float W = 11.2;     // linear white point value
                float ExposureBias = 2.0;
                vec3 curr = uncharted2Tonemap_func(ExposureBias * rgbLinear);
                vec3 whiteScale = 1.0 / uncharted2Tonemap_func(vec3(W));
                return curr * whiteScale;
            }
        """)

        generator.appendMain("""
            vec3 ${name}_color = uncharted2Tonemap(${inColor.ref3f()});
            ${outColor.declare()} = vec4(pow(${name}_color, vec3(1.0/${inGamma.ref1f()})), ${inColor.ref4f()}.a);
        """)
    }

    private fun generateReinhard(generator: CodeGenerator) {
        generator.appendMain("""
            vec3 ${name}_color = ${inColor.ref3f()} / (${inColor.ref3f()} + vec3(1.0));
            ${outColor.declare()} = vec4(pow(${name}_color, vec3(1.0/${inGamma.ref1f()})), ${inColor.ref4f()}.a);
        """)
    }

    private fun generateJimHejlRichardBurgessDawson(generator: CodeGenerator) {
        generator.appendMain("""
            vec3 ${name}_color = max(vec3(0), ${inColor.ref3f()} - 0.004);
            ${outColor.declare()} = vec4((${name}_color * (6.2 * ${name}_color + 0.5)) / (${name}_color * (6.2 * ${name}_color + 1.7) + 0.06), 1.0);
        """)
    }

    override fun generateCode(generator: CodeGenerator) {
        generateUncharted2(generator)
    }
}

