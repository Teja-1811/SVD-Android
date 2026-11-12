package com.svd.svdagencies.screenshots;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import com.svd.svdagencies.R;
import com.svd.svdagencies.utils.SharedDatePickerDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies", qualifiers="w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SharedDatePickerDialogTest {
    @Test public void selectingRequiresConfirmationAndCancelDoesNotSubmit() {
        try (ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup()) {
            AtomicInteger calls = new AtomicInteger();
            SharedDatePickerDialog dialog = new SharedDatePickerDialog(host.get(), (picker,y,m,d) -> {
                calls.incrementAndGet(); assertEquals(2026,y); assertEquals(10,m); assertEquals(5,d);
            }, 2026,10,2);
            dialog.show();
            dialog.getDatePicker().updateDate(2026,10,5);
            assertEquals(0,calls.get());
            dialog.findViewById(R.id.btnSelectSharedDate).performClick();
            assertEquals(1,calls.get()); assertFalse(dialog.isShowing());
            SharedDatePickerDialog cancelled = new SharedDatePickerDialog(host.get(), (picker,y,m,d) -> calls.incrementAndGet(), 2026,10,2);
            cancelled.show(); cancelled.findViewById(R.id.btnCancelSharedDate).performClick();
            assertEquals(1,calls.get()); assertFalse(cancelled.isShowing());
        }
    }
    @Test public void dateLimitsRemainAvailableAndActionsFitCompactScreen() throws Exception {
        try (ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup()) {
            SharedDatePickerDialog dialog = new SharedDatePickerDialog(host.get(), null,2026,10,2);
            Calendar min = Calendar.getInstance(); min.set(2026,10,4,0,0,0); min.set(Calendar.MILLISECOND,0);
            dialog.getDatePicker().setMinDate(min.getTimeInMillis());
            assertEquals(min.getTimeInMillis(),dialog.getDatePicker().getMinDate());
            dialog.show(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            View decor = dialog.getWindow().getDecorView();
            int width=dialog.getWindow().getAttributes().width, height=dialog.getWindow().getAttributes().height;
            decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            decor.layout(0,0,width,height);
            for(int id:new int[]{R.id.btnCancelSharedDate,R.id.btnSelectSharedDate}) {
                View action=dialog.findViewById(id); int[] point=new int[2]; action.getLocationInWindow(point);
                assertTrue(action.getHeight()>0); assertTrue(point[1]>=0); assertTrue(point[1]+action.getHeight()<=height);
            }
            Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); decor.draw(new Canvas(image));
            File output=new File(System.getProperty("portal.snapshot.dir"),"shared-calendar.png"); output.getParentFile().mkdirs();
            try(FileOutputStream stream=new FileOutputStream(output)){image.compress(Bitmap.CompressFormat.PNG,100,stream);}
            image.recycle(); dialog.dismiss();
        }
    }
}
