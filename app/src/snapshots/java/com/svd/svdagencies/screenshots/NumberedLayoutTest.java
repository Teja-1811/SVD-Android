package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.svd.svdagencies.R;
import com.svd.svdagencies.base.BaseActivity;
import com.svd.svdagencies.utils.CompactUi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,application=Application.class,manifest=Config.NONE,packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NumberedLayoutTest {
    public static class Host extends BaseActivity {}
    private Rect bounds(View view,ViewGroup root) {
        Rect rect=new Rect(0,0,view.getWidth(),view.getHeight());root.offsetDescendantRectToMyCoords(view,rect);return rect;
    }
    private void sideBySide(ViewGroup root,int first,int second) {
        Rect a=bounds(root.findViewById(first),root),b=bounds(root.findViewById(second),root);
        assertTrue("First control must have usable width",a.width()>=36);
        assertTrue("Second control must have usable width",b.width()>=36);
        assertTrue("Controls must remain in the same row: "+a+" / "+b,a.top<b.bottom && b.top<a.bottom);
        assertTrue("Controls must not overlap: "+a+" / "+b,a.right<=b.left);
        assertTrue("Controls must fit the screen",a.left>=0 && b.right<=root.getWidth());
    }
    @Test public void requestedRowsAndWidthsSurvivePhoneAndTabletSizes() {
        int[][] pairs={
            {R.layout.admin_cashbook,R.id.btnAddExpense,R.id.btnPayDeliverySalary},
            {R.layout.admin_category_sales_dashboard,R.id.spinnerPeriod,R.id.btnSelectSalesDate},
            {R.layout.admin_companies_due,R.id.dueMonthFilter,R.id.dueYearFilter},
            {R.layout.admin_customer_payment,R.id.etFilterCustomer,R.id.btnFilter},
            {R.layout.admin_delivery_agent_summary_header,R.id.autoCompleteAgent,R.id.btnClear},
            {R.layout.admin_dialog_leakage,R.id.actvType,R.id.etDate},
            {R.layout.admin_expences_add,R.id.etDate,R.id.spinnerCategory},
            {R.layout.delivery_bill_to_customer,R.id.autoRoute,R.id.autoCustomer}
        };
        for(int width:new int[]{320,390,600,840}) {
            RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-notnight-mdpi");
            RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
            ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
            try {
                for(int[] entry:pairs) {
                    ViewGroup root=(ViewGroup)LayoutInflater.from(controller.get()).inflate(entry[0],null,false);
                    controller.get().setContentView(root);
                    for(int pass=0;pass<3;pass++) {
                        CompactUi.applyTree(root);
                        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(3000,View.MeasureSpec.EXACTLY));
                        root.layout(0,0,width,3000);
                    }
                    sideBySide(root,entry[1],entry[2]);
                    if(entry[0]==R.layout.delivery_bill_to_customer) {
                        float ratio=(float)root.findViewById(R.id.autoRoute).getWidth()/root.findViewById(R.id.autoCustomer).getWidth();
                        assertEquals("Route/customer proportions",35f/65f,ratio,.09f);
                        RecyclerView items=root.findViewById(R.id.rvItemCatalog);
                        assertEquals(2,((GridLayoutManager)items.getLayoutManager()).getSpanCount());
                    }
                }
            } finally {controller.pause().stop().destroy();}
        }
    }
    @Test public void customerIconActionsRetainClicksAndAccessibleNames() {
        ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
        try {
            ViewGroup root=(ViewGroup)LayoutInflater.from(controller.get()).inflate(R.layout.admin_customer_card,null,false);
            controller.get().setContentView(root);CompactUi.applyTree(root);
            verifyIcons(root);
        } finally {controller.pause().stop().destroy();}
    }
    private void verifyIcons(View view) {
        if(view instanceof MaterialButton && "svd-icon-only".equals(view.getTag())) {
            MaterialButton button=(MaterialButton)view;
            assertEquals("",button.getText().toString());assertNotNull(button.getIcon());
            assertTrue(button.getContentDescription()!=null && button.getContentDescription().length()>0);
            java.util.concurrent.atomic.AtomicInteger clicks=new java.util.concurrent.atomic.AtomicInteger();
            button.setOnClickListener(v->clicks.incrementAndGet());button.performClick();assertEquals(1,clicks.get());
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)verifyIcons(((ViewGroup)view).getChildAt(i));
    }
}
