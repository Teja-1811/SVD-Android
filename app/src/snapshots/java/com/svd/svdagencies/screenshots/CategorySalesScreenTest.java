package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.svd.svdagencies.R;
import com.svd.svdagencies.data.api.admin.CategorySalesResponse;
import com.svd.svdagencies.ui.admin.sales.CategorySalesActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
public class CategorySalesScreenTest {
    private volatile okhttp3.HttpUrl lastUrl;
    private final List<String> sampleCategories=Arrays.asList("Milk", "Curd", "Other dairy products with a longer category name");

    private void fixtures() throws Exception {
        Field context=com.svd.svdagencies.App.class.getDeclaredField("context");
        context.setAccessible(true);context.set(null,RuntimeEnvironment.getApplication());
        RuntimeEnvironment.getApplication().getSharedPreferences("svd_session",Context.MODE_PRIVATE).edit()
            .putString("role","admin").putString("token","local-screen-test").putInt("user_id",90001).commit();
        okhttp3.Interceptor fixture=chain -> {
            lastUrl=chain.request().url();
            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"),
                "{\"period\":\"year\",\"categories\":[\"Milk\",\"Curd\",\"Other dairy products with a longer category name\"],\"total_volume\":160.5,\"data_by_category\":{\"Milk\":120,\"Curd\":40.5},\"amount_by_category\":{\"Milk\":12345678.9,\"Curd\":2500,\"Other dairy products with a longer category name\":500}}")).build();
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

    private void awaitResponse() throws Exception {
        okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit().callFactory();
        for(int pass=0;pass<4;pass++) {
            for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(10);
            ShadowLooper.idleMainLooper();
        }
    }

    @Test public void filtersAndResultsRemainClearAcrossSizesAndAppearances() throws Exception {
        fixtures();
        for(boolean night:new boolean[]{false,true}) for(int width:new int[]{320,390,600,840}) {
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<CategorySalesActivity> controller=Robolectric.buildActivity(CategorySalesActivity.class).setup();
            try {
                CategorySalesActivity activity=controller.get();awaitResponse();
                View root=activity.findViewById(android.R.id.content);
                com.svd.svdagencies.utils.CompactUi.applyTree(root);
                assertTrue(text(root).contains("Sales amount"));assertTrue(text(root).contains("Volume sold"));
                assertTrue(text(root).contains("Milk"));assertEquals(View.GONE,activity.findViewById(R.id.tvSalesFilterHelp).getVisibility());
                Field date=CategorySalesActivity.class.getDeclaredField("selectedDate");date.setAccessible(true);
                ((Calendar)date.get(activity)).set(2026,Calendar.JANUARY,1);
                ((Spinner)activity.findViewById(R.id.spinnerPeriod)).setSelection(2);
                awaitResponse();
                assertEquals("2025-12-29",lastUrl.queryParameter("start_date"));
                assertEquals("2026-01-04",lastUrl.queryParameter("end_date"));
                assertEquals(View.GONE,activity.findViewById(R.id.tvSalesFilterHelp).getVisibility());
                capture(activity,root,width,"sales-"+width+(night?"-dark":"-light"));
                Method render=CategorySalesActivity.class.getDeclaredMethod("render",CategorySalesResponse.class);render.setAccessible(true);
                render.invoke(activity,new CategorySalesResponse("week",sampleCategories,Collections.emptyMap(),Collections.emptyMap(),0.0));
                assertTrue(text(root).contains("No sales in this period"));
                if(width==390)capture(activity,root,width,"empty"+(night?"-dark":"-light"));
                Method failure=CategorySalesActivity.class.getDeclaredMethod("showFailure",String.class);failure.setAccessible(true);
                failure.invoke(activity,"29 Dec 2025 – 04 Jan 2026");
                assertTrue(text(root).contains("Try again"));assertFalse(text(root).contains("No sales in this period"));
                if(width==390)capture(activity,root,width,"error"+(night?"-dark":"-light"));
            } finally {controller.pause().stop().destroy();}
        }
    }

    private String text(View view) {
        StringBuilder text=new StringBuilder();if(view instanceof TextView)text.append(((TextView)view).getText()).append('\n');
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)text.append(text(((ViewGroup)view).getChildAt(i)));
        return text.toString();
    }

    private void capture(CategorySalesActivity activity,View root,int width,String name) throws Exception {
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));
        root.layout(0,0,width,844);
        View content=activity.findViewById(R.id.categorySalesContent);
        int height=Math.min(6000,Math.max(844,content.getMeasuredHeight()+720));
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        root.layout(0,0,width,height);
        assertMoneyReadable(root);
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
        File folder=new File(System.getProperty("portal.snapshot.dir"),"category-sales-review");folder.mkdirs();
        try(FileOutputStream output=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}
        bitmap.recycle();
    }

    private void assertMoneyReadable(View view) {
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            if(text.getText().toString().startsWith("₹") && text.getLayout()!=null) {
                assertEquals("Monetary amount must not split across lines",1,text.getLayout().getLineCount());
                assertEquals(text.getText().length(),text.getLayout().getLineEnd(0));
                assertEquals(0,text.getLayout().getEllipsisCount(0));
            }
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertMoneyReadable(((ViewGroup)view).getChildAt(i));
    }
}
