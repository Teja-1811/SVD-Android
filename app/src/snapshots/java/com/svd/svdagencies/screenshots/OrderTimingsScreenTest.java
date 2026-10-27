package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.svd.svdagencies.R;
import com.svd.svdagencies.ui.admin.AdminOrderTimingsActivity;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class OrderTimingsScreenTest {
    private void fixtures() throws Exception {
        Field context=com.svd.svdagencies.App.class.getDeclaredField("context");context.setAccessible(true);
        context.set(null,RuntimeEnvironment.getApplication());
        RuntimeEnvironment.getApplication().getSharedPreferences("svd_session",Context.MODE_PRIVATE).edit()
            .putString("role","admin").putString("token","local-screen-test").putInt("user_id",90001).commit();
        okhttp3.Interceptor fixture=chain -> {
            assertEquals("GET",chain.request().method());
            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"),
                "{\"success\":true,\"current_status\":\"Ordering is open\",\"settings\":{\"opens_at\":\"09:00\",\"closes_at\":\"16:30\",\"working_days\":[0,1,2,3,4,5,6],\"override\":\"schedule\",\"override_until\":null}}")).build();
        };
        retrofit2.Retrofit retrofit=com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit();
        Field interceptors=okhttp3.OkHttpClient.class.getDeclaredField("interceptors");interceptors.setAccessible(true);
        interceptors.set(retrofit.callFactory(),Collections.singletonList(fixture));
        for(retrofit2.CallAdapter.Factory factory:retrofit.callAdapterFactories()) {
            try {Field executor=factory.getClass().getDeclaredField("callbackExecutor");executor.setAccessible(true);
                executor.set(factory,(java.util.concurrent.Executor)command -> new android.os.Handler(android.os.Looper.getMainLooper()).post(command));
            } catch(NoSuchFieldException ignored) {}
        }
    }

    @Test public void timingButtonsStaySideBySideAndActionsWrapWithoutClipping() throws Exception {
        fixtures();
        for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390,600,840}) {
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<AdminOrderTimingsActivity> controller=Robolectric.buildActivity(AdminOrderTimingsActivity.class).setup();
            try {
                AdminOrderTimingsActivity activity=controller.get();
                okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit().callFactory();
                for(int pass=0;pass<4;pass++){for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(10);ShadowLooper.idleMainLooper();}
                View root=activity.findViewById(android.R.id.content);
                measure(root,width,1200);
                MaterialButton opening=button(activity,"openingButton"), closing=button(activity,"closingButton");
                assertTrue(button(activity,"saveButton").isEnabled());
                assertEquals("Opening\n09:00",opening.getText().toString());
                assertEquals("Closing\n16:30",closing.getText().toString());
                assertEquals(opening.getTop(),closing.getTop());assertTrue(opening.getRight()<=closing.getLeft());
                View fields=activity.findViewById(R.id.orderTimingsFields);
                List<MaterialButton> buttons=new ArrayList<>();collect(fields,buttons);
                assertEquals(6,buttons.size());
                for(MaterialButton button:buttons) {
                    assertTrue(button.getWidth()>=48);assertTrue(button.getHeight()>=48);
                    if(com.svd.svdagencies.utils.CompactUi.isRefreshControl(button)){assertEquals(48,button.getWidth());assertEquals(48,button.getHeight());}
                    assertTrue(button.getLeft()>=0);assertTrue(button.getRight()<=((View)button.getParent()).getWidth());
                    android.text.Layout layout=button.getLayout();
                    assertEquals(button.getText().length(),layout.getLineEnd(layout.getLineCount()-1));
                }
                String text=text(fields);
                assertFalse(text.contains("pre-orders"));assertFalse(text.contains("Closing earlier"));
                int height=Math.max(844,fields.getMeasuredHeight()+100);measure(root,width,height);
                Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
                File folder=new File(System.getProperty("portal.snapshot.dir"),"order-timings-review");folder.mkdirs();
                try(FileOutputStream output=new FileOutputStream(new File(folder,"timings-"+width+(night?"-dark":"-light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}
                bitmap.recycle();
            } finally {controller.pause().stop().destroy();}
        }
    }
    private MaterialButton button(AdminOrderTimingsActivity activity,String name) throws Exception {
        Field field=AdminOrderTimingsActivity.class.getDeclaredField(name);field.setAccessible(true);return (MaterialButton)field.get(activity);
    }
    private void measure(View root,int width,int height){root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);}
    private void collect(View view,List<MaterialButton> buttons){if(view instanceof MaterialButton)buttons.add((MaterialButton)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),buttons);}
    private String text(View view){StringBuilder value=new StringBuilder();if(view instanceof TextView)value.append(((TextView)view).getText());if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)value.append(text(((ViewGroup)view).getChildAt(i)));return value.toString();}
}
