package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.view.*;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.svd.svdagencies.R;
import com.svd.svdagencies.base.BaseActivity;
import com.svd.svdagencies.utils.CompactLinearLayout;
import com.svd.svdagencies.utils.CompactUi;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,application=Application.class,manifest=Config.NONE,packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CompactUiTest {
    public static class Host extends BaseActivity {}
    public static class CountingText extends TextView {
        int sizeChanges;
        public CountingText(android.content.Context context){super(context);}
        @Override public void setTextSize(int unit,float value){sizeChanges++;super.setTextSize(unit,value);}
    }
    public static class CountingButton extends MaterialButton {
        int layoutRequests;
        public CountingButton(android.content.Context context){super(context);}
        @Override public void requestLayout(){layoutRequests++;super.requestLayout();}
    }
    @Test public void compactControlsWrapAndKeepDataAndClickHandlers() {
        for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390,600,840}) {
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
            try {
                Host activity=controller.get();
                CompactLinearLayout root=new CompactLinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,16,16,16);
                CompactLinearLayout row=new CompactLinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(row,new LinearLayout.LayoutParams(-1,-2));
                AutoCompleteTextView company=new AutoCompleteTextView(activity);company.setId(R.id.spinnerCompanyFilter);company.setText("All companies");company.setInputType(0);
                row.addView(company,new LinearLayout.LayoutParams(0,-2,1));
                EditText date=new EditText(activity);date.setId(R.id.etEntryDate);date.setText("2026-10-25");date.setInputType(0);date.setFocusable(false);
                AtomicInteger picks=new AtomicInteger();date.setOnClickListener(view->picks.incrementAndGet());
                row.addView(date,new LinearLayout.LayoutParams(0,-2,1));
                CountingButton refresh=new CountingButton(activity);refresh.setText("Refresh");
                AtomicInteger reloads=new AtomicInteger();refresh.setOnClickListener(view->reloads.incrementAndGet());
                row.addView(refresh,new LinearLayout.LayoutParams(0,-2,1));
                ImageButton calendar=new ImageButton(activity);calendar.setId(R.id.etMonth);
                calendar.setImageResource(R.drawable.ic_calendar);row.addView(calendar,new LinearLayout.LayoutParams(52,52));
                CountingText data=new CountingText(activity);data.setId(R.id.tvMessage);data.setText("Sales, accounts and daily operations.");data.setTextSize(32);
                root.addView(data);
                TextView helper=new TextView(activity);helper.setId(R.id.tvSalesFilterHelp);helper.setText("A helper paragraph");root.addView(helper);
                MaterialButton update=new MaterialButton(activity);update.setId(R.id.btnSubmitUpdate);update.setText("Update Stock Levels");root.addView(update);
                activity.setContentView(root);
                int fontCalls=-1;
                for(int pass=0;pass<3;pass++) {
                    int before=refresh.layoutRequests;
                    CompactUi.applyTree(root);
                    if(pass>0)assertEquals("Settled styling must not request another layout",before,refresh.layoutRequests);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));root.layout(0,0,width,844);
                    if(pass==0)fontCalls=data.sizeChanges;else assertEquals("Typography must settle after the initial pass",fontCalls,data.sizeChanges);
                }
                assertEquals(View.VISIBLE,data.getVisibility());assertEquals("Sales, accounts and daily operations.",data.getText().toString());
                assertEquals(View.GONE,helper.getVisibility());
                assertEquals(Gravity.CENTER,company.getGravity());assertEquals(Gravity.CENTER,date.getGravity());
                assertEquals("2026-10-25",date.getText().toString());assertNotNull(date.getCompoundDrawablesRelative()[0]);
                assertEquals(48,refresh.getWidth());assertEquals(48,refresh.getHeight());
                assertFalse(CompactUi.isDateField(update));assertEquals("Update Stock Levels",update.getText().toString());
                assertTrue(data.getTextSize()/data.getResources().getDisplayMetrics().scaledDensity<=20.1f);
                assertEquals(52,calendar.getHeight());assertEquals(52,calendar.getWidth());
                View[] fields={company,date,refresh,calendar};
                for(View field:fields){assertTrue(field.getLeft()>=0);assertTrue(field.getRight()<=row.getWidth());assertTrue(field.getHeight()>=48);}
                for(int first=0;first<fields.length;first++)for(int second=first+1;second<fields.length;second++) {
                    android.graphics.Rect a=new android.graphics.Rect(fields[first].getLeft(),fields[first].getTop(),fields[first].getRight(),fields[first].getBottom());
                    android.graphics.Rect b=new android.graphics.Rect(fields[second].getLeft(),fields[second].getTop(),fields[second].getRight(),fields[second].getBottom());
                    assertFalse("Filter controls overlap",android.graphics.Rect.intersects(a,b));
                }
                date.performClick();refresh.performClick();assertEquals(1,picks.get());assertEquals(1,reloads.get());
                refresh.setEnabled(false);CompactUi.applyTree(root);assertFalse(refresh.isEnabled());
                for(int resource:new int[]{R.layout.customer_bills,R.layout.user_bills}) {
                    View bills=activity.getLayoutInflater().inflate(resource,null,false);
                    CompactUi.applyTree(bills);
                    bills.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));bills.layout(0,0,width,844);
                    TextView legacy=bills.findViewById(R.id.btnFilter);
                    android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888);
                    legacy.draw(new android.graphics.Canvas(pixels));
                    int background=pixels.getPixel(4,24);
                    int foreground=legacy instanceof MaterialButton ? ((MaterialButton)legacy).getIconTint().getDefaultColor() : legacy.getCompoundDrawableTintList().getDefaultColor();
                    assertTrue("Legacy refresh icon must contrast with its actual rendered fill",androidx.core.graphics.ColorUtils.calculateContrast(foreground,background)>=4.5);
                    int visibleIconPixels=0;
                    for(int y=14;y<34;y++)for(int x=14;x<34;x++)
                        if(androidx.core.graphics.ColorUtils.calculateContrast(pixels.getPixel(x,y),background)>=4.5)visibleIconPixels++;
                    assertTrue("Refresh glyph must be visible in the rendered bitmap",visibleIconPixels>=15);
                    assertEquals(48,legacy.getWidth());assertEquals(48,legacy.getHeight());pixels.recycle();
                }

            } finally {controller.pause().stop().destroy();}
        }
    }
}
