package com.example.localkeyhook;

import android.app.Application;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class MyApp extends Application {

	private static final String TAG = MyApp.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();
		LocalKeyHook.init(this, new LocalKeyHook.Handler() {
			@Override
			public void onPreKeyEvent(View view, KeyEvent event) {
				Log.i(TAG, "onPreKeyEvent event=" + event);
			}

			@Override
			public boolean onKeyEvent(View view, KeyEvent event) {
				Log.i(TAG, "onKeyEvent event=" + event);
				if (event instanceof KeyEvent && ((KeyEvent) event).getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
					Log.e(TAG, "hooked!!!");
					return true;
				}
				return false;
			}
		});
	}
}
