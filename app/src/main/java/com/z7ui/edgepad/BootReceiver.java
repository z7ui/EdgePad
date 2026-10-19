package com.z7ui.edgepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** 开机或覆盖安装后把手柄重新拉起来。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String a = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            return;
        }
        boolean any = Config.sideOn(context, Config.LEFT) || Config.sideOn(context, Config.RIGHT);
        EdgeMode.ensure(context);
        if (Settings.canDrawOverlays(context) && any) {
            EdgeService.refresh(context);
        }
    }
}
