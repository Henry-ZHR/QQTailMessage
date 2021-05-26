package moe.henry_zhr.qq_tail_message;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

@Keep
public class MainHook implements IXposedHookLoadPackage {
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    if (!"com.tencent.mobileqq".equals(lpparam.packageName))
      return;
    Class<?> baseBubbleBuilderClass =
        lpparam.classLoader.loadClass("com.tencent.mobileqq.activity.aio.BaseBubbleBuilder");
    Class<?> baseChatItemLayoutClass =
        lpparam.classLoader.loadClass("com.tencent.mobileqq.activity.aio.BaseChatItemLayout");
    Class<?> chatMessageClass =
        lpparam.classLoader.loadClass("com.tencent.mobileqq.data.ChatMessage");
    Method foundMethod = null;
    for (Method method : baseBubbleBuilderClass.getDeclaredMethods()) {
      if (method.getParameterTypes().length == 6 &&
          method.getParameterTypes()[0] == int.class &&
          method.getParameterTypes()[1] == int.class &&
          method.getParameterTypes()[2] == chatMessageClass &&
          method.getParameterTypes()[3] == View.class &&
          method.getParameterTypes()[4] == ViewGroup.class) {
        foundMethod = method;
      }
    }
    XposedBridge.hookMethod(foundMethod, new XC_MethodHook() {
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws
                                                              IllegalAccessException,
                                                              InvocationTargetException,
                                                              NoSuchFieldException,
                                                              NoSuchMethodException,
                                                              NullPointerException {
        Object chatMessage = param.args[2];
        long time = chatMessageClass.getField("time").getLong(chatMessage);
        String messageType = chatMessage.getClass().getSimpleName();
        if (messageType.startsWith("MessageFor")) {
          messageType = messageType.substring(10);
        }
//        String senderuin = (String) chatMessageClass.getField("senderuin").get(chatMessage);
        Object layout = param.getResult();
        Method setTailMessageMethod = baseChatItemLayoutClass.getMethod("setTailMessage",
                                                                        boolean.class,
                                                                        CharSequence.class,
                                                                        View.OnClickListener.class);
        setTailMessageMethod.invoke(layout, true, new SimpleDateFormat("MM-dd HH:mm:ss",
                                                                       Locale.SIMPLIFIED_CHINESE).format(
            new Date(time * 1000)) + " " + messageType, null);
      }
    });
  }
}
