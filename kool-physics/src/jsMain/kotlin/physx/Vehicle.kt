/*
 * Generated from WebIDL by webidl-util
 */
@file:Suppress("UnsafeCastFromDynamic", "ClassName", "FunctionName", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "unused")

package physx

external interface PxVehicleTopLevelFunctions {
    /**
     * @param physics WebIDL type: [PxPhysics] (Ref)
     * @return WebIDL type: boolean
     */
    fun InitVehicleSDK(physics: PxPhysics): Boolean

    /**
     * @param nbSprungMasses        WebIDL type: unsigned long
     * @param sprungMassCoordinates WebIDL type: [PxVec3] (Const)
     * @param centreOfMass          WebIDL type: [PxVec3] (Const, Ref)
     * @param totalMass             WebIDL type: float
     * @param gravityDirection      WebIDL type: unsigned long
     * @param sprungMasses          WebIDL type: [PxRealPtr] (Ref)
     */
    fun PxVehicleComputeSprungMasses(nbSprungMasses: Int, sprungMassCoordinates: PxVec3, centreOfMass: PxVec3, totalMass: Float, gravityDirection: Int, sprungMasses: PxRealPtr)

    /**
     * @param batchQuery          WebIDL type: [PxBatchQuery]
     * @param vehicles            WebIDL type: [Vector_PxVehicleWheels] (Ref)
     * @param nbSceneQueryResults WebIDL type: unsigned long
     * @param sceneQueryResults   WebIDL type: [PxRaycastQueryResult]
     */
    fun PxVehicleSuspensionRaycasts(batchQuery: PxBatchQuery, vehicles: Vector_PxVehicleWheels, nbSceneQueryResults: Int, sceneQueryResults: PxRaycastQueryResult)

    /**
     * @param timestep                                  WebIDL type: float
     * @param gravity                                   WebIDL type: [PxVec3] (Const, Ref)
     * @param vehicleDrivableSurfaceToTireFrictionPairs WebIDL type: [PxVehicleDrivableSurfaceToTireFrictionPairs] (Const, Ref)
     * @param vehicles                                  WebIDL type: [Vector_PxVehicleWheels] (Ref)
     * @param vehicleWheelQueryResults                  WebIDL type: [PxVehicleWheelQueryResult]
     */
    fun PxVehicleUpdates(timestep: Float, gravity: PxVec3, vehicleDrivableSurfaceToTireFrictionPairs: PxVehicleDrivableSurfaceToTireFrictionPairs, vehicles: Vector_PxVehicleWheels, vehicleWheelQueryResults: PxVehicleWheelQueryResult)

    /**
     * @param up      WebIDL type: [PxVec3] (Const, Ref)
     * @param forward WebIDL type: [PxVec3] (Const, Ref)
     */
    fun VehicleSetBasisVectors(up: PxVec3, forward: PxVec3)

    /**
     * @param vehicleUpdateMode WebIDL type: [PxVehicleUpdateModeEnum] (enum)
     */
    fun VehicleSetUpdateMode(vehicleUpdateMode: Int)

    /**
     * @param tireData WebIDL type: [PxVehicleTireData]
     * @param m        WebIDL type: unsigned long
     * @param n        WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun PxVehicleTireData_getFrictionVsSlipGraph(tireData: PxVehicleTireData, m: Int, n: Int): Float

    /**
     * @param tireData WebIDL type: [PxVehicleTireData]
     * @param m        WebIDL type: unsigned long
     * @param n        WebIDL type: unsigned long
     * @param value    WebIDL type: float
     */
    fun PxVehicleTireData_setFrictionVsSlipGraph(tireData: PxVehicleTireData, m: Int, n: Int, value: Float)

}

external interface PxVehicleAckermannGeometryData {
    /**
     * WebIDL type: float
     */
    var mAccuracy: Float
    /**
     * WebIDL type: float
     */
    var mFrontWidth: Float
    /**
     * WebIDL type: float
     */
    var mRearWidth: Float
    /**
     * WebIDL type: float
     */
    var mAxleSeparation: Float
}

fun PxVehicleAckermannGeometryData(): PxVehicleAckermannGeometryData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleAckermannGeometryData()")
}

external interface PxVehicleAntiRollBarData {
    /**
     * WebIDL type: unsigned long
     */
    var mWheel0: Int
    /**
     * WebIDL type: unsigned long
     */
    var mWheel1: Int
    /**
     * WebIDL type: float
     */
    var mStiffness: Float
}

fun PxVehicleAntiRollBarData(): PxVehicleAntiRollBarData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleAntiRollBarData()")
}

external interface PxVehicleAutoBoxData {
    /**
     * WebIDL type: float
     */
    var mUpRatios: Array<Float>
    /**
     * WebIDL type: float
     */
    var mDownRatios: Array<Float>

    /**
     * @param latency WebIDL type: float
     */
    fun setLatency(latency: Float)

    /**
     * @return WebIDL type: float
     */
    fun getLatency(): Float

    /**
     * @param a WebIDL type: [PxVehicleGearEnum] (enum)
     * @return WebIDL type: float
     */
    fun getUpRatios(a: Int): Float

    /**
     * @param a     WebIDL type: [PxVehicleGearEnum] (enum)
     * @param ratio WebIDL type: float
     */
    fun setUpRatios(a: Int, ratio: Float)

    /**
     * @param a WebIDL type: [PxVehicleGearEnum] (enum)
     * @return WebIDL type: float
     */
    fun getDownRatios(a: Int): Float

    /**
     * @param a     WebIDL type: [PxVehicleGearEnum] (enum)
     * @param ratio WebIDL type: float
     */
    fun setDownRatios(a: Int, ratio: Float)

}

fun PxVehicleAutoBoxData(): PxVehicleAutoBoxData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleAutoBoxData()")
}

external interface PxVehicleChassisData {
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var mMOI: PxVec3
    /**
     * WebIDL type: float
     */
    var mMass: Float
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var mCMOffset: PxVec3
}

fun PxVehicleChassisData(): PxVehicleChassisData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleChassisData()")
}

external interface PxVehicleClutchData {
    /**
     * WebIDL type: float
     */
    var mStrength: Float
    /**
     * WebIDL type: [PxVehicleClutchAccuracyModeEnum] (enum)
     */
    var mAccuracyMode: Int
    /**
     * WebIDL type: unsigned long
     */
    var mEstimateIterations: Int
}

fun PxVehicleClutchData(): PxVehicleClutchData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleClutchData()")
}

external interface PxVehicleDifferential4WData {
    /**
     * WebIDL type: float
     */
    var mFrontRearSplit: Float
    /**
     * WebIDL type: float
     */
    var mFrontLeftRightSplit: Float
    /**
     * WebIDL type: float
     */
    var mRearLeftRightSplit: Float
    /**
     * WebIDL type: float
     */
    var mCentreBias: Float
    /**
     * WebIDL type: float
     */
    var mFrontBias: Float
    /**
     * WebIDL type: float
     */
    var mRearBias: Float
    /**
     * WebIDL type: [PxVehicleDifferential4WDataEnum] (enum)
     */
    var mType: Int
}

fun PxVehicleDifferential4WData(): PxVehicleDifferential4WData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleDifferential4WData()")
}

external interface PxVehicleDrivableSurfaceToTireFrictionPairs {
    /**
     * @param maxNbTireTypes    WebIDL type: unsigned long
     * @param maxNbSurfaceTypes WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleDrivableSurfaceToTireFrictionPairs]
     */
    fun allocate(maxNbTireTypes: Int, maxNbSurfaceTypes: Int): PxVehicleDrivableSurfaceToTireFrictionPairs

    /**
     * @param nbTireTypes              WebIDL type: unsigned long
     * @param nbSurfaceTypes           WebIDL type: unsigned long
     * @param drivableSurfaceMaterials WebIDL type: [PxMaterialPtr]
     * @param drivableSurfaceTypes     WebIDL type: [PxVehicleDrivableSurfaceType] (Const)
     */
    fun setup(nbTireTypes: Int, nbSurfaceTypes: Int, drivableSurfaceMaterials: PxMaterialPtr, drivableSurfaceTypes: PxVehicleDrivableSurfaceType)

    fun release()

    /**
     * @param surfaceType WebIDL type: unsigned long
     * @param tireType    WebIDL type: unsigned long
     * @param value       WebIDL type: float
     */
    fun setTypePairFriction(surfaceType: Int, tireType: Int, value: Float)

    /**
     * @param surfaceType WebIDL type: unsigned long
     * @param tireType    WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getTypePairFriction(surfaceType: Int, tireType: Int): Float

    /**
     * @return WebIDL type: unsigned long
     */
    fun getMaxNbSurfaceTypes(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getMaxNbTireTypes(): Int

}

external interface PxVehicleDrivableSurfaceType {
    /**
     * WebIDL type: unsigned long
     */
    var mType: Int
}

fun PxVehicleDrivableSurfaceType(): PxVehicleDrivableSurfaceType {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleDrivableSurfaceType()")
}

external interface PxVehicleDrive : PxVehicleWheels {
    /**
     * WebIDL type: [PxVehicleDriveDynData] (Value)
     */
    var mDriveDynData: PxVehicleDriveDynData
}

external interface PxVehicleDrive4W : PxVehicleDrive {
    /**
     * WebIDL type: [PxVehicleDriveSimData4W] (Value)
     */
    var mDriveSimData: PxVehicleDriveSimData4W

    /**
     * @param nbWheels WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleDrive4W]
     */
    fun allocate(nbWheels: Int): PxVehicleDrive4W

    fun free()

    /**
     * @param physics           WebIDL type: [PxPhysics]
     * @param vehActor          WebIDL type: [PxRigidDynamic]
     * @param wheelsData        WebIDL type: [PxVehicleWheelsSimData] (Const, Ref)
     * @param driveData         WebIDL type: [PxVehicleDriveSimData4W] (Const, Ref)
     * @param nbNonDrivenWheels WebIDL type: unsigned long
     */
    fun setup(physics: PxPhysics, vehActor: PxRigidDynamic, wheelsData: PxVehicleWheelsSimData, driveData: PxVehicleDriveSimData4W, nbNonDrivenWheels: Int)

    fun setToRestState()

}

external interface PxVehicleDriveDynData {
    /**
     * WebIDL type: float
     */
    var mControlAnalogVals: Array<Float>
    /**
     * WebIDL type: boolean
     */
    var mUseAutoGears: Boolean
    /**
     * WebIDL type: boolean
     */
    var mGearUpPressed: Boolean
    /**
     * WebIDL type: boolean
     */
    var mGearDownPressed: Boolean
    /**
     * WebIDL type: unsigned long
     */
    var mCurrentGear: Int
    /**
     * WebIDL type: unsigned long
     */
    var mTargetGear: Int
    /**
     * WebIDL type: float
     */
    var mEnginespeed: Float
    /**
     * WebIDL type: float
     */
    var mGearSwitchTime: Float
    /**
     * WebIDL type: float
     */
    var mAutoBoxSwitchTime: Float

    fun setToRestState()

    /**
     * @param type      WebIDL type: unsigned long
     * @param analogVal WebIDL type: float
     */
    fun setAnalogInput(type: Int, analogVal: Float)

    /**
     * @param type WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getAnalogInput(type: Int): Float

    /**
     * @param digitalVal WebIDL type: boolean
     */
    fun setGearUp(digitalVal: Boolean)

    /**
     * @param digitalVal WebIDL type: boolean
     */
    fun setGearDown(digitalVal: Boolean)

    /**
     * @return WebIDL type: boolean
     */
    fun getGearUp(): Boolean

    /**
     * @return WebIDL type: boolean
     */
    fun getGearDown(): Boolean

    /**
     * @param useAutoGears WebIDL type: boolean
     */
    fun setUseAutoGears(useAutoGears: Boolean)

    /**
     * @return WebIDL type: boolean
     */
    fun getUseAutoGears(): Boolean

    fun toggleAutoGears()

    /**
     * @param currentGear WebIDL type: unsigned long
     */
    fun setCurrentGear(currentGear: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getCurrentGear(): Int

    /**
     * @param targetGear WebIDL type: unsigned long
     */
    fun setTargetGear(targetGear: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getTargetGear(): Int

    /**
     * @param targetGear WebIDL type: unsigned long
     */
    fun startGearChange(targetGear: Int)

    /**
     * @param targetGear WebIDL type: unsigned long
     */
    fun forceGearChange(targetGear: Int)

    /**
     * @param speed WebIDL type: float
     */
    fun setEngineRotationSpeed(speed: Float)

    /**
     * @return WebIDL type: float
     */
    fun getEngineRotationSpeed(): Float

    /**
     * @return WebIDL type: float
     */
    fun getGearSwitchTime(): Float

    /**
     * @return WebIDL type: float
     */
    fun getAutoBoxSwitchTime(): Float

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbAnalogInput(): Int

    /**
     * @param gearChange WebIDL type: unsigned long
     */
    fun setGearChange(gearChange: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getGearChange(): Int

    /**
     * @param switchTime WebIDL type: float
     */
    fun setGearSwitchTime(switchTime: Float)

    /**
     * @param autoBoxSwitchTime WebIDL type: float
     */
    fun setAutoBoxSwitchTime(autoBoxSwitchTime: Float)

}

external interface PxVehicleDriveSimData {
    /**
     * @return WebIDL type: [PxVehicleEngineData] (Const, Ref)
     */
    fun getEngineData(): PxVehicleEngineData

    /**
     * @param engine WebIDL type: [PxVehicleEngineData] (Const, Ref)
     */
    fun setEngineData(engine: PxVehicleEngineData)

    /**
     * @return WebIDL type: [PxVehicleGearsData] (Const, Ref)
     */
    fun getGearsData(): PxVehicleGearsData

    /**
     * @param gears WebIDL type: [PxVehicleGearsData] (Const, Ref)
     */
    fun setGearsData(gears: PxVehicleGearsData)

    /**
     * @return WebIDL type: [PxVehicleClutchData] (Const, Ref)
     */
    fun getClutchData(): PxVehicleClutchData

    /**
     * @param clutch WebIDL type: [PxVehicleClutchData] (Const, Ref)
     */
    fun setClutchData(clutch: PxVehicleClutchData)

    /**
     * @return WebIDL type: [PxVehicleAutoBoxData] (Const, Ref)
     */
    fun getAutoBoxData(): PxVehicleAutoBoxData

    /**
     * @param clutch WebIDL type: [PxVehicleAutoBoxData] (Const, Ref)
     */
    fun setAutoBoxData(clutch: PxVehicleAutoBoxData)

}

fun PxVehicleDriveSimData(): PxVehicleDriveSimData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleDriveSimData()")
}

external interface PxVehicleDriveSimData4W : PxVehicleDriveSimData {
    /**
     * @return WebIDL type: [PxVehicleDifferential4WData] (Const, Ref)
     */
    fun getDiffData(): PxVehicleDifferential4WData

    /**
     * @return WebIDL type: [PxVehicleAckermannGeometryData] (Const, Ref)
     */
    fun getAckermannGeometryData(): PxVehicleAckermannGeometryData

    /**
     * @param diff WebIDL type: [PxVehicleDifferential4WData] (Const, Ref)
     */
    fun setDiffData(diff: PxVehicleDifferential4WData)

    /**
     * @param ackermannData WebIDL type: [PxVehicleAckermannGeometryData] (Const, Ref)
     */
    fun setAckermannGeometryData(ackermannData: PxVehicleAckermannGeometryData)

}

fun PxVehicleDriveSimData4W(): PxVehicleDriveSimData4W {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleDriveSimData4W()")
}

external interface PxVehicleEngineData {
    /**
     * WebIDL type: [PxEngineTorqueLookupTable] (Value)
     */
    var mTorqueCurve: PxEngineTorqueLookupTable
    /**
     * WebIDL type: float
     */
    var mMOI: Float
    /**
     * WebIDL type: float
     */
    var mPeakTorque: Float
    /**
     * WebIDL type: float
     */
    var mMaxOmega: Float
    /**
     * WebIDL type: float
     */
    var mDampingRateFullThrottle: Float
    /**
     * WebIDL type: float
     */
    var mDampingRateZeroThrottleClutchEngaged: Float
    /**
     * WebIDL type: float
     */
    var mDampingRateZeroThrottleClutchDisengaged: Float
}

fun PxVehicleEngineData(): PxVehicleEngineData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleEngineData()")
}

external interface PxEngineTorqueLookupTable {
    /**
     * WebIDL type: float
     */
    var mDataPairs: Array<Float>
    /**
     * WebIDL type: unsigned long
     */
    var mNbDataPairs: Int

    /**
     * @param x WebIDL type: float
     * @param y WebIDL type: float
     */
    fun addPair(x: Float, y: Float)

    /**
     * @param x WebIDL type: float
     * @return WebIDL type: float
     */
    fun getYVal(x: Float): Float

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbDataPairs(): Int

    fun clear()

    /**
     * @param i WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getX(i: Int): Float

    /**
     * @param i WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getY(i: Int): Float

}

fun PxEngineTorqueLookupTable(): PxEngineTorqueLookupTable {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxEngineTorqueLookupTable()")
}

external interface PxVehicleGearsData {
    /**
     * WebIDL type: float
     */
    var mRatios: Array<Float>
    /**
     * WebIDL type: float
     */
    var mFinalRatio: Float
    /**
     * WebIDL type: unsigned long
     */
    var mNbRatios: Int
    /**
     * WebIDL type: float
     */
    var mSwitchTime: Float

    /**
     * @param a WebIDL type: [PxVehicleGearEnum] (enum)
     * @return WebIDL type: float
     */
    fun getGearRatio(a: Int): Float

    /**
     * @param a     WebIDL type: [PxVehicleGearEnum] (enum)
     * @param ratio WebIDL type: float
     */
    fun setGearRatio(a: Int, ratio: Float)

}

fun PxVehicleGearsData(): PxVehicleGearsData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleGearsData()")
}

external interface PxVehicleSuspensionData {
    /**
     * WebIDL type: float
     */
    var mSpringStrength: Float
    /**
     * WebIDL type: float
     */
    var mSpringDamperRate: Float
    /**
     * WebIDL type: float
     */
    var mMaxCompression: Float
    /**
     * WebIDL type: float
     */
    var mMaxDroop: Float
    /**
     * WebIDL type: float
     */
    var mSprungMass: Float
    /**
     * WebIDL type: float
     */
    var mCamberAtRest: Float
    /**
     * WebIDL type: float
     */
    var mCamberAtMaxCompression: Float
    /**
     * WebIDL type: float
     */
    var mCamberAtMaxDroop: Float

    /**
     * @param newSprungMass WebIDL type: float
     */
    fun setMassAndPreserveNaturalFrequency(newSprungMass: Float)

}

fun PxVehicleSuspensionData(): PxVehicleSuspensionData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleSuspensionData()")
}

external interface PxVehicleTireData {
    /**
     * WebIDL type: float
     */
    var mLatStiffX: Float
    /**
     * WebIDL type: float
     */
    var mLatStiffY: Float
    /**
     * WebIDL type: float
     */
    var mLongitudinalStiffnessPerUnitGravity: Float
    /**
     * WebIDL type: float
     */
    var mCamberStiffnessPerUnitGravity: Float
    /**
     * WebIDL type: unsigned long
     */
    var mType: Int
}

fun PxVehicleTireData(): PxVehicleTireData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleTireData()")
}

external interface PxVehicleTireLoadFilterData {
    /**
     * WebIDL type: float
     */
    var mMinNormalisedLoad: Float
    /**
     * WebIDL type: float
     */
    var mMinFilteredNormalisedLoad: Float
    /**
     * WebIDL type: float
     */
    var mMaxNormalisedLoad: Float
    /**
     * WebIDL type: float
     */
    var mMaxFilteredNormalisedLoad: Float

    /**
     * @return WebIDL type: float
     */
    fun getDenominator(): Float

}

fun PxVehicleTireLoadFilterData(): PxVehicleTireLoadFilterData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleTireLoadFilterData()")
}

external interface PxVehicleWheelData {
    /**
     * WebIDL type: float
     */
    var mRadius: Float
    /**
     * WebIDL type: float
     */
    var mWidth: Float
    /**
     * WebIDL type: float
     */
    var mMass: Float
    /**
     * WebIDL type: float
     */
    var mMOI: Float
    /**
     * WebIDL type: float
     */
    var mDampingRate: Float
    /**
     * WebIDL type: float
     */
    var mMaxBrakeTorque: Float
    /**
     * WebIDL type: float
     */
    var mMaxHandBrakeTorque: Float
    /**
     * WebIDL type: float
     */
    var mMaxSteer: Float
    /**
     * WebIDL type: float
     */
    var mToeAngle: Float
}

fun PxVehicleWheelData(): PxVehicleWheelData {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleWheelData()")
}

external interface PxVehicleWheelQueryResult {
    /**
     * WebIDL type: [PxWheelQueryResult]
     */
    var wheelQueryResults: PxWheelQueryResult
    /**
     * WebIDL type: unsigned long
     */
    var nbWheelQueryResults: Int
}

fun PxVehicleWheelQueryResult(): PxVehicleWheelQueryResult {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleWheelQueryResult()")
}

external interface PxVehicleWheels : PxBase {
    /**
     * WebIDL type: [PxVehicleWheelsSimData] (Value)
     */
    var mWheelsSimData: PxVehicleWheelsSimData
    /**
     * WebIDL type: [PxVehicleWheelsDynData] (Value)
     */
    var mWheelsDynData: PxVehicleWheelsDynData

    /**
     * @return WebIDL type: unsigned long
     */
    fun getVehicleType(): Int

    /**
     * @return WebIDL type: [PxRigidDynamic]
     */
    fun getRigidDynamicActor(): PxRigidDynamic

    /**
     * @return WebIDL type: float
     */
    fun computeForwardSpeed(): Float

    /**
     * @return WebIDL type: float
     */
    fun computeSidewaysSpeed(): Float

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbNonDrivenWheels(): Int

}

external interface PxVehicleWheelsDynData {
    fun setToRestState()

    /**
     * @param wheelIdx WebIDL type: unsigned long
     * @param speed    WebIDL type: float
     */
    fun setWheelRotationSpeed(wheelIdx: Int, speed: Float)

    /**
     * @param wheelIdx WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getWheelRotationSpeed(wheelIdx: Int): Float

    /**
     * @param wheelIdx WebIDL type: unsigned long
     * @param angle    WebIDL type: float
     */
    fun setWheelRotationAngle(wheelIdx: Int, angle: Float)

    /**
     * @param wheelIdx WebIDL type: unsigned long
     * @return WebIDL type: float
     */
    fun getWheelRotationAngle(wheelIdx: Int): Float

    /**
     * @param src      WebIDL type: [PxVehicleWheelsDynData] (Const, Ref)
     * @param srcWheel WebIDL type: unsigned long
     * @param trgWheel WebIDL type: unsigned long
     */
    fun copy(src: PxVehicleWheelsDynData, srcWheel: Int, trgWheel: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelRotationSpeed(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelRotationAngle(): Int

}

external interface PxVehicleWheelsSimData {
    /**
     * @param nbWheels WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleWheelsSimData]
     */
    fun allocate(nbWheels: Int): PxVehicleWheelsSimData

    /**
     * @param chassisMass WebIDL type: float
     */
    fun setChassisMass(chassisMass: Float)

    fun free()

    /**
     * @param src      WebIDL type: [PxVehicleWheelsSimData] (Const, Ref)
     * @param srcWheel WebIDL type: unsigned long
     * @param trgWheel WebIDL type: unsigned long
     */
    fun copy(src: PxVehicleWheelsSimData, srcWheel: Int, trgWheel: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheels(): Int

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleSuspensionData] (Const, Ref)
     */
    fun getSuspensionData(id: Int): PxVehicleSuspensionData

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleWheelData] (Const, Ref)
     */
    fun getWheelData(id: Int): PxVehicleWheelData

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleTireData] (Const, Ref)
     */
    fun getTireData(id: Int): PxVehicleTireData

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVec3] (Const, Ref)
     */
    fun getSuspTravelDirection(id: Int): PxVec3

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVec3] (Const, Ref)
     */
    fun getSuspForceAppPointOffset(id: Int): PxVec3

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVec3] (Const, Ref)
     */
    fun getTireForceAppPointOffset(id: Int): PxVec3

    /**
     * @param id WebIDL type: unsigned long
     * @return WebIDL type: [PxVec3] (Const, Ref)
     */
    fun getWheelCentreOffset(id: Int): PxVec3

    /**
     * @param wheelId WebIDL type: unsigned long
     * @return WebIDL type: long
     */
    fun getWheelShapeMapping(wheelId: Int): Int

    /**
     * @param suspId WebIDL type: unsigned long
     * @return WebIDL type: [PxFilterData] (Const, Ref)
     */
    fun getSceneQueryFilterData(suspId: Int): PxFilterData

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbAntiRollBars(): Int

    /**
     * @param antiRollId WebIDL type: unsigned long
     * @return WebIDL type: [PxVehicleAntiRollBarData] (Const, Ref)
     */
    fun getAntiRollBarData(antiRollId: Int): PxVehicleAntiRollBarData

    /**
     * @return WebIDL type: [PxVehicleTireLoadFilterData] (Const, Ref)
     */
    fun getTireLoadFilterData(): PxVehicleTireLoadFilterData

    /**
     * @param id   WebIDL type: unsigned long
     * @param susp WebIDL type: [PxVehicleSuspensionData] (Const, Ref)
     */
    fun setSuspensionData(id: Int, susp: PxVehicleSuspensionData)

    /**
     * @param id    WebIDL type: unsigned long
     * @param wheel WebIDL type: [PxVehicleWheelData] (Const, Ref)
     */
    fun setWheelData(id: Int, wheel: PxVehicleWheelData)

    /**
     * @param id   WebIDL type: unsigned long
     * @param tire WebIDL type: [PxVehicleTireData] (Const, Ref)
     */
    fun setTireData(id: Int, tire: PxVehicleTireData)

    /**
     * @param id  WebIDL type: unsigned long
     * @param dir WebIDL type: [PxVec3] (Const, Ref)
     */
    fun setSuspTravelDirection(id: Int, dir: PxVec3)

    /**
     * @param id     WebIDL type: unsigned long
     * @param offset WebIDL type: [PxVec3] (Const, Ref)
     */
    fun setSuspForceAppPointOffset(id: Int, offset: PxVec3)

    /**
     * @param id     WebIDL type: unsigned long
     * @param offset WebIDL type: [PxVec3] (Const, Ref)
     */
    fun setTireForceAppPointOffset(id: Int, offset: PxVec3)

    /**
     * @param id     WebIDL type: unsigned long
     * @param offset WebIDL type: [PxVec3] (Const, Ref)
     */
    fun setWheelCentreOffset(id: Int, offset: PxVec3)

    /**
     * @param wheelId WebIDL type: unsigned long
     * @param shapeId WebIDL type: long
     */
    fun setWheelShapeMapping(wheelId: Int, shapeId: Int)

    /**
     * @param suspId       WebIDL type: unsigned long
     * @param sqFilterData WebIDL type: [PxFilterData] (Const, Ref)
     */
    fun setSceneQueryFilterData(suspId: Int, sqFilterData: PxFilterData)

    /**
     * @param tireLoadFilter WebIDL type: [PxVehicleTireLoadFilterData] (Const, Ref)
     */
    fun setTireLoadFilterData(tireLoadFilter: PxVehicleTireLoadFilterData)

    /**
     * @param antiRoll WebIDL type: [PxVehicleAntiRollBarData] (Const, Ref)
     * @return WebIDL type: unsigned long
     */
    fun addAntiRollBarData(antiRoll: PxVehicleAntiRollBarData): Int

    /**
     * @param wheel WebIDL type: unsigned long
     */
    fun disableWheel(wheel: Int)

    /**
     * @param wheel WebIDL type: unsigned long
     */
    fun enableWheel(wheel: Int)

    /**
     * @param wheel WebIDL type: unsigned long
     * @return WebIDL type: boolean
     */
    fun getIsWheelDisabled(wheel: Int): Boolean

    /**
     * @param thresholdLongitudinalSpeed   WebIDL type: float
     * @param lowForwardSpeedSubStepCount  WebIDL type: unsigned long
     * @param highForwardSpeedSubStepCount WebIDL type: unsigned long
     */
    fun setSubStepCount(thresholdLongitudinalSpeed: Float, lowForwardSpeedSubStepCount: Int, highForwardSpeedSubStepCount: Int)

    /**
     * @param minLongSlipDenominator WebIDL type: float
     */
    fun setMinLongSlipDenominator(minLongSlipDenominator: Float)

    /**
     * @param flags WebIDL type: [PxVehicleWheelsSimFlags] (Ref)
     */
    fun setFlags(flags: PxVehicleWheelsSimFlags)

    /**
     * @return WebIDL type: [PxVehicleWheelsSimFlags] (Value)
     */
    fun getFlags(): PxVehicleWheelsSimFlags

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheels4(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbSuspensionData(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelData(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbSuspTravelDirection(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbTireData(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbSuspForceAppPointOffset(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbTireForceAppPointOffset(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelCentreOffset(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelShapeMapping(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbSceneQueryFilterData(): Int

    /**
     * @return WebIDL type: float
     */
    fun getMinLongSlipDenominator(): Float

    /**
     * @param f WebIDL type: float
     */
    fun setThresholdLongSpeed(f: Float)

    /**
     * @return WebIDL type: float
     */
    fun getThresholdLongSpeed(): Float

    /**
     * @param f WebIDL type: unsigned long
     */
    fun setLowForwardSpeedSubStepCount(f: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getLowForwardSpeedSubStepCount(): Int

    /**
     * @param f WebIDL type: unsigned long
     */
    fun setHighForwardSpeedSubStepCount(f: Int)

    /**
     * @return WebIDL type: unsigned long
     */
    fun getHighForwardSpeedSubStepCount(): Int

    /**
     * @param wheel WebIDL type: unsigned long
     * @param state WebIDL type: boolean
     */
    fun setWheelEnabledState(wheel: Int, state: Boolean)

    /**
     * @param wheel WebIDL type: unsigned long
     * @return WebIDL type: boolean
     */
    fun getWheelEnabledState(wheel: Int): Boolean

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbWheelEnabledState(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbAntiRollBars4(): Int

    /**
     * @return WebIDL type: unsigned long
     */
    fun getNbAntiRollBarData(): Int

    /**
     * @param id       WebIDL type: unsigned long
     * @param antiRoll WebIDL type: [PxVehicleAntiRollBarData] (Const, Ref)
     */
    fun setAntiRollBarData(id: Int, antiRoll: PxVehicleAntiRollBarData)

}

external interface PxVehicleWheelsSimFlags {
    /**
     * @param flag WebIDL type: [PxVehicleWheelsSimFlagEnum] (enum)
     * @return WebIDL type: boolean
     */
    fun isSet(flag: Int): Boolean

    /**
     * @param flag WebIDL type: [PxVehicleWheelsSimFlagEnum] (enum)
     */
    fun set(flag: Int)

    /**
     * @param flag WebIDL type: [PxVehicleWheelsSimFlagEnum] (enum)
     */
    fun clear(flag: Int)

}

/**
 * @param flags WebIDL type: unsigned long
 */
fun PxVehicleWheelsSimFlags(flags: Int): PxVehicleWheelsSimFlags {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxVehicleWheelsSimFlags(flags)")
}

external interface PxWheelQueryResult {
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var suspLineStart: PxVec3
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var suspLineDir: PxVec3
    /**
     * WebIDL type: float
     */
    var suspLineLength: Float
    /**
     * WebIDL type: boolean
     */
    var isInAir: Boolean
    /**
     * WebIDL type: [PxActor]
     */
    var tireContactActor: PxActor
    /**
     * WebIDL type: [PxShape]
     */
    var tireContactShape: PxShape
    /**
     * WebIDL type: [PxMaterial] (Const)
     */
    var tireSurfaceMaterial: PxMaterial
    /**
     * WebIDL type: unsigned long
     */
    var tireSurfaceType: Int
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var tireContactPoint: PxVec3
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var tireContactNormal: PxVec3
    /**
     * WebIDL type: float
     */
    var tireFriction: Float
    /**
     * WebIDL type: float
     */
    var suspJounce: Float
    /**
     * WebIDL type: float
     */
    var suspSpringForce: Float
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var tireLongitudinalDir: PxVec3
    /**
     * WebIDL type: [PxVec3] (Value)
     */
    var tireLateralDir: PxVec3
    /**
     * WebIDL type: float
     */
    var longitudinalSlip: Float
    /**
     * WebIDL type: float
     */
    var lateralSlip: Float
    /**
     * WebIDL type: float
     */
    var steerAngle: Float
    /**
     * WebIDL type: [PxTransform] (Value)
     */
    var localPose: PxTransform
}

fun PxWheelQueryResult(): PxWheelQueryResult {
    val module = PhysXJsLoader.physXJs
    return js("new module.PxWheelQueryResult()")
}

object PxVehicleClutchAccuracyModeEnum {
    val eESTIMATE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleClutchAccuracyModeEnum_eESTIMATE()
    val eBEST_POSSIBLE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleClutchAccuracyModeEnum_eBEST_POSSIBLE()
}

object PxVehicleDifferential4WDataEnum {
    val eDIFF_TYPE_LS_4WD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_LS_4WD()
    val eDIFF_TYPE_LS_FRONTWD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_LS_FRONTWD()
    val eDIFF_TYPE_LS_REARWD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_LS_REARWD()
    val eDIFF_TYPE_OPEN_4WD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_OPEN_4WD()
    val eDIFF_TYPE_OPEN_FRONTWD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_OPEN_FRONTWD()
    val eDIFF_TYPE_OPEN_REARWD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eDIFF_TYPE_OPEN_REARWD()
    val eMAX_NB_DIFF_TYPES: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDifferential4WDataEnum_eMAX_NB_DIFF_TYPES()
}

object PxVehicleDrive4WControlEnum {
    val eANALOG_INPUT_ACCEL: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eANALOG_INPUT_ACCEL()
    val eANALOG_INPUT_BRAKE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eANALOG_INPUT_BRAKE()
    val eANALOG_INPUT_HANDBRAKE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eANALOG_INPUT_HANDBRAKE()
    val eANALOG_INPUT_STEER_LEFT: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eANALOG_INPUT_STEER_LEFT()
    val eANALOG_INPUT_STEER_RIGHT: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eANALOG_INPUT_STEER_RIGHT()
    val eMAX_NB_DRIVE4W_ANALOG_INPUTS: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleDrive4WControlEnum_eMAX_NB_DRIVE4W_ANALOG_INPUTS()
}

object PxVehicleGearEnum {
    val eREVERSE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eREVERSE()
    val eNEUTRAL: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eNEUTRAL()
    val eFIRST: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eFIRST()
    val eSECOND: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eSECOND()
    val eTHIRD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTHIRD()
    val eFOURTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eFOURTH()
    val eFIFTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eFIFTH()
    val eSIXTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eSIXTH()
    val eSEVENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eSEVENTH()
    val eEIGHTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eEIGHTH()
    val eNINTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eNINTH()
    val eTENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTENTH()
    val eELEVENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eELEVENTH()
    val eTWELFTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWELFTH()
    val eTHIRTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTHIRTEENTH()
    val eFOURTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eFOURTEENTH()
    val eFIFTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eFIFTEENTH()
    val eSIXTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eSIXTEENTH()
    val eSEVENTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eSEVENTEENTH()
    val eEIGHTEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eEIGHTEENTH()
    val eNINETEENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eNINETEENTH()
    val eTWENTIETH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTIETH()
    val eTWENTYFIRST: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYFIRST()
    val eTWENTYSECOND: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYSECOND()
    val eTWENTYTHIRD: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYTHIRD()
    val eTWENTYFOURTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYFOURTH()
    val eTWENTYFIFTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYFIFTH()
    val eTWENTYSIXTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYSIXTH()
    val eTWENTYSEVENTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYSEVENTH()
    val eTWENTYEIGHTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYEIGHTH()
    val eTWENTYNINTH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTWENTYNINTH()
    val eTHIRTIETH: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eTHIRTIETH()
    val eGEARSRATIO_COUNT: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleGearEnum_eGEARSRATIO_COUNT()
}

object PxVehicleUpdateModeEnum {
    val eVELOCITY_CHANGE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleUpdateModeEnum_eVELOCITY_CHANGE()
    val eACCELERATION: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleUpdateModeEnum_eACCELERATION()
}

object PxVehicleWheelsSimFlagEnum {
    val eLIMIT_SUSPENSION_EXPANSION_VELOCITY: Int get() = PhysXJsLoader.physXJs._emscripten_enum_PxVehicleWheelsSimFlagEnum_eLIMIT_SUSPENSION_EXPANSION_VELOCITY()
}

object VehicleSurfaceTypeMask {
    val DRIVABLE_SURFACE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_VehicleSurfaceTypeMask_DRIVABLE_SURFACE()
    val UNDRIVABLE_SURFACE: Int get() = PhysXJsLoader.physXJs._emscripten_enum_VehicleSurfaceTypeMask_UNDRIVABLE_SURFACE()
}

