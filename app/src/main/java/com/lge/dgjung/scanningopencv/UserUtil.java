package com.lge.dgjung.scanningopencv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class UserUtil {

    public static boolean hasPermissions(Context context, String[] permissions) {
        int result;
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(context, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
