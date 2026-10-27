package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import com.svd.svdagencies.R;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CalendarActionsTest {
    public static class Host extends AppCompatActivity {}

    @Test public void calendarActionsAreReadableAndWorkInEveryPalette() throws Exception {
        int[] themes={R.style.Theme_SVDAgency,R.style.Theme_SVDAgency_Ocean,R.style.Theme_SVDAgency_Forest,R.style.Theme_SVDAgency_User};
        for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390})for(int themeIndex=0;themeIndex<themes.length;themeIndex++) {
            RuntimeEnvironment.setQualifiers("w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<Host> controller=Robolectric.buildActivity(Host.class);
            Host activity=controller.get();activity.setTheme(themes[themeIndex]);controller.setup();
            try {
                AtomicInteger selections=new AtomicInteger();
                DatePickerDialog dialog=new DatePickerDialog(activity,(picker,year,month,day)->{
                    assertEquals(2026,year);assertEquals(9,month);assertEquals(24,day);selections.incrementAndGet();
                },2026,9,24);
                dialog.show();
                int background=dialog.getContext().getColor(R.color.surface_background);
                // Measure the real dialog actions independently: Robolectric's SDK 34
                // month pager cannot reliably lay out the full native calendar.
                LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.HORIZONTAL);
                root.setGravity(android.view.Gravity.END);root.setPadding(12,12,12,12);root.setBackgroundColor(background);
                for(int action:new int[]{DialogInterface.BUTTON_NEGATIVE,DialogInterface.BUTTON_POSITIVE}) {
                    Button button=dialog.getButton(action);
                    ((ViewGroup)button.getParent()).removeView(button);
                    root.addView(button,new LinearLayout.LayoutParams(-2,-2));
                    assertEquals(View.VISIBLE,button.getVisibility());
                    assertTrue("Calendar action has no text",button.getText().length()>0);
                    assertNotNull("Calendar actions need filled rounded backgrounds",button.getBackground());
                    Bitmap fillSample=Bitmap.createBitmap(100,48,Bitmap.Config.ARGB_8888);
                    button.getBackground().setBounds(0,0,100,48);button.getBackground().draw(new Canvas(fillSample));
                    int fill=fillSample.getPixel(50,24);
                    assertTrue("Calendar action corners must be rounded",android.graphics.Color.alpha(fillSample.getPixel(5,1))<255);
                    fillSample.recycle();
                    assertEquals("Action background must be opaque",255,android.graphics.Color.alpha(fill));
                    double contrast=ColorUtils.calculateContrast(button.getCurrentTextColor(),fill);
                    assertTrue("Calendar action contrast is "+contrast+" night="+night+" palette="+themeIndex,contrast>=4.5);
                }
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                root.layout(0,0,width,root.getMeasuredHeight());
                for(int action:new int[]{DialogInterface.BUTTON_NEGATIVE,DialogInterface.BUTTON_POSITIVE}) {
                    Button button=dialog.getButton(action);
                    assertTrue("Calendar action is too short",button.getMeasuredHeight()>=48);
                    android.text.Layout layout=button.getLayout();
                    assertEquals(0,layout.getEllipsisCount(layout.getLineCount()-1));
                }
                File folder=new File(System.getProperty("portal.snapshot.dir"),"calendar-review");folder.mkdirs();
                Bitmap bitmap=Bitmap.createBitmap(width,root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
                try(FileOutputStream output=new FileOutputStream(new File(folder,"calendar-"+themeIndex+"-"+width+(night?"-dark":"-light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}
                bitmap.recycle();
                dialog.onClick(dialog,DialogInterface.BUTTON_POSITIVE);assertEquals(1,selections.get());
                dialog.onClick(dialog,DialogInterface.BUTTON_NEGATIVE);assertEquals(1,selections.get());
                dialog.dismiss();
                androidx.appcompat.app.AlertDialog material=new com.svd.svdagencies.utils.WhiteMaterialAlertDialogBuilder(activity)
                    .setTitle("Settings").setMessage("Sample settings")
                    .setNegativeButton("Cancel",null).setPositiveButton("Save",null).show();
                for(int action:new int[]{DialogInterface.BUTTON_NEGATIVE,DialogInterface.BUTTON_POSITIVE}) {
                    Button button=material.getButton(action);
                    assertNotNull(button.getBackground());
                    Bitmap sample=Bitmap.createBitmap(120,48,Bitmap.Config.ARGB_8888);
                    button.getBackground().setBounds(0,0,120,48);button.getBackground().draw(new Canvas(sample));
                    int fill=sample.getPixel(60,24);
                    assertTrue("Material action corners must be rounded",android.graphics.Color.alpha(sample.getPixel(5,1))<255);
                    sample.recycle();
                    assertEquals("Material dialog action must have a filled background",255,android.graphics.Color.alpha(fill));
                    assertTrue("Material dialog action text must contrast with its actual fill",
                        ColorUtils.calculateContrast(button.getCurrentTextColor(),fill)>=4.5);
                }
                material.dismiss();
            } finally {controller.pause().stop().destroy();}
        }
    }
}
