package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.svd.svdagencies.R;
import com.svd.svdagencies.ui.ToolsActivity;
import com.svd.svdagencies.data.api.auth.ApiClient;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,application=Application.class,manifest=Config.NONE,packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SettingsScreenTest {
    private View text(View view,String value) {
        if(view instanceof TextView && value.contentEquals(((TextView)view).getText()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            View found=text(((ViewGroup)view).getChildAt(i),value);if(found!=null)return found;
        }
        return null;
    }
    private void settle(okhttp3.OkHttpClient client) throws Exception {
        for(int pass=0;pass<8;pass++){
            for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(10);
            ShadowLooper.idleMainLooper();
        }
    }
    @Test public void settingsRestrictsBusinessFieldsAndPostsRealDetails() throws Exception {
        Field appContext=com.svd.svdagencies.App.class.getDeclaredField("context");
        appContext.setAccessible(true);appContext.set(null,RuntimeEnvironment.getApplication());
        AtomicReference<String> saved=new AtomicReference<>();
        AtomicReference<String> savedPath=new AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean certificateUnavailable=new java.util.concurrent.atomic.AtomicBoolean();
        okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)ApiClient.INSTANCE.getRetrofit().callFactory();
        Field interceptors=okhttp3.OkHttpClient.class.getDeclaredField("interceptors");interceptors.setAccessible(true);
        interceptors.set(client,Collections.singletonList((okhttp3.Interceptor)chain->{
            if(chain.request().method().equals("POST")){
                savedPath.set(chain.request().url().encodedPath());
                okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);saved.set(buffer.readUtf8());
            }
            String json="{\"business\":{\"pan\":\"ABCDE1234F\",\"fssai\":\"20126111000019\",\"lin\":\"LIN-1\",\"trade_licence\":\"TL-1\",\"details\":\"\"},\"gst\":{\"gstin\":\"\",\"effective_from\":null,\"enabled\":false}}";
            int code=200;
            if(certificateUnavailable.get()) {
                if(chain.request().url().encodedPath().contains("business-settings")) {code=404;json="{}";}
                else json="{\"config\":{\"gstin\":\"\",\"effective_from\":null,\"enabled\":false}}";
            }
            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(code).message("Isolated settings fixture")
                .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"),json)).build();
        }));
        for(String role:new String[]{"customer","user","delivery","admin"}) {
            RuntimeEnvironment.getApplication().getSharedPreferences("svd_session",Context.MODE_PRIVATE).edit().clear()
                .putString("role",role).putString("token","isolated-settings-test").commit();
            ActivityController<ToolsActivity> controller=Robolectric.buildActivity(ToolsActivity.class).setup();
            try {
                settle(client);
                ViewGroup root=controller.get().findViewById(android.R.id.content);
                assertNull(text(root,"App tools"));assertNull(text(root,"Information & policies"));
                assertNotNull(root.findViewById(R.id.paletteChoices));
                LinearLayout business=root.findViewById(R.id.toolsList);
                if(!role.equals("admin")) {
                    assertEquals(View.GONE,business.getVisibility());assertEquals(0,business.getChildCount());
                    assertNull(text(root,"Save business details"));
                } else {
                    TextInputEditText pan=root.findViewById(R.id.businessPan);
                    assertEquals("ABCDE1234F",pan.getText().toString());
                    pan.setText("PQRST9876A");
                    View save=text(root,"Save business details");assertTrue(save.isEnabled());save.performClick();settle(client);
                    assertNotNull(saved.get());
                    org.json.JSONObject payload=new org.json.JSONObject(saved.get());
                    assertEquals("PQRST9876A",payload.getString("pan"));
                    assertEquals("/api/business-settings/",savedPath.get());
                    assertEquals("LIN-1",payload.getString("lin"));assertFalse(payload.has("gstin"));
                    assertNotNull(text(root,"Certificate details saved"));
                    View date=text(root,"Select date");assertNotNull(date);date.performClick();
                    android.app.DatePickerDialog picker=(android.app.DatePickerDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
                    picker.getDatePicker().updateDate(2026,9,26);
                    picker.onClick(picker,android.content.DialogInterface.BUTTON_POSITIVE);picker.dismiss();
                    text(root,"Save GST settings").performClick();settle(client);
                    payload=new org.json.JSONObject(saved.get());
                    assertEquals("/api/gst/settings/",savedPath.get());
                    assertEquals("2026-10-26",payload.getString("effective_from"));
                    assertFalse(payload.getBoolean("enabled"));assertFalse(payload.has("pan"));
                }
            } finally {controller.pause().stop().destroy();}
        }
        certificateUnavailable.set(true);
        ActivityController<ToolsActivity> fallback=Robolectric.buildActivity(ToolsActivity.class).setup();
        try {
            settle(client);
            View root=fallback.get().findViewById(android.R.id.content);
            assertFalse(text(root,"Save business details").isEnabled());
            assertTrue("Existing GST settings must remain usable before the certificate deployment",text(root,"Save GST settings").isEnabled());
        } finally {fallback.pause().stop().destroy();}
    }
}
