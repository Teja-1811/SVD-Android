package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.content.Intent;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.button.MaterialButton;
import com.svd.svdagencies.R;
import com.svd.svdagencies.utils.AdminSidebar;
import com.svd.svdagencies.utils.CollapsibleNavigation;
import com.svd.svdagencies.ui.admin.AdminBaseActivity;
import java.io.*;
import java.util.*;
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
 private NavigationView nav(Host host) {
  LinearLayout root=new LinearLayout(host);root.setOrientation(1);host.setContentView(root);
  NavigationView nav=new NavigationView(host);nav.inflateMenu(R.menu.menu_admin_drawer);root.addView(nav);return nav;
 }
 private MenuItem row(NavigationView nav,int id){MenuItem item=nav.getMenu().findItem(id);assertNotNull(item);return item;}
 @Test public void allWebsiteDestinationsHaveNativeRoutesAndCorrectActionExtras(){
  ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
  try{
   Host host=controller.get();NavigationView nav=nav(host);int count=0;
   List<String> roots=new ArrayList<>();
   for(int i=0;i<nav.getMenu().size();i++){
    MenuItem parent=nav.getMenu().getItem(i);roots.add(parent.getTitle().toString());
    if(parent.hasSubMenu())for(int j=0;j<parent.getSubMenu().size();j++){
     assertNotNull(AdminSidebar.intentFor(host,parent.getSubMenu().getItem(j).getItemId()));count++;
    }else if(parent.getItemId()!=R.id.nav_logout){assertNotNull(AdminSidebar.intentFor(host,parent.getItemId()));count++;}
   }
   assertEquals(37,count);
   assertEquals(Arrays.asList("Dashboard","Customer","Bills","Stock","Products","Companies","Routes","Cashbook","Statement","Monthly Statement","Offers and Subscriptions","Enquiries","Settings","Category Sales","Delivery Data","Due Details","GST","Log out"),roots);
   assertEquals("delivery",AdminSidebar.intentFor(host,R.id.nav_customers).getStringExtra("DEFAULT_CUSTOMER_TYPE"));
   assertEquals("retailer",AdminSidebar.intentFor(host,R.id.nav_retailers).getStringExtra("DEFAULT_CUSTOMER_TYPE"));
   assertEquals("deleted",AdminSidebar.intentFor(host,R.id.nav_deleted_customers).getStringExtra("history_kind"));
   assertEquals("orders",AdminSidebar.intentFor(host,R.id.nav_orders_history).getStringExtra("history_kind"));
   assertEquals(R.id.btnRecordLeakage,AdminSidebar.intentFor(host,R.id.nav_add_leakage).getIntExtra(AdminSidebar.ACTION,0));
   assertEquals(R.id.btnCommission,AdminSidebar.intentFor(host,R.id.nav_add_commission).getIntExtra(AdminSidebar.ACTION,0));
   assertEquals("gst",AdminSidebar.intentFor(host,R.id.nav_gst_configuration).getStringExtra("settings_section"));
   assertEquals("licences",AdminSidebar.intentFor(host,R.id.nav_licences).getStringExtra("settings_section"));
  }finally{controller.pause().stop().destroy();}
 }
 @Test public void groupsToggleWithoutNavigationAndDoNotDuplicateScreenActions(){
  ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
  try{
   Host host=controller.get();NavigationView nav=nav(host);AtomicInteger navigations=new AtomicInteger();
   nav.setNavigationItemSelectedListener(item->{navigations.incrementAndGet();return true;});
   MaterialButton add=new MaterialButton(host);add.setId(R.id.fabAddCustomer);add.setText("Add Customer");((LinearLayout)nav.getParent()).addView(add);
   host.getSharedPreferences("milk_agency_sidebar",0).edit().clear().commit();
   CollapsibleNavigation.install(host,nav);
   assertEquals(View.VISIBLE,add.getVisibility());
   assertEquals("Customer",row(nav,R.id.nav_section_customer).getTitle());
   assertFalse(row(nav,R.id.nav_add_customer).isVisible());
   nav.getMenu().performIdentifierAction(R.id.nav_section_customer,0);
   assertTrue(row(nav,R.id.nav_add_customer).isVisible());assertEquals(0,navigations.get());
   nav.getMenu().performIdentifierAction(R.id.nav_section_customer,0);
   assertFalse(row(nav,R.id.nav_add_customer).isVisible());assertEquals(0,navigations.get());
   int count=nav.getMenu().size();CollapsibleNavigation.install(host,nav);assertEquals(count,nav.getMenu().size());
   int adds=0;for(int i=0;i<count;i++)if(nav.getMenu().getItem(i).getTitle().toString().trim().equals("Add Customer"))adds++;
   assertEquals(1,adds);assertEquals(47,count);
   nav.getMenu().performIdentifierAction(R.id.nav_section_bills,0);
   nav.getMenu().performIdentifierAction(R.id.nav_bills,0);assertEquals(1,navigations.get());
  }finally{controller.pause().stop().destroy();}
 }
 @Test public void actionExtrasAreConsumedOnceEvenWhenQueuedTwice(){
  ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
  try{
   Host host=controller.get();LinearLayout root=new LinearLayout(host);host.setContentView(root);
   MaterialButton action=new MaterialButton(host);action.setId(R.id.btnAddRoute);root.addView(action);
   AtomicInteger clicks=new AtomicInteger();action.setOnClickListener(v->clicks.incrementAndGet());
   host.setIntent(new Intent().putExtra(AdminSidebar.ACTION,R.id.btnAddRoute));
   AdminSidebar.runPendingAction(host,true);AdminSidebar.runPendingAction(host,true);
   Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,clicks.get());
   assertFalse(host.getIntent().hasExtra(AdminSidebar.ACTION));
  }finally{controller.pause().stop().destroy();}
 }
 @Test public void sidebarFitsPhoneAndTabletWidthsWithLargeFonts() throws Exception{
  for(boolean night:new boolean[]{false,true})for(int width:new int[]{320,390,600,840}){
   RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");RuntimeEnvironment.setFontScale(1.5f);
   ActivityController<Host> controller=Robolectric.buildActivity(Host.class).setup();
   try{
    Host host=controller.get();NavigationView nav=nav(host);CollapsibleNavigation.install(host,nav);
    nav.getMenu().performIdentifierAction(R.id.nav_section_customer,0);
    assertTrue(nav.getLayoutParams().width<=width-40);assertEquals(2,nav.getItemMaxLines());
    nav.measure(View.MeasureSpec.makeMeasureSpec(nav.getLayoutParams().width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));nav.layout(0,0,nav.getMeasuredWidth(),nav.getMeasuredHeight());
    assertTrue(nav.getMeasuredWidth()>0);assertEquals(844,nav.getMeasuredHeight());
    Bitmap bitmap=Bitmap.createBitmap(nav.getMeasuredWidth(),844,Bitmap.Config.ARGB_8888);nav.draw(new Canvas(bitmap));
    File dir=new File(System.getProperty("portal.snapshot.dir"),"milk-sidebar");dir.mkdirs();
    try(FileOutputStream stream=new FileOutputStream(new File(dir,"sidebar-"+width+(night?"-dark":"-light")+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
   }finally{controller.pause().stop().destroy();}
  }
 }
}
