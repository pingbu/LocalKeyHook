package com.example.localkeyhook;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;

public final class LocalKeyHook {

	protected static final String TAG = LocalKeyHook.class.getSimpleName();

	public interface Handler {
		public void onPreKeyEvent(View view, KeyEvent event);

		public boolean onKeyEvent(View view, KeyEvent event);
	}

	private static Handler sHandler;
	private static Object sWindowSession;

	private static class FallbackEventHandlerProxy implements InvocationHandler {

		private View mView;
		private Object mFallbackEventHandler;

		public FallbackEventHandlerProxy(Object fallbackEventHandler) {
			mFallbackEventHandler = fallbackEventHandler;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("setView")) {
				mView = (View) args[0];
			} else if (method.getName().equals("preDispatchKeyEvent")) {
				KeyEvent event = (KeyEvent) args[0];
				sHandler.onPreKeyEvent(mView, event);
			} else if (method.getName().equals("dispatchKeyEvent")) {
				KeyEvent event = (KeyEvent) args[0];
				if (sHandler.onKeyEvent(mView, event))
					return true;
			}
			return method.invoke(mFallbackEventHandler, args);
		}
	}

	private static InvocationHandler sWindowSessionHook = new InvocationHandler() {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().startsWith("add")
					&& method.getParameterTypes()[args.length - 1].getSimpleName().equals("InputChannel")) {
				Utils.logD(TAG, "invoke sWindowSession.%s", method.getName());

				try {
					Object window = args[0];

					Field f = window.getClass().getDeclaredField("mViewAncestor");
					f.setAccessible(true);
					WeakReference<?> viewRootImplRef = (WeakReference<?>) f.get(window);

					Object viewRootImpl = viewRootImplRef.get();

					f = viewRootImpl.getClass().getDeclaredField("mFallbackEventHandler");
					f.setAccessible(true);
					f.set(viewRootImpl,
							Proxy.newProxyInstance(getClass().getClassLoader(),
									new Class<?>[] { Class.forName("android.view.FallbackEventHandler") },
									new FallbackEventHandlerProxy(f.get(viewRootImpl))));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return method.invoke(sWindowSession, args);
		}
	};

	public static boolean init(Context context, Handler handler) {
		try {
			Class<?> wmGlobalClass = Class.forName("android.view.WindowManagerGlobal");
			Field sWindowSessionFiled = wmGlobalClass.getDeclaredField("sWindowSession");
			sWindowSessionFiled.setAccessible(true);

			sWindowSession = sWindowSessionFiled.get(null);
			if (sWindowSession != null)
				throw new Exception(TAG + " must be init once while app create");

			sHandler = handler;
			try {
				Method m = wmGlobalClass.getMethod("getWindowSession");
				sWindowSession = m.invoke(null);
			} catch (NoSuchMethodException nsme) {
				Method m = wmGlobalClass.getMethod("getWindowSession", Looper.class);
				sWindowSession = m.invoke(null, Looper.myLooper());
			}
			sWindowSessionFiled.set(null, Proxy.newProxyInstance(context.getClassLoader(),
					new Class<?>[] { Class.forName("android.view.IWindowSession") }, sWindowSessionHook));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
