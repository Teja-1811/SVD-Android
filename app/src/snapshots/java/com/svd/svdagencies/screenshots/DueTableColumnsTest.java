package com.svd.svdagencies.screenshots;
import android.app.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import com.svd.svdagencies.R;
import com.svd.svdagencies.ui.delivery.*;
import com.svd.svdagencies.data.model.delivery.DeliveryAgentDailyDueRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies", qualifiers="w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DueTableColumnsTest {
 @Test public void columnsFitContentAndMatchHeaderAtLargeFontScale() throws Exception {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);
   a.getResources().getDisplayMetrics().scaledDensity=1.3f*a.getResources().getDisplayMetrics().density;
   var due=new DeliveryAgentDailyDueRecord(1,2,"Ramesh Kumar", "2026-11-02",12345678.90,10000,1234.50,12336913.40,12346913.40,0);
   DueTableColumns columns=new DueTableColumns(a);columns.update(java.util.List.of(due));
   LinearLayout parent=new LinearLayout(a);parent.setOrientation(LinearLayout.VERTICAL);
   var adapter=new DeliveryAgentDuesTableHeaderAdapter(columns);
   var header=adapter.onCreateViewHolder(parent,0);adapter.onBindViewHolder(header,0);
   View body=LayoutInflater.from(a).inflate(R.layout.admin_delivery_agent_due_row,parent,false);
   new DeliveryAgentDailyDueAdapter.ViewHolder(body).bind(due);columns.apply(body);
   int[] ids={R.id.tvDueDate,R.id.tvDueAgent,R.id.tvDueOpening,R.id.tvDueCollected,R.id.tvDueTotal,R.id.tvDueSubmitted,R.id.tvDueBalance};
   for(int id:ids){TextView h=header.itemView.findViewById(id),b=body.findViewById(id);
    assertEquals(h.getLayoutParams().width,b.getLayoutParams().width);
    assertEquals(Gravity.CENTER,h.getGravity());assertEquals(Gravity.CENTER,b.getGravity());
    assertTrue(h.getLayoutParams().width>=h.getPaint().measureText(h.getText().toString())+h.getPaddingLeft()+h.getPaddingRight());
    assertTrue(b.getLayoutParams().width>=b.getPaint().measureText(b.getText().toString())+b.getPaddingLeft()+b.getPaddingRight());
   }
   assertFalse(((HorizontalScrollView)body).isFillViewport());
   parent.addView(header.itemView);parent.addView(body);a.setContentView(parent);
   ViewGroup row=(ViewGroup)((HorizontalScrollView)body).getChildAt(0);int width=row.getLayoutParams().width;
   parent.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));parent.layout(0,0,width,parent.getMeasuredHeight());
   for(int id:ids){assertEquals(header.itemView.findViewById(id).getLeft(),body.findViewById(id).getLeft());assertEquals(header.itemView.findViewById(id).getRight(),body.findViewById(id).getRight());}
  }
 }
}
