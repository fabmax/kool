package de.fabmax.kool.gl

import de.fabmax.kool.KoolContext

class ProgramResource private constructor(glRef: Any, ctx: KoolContext) : GlResource(glRef, Type.PROGRAM, ctx) {
    companion object {
        fun create(ctx: KoolContext): ProgramResource {
            return ProgramResource(glCreateProgram(), ctx)
        }
    }

    init {
        ctx.memoryMgr.memoryAllocated(this, 1)
    }

    override fun delete(ctx: KoolContext) {
        ctx.checkIsGlThread()
        glDeleteProgram(this)
        super.delete(ctx)
    }

    fun attachShader(shader: ShaderResource, ctx: KoolContext) {
        ctx.checkIsGlThread()
        glAttachShader(this, shader)
    }

    fun link(ctx: KoolContext): Boolean {
        ctx.checkIsGlThread()
        glLinkProgram(this)
        return glGetProgrami(this, GL_LINK_STATUS) == GL_TRUE
    }

    fun getInfoLog(ctx: KoolContext): String {
        ctx.checkIsGlThread()
        return glGetProgramInfoLog(this)
    }
}