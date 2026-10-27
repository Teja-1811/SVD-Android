package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.*;
import com.google.android.material.color.MaterialColors;
import com.svd.svdagencies.R;
import com.svd.svdagencies.base.BaseActivity;
import com.svd.svdagencies.utils.CompactUi;
import com.svd.svdagencies.utils.DateChipPresentation;
import java.io.*;
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
public class DateChipTest {
    public static class Host extends BaseActivity {}

    @Test public void dayMonthYearFormattingPreservesInvalidDatesMonthsAndTime() {
        assertEquals("24-10-2026",DateChipPresentation.display("2026-10-24"));
        assertEquals("24-10-2026",DateChipPresentation.display("24 Oct 2026"));
        assertEquals("24-10-2026",DateChipPresentation.display("24 October 2026"));
        assertEquals("24-10-2026",DateChipPresentation.display("24-Oct-2026"));
        assertEquals("24-10-2026",DateChipPresentation.display("24/10/2026"));
        assertEquals("29-02-2024",DateChipPresentation.display("2024-02-29"));
        assertEquals("2026-02-30",DateChipPresentation.display("2026-02-30"));
        assertEquals("October 2026",DateChipPresentation.display("October 2026"));
        assertEquals("Select date",DateChipPresentation.display("Select date"));
        assertEquals("24-10-2026 – 30-10-2026",DateChipPresentation.display("2026-10-24 – 2026-10-30"));
        assertEquals("24-10-2026 18:30",DateChipPresentation.display("2026-10-24 18:30"));
    }

    @Test public void dateChipsKeepRawApiValuesCallbacksAndInputContracts() {
        ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
        try {
            Host host=controller.get();
            LinearLayout root=new LinearLayout(host);root.setOrientation(LinearLayout.VERTICAL);
            EditText date=new EditText(host);date.setId(R.id.etDate);date.setInputType(0);date.setFocusable(false);date.setText("2026-10-24");
            int inputType=date.getInputType();AtomicInteger clicks=new AtomicInteger();AtomicInteger changes=new AtomicInteger();
            date.setOnClickListener(v->clicks.incrementAndGet());
            date.addTextChangedListener(new TextWatcher(){
                public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                public void onTextChanged(CharSequence s,int start,int before,int count){}
                public void afterTextChanged(Editable text){assertTrue("Callbacks must receive the source date",text.toString().matches("2026-10-(24|26)"));changes.incrementAndGet();}
            });
            root.addView(date);host.setContentView(root);CompactUi.applyTree(root);
            assertEquals("2026-10-24",date.getText().toString());assertEquals(inputType,date.getInputType());assertFalse(date.isFocusable());
            assertEquals("24-10-2026",date.getTransformationMethod().getTransformation(date.getText(),date).toString());
            int settled=changes.get();CompactUi.applyTree(root);CompactUi.applyTree(root);assertEquals("Styling must settle",settled,changes.get());
            assertEquals(0,clicks.get());date.performClick();assertEquals(1,clicks.get());
            date.setText("2026-10-26");assertEquals("26-10-2026",date.getTransformationMethod().getTransformation(date.getText(),date).toString());
            assertEquals("2026-10-26",date.getText().toString());
            date.setEnabled(false);CompactUi.applyTree(root);assertFalse(date.isEnabled());
        } finally {controller.pause().stop().destroy();}
    }

    @Test public void outlinedCalendarChipsRenderAcrossSizesAndAppearances() throws Exception {
        for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390,600,840}){
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
            try{
                Host host=controller.get();LinearLayout root=new LinearLayout(host);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,16,16,16);
                root.setBackgroundColor(host.getColor(R.color.surface_background));
                MaterialButton button=new MaterialButton(host);button.setId(R.id.btnSelectSalesDate);button.setText("2026-10-24");root.addView(button,new LinearLayout.LayoutParams(-2,-2));
                Button legacy=new Button(host);legacy.setId(R.id.tvStartDate);legacy.setText("24 October 2026");root.addView(legacy,new LinearLayout.LayoutParams(-2,-2));
                View bill=LayoutInflater.from(host).inflate(R.layout.admin_bill_create,null,false);
                TextInputEditText field=bill.findViewById(R.id.tvBillDate);
                View parent=(View)field.getParent();
                TextInputLayout holder=parent instanceof TextInputLayout?(TextInputLayout)parent:(TextInputLayout)parent.getParent();
                holder.setHintAnimationEnabled(false);field.setText("2026-10-24");
                ((ViewGroup)holder.getParent()).removeView(holder);
                root.addView(holder,new LinearLayout.LayoutParams(-2,-2));
                MaterialButton month=new MaterialButton(host);month.setId(R.id.etMonth);month.setText("October 2026");root.addView(month,new LinearLayout.LayoutParams(-2,-2));
                host.setContentView(root);
                for(int pass=0;pass<3;pass++){CompactUi.applyTree(root);root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,width,root.getMeasuredHeight());}
                assertEquals("24-10-2026",button.getLayout().getText().toString());
                assertEquals("24-10-2026",legacy.getLayout().getText().toString());
                assertEquals("24-10-2026",field.getLayout().getText().toString());
                assertEquals("October 2026",month.getLayout().getText().toString());
                assertTrue(button.getStrokeWidth()>=1);assertTrue(button.getCornerRadius()>=24);
                assertNotNull(button.getIcon());assertNotNull(field.getCompoundDrawablesRelative()[0]);
                assertFalse(holder.isEndIconVisible());
                int primary=MaterialColors.getColor(button,com.google.android.material.R.attr.colorPrimary);
                assertEquals(primary,button.getCurrentTextColor());
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(primary,MaterialColors.getColor(button,com.google.android.material.R.attr.colorSurface))>=4.5);
                for(TextView date:new TextView[]{button,legacy,field}){
                    assertTrue(date.getHeight()>=48);assertEquals(0,date.getLayout().getEllipsisCount(date.getLayout().getLineCount()-1));
                }
                Bitmap bitmap=Bitmap.createBitmap(width,root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
                int left=field.getLeft(),top=field.getTop();View ancestor=(View)field.getParent();
                while(ancestor!=root){left+=ancestor.getLeft();top+=ancestor.getTop();ancestor=(View)ancestor.getParent();}
                int ink=0;
                for(int y=top+12;y<top+field.getHeight()-10;y++)
                    for(int x=left+field.getCompoundPaddingLeft();x<left+field.getWidth()-field.getCompoundPaddingRight();x++){
                        int pixel=bitmap.getPixel(x,y);
                        if(Math.abs(Color.red(pixel)-Color.red(primary))<35 && Math.abs(Color.green(pixel)-Color.green(primary))<35 && Math.abs(Color.blue(pixel)-Color.blue(primary))<35)ink++;
                    }
                assertTrue("The outlined input date must have visible rendered text, width="+field.getWidth()+", scroll="+field.getScrollX(),ink>30);
                File dir=new File(System.getProperty("portal.snapshot.dir"),"date-chips-review");dir.mkdirs();
                try(FileOutputStream stream=new FileOutputStream(new File(dir,"date-chips-"+width+(night?"-dark":"-light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
            }finally{controller.pause().stop().destroy();}
        }
    }
}
