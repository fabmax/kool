package de.fabmax.kool.modules.gltf

import de.fabmax.kool.Assets
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.deferred.DeferredKslPbrShader
import de.fabmax.kool.pipeline.shading.AlphaMode
import de.fabmax.kool.pipeline.shading.DepthShader
import de.fabmax.kool.scene.*
import de.fabmax.kool.scene.animation.*
import de.fabmax.kool.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min

suspend fun GltfFile(data: Uint8Buffer, filePath: String): GltfFile {
    val gltfData = if (filePath.lowercase().endsWith(".gz")) {
        data.inflate()
    } else {
        data
    }
    val gltfFile = when (val type = filePath.lowercase().removeSuffix(".gz").substringAfterLast('.')) {
        "gltf" -> GltfFile.fromJson(gltfData.decodeToString())
        "glb" -> loadGlb(gltfData)
        else -> error("Invalid gltf file type: $type ($filePath)")
    }

    val modelBasePath = if (filePath.contains('/')) {
        filePath.substringBeforeLast('/')
    } else { "." }

    gltfFile.let { m ->
        m.buffers.filter { it.uri != null }.forEach {
            val uri = it.uri!!
            val bufferPath = if (uri.startsWith("data:", true)) { uri } else { "$modelBasePath/$uri" }
            it.data = Assets.loadBlobAsset(bufferPath)
        }
        m.images.filter { it.uri != null }.forEach { it.uri = "$modelBasePath/${it.uri}" }
        m.updateReferences()
    }
    return gltfFile
}

private fun loadGlb(data: Uint8Buffer): GltfFile {
    val str = DataStream(data)

    // file header
    val magic = str.readUInt()
    val version = str.readUInt()
    //val fileLength = str.readUInt()
    str.readUInt()
    if (magic != GltfFile.GLB_FILE_MAGIC) {
        error("Unexpected glTF magic number: $magic (should be ${GltfFile.GLB_FILE_MAGIC} / 'glTF')")
    }
    if (version != 2) {
        logW("loadGlb") { "Unexpected glTF version: $version (should be 2) - stuff might not work as expected" }
    }

    // chunk 0 - JSON content
    var chunkLen = str.readUInt()
    var chunkType = str.readUInt()
    if (chunkType != GltfFile.GLB_CHUNK_MAGIC_JSON) {
        error("Unexpected chunk type for chunk 0: $chunkType (should be ${GltfFile.GLB_CHUNK_MAGIC_JSON} / 'JSON')")
    }
    val jsonData = str.readData(chunkLen).toArray()
    val model = GltfFile.fromJson(jsonData.decodeToString())

    // remaining data chunks
    var iChunk = 1
    while (str.hasRemaining()) {
        chunkLen = str.readUInt()
        chunkType = str.readUInt()
        if (chunkType == GltfFile.GLB_CHUNK_MAGIC_BIN) {
            model.buffers[iChunk-1].data = str.readData(chunkLen)

        } else {
            logW("loadGlb") { "Unexpected chunk type for chunk $iChunk: $chunkType (should be ${GltfFile.GLB_CHUNK_MAGIC_BIN} / ' BIN')" }
            str.index += chunkLen
        }
        iChunk++
    }

    return model
}

/**
 * The root object for a glTF asset.
 *
 * @param extensionsUsed     Names of glTF extensions used somewhere in this asset.
 * @param extensionsRequired Names of glTF extensions required to properly load this asset.
 * @param accessors          An array of accessors.
 * @param animations         An array of keyframe animations.
 * @param asset              Metadata about the glTF asset.
 * @param buffers            An array of buffers.
 * @param bufferViews        An array of bufferViews.
 * @param images             An array of images.
 * @param materials          An array of materials.
 * @param meshes             An array of meshes.
 * @param nodes              An array of nodes.
 * @param scene              The index of the default scene
 * @param scenes             An array of scenes.
 * @param skins              An array of skins.
 * @param textures           An array of textures.
 */
@Serializable
data class GltfFile(
    val extensionsUsed: List<String> = emptyList(),
    val extensionsRequired: List<String> = emptyList(),
    val accessors: List<GltfAccessor> = emptyList(),
    val animations: List<GltfAnimation> = emptyList(),
    val asset: GltfAsset,
    val buffers: List<GltfBuffer> = emptyList(),
    val bufferViews: List<GltfBufferView> = emptyList(),
    //val cameras List<Camera> = emptyList(),
    val images: List<GltfImage> = emptyList(),
    val materials: List<GltfMaterial> = emptyList(),
    val meshes: List<GltfMesh> = emptyList(),
    val nodes: List<GltfNode> = emptyList(),
    val samplers: List<GltfSampler> = emptyList(),
    val scene: Int = 0,
    val scenes: List<GltfScene> = emptyList(),
    val skins: List<GltfSkin> = emptyList(),
    val textures: List<GltfTexture> = emptyList()
) {

    fun makeModel(modelCfg: GltfLoadConfig = GltfLoadConfig(), scene: Int = this.scene): Model {
        return ModelGenerator(modelCfg).makeModel(scenes[scene])
    }

    internal fun updateReferences() {
        accessors.forEach {
            if (it.bufferView >= 0) {
                it.bufferViewRef = bufferViews[it.bufferView]
            }
            it.sparse?.let { sparse ->
                sparse.indices.bufferViewRef = bufferViews[sparse.indices.bufferView]
                sparse.values.bufferViewRef = bufferViews[sparse.values.bufferView]
            }
        }
        animations.forEach { anim ->
            anim.samplers.forEach {
                it.inputAccessorRef = accessors[it.input]
                it.outputAccessorRef = accessors[it.output]
            }
            anim.channels.forEach {
                it.samplerRef = anim.samplers[it.sampler]
                if (it.target.node >= 0) {
                    it.target.nodeRef = nodes[it.target.node]
                }
            }
        }
        bufferViews.forEach { it.bufferRef = buffers[it.buffer] }
        images.filter { it.bufferView >= 0 }.forEach { it.bufferViewRef = bufferViews[it.bufferView] }
        meshes.forEach { mesh ->
            mesh.primitives.forEach {
                if (it.material >= 0) {
                    it.materialRef = materials[it.material]
                }
            }
        }
        nodes.forEach {
            it.childRefs = it.children.map { iNd -> nodes[iNd] }
            if (it.mesh >= 0) {
                it.meshRef = meshes[it.mesh]
            }
            if (it.skin >= 0) {
                it.skinRef = skins[it.skin]
            }
        }
        scenes.forEach { it.nodeRefs = it.nodes.map { iNd -> nodes[iNd] } }
        skins.forEach {
            if (it.inverseBindMatrices >= 0) {
                it.inverseBindMatrixAccessorRef = accessors[it.inverseBindMatrices]
            }
            it.jointRefs = it.joints.map { iJt -> nodes[iJt] }
        }
        textures.forEach {
            it.imageRef = images[it.source]
            it.samplerRef = samplers.getOrNull(it.sampler)
        }
    }

    private inner class ModelGenerator(val cfg: GltfLoadConfig) {
        val modelAnimations = mutableListOf<Animation>()
        val modelNodes = mutableMapOf<GltfNode, Node>()
        val meshesByMaterial = mutableMapOf<Int, MutableSet<Mesh>>()
        val meshMaterials = mutableMapOf<Mesh, GltfMaterial?>()

        fun makeModel(scene: GltfScene): Model {
            val model = Model(scene.name ?: "model_scene")
            scene.nodeRefs.forEach { nd -> model += nd.makeNode(model, cfg) }

            if (cfg.loadAnimations) makeTrsAnimations()
            if (cfg.loadAnimations) makeSkins(model)
            modelNodes.forEach { (node, grp) -> node.createMeshes(model, grp, cfg) }
            if (cfg.loadAnimations) makeMorphAnimations()
            modelAnimations.filter { it.channels.isNotEmpty() }.forEach { modelAnim ->
                modelAnim.prepareAnimation()
                model.animations += modelAnim
            }
            model.disableAllAnimations()

            if (cfg.applyTransforms && model.animations.isEmpty()) applyTransforms(model)
            if (cfg.mergeMeshesByMaterial) mergeMeshesByMaterial(model)
            if (cfg.sortNodesByAlpha) model.sortNodesByAlpha()
            if (cfg.removeEmptyNodes) model.removeEmpty()

            return model
        }

        private fun Node.removeEmpty() {
            children.filter { it !is Mesh }.forEach {
                it.removeEmpty()
                if (it.children.isEmpty()) {
                    removeNode(it)
                }
            }
        }

        private fun makeTrsAnimations() {
            animations.forEach { anim ->
                val modelAnim = Animation(anim.name)
                modelAnimations += modelAnim

                val animNodes = mutableMapOf<Node, AnimationNode>()
                anim.channels.forEach { channel ->
                    val nodeGrp = modelNodes[channel.target.nodeRef]
                    if (nodeGrp != null) {
                        val animationNd = animNodes.getOrPut(nodeGrp) { AnimatedTransformGroup(nodeGrp) }
                        when (channel.target.path) {
                            GltfAnimation.Target.PATH_TRANSLATION -> makeTranslationAnimation(channel, animationNd, modelAnim)
                            GltfAnimation.Target.PATH_ROTATION -> makeRotationAnimation(channel, animationNd, modelAnim)
                            GltfAnimation.Target.PATH_SCALE -> makeScaleAnimation(channel, animationNd, modelAnim)
                        }
                    }
                }
            }
        }

        private fun makeTranslationAnimation(animCh: GltfAnimation.Channel, animNd: AnimationNode, modelAnim: Animation) {
            val inputAcc = animCh.samplerRef.inputAccessorRef
            val outputAcc = animCh.samplerRef.outputAccessorRef

            if (inputAcc.type != GltfAccessor.TYPE_SCALAR || inputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported translation animation input accessor: type = ${inputAcc.type}, component type = ${inputAcc.componentType}, should be SCALAR and 5126 (float)" }
                return
            }
            if (outputAcc.type != GltfAccessor.TYPE_VEC3 || outputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported translation animation output accessor: type = ${outputAcc.type}, component type = ${outputAcc.componentType}, should be VEC3 and 5126 (float)" }
                return
            }

            val transChannel = TranslationAnimationChannel("${modelAnim.name}_translation", animNd)
            val interpolation = when (animCh.samplerRef.interpolation) {
                GltfAnimation.Sampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
                GltfAnimation.Sampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }
            modelAnim.channels += transChannel

            val inTime = FloatAccessor(inputAcc)
            val outTranslation = Vec3fAccessor(outputAcc)
            for (i in 0 until min(inputAcc.count, outputAcc.count)) {
                val t = inTime.next()
                val transKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                    val startTan = outTranslation.next()
                    val point = outTranslation.next()
                    val endTan = outTranslation.next()
                    CubicTranslationKey(t, point, startTan, endTan)
                } else {
                    TranslationKey(t, outTranslation.next())
                }
                transKey.interpolation = interpolation
                transChannel.keys[t] = transKey
            }
        }

        private fun makeRotationAnimation(animCh: GltfAnimation.Channel, animNd: AnimationNode, modelAnim: Animation) {
            val inputAcc = animCh.samplerRef.inputAccessorRef
            val outputAcc = animCh.samplerRef.outputAccessorRef

            if (inputAcc.type != GltfAccessor.TYPE_SCALAR || inputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported rotation animation input accessor: type = ${inputAcc.type}, component type = ${inputAcc.componentType}, should be SCALAR and 5126 (float)" }
                return
            }
            if (outputAcc.type != GltfAccessor.TYPE_VEC4 || outputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported rotation animation output accessor: type = ${outputAcc.type}, component type = ${outputAcc.componentType}, should be VEC4 and 5126 (float)" }
                return
            }

            val rotChannel = RotationAnimationChannel("${modelAnim.name}_rotation", animNd)
            val interpolation = when (animCh.samplerRef.interpolation) {
                GltfAnimation.Sampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
                GltfAnimation.Sampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }
            modelAnim.channels += rotChannel

            val inTime = FloatAccessor(inputAcc)
            val outRotation = Vec4fAccessor(outputAcc)
            for (i in 0 until min(inputAcc.count, outputAcc.count)) {
                val t = inTime.next()
                val rotKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                    val startTan = outRotation.next().toQuatF()
                    val point = outRotation.next().toQuatF()
                    val endTan = outRotation.next().toQuatF()
                    CubicRotationKey(t, point, startTan, endTan)
                } else {
                    RotationKey(t, outRotation.next().toQuatF())
                }
                rotKey.interpolation = interpolation
                rotChannel.keys[t] = rotKey
            }
        }

        private fun makeScaleAnimation(animCh: GltfAnimation.Channel, animNd: AnimationNode, modelAnim: Animation) {
            val inputAcc = animCh.samplerRef.inputAccessorRef
            val outputAcc = animCh.samplerRef.outputAccessorRef

            if (inputAcc.type != GltfAccessor.TYPE_SCALAR || inputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported scale animation input accessor: type = ${inputAcc.type}, component type = ${inputAcc.componentType}, should be SCALAR and 5126 (float)" }
                return
            }
            if (outputAcc.type != GltfAccessor.TYPE_VEC3 || outputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported scale animation output accessor: type = ${outputAcc.type}, component type = ${outputAcc.componentType}, should be VEC3 and 5126 (float)" }
                return
            }

            val scaleChannel = ScaleAnimationChannel("${modelAnim.name}_scale", animNd)
            val interpolation = when (animCh.samplerRef.interpolation) {
                GltfAnimation.Sampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
                GltfAnimation.Sampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }
            modelAnim.channels += scaleChannel

            val inTime = FloatAccessor(inputAcc)
            val outScale = Vec3fAccessor(outputAcc)
            for (i in 0 until min(inputAcc.count, outputAcc.count)) {
                val t = inTime.next()
                val scaleKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                    val startTan = outScale.next()
                    val point = outScale.next()
                    val endTan = outScale.next()
                    CubicScaleKey(t, point, startTan, endTan)
                } else {
                    ScaleKey(t, outScale.next())
                }
                scaleKey.interpolation = interpolation
                scaleChannel.keys[t] = scaleKey
            }
        }

        private fun makeMorphAnimations() {
            animations.forEachIndexed { iAnim, anim ->
                anim.channels.forEach { channel ->
                    if (channel.target.path == GltfAnimation.Target.PATH_WEIGHTS) {
                        val modelAnim = modelAnimations[iAnim]
                        val gltfMesh = channel.target.nodeRef?.meshRef
                        val nodeGrp = modelNodes[channel.target.nodeRef]
                        nodeGrp?.children?.filterIsInstance<Mesh>()?.forEach {
                            makeWeightAnimation(gltfMesh!!, channel, MorphAnimatedMesh(it), modelAnim)
                        }
                    }
                }
            }
        }

        private fun makeWeightAnimation(gltfMesh: GltfMesh, animCh: GltfAnimation.Channel, animNd: MorphAnimatedMesh, modelAnim: Animation) {
            val inputAcc = animCh.samplerRef.inputAccessorRef
            val outputAcc = animCh.samplerRef.outputAccessorRef

            if (inputAcc.type != GltfAccessor.TYPE_SCALAR || inputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported weight animation input accessor: type = ${inputAcc.type}, component type = ${inputAcc.componentType}, should be SCALAR and 5126 (float)" }
                return
            }
            if (outputAcc.type != GltfAccessor.TYPE_SCALAR || outputAcc.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
                logW { "Unsupported weight animation output accessor: type = ${outputAcc.type}, component type = ${inputAcc.componentType}, should be VEC3 and 5126 (float)" }
                return
            }

            val weightChannel = WeightAnimationChannel("${modelAnim.name}_weight", animNd)
            val interpolation = when (animCh.samplerRef.interpolation) {
                GltfAnimation.Sampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
                GltfAnimation.Sampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }
            modelAnim.channels += weightChannel

            val morphTargets = gltfMesh.primitives[0].targets
            val nAttribs = gltfMesh.primitives[0].targets.sumOf { it.size }
            val inTimes = FloatAccessor(inputAcc)
            val outWeight = FloatAccessor(outputAcc)
            for (i in 0 until min(inputAcc.count, outputAcc.count)) {
                val t = inTimes.next()
                val weightKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                    val startTan = FloatArray(nAttribs)
                    val point = FloatArray(nAttribs)
                    val endTan = FloatArray(nAttribs)

                    var iAttrib = 0
                    for (m in morphTargets.indices) {
                        val w = outWeight.next()
                        for (j in 0 until morphTargets[m].size) {
                            startTan[iAttrib++] = w
                        }
                    }
                    iAttrib = 0
                    for (m in morphTargets.indices) {
                        val w = outWeight.next()
                        for (j in 0 until morphTargets[m].size) {
                            point[iAttrib++] = w
                        }
                    }
                    iAttrib = 0
                    for (m in morphTargets.indices) {
                        val w = outWeight.next()
                        for (j in 0 until morphTargets[m].size) {
                            endTan[iAttrib++] = w
                        }
                    }
                    CubicWeightKey(t, point, startTan, endTan)

                } else {
                    val attribWeights = FloatArray(nAttribs)
                    var iAttrib = 0
                    for (m in morphTargets.indices) {
                        val w = outWeight.next()
                        for (j in 0 until morphTargets[m].size) {
                            attribWeights[iAttrib++] = w
                        }
                    }
                    WeightKey(t, attribWeights)
                }

                weightKey.interpolation = interpolation
                weightChannel.keys[t] = weightKey
            }
        }

        private fun makeSkins(model: Model) {
            skins.forEach { skin ->
                val modelSkin = Skin()
                val invBinMats = skin.inverseBindMatrixAccessorRef?.let { Mat4fAccessor(it) }
                if (invBinMats != null) {
                    // 1st pass: make SkinNodes for specified nodes / TransformGroups
                    val skinNodes = mutableMapOf<GltfNode, Skin.SkinNode>()
                    skin.jointRefs.forEach { joint ->
                        val jointGrp = modelNodes[joint]!!
                        val invBindMat = invBinMats.next()
                        val skinNode = Skin.SkinNode(jointGrp, invBindMat)
                        modelSkin.nodes += skinNode
                        skinNodes[joint] = skinNode
                    }
                    // 2nd pass: build SkinNode hierarchy
                    skin.jointRefs.forEach { joint ->
                        val skinNode = skinNodes[joint]
                        if (skinNode != null) {
                            joint.childRefs.forEach { child ->
                                val childNode = skinNodes[child]
                                childNode?.let { skinNode.addChild(it) }
                            }
                        }
                    }
                    model.skins += modelSkin
                }
            }
        }

        private fun Node.sortNodesByAlpha(): Float {
            val childAlphas = mutableMapOf<Node, Float>()
            var avgAlpha = 0f
            for (child in children) {
                val a = if (child is Mesh && !child.isOpaque) {
                    0f
                } else {
                    child.sortNodesByAlpha()
                }
                childAlphas[child] = a
                avgAlpha += a
            }
            sortChildrenBy { -(childAlphas[it] ?: 1f) }
            if (children.isNotEmpty()) {
                avgAlpha /= children.size
            }
            return avgAlpha
        }

        private fun mergeMeshesByMaterial(model: Model) {
            model.mergeMeshesByMaterial()
        }

        private fun Node.mergeMeshesByMaterial() {
            children.filter{ it.children.isNotEmpty() }.forEach { it.mergeMeshesByMaterial() }

            meshesByMaterial.values.forEach { sameMatMeshes ->
                val mergeMeshes = children.filter { it in sameMatMeshes }.map { it as Mesh }
                if (mergeMeshes.size > 1) {
                    val r = mergeMeshes[0]
                    for (i in 1 until mergeMeshes.size) {
                        val m = mergeMeshes[i]
                        if (m.geometry.vertexAttributes == r.geometry.vertexAttributes) {
                            r.geometry.addGeometry(m.geometry)
                            removeNode(m)
                        }
                    }
                }
            }
        }

        private fun applyTransforms(model: Model) {
            val transform = Mat4fStack()
            transform.setIdentity()
            model.applyTransforms(transform, model)
        }

        private fun Node.applyTransforms(transform: Mat4fStack, rootGroup: Node) {
            transform.push()
            transform.mul(this.transform.matrixF)

            children.filterIsInstance<Mesh>().forEach {
                it.geometry.batchUpdate(true) {
                    forEach { v ->
                        transform.transform(v.position, 1f)
                        transform.transform(v.normal, 0f)
                        val tan3 = MutableVec3f(v.tangent.xyz)
                        transform.transform(tan3, 0f)
                        v.tangent.set(tan3, v.tangent.w)
                    }
                }
                if (rootGroup != this) {
                    rootGroup += it
                }
            }

            children.filter { it.children.isNotEmpty() }.forEach {
                it.applyTransforms(transform, rootGroup)
                removeNode(it)
            }

            transform.pop()
        }

        private fun GltfNode.makeNode(model: Model, cfg: GltfLoadConfig): Node {
            val modelNdName = name ?: "node_${model.nodes.size}"
            val nodeGrp = Node(modelNdName)
            modelNodes[this] = nodeGrp
            model.nodes[modelNdName] = nodeGrp

            if (matrix != null) {
                val t = MatrixTransformF()
                t.matrixF.set(matrix.toFloatArray())
                nodeGrp.transform = t
            } else {
                val t = translation?.let { Vec3f(it[0], it[1], it[2]) } ?: Vec3f.ZERO
                val r = rotation?.let { QuatF(it[0], it[1], it[2], it[3]) } ?: QuatF.IDENTITY
                val s = scale?.let { Vec3f(it[0], it[1], it[2]) } ?: Vec3f.ONES
                nodeGrp.transform.setCompositionOf(t, r, s)
            }

            childRefs.forEach {
                nodeGrp += it.makeNode(model, cfg)
            }

            return nodeGrp
        }

        private fun GltfNode.createMeshes(model: Model, nodeGrp: Node, cfg: GltfLoadConfig) {
            meshRef?.primitives?.forEachIndexed { index, prim ->
                val name = "${meshRef?.name ?: "${nodeGrp.name}.mesh"}_$index"
                val geometry = prim.toGeometry(cfg, accessors)
                if (!geometry.isEmpty()) {
                    var isFrustumChecked = true
                    var meshSkin: Skin? = null
                    var morphWeights: FloatArray? = null

                    if (cfg.loadAnimations && cfg.applySkins && skin >= 0) {
                        meshSkin = model.skins[skin]
                        isFrustumChecked = false
                    }
                    if (cfg.loadAnimations && cfg.applyMorphTargets && prim.targets.isNotEmpty()) {
                        morphWeights = FloatArray(prim.targets.sumOf { it.size })
                        isFrustumChecked = false
                    }

                    val instances = if (cfg.addInstanceAttributes.isNotEmpty()) {
                        MeshInstanceList(cfg.addInstanceAttributes)
                    } else {
                        null
                    }

                    val mesh = Mesh(geometry, instances = instances, morphWeights = morphWeights, skin = meshSkin, name = name)
                    mesh.isFrustumChecked = isFrustumChecked

                    nodeGrp += mesh
                    if (meshSkin != null) {
                        val skeletonRoot = skins[skin].skeleton
                        if (skeletonRoot >= 0) {
                            nodeGrp -= mesh
                            modelNodes[nodes[skeletonRoot]]!! += mesh
                        }
                    }

                    meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
                    meshMaterials[mesh] = prim.materialRef

                    if (cfg.applyMaterials) {
                        makeKslMaterial(prim, mesh, cfg, model)
                    }
                    model.meshes[name] = mesh
                }
            }
        }

        private fun makeKslMaterial(prim: GltfMesh.Primitive, mesh: Mesh, cfg: GltfLoadConfig, model: Model) {
            var isDeferred = cfg.materialConfig.isDeferredShading
            val useVertexColor = prim.attributes.containsKey(GltfMesh.Primitive.ATTRIBUTE_COLOR_0)

            val pbrConfig = DeferredKslPbrShader.Config.Builder().apply {
                val material = prim.materialRef
                if (material != null) {
                    material.applyTo(this, useVertexColor, this@GltfFile)
                } else {
                    color {
                        uniformColor(Color.GRAY.toLinear())
                    }
                }

                vertices {
                    if (mesh.skin != null) {
                        enableArmature(min(cfg.materialConfig.maxNumberOfJoints, mesh.skin.nodes.size))
                        if (cfg.materialConfig.maxNumberOfJoints < mesh.skin.nodes.size) {
                            logE("GltfFile") { "\"${model.name}\": Maximum number of joints exceeded (mesh has ${mesh.skin.nodes.size}, materialConfig.maxNumberOfJoints is ${cfg.materialConfig.maxNumberOfJoints})" }
                        }
                    }
                    if (mesh.morphWeights != null) {
                        morphAttributes += mesh.geometry.getMorphAttributes()
                    }
                }

                cfg.materialConfig.let { matCfg ->
                    maxNumberOfLights = matCfg.maxNumberOfLights
                    shadow {
                        addShadowMaps(matCfg.shadowMaps)
                    }
                    matCfg.scrSpcAmbientOcclusionMap?.let {
                        enableSsao(it)
                    }
                    matCfg.environmentMaps?.let { ibl ->
                        imageBasedAmbientColor(ibl.irradianceMap)
                        reflectionMap = ibl.reflectionMap
                    }
                }
                cfg.pbrBlock?.invoke(this, prim)

                if (alphaMode is AlphaMode.Blend) {
                    mesh.isOpaque = false
                    // transparent / blended meshes must be rendered in forward pass
                    isDeferred = false
                }

                colorCfg.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
                emissionCfg.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
                normalMapCfg.defaultNormalMap?.let { model.textures[it.name] = it }
                roughnessCfg.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
                metallicCfg.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
                aoCfg.materialAo.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
                vertexCfg.displacementCfg.primaryTexture?.defaultTexture?.let { model.textures[it.name] = it }
            }

            mesh.shader = if (isDeferred) {
                pbrConfig.pipelineCfg.blendMode = BlendMode.DISABLED
                DeferredKslPbrShader(pbrConfig.build())
            } else {
                KslPbrShader(pbrConfig.build())
            }

            if (pbrConfig.alphaMode is AlphaMode.Mask) {
                mesh.depthShaderConfig = DepthShader.Config.forMesh(
                    mesh,
                    pbrConfig.pipelineCfg.cullMethod,
                    pbrConfig.alphaMode,
                    pbrConfig.colorCfg.primaryTexture?.defaultTexture
                )
            }
        }
    }

    companion object {
        const val GLB_FILE_MAGIC = 0x46546c67
        const val GLB_CHUNK_MAGIC_JSON = 0x4e4f534a
        const val GLB_CHUNK_MAGIC_BIN = 0x004e4942

        private val jsonFmt = Json {
            isLenient = true
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = true
        }

        fun fromJson(json: String): GltfFile {
            return jsonFmt.decodeFromString(serializer(), json)
        }
    }
}
