package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.svd.svdagencies.R;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ResponsiveScreensTest {
    public static class Host extends AppCompatActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.Theme_SVDAgency);
            super.onCreate(state);
        }
    }

    private void checkFields(View view, int width) {
        if(view.getVisibility() == View.GONE) return;
        if(view instanceof EditText && view.getMeasuredWidth() > 0) {
            assertTrue("Field exceeds screen: " + view.getId(), view.getMeasuredWidth() <= width);
            assertTrue("Field clipped vertically: " + view.getId(), view.getMeasuredHeight() >= 48);
        }
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) checkFields(((ViewGroup)view).getChildAt(i),width);
    }

    @Test public void compactXmlScreensFitPhonesTabletsAndLargeText() throws Exception {
        int[] layouts={R.layout.activity_tools,R.layout.include_business_settings,R.layout.item_admin_route,
            R.layout.admin_offers_content,R.layout.admin_subscriptions_content,R.layout.activity_login,
            R.layout.admin_bill_create,R.layout.admin_customer_add,R.layout.customer_payment};
        File output=new File(System.getProperty("portal.snapshot.dir"),"responsive");
        output.mkdirs();
        for(int width:new int[]{320,390,600,840}) for(float scale:new float[]{1f,1.5f}) {
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h900dp-mdpi");
            RuntimeEnvironment.setFontScale(scale);
            ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
            try {
                for(int layout:layouts) {
                    View root=host.get().getLayoutInflater().inflate(layout,null,false);
                    host.get().setContentView(root);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));
                    root.layout(0,0,width,900);
                    checkFields(root,width);
                    EditText upi=root.findViewById(R.id.businessUpi);
                    if(upi!=null) {
                        assertEquals(upi.getResources().getDimension(R.dimen.svd_text_body),upi.getTextSize(),0.1f);
                        upi.setText("agency@bank");
                        assertEquals("agency@bank",upi.getText().toString());
                    }
                    org.robolectric.shadows.ShadowLooper.idleMainLooper(1,java.util.concurrent.TimeUnit.SECONDS);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));
                    root.layout(0,0,width,900);
                    Bitmap image=Bitmap.createBitmap(width,900,Bitmap.Config.ARGB_8888);
                    Canvas canvas=new Canvas(image); canvas.drawColor(androidx.core.content.ContextCompat.getColor(host.get(),R.color.window_background)); root.draw(canvas);
                    String name=root.getResources().getResourceEntryName(layout)+"-"+width+"-"+scale+".png";
                    try(FileOutputStream file=new FileOutputStream(new File(output,name))) { image.compress(Bitmap.CompressFormat.PNG,100,file); }
                    image.recycle();
                }
            } finally {host.pause().stop().destroy();}
        }
    }
}
