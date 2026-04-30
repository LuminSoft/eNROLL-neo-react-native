package com.luminsoft.enroll.neo.reactnative

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.module.model.ReactModuleInfo

class EnrollNeoPackage : TurboReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
        return if (name == EnrollNeoModule.NAME) {
            EnrollNeoModule(reactContext)
        } else {
            null
        }
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        return ReactModuleInfoProvider {
            val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
            moduleInfos[EnrollNeoModule.NAME] = ReactModuleInfo(
                EnrollNeoModule.NAME,
                EnrollNeoModule.NAME,
                false,  // canOverrideExistingModule
                false,  // needsEagerInit
                false,  // isCxxModule
                BuildConfig.IS_NEW_ARCHITECTURE_ENABLED   // isTurboModule
            )
            moduleInfos
        }
    }
}
