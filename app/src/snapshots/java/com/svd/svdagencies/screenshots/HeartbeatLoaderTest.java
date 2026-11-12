package com.svd.svdagencies.screenshots;
import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.material.button.MaterialButton;
import com.svd.svdagencies.R;
import com.svd.svdagencies.utils.*;
import java.io.*;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies", qualifiers="w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HeartbeatLoaderTest {
 @Test public void overlayWaitsForAllLoadingSourcesAndStopsWhenHidden() throws Exception {
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);FrameLayout page=new FrameLayout(a);View anchor=new View(a);page.addView(anchor);a.setContentView(page);
   LoadingOverlayManager.INSTANCE.show(a);LoadingOverlayManager.INSTANCE.show(anchor);LoadingOverlayManager.INSTANCE.show(a);
   ViewGroup content=a.findViewById(android.R.id.content);assertEquals(2,content.getChildCount());
   FrameLayout overlay=(FrameLayout)content.getChildAt(1);HeartbeatLogoView logo=(HeartbeatLogoView)overlay.getChildAt(0);
   assertEquals(View.VISIBLE,overlay.getVisibility());assertEquals("Loading",logo.getContentDescription());
   LoadingOverlayManager.INSTANCE.hide(a);assertEquals(View.VISIBLE,overlay.getVisibility());
   Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900));
   int width=logo.getLayoutParams().width,height=logo.getLayoutParams().height;assertEquals(width,height);
   logo.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));logo.layout(0,0,width,height);
   Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);logo.draw(new Canvas(bitmap));
   assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(0,0)));
   File output=new File(System.getProperty("portal.snapshot.dir"),"heartbeat-loader.png");output.getParentFile().mkdirs();try(FileOutputStream stream=new FileOutputStream(output)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
   LoadingOverlayManager.INSTANCE.hide(anchor);assertEquals(View.GONE,overlay.getVisibility());assertFalse(logo.isAnimating());
  }
 }
 @Test public void legacyStatusTextUsesLogoAndRestoresAfterLoading(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);android.widget.TextView status=new android.widget.TextView(a);status.setText("Loading settings...");a.setContentView(status);
   CompactUi.applyTree(status);assertEquals(0f,status.getAlpha(),0f);
   ViewGroup content=a.findViewById(android.R.id.content);assertEquals(2,content.getChildCount());assertEquals(View.VISIBLE,content.getChildAt(1).getVisibility());
   status.setText("Ready");assertEquals(1f,status.getAlpha(),0f);assertEquals(View.GONE,content.getChildAt(1).getVisibility());
  }
 }
 @Test public void gestureRefreshUsesLogoAndNeverDrawsTheNativeSpinner() throws Exception {
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);AppSwipeRefreshLayout swipe=new AppSwipeRefreshLayout(a);swipe.addView(new View(a));a.setContentView(swipe);
   final int[] calls={0};swipe.setOnRefreshListener(()->calls[0]++);
   java.lang.reflect.Field field=androidx.swiperefreshlayout.widget.SwipeRefreshLayout.class.getDeclaredField("mListener");field.setAccessible(true);
   ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener)field.get(swipe)).onRefresh();
   assertEquals(1,calls[0]);assertTrue(swipe.isRefreshing());
   ViewGroup content=a.findViewById(android.R.id.content);assertEquals(2,content.getChildCount());assertEquals(View.VISIBLE,content.getChildAt(1).getVisibility());
   boolean found=false;for(int i=0;i<swipe.getChildCount();i++){View child=swipe.getChildAt(i);if(child.getClass().getName().equals("androidx.swiperefreshlayout.widget.CircleImageView")){found=true;assertEquals(1f,child.getAlpha(),0f);assertEquals(0f,child.getElevation(),0f);}}
   assertTrue(found);swipe.setRefreshing(false);assertEquals(View.GONE,content.getChildAt(1).getVisibility());
  }
 }
 @Test public void actualPullGestureInvokesRefreshAfterItsAnimation() throws Exception {
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);AppSwipeRefreshLayout swipe=new AppSwipeRefreshLayout(a);swipe.addView(new View(a));a.setContentView(swipe);
   int size=(int)(360*a.getResources().getDisplayMetrics().density);swipe.measure(View.MeasureSpec.makeMeasureSpec(size,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size*2,View.MeasureSpec.EXACTLY));swipe.layout(0,0,size,size*2);swipe.setDistanceToTriggerSync(40);
   final int[] calls={0};swipe.setOnRefreshListener(()->calls[0]++);long down=android.os.SystemClock.uptimeMillis();
   int[] actions={android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE,android.view.MotionEvent.ACTION_MOVE,android.view.MotionEvent.ACTION_UP};float[] ys={30,80,350,350};
   for(int i=0;i<actions.length;i++){android.view.MotionEvent event=android.view.MotionEvent.obtain(down,down+i*50,actions[i],size/2f,ys[i],0);swipe.dispatchTouchEvent(event);event.recycle();}
   assertTrue((Boolean)org.robolectric.util.ReflectionHelpers.getField(swipe,"mRefreshing"));
   // Robolectric does not dispatch the legacy child animation-end callback on drawn frames.
   View indicator=null;for(int i=0;i<swipe.getChildCount();i++){View child=swipe.getChildAt(i);if(child.getClass().getName().equals("androidx.swiperefreshlayout.widget.CircleImageView"))indicator=child;}
   assertNotNull(indicator);assertNotNull(indicator.getAnimation());org.robolectric.util.ReflectionHelpers.callInstanceMethod(indicator,"onAnimationEnd");
   assertEquals(1,calls[0]);assertTrue(swipe.isRefreshing());swipe.setRefreshing(false);
  }
 }
 @Test public void lateLoadingMessagesAreDetectedWithoutChangingButtonLoaders(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);FrameLayout page=new FrameLayout(a);a.setContentView(page);
   ViewGroup content=a.findViewById(android.R.id.content);LoadingStatus.INSTANCE.install(content);Shadows.shadowOf(Looper.getMainLooper()).idle();
   MaterialButton button=new MaterialButton(a);button.setText("Loading...");page.addView(button);
   content.getViewTreeObserver().dispatchOnGlobalLayout();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,content.getChildCount());assertEquals(1f,button.getAlpha(),0f);
   android.widget.TextView status=new android.widget.TextView(a);status.setText("Loading report...");page.addView(status);
   content.getViewTreeObserver().dispatchOnGlobalLayout();Shadows.shadowOf(Looper.getMainLooper()).idle();
   assertEquals(0f,status.getAlpha(),0f);assertEquals(2,content.getChildCount());assertEquals(View.VISIBLE,content.getChildAt(1).getVisibility());
   status.setText("Ready");assertEquals(1f,status.getAlpha(),0f);assertEquals(View.GONE,content.getChildAt(1).getVisibility());
  }
 }
 @Test public void invoiceLinksRemainWholeAcrossFormattingBreaks(){
  String path="/milk_agency/public-invoice/3272/token:signature/";
  assertEquals("https://svd-dqw3.onrender.com"+path,ShareLinks.INSTANCE.normalize("https://svd-dqw3.onr \n\u200bender.com"+path,"https://svd-dqw3.onrender.com/"));
  assertEquals("https://www.svdagencies.shop"+path,ShareLinks.INSTANCE.normalize("https://www.svdagencies.shop"+path,"https://svd-dqw3.onrender.com/"));
  assertEquals("https://svd-dqw3.onrender.com"+path,ShareLinks.INSTANCE.normalize(path,"https://svd-dqw3.onrender.com/"));
  assertNull(ShareLinks.INSTANCE.normalize("javascript:alert(1)","https://svd-dqw3.onrender.com/"));
  assertNull(ShareLinks.INSTANCE.normalize("", "https://svd-dqw3.onrender.com/"));
 }
 @Test public void buttonLoaderKeepsItsOriginalSpinnerBehavior(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();a.setTheme(R.style.Theme_SVDAgency);MaterialButton button=new MaterialButton(a);button.setText("Save");a.setContentView(button);
   ViewExtensionsKt.showLoading(button,true,"Saving...");assertFalse(button.isEnabled());assertEquals("Saving...",button.getText().toString());assertTrue(button.getIcon() instanceof android.graphics.drawable.Animatable);
   assertEquals(1,((ViewGroup)a.findViewById(android.R.id.content)).getChildCount());
   ViewExtensionsKt.showLoading(button,false,null);assertTrue(button.isEnabled());assertEquals("Save",button.getText().toString());
  }
 }
}
