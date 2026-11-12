package com.svd.svdagencies.screenshots;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.svd.svdagencies.R;
import com.svd.svdagencies.utils.*;
import com.svd.svdagencies.ui.admin.stock.StockUpdateAdapter;
import com.svd.svdagencies.data.model.admin.stock.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies", qualifiers="w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LayoutRepairRegressionTest {
    private int dp(Activity a,int value){return (int)(value*a.getResources().getDisplayMetrics().density+.5f);}
    private void layout(View view,int w,int h){view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private void preview(View view,String name)throws Exception{
        Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));
        File out=new File(System.getProperty("portal.snapshot.dir"),name+".png");out.getParentFile().mkdirs();try(FileOutputStream stream=new FileOutputStream(out)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
    }
    public static class CountedDropdown extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {
        int requests;
        CountedDropdown(Activity context){super(context);}
        @Override public void requestLayout(){requests++;super.requestLayout();}
    }
    @Test public void stylingDropdownAgainDoesNotRequestAnotherLayout(){
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);
            CountedDropdown dropdown=new CountedDropdown(a);dropdown.setLayoutParams(new LinearLayout.LayoutParams(dp(a,160),dp(a,44)));
            CompactUi.applyTree(dropdown);dropdown.requests=0;
            CompactUi.applyTree(dropdown);CompactUi.applyTree(dropdown);
            assertEquals(0,dropdown.requests);
        }
    }
    @Test public void dueColumnsKeepTheirWidthAndRowsScrollTogether() throws Exception {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);
            RecyclerView list=new RecyclerView(a);list.setLayoutManager(new LinearLayoutManager(a));
            list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>(){
                public int getItemCount(){return 2;}
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,int type){return new RecyclerView.ViewHolder(LayoutInflater.from(a).inflate(R.layout.admin_delivery_agent_due_row,parent,false)){};}
                public void onBindViewHolder(RecyclerView.ViewHolder holder,int position){
                    int[] ids={R.id.tvDueDate,R.id.tvDueAgent,R.id.tvDueOpening,R.id.tvDueCollected,R.id.tvDueTotal,R.id.tvDueSubmitted,R.id.tvDueBalance};
                    String[] values=position==0?new String[]{"Date","Agent Name","Op Due","Collected","Total","Submitted","Due"}:new String[]{"2026-11-02","Ramesh","6000.00","12835.00","18835.00","10000.00","8835.00"};
                    for(int i=0;i<ids.length;i++)((TextView)holder.itemView.findViewById(ids[i])).setText(values[i]);
                    CompactUi.applyTree(holder.itemView);
                }
            });
            a.setContentView(list);layout(list,dp(a,360),dp(a,240));
            assertEquals(2,list.getChildCount());
            HorizontalScrollView first=(HorizontalScrollView)list.getChildAt(0), second=(HorizontalScrollView)list.getChildAt(1);
            assertTrue(first.getChildAt(0).getWidth()>=dp(a,900));
            assertEquals(1,((TextView)first.findViewById(R.id.tvDueCollected)).getLayout().getLineCount());
            first.scrollTo(dp(a,120),0);assertEquals(dp(a,120),first.getScrollX());assertEquals(first.getScrollX(),second.getScrollX());
            first.scrollTo(0,0);preview(list,"due-table");
        }
    }
    @Test public void existingHiddenScrollbarsStayDisabledAndRefreshIconFits() throws Exception {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);LinearLayout root=new LinearLayout(a);root.setOrientation(1);
            HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);scroll.addView(new TextView(a));root.addView(scroll);
            MaterialButton reset=new MaterialButton(a);reset.setText("");reset.setContentDescription("Refresh filters");reset.setIconResource(R.drawable.ic_refresh);reset.setLayoutParams(new LinearLayout.LayoutParams(dp(a,32),dp(a,32)));root.addView(reset);
            a.setContentView(root);CompactUi.applyTree(root);ScrollableTables.INSTANCE.install(root);layout(root,dp(a,360),dp(a,300));
            assertFalse(scroll.isHorizontalScrollBarEnabled());assertEquals(dp(a,48),reset.getLayoutParams().width);
            assertTrue(reset.getIconSize()+reset.getPaddingLeft()+reset.getPaddingRight()<=reset.getWidth());preview(root,"refresh-control");
        }
    }
    @Test public void stockFormHasOneScrollOwnerAndRebindingKeepsDrafts() throws Exception {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
            Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);
            LinearLayout parent=new LinearLayout(a);parent.setOrientation(1);
            androidx.core.widget.NestedScrollView oldScroll=new androidx.core.widget.NestedScrollView(a);oldScroll.setId(R.id.stockUpdateScroll);
            LinearLayout column=new LinearLayout(a);column.setOrientation(1);oldScroll.addView(column);
            column.addView(new TextView(a));RecyclerView list=new RecyclerView(a);list.setLayoutManager(new LinearLayoutManager(a));column.addView(list);column.addView(new TextView(a));
            parent.addView(oldScroll,new LinearLayout.LayoutParams(-1,0,1));parent.addView(new TextView(a));
            StockUpdateAdapter adapter=new StockUpdateAdapter(Collections.emptyList());adapter.updateCompanies(Collections.singletonList(new StockCompany(1,"Dodla")));
            StockEntryScrolling.install(oldScroll,list,adapter);
            assertNull(parent.findViewById(R.id.stockUpdateScroll));assertSame(parent,list.getParent());assertTrue(list.getAdapter() instanceof androidx.recyclerview.widget.ConcatAdapter);
            Gson gson=new Gson();StockItem item=gson.fromJson("{\"id\":1,\"name\":\"Milk\",\"stock_quantity\":10,\"company_id\":1,\"pcs_count\":24,\"base_price\":10,\"ltr\":1}",StockItem.class);
            adapter.updateList(Collections.singletonList(item));
            RecyclerView.ViewHolder holder=adapter.createViewHolder(list,adapter.getItemViewType(1));adapter.bindViewHolder(holder,1);
            EditText crates=holder.itemView.findViewById(R.id.etCrates);crates.setText("2");assertEquals(1,adapter.getUpdates().size());
            adapter.bindViewHolder(holder,1);assertEquals("2",crates.getText().toString());assertEquals(1,adapter.getUpdates().size());
        }
    }
}
