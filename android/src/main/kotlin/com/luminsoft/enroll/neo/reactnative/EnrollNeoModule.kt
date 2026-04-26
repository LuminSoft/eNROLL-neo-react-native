package com.luminsoft.enroll.neo.reactnative

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.luminsoft.enroll_sdk.core.models.EnrollCallback
import com.luminsoft.enroll_sdk.core.models.EnrollEnvironment
import com.luminsoft.enroll_sdk.core.models.EnrollFailedModel
import com.luminsoft.enroll_sdk.core.models.EnrollForcedDocumentType
import com.luminsoft.enroll_sdk.core.models.EnrollMode
import com.luminsoft.enroll_sdk.core.models.EnrollSuccessModel
import com.luminsoft.enroll_sdk.core.models.LocalizationCode
import com.luminsoft.enroll_sdk.main.main_data.main_models.get_onboaring_configurations.EkycStepType
import com.luminsoft.enroll_sdk.sdk.eNROLL
import com.luminsoft.enroll_sdk.ui_components.theme.AppColors

class EnrollNeoModule internal constructor(context: ReactApplicationContext) :
    EnrollNeoSpec(context) {

    override fun getName(): String = NAME

    @Volatile
    private var isFlowInProgress = false
    private var listenerCount = 0

    @ReactMethod
    fun addListener(eventName: String) {
        listenerCount++
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        listenerCount -= count
        if (listenerCount < 0) listenerCount = 0
    }

    // ------------------------------------------------------------------
    // Plugin method exposed to JavaScript
    // ------------------------------------------------------------------

    @ReactMethod
    override fun startEnroll(options: ReadableMap, promise: Promise) {
        if (isFlowInProgress) {
            promise.reject("FLOW_IN_PROGRESS", "An enrollment flow is already in progress")
            return
        }

        val currentActivity = currentActivity
        if (currentActivity == null) {
            promise.reject("ACTIVITY_ERROR", "Activity is not available")
            return
        }

        // ---- Required parameters ----
        val tenantId = options.getString("tenantId")
        if (tenantId.isNullOrEmpty()) {
            promise.reject("INVALID_ARGUMENT", "tenantId is required")
            return
        }

        val tenantSecret = options.getString("tenantSecret")
        if (tenantSecret.isNullOrEmpty()) {
            promise.reject("INVALID_ARGUMENT", "tenantSecret is required")
            return
        }

        val enrollModeStr = options.getString("enrollMode")
        if (enrollModeStr.isNullOrEmpty()) {
            promise.reject("INVALID_ARGUMENT", "enrollMode is required")
            return
        }

        val enrollMode = parseEnrollMode(enrollModeStr)
        if (enrollMode == null) {
            promise.reject("INVALID_ARGUMENT", "Invalid enrollMode: $enrollModeStr")
            return
        }

        // ---- Conditionally required parameters ----
        val applicantId = options.getString("applicantId") ?: ""
        val levelOfTrust = options.getString("levelOfTrust") ?: ""
        val templateId = options.getString("templateId") ?: ""

        if (enrollMode == EnrollMode.AUTH) {
            if (applicantId.isEmpty()) {
                promise.reject("INVALID_ARGUMENT", "applicantId is required for auth mode")
                return
            }
            if (levelOfTrust.isEmpty()) {
                promise.reject("INVALID_ARGUMENT", "levelOfTrust is required for auth mode")
                return
            }
        }

        if (enrollMode == EnrollMode.SIGN_CONTRACT) {
            if (templateId.isEmpty()) {
                promise.reject("INVALID_ARGUMENT", "templateId is required for signContract mode")
                return
            }
        }

        // ---- Optional parameters ----
        val enrollEnvironment = parseEnrollEnvironment(options.getString("enrollEnvironment"))
        val localizationCode = parseLocalizationCode(options.getString("localizationCode"))
        val googleApiKey = options.getString("googleApiKey") ?: ""
        val skipTutorial = if (options.hasKey("skipTutorial")) options.getBoolean("skipTutorial") else false
        val correlationId = options.getString("correlationId") ?: ""
        val requestId = options.getString("requestId") ?: ""
        val contractParameters = options.getString("contractParameters") ?: ""
        val enrollForcedDocumentType = parseEnrollForcedDocumentType(options.getString("enrollForcedDocumentType"))
        val exitStep = parseExitStep(options.getString("enrollExitStep"))

        // ---- Colors ----
        val defaultAppColors = AppColors(
            primary = Color(0xFF1D56B8),
            secondary = Color(0xFF5791DB.toInt()),
            backGround = Color(0xFFFFFFFF),
            textColor = Color(0xFF004194.toInt()),
            errorColor = Color(0xFFDB305B),
            successColor = Color(0xFF61CC3D.toInt()),
            warningColor = Color(0xFFF9D548),
            white = Color(0xFFFFFFFF),
            appBlack = Color(0xFF333333)
        )

        val appColors = if (options.hasKey("enrollColors") && options.getMap("enrollColors") != null) {
            parseEnrollColors(options.getMap("enrollColors")!!, defaultAppColors)
        } else {
            defaultAppColors
        }

        // ---- Launch the SDK ----
        isFlowInProgress = true

        try {
            eNROLL.init(
                tenantId,
                tenantSecret,
                applicantId,
                levelOfTrust,
                enrollMode,
                enrollEnvironment,
                localizationCode = localizationCode,
                enrollCallback = object : EnrollCallback {
                    override fun success(enrollSuccessModel: EnrollSuccessModel) {
                        Log.d(TAG, "eNROLL success: ${enrollSuccessModel.enrollMessage}")
                        isFlowInProgress = false

                        val result = Arguments.createMap()
                        result.putString("applicantId", enrollSuccessModel.applicantId ?: "")
                        result.putString("enrollMessage", enrollSuccessModel.enrollMessage)
                        result.putString("documentId", enrollSuccessModel.documentId)
                        result.putString("requestId", enrollSuccessModel.requestId)
                        result.putBoolean("exitStepCompleted", enrollSuccessModel.exitStepCompleted)
                        result.putString("completedStepName", enrollSuccessModel.completedStepName)
                        promise.resolve(result)
                    }

                    override fun error(enrollFailedModel: EnrollFailedModel) {
                        Log.e(TAG, "eNROLL error: ${enrollFailedModel.failureMessage}")
                        isFlowInProgress = false
                        promise.reject(
                            "ENROLL_ERROR",
                            enrollFailedModel.failureMessage,
                            null as Throwable?
                        )
                    }

                    override fun getRequestId(rid: String) {
                        Log.d(TAG, "eNROLL requestId: $rid")
                        val params = Arguments.createMap()
                        params.putString("requestId", rid)
                        sendEvent("onRequestId", params)
                    }
                },
                googleApiKey = googleApiKey,
                skipTutorial = skipTutorial,
                correlationId = correlationId,
                appColors = appColors,
                enrollForcedDocumentType = enrollForcedDocumentType,
                requestId = requestId,
                templateId = templateId,
                contractParameters = contractParameters,
                exitStep = exitStep
            )

            eNROLL.launch(currentActivity)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting enrollment: ${e.message}", e)
            isFlowInProgress = false
            promise.reject("ENROLL_LAUNCH_ERROR", "Failed to start enrollment: ${e.message}", e)
        }
    }

    // ------------------------------------------------------------------
    // Event emitter
    // ------------------------------------------------------------------

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    // ------------------------------------------------------------------
    // Enum parsers
    // ------------------------------------------------------------------

    private fun parseEnrollMode(mode: String?): EnrollMode? {
        return when (mode) {
            "onboarding" -> EnrollMode.ONBOARDING
            "auth" -> EnrollMode.AUTH
            "update" -> EnrollMode.UPDATE
            "signContract" -> EnrollMode.SIGN_CONTRACT
            "forgetProfileData" -> EnrollMode.FORGET_PROFILE_DATA
            else -> null
        }
    }

    private fun parseEnrollEnvironment(env: String?): EnrollEnvironment {
        return when (env) {
            "production" -> EnrollEnvironment.PRODUCTION
            else -> EnrollEnvironment.STAGING
        }
    }

    private fun parseLocalizationCode(code: String?): LocalizationCode {
        return when (code) {
            "ar" -> LocalizationCode.AR
            else -> LocalizationCode.EN
        }
    }

    private fun parseEnrollForcedDocumentType(type: String?): EnrollForcedDocumentType {
        return when (type) {
            "nationalIdOnly" -> EnrollForcedDocumentType.NATIONAL_ID_ONLY
            "passportOnly" -> EnrollForcedDocumentType.PASSPORT_ONLY
            else -> EnrollForcedDocumentType.NATIONAL_ID_OR_PASSPORT
        }
    }

    private fun parseExitStep(step: String?): EkycStepType? {
        return when (step) {
            "phoneOtp" -> EkycStepType.PhoneOtp
            "personalConfirmation" -> EkycStepType.PersonalConfirmation
            "smileLiveness" -> EkycStepType.SmileLiveness
            "emailOtp" -> EkycStepType.EmailOtp
            "saveMobileDevice" -> EkycStepType.SaveMobileDevice
            "deviceLocation" -> EkycStepType.DeviceLocation
            "password" -> EkycStepType.SettingPassword
            "securityQuestions" -> EkycStepType.SecurityQuestions
            "amlCheck" -> EkycStepType.AmlCheck
            "termsAndConditions" -> EkycStepType.TermsConditions
            "electronicSignature" -> EkycStepType.ElectronicSignature
            "ntraCheck" -> EkycStepType.NtraCheck
            "csoCheck" -> EkycStepType.CsoCheck
            else -> null
        }
    }

    // ------------------------------------------------------------------
    // Color parsing
    // ------------------------------------------------------------------

    private fun parseEnrollColors(colorsMap: ReadableMap, defaults: AppColors): AppColors {
        return AppColors(
            primary = parseSingleColor(colorsMap, "primary") ?: defaults.primary,
            secondary = parseSingleColor(colorsMap, "secondary") ?: defaults.secondary,
            backGround = parseSingleColor(colorsMap, "appBackgroundColor") ?: defaults.backGround,
            textColor = parseSingleColor(colorsMap, "textColor") ?: defaults.textColor,
            errorColor = parseSingleColor(colorsMap, "errorColor") ?: defaults.errorColor,
            successColor = parseSingleColor(colorsMap, "successColor") ?: defaults.successColor,
            warningColor = parseSingleColor(colorsMap, "warningColor") ?: defaults.warningColor,
            white = parseSingleColor(colorsMap, "appWhite") ?: defaults.white,
            appBlack = parseSingleColor(colorsMap, "appBlack") ?: defaults.appBlack
        )
    }

    private fun parseSingleColor(parentMap: ReadableMap, key: String): Color? {
        if (!parentMap.hasKey(key)) return null
        val map = parentMap.getMap(key) ?: return null
        val r = if (map.hasKey("r")) map.getInt("r") else return null
        val g = if (map.hasKey("g")) map.getInt("g") else return null
        val b = if (map.hasKey("b")) map.getInt("b") else return null
        val opacity = if (map.hasKey("opacity")) map.getDouble("opacity") else 1.0
        return Color(
            red = r / 255f,
            green = g / 255f,
            blue = b / 255f,
            alpha = opacity.toFloat()
        )
    }

    companion object {
        const val NAME = "EnrollNeoReactNative"
        private const val TAG = "EnrollNeoReactNative"
    }
}
