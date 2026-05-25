package moe.kmi.noki

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class BaseHook: XposedModule() {
    companion object {
        const val TAG = "Noki"
    }


    @SuppressLint("PrivateApi")
    override fun onPackageReady(param: PackageReadyParam) {
        val nfcServiceClass = Class.forName("com.android.nfc.NfcService",
            true, param.classLoader)
        val nfcAdapterServiceClass = Class.forName("com.android.nfc.NfcService\$NfcAdapterService",
            true, param.classLoader)
        val iAppCallbackClass = Class.forName("android.nfc.IAppCallback",
            true, param.classLoader)
        val maybeDisconnectTargetMethod = nfcServiceClass.getDeclaredMethod("maybeDisconnectTarget")
        val setReaderModeMethod = nfcAdapterServiceClass.getDeclaredMethod("setReaderMode",
            IBinder::class.java, iAppCallbackClass,
            Int::class.java, Bundle::class.java
        )
        val notifyTagAbortMethod = nfcServiceClass.getDeclaredMethod("notifyTagAbort")

        val isInSetReaderMode = ThreadLocal<Boolean>()
        hook(setReaderModeMethod).intercept { chain ->
            isInSetReaderMode.set(true)
            chain.proceed()
            isInSetReaderMode.set(false)
        }

        hook(maybeDisconnectTargetMethod).intercept { chain ->
            if (isInSetReaderMode.get() == true) {
                log(Log.INFO, TAG, "Skipped maybeDisconnectTarget")
            } else { chain.proceed() }
        }

        hook(notifyTagAbortMethod).intercept { _ -> null }
    }
}
