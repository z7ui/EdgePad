package com.z7ui.edgepad;

import android.app.Application;

/** 只做一件事：装上崩溃留档。 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Crashes.install(this);
    }
}
