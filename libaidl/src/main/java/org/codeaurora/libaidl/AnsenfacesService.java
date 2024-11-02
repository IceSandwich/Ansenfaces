package org.codeaurora.libaidl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

public class AnsenfacesService {
    private static final String TAG = AnsenfacesService.class.getSimpleName();

    public static boolean Bind(Context context, ServiceConnection connection) {
        String[] packageNames = {
                "org.codeaurora.snapcam"
        };
        String ServiceClass = ".CameraService";

        boolean success = false;
        for (String packageName : packageNames) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    packageName,
                    packageName + ServiceClass
            ));
            if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE) == true) {
                success = true;
                break;
            }

            Log.e(TAG, "Bind: Try " + packageName + " but failed.");
        }

        return success;
    }
}
