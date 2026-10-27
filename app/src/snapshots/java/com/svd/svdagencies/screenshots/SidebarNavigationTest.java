package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.*;
import com.svd.svdagencies.R;
import com.svd.svdagencies.utils.CollapsibleNavigation;
import com.svd.svdagencies.ui.admin.AdminBaseActivity;
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
public class SidebarNavigationTest {
 public static class Host extends AdminBaseActivity {}
 private MenuItem named(NavigationView nav,String text){for(int i=0;i<nav.getMenu().size();i++){MenuItem row=nav.getMenu().getItem(i);if(row.getTitle().toString().trim().startsWith(text))return row;}throw new AssertionError(text);}
 @Test public void groupsCollapseWithoutNavigatingAndOriginalActionsKeepTheirHandlers(){
  ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
  try{
   Host host=controller.get();LinearLayout body=new LinearLayout(host);body.setOrientation(1);
   EditText search=new EditText(host);search.setText("Milk");body.addView(search);
   MaterialButton add=new MaterialButton(host);add.setId(R.id.fabAddCustomer);add.setText("Add customer");body.addView(add);
   MaterialButton formAction=new MaterialButton(host);formAction.setId(R.id.btnAddItem);formAction.setText("Add item");body.addView(formAction);
   MaterialButton download=new MaterialButton(host);download.setText("Download PDF");body.addView(download);
   ImageButton orders=new ImageButton(host);orders.setId(R.id.btnViewOrders);orders.setContentDescription("View orders");body.addView(orders);
   ChipGroup types=new ChipGroup(host);types.setSingleSelection(true);Chip retailer=new Chip(host);retailer.setId(R.id.chipRetailer);retailer.setText("Retailers");retailer.setCheckable(true);types.addView(retailer);body.addView(types);
   AtomicInteger additions=new AtomicInteger(),changes=new AtomicInteger(),navigations=new AtomicInteger();
   add.setOnClickListener(v->additions.incrementAndGet());types.setOnCheckedStateChangeListener((g,ids)->changes.incrementAndGet());
   NavigationView nav=new NavigationView(host);nav.inflateMenu(R.menu.menu_admin_drawer);nav.setNavigationItemSelectedListener(item->{navigations.incrementAndGet();return true;});body.addView(nav);
   host.setContentView(body);CollapsibleNavigation.install(host,nav);
   assertEquals(View.GONE,add.getVisibility());assertEquals(View.GONE,types.getVisibility());
   assertEquals("Milk",search.getText().toString());assertEquals(View.VISIBLE,search.getVisibility());
   assertEquals("An item form action must remain with its form",View.VISIBLE,formAction.getVisibility());assertEquals(View.GONE,download.getVisibility());assertNotNull(named(nav,"Download PDF"));
   assertEquals(View.GONE,orders.getVisibility());assertNotNull(named(nav,"View orders"));
   MenuItem header=named(nav,"Customers"),action=named(nav,"Add customer"),type=named(nav,"Retailers");
   assertTrue(action.isVisible());nav.getMenu().performIdentifierAction(header.getItemId(),0);assertFalse(action.isVisible());assertEquals(0,navigations.get());
   nav.getMenu().performIdentifierAction(header.getItemId(),0);assertTrue(action.isVisible());
   nav.getMenu().performIdentifierAction(action.getItemId(),0);assertEquals(1,additions.get());
   add.setEnabled(false);CollapsibleNavigation.applyTree(host,body);assertFalse(action.isEnabled());
   nav.getMenu().performIdentifierAction(action.getItemId(),0);assertEquals(1,additions.get());
   add.setEnabled(true);CollapsibleNavigation.applyTree(host,body);assertTrue(action.isEnabled());
   nav.getMenu().performIdentifierAction(type.getItemId(),0);assertEquals(R.id.chipRetailer,types.getCheckedChipId());assertEquals(1,changes.get());
   nav.getMenu().performIdentifierAction(named(nav,"Bills & orders").getItemId(),0);
   nav.getMenu().performIdentifierAction(R.id.nav_bills,0);assertEquals(1,navigations.get());
   int count=nav.getMenu().size();CollapsibleNavigation.install(host,nav);assertEquals(count,nav.getMenu().size());
  }finally{controller.pause().stop().destroy();}
 }
 @Test public void everyRoleRetainsDestinationsAndDrawersRenderAcrossWidths() throws Exception{
  for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390,600,840}){
   RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");RuntimeEnvironment.setFontScale(width==320?1.5f:1f);
   ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
   try{
    Host host=controller.get();LinearLayout root=new LinearLayout(host);root.setOrientation(1);host.setContentView(root);
    for(int menu:new int[]{R.menu.menu_admin_drawer,R.menu.menu_customer_drawer,R.menu.menu_user_drawer,R.menu.menu_delivery_drawer}){
     NavigationView nav=new NavigationView(host);nav.inflateMenu(menu);root.addView(nav);
     java.util.List<Integer> ids=new java.util.ArrayList<>();for(int i=0;i<nav.getMenu().size();i++){MenuItem row=nav.getMenu().getItem(i);if(row.isVisible()&&!row.hasSubMenu())ids.add(row.getItemId());}
     CollapsibleNavigation.install(host,nav);for(int id:ids)assertNotNull(nav.getMenu().findItem(id));
     assertNull("Retired policy sections must stay hidden",nav.getMenu().findItem(R.id.nav_privacy));
     if(menu==R.menu.menu_admin_drawer){
      MenuItem header=named(nav,"Customers");if(!header.getTitle().toString().endsWith("▴"))nav.getMenu().performIdentifierAction(header.getItemId(),0);
      nav.measure(View.MeasureSpec.makeMeasureSpec(Math.min(width,320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));nav.layout(0,0,nav.getMeasuredWidth(),nav.getMeasuredHeight());
      Bitmap bitmap=Bitmap.createBitmap(nav.getMeasuredWidth(),nav.getMeasuredHeight(),Bitmap.Config.ARGB_8888);nav.draw(new Canvas(bitmap));File dir=new File(System.getProperty("portal.snapshot.dir"),"sidebar-review");dir.mkdirs();
      try(FileOutputStream stream=new FileOutputStream(new File(dir,"sidebar-"+width+(night?"-dark":"-light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
     }
     root.removeView(nav);
    }
   }finally{controller.pause().stop().destroy();}
  }
 }
}
