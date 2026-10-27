package com.svd.svdagencies.screenshots;



import android.app.Application;

import android.content.Context;

import android.content.Intent;

import android.graphics.Bitmap;

import android.graphics.Canvas;

import android.graphics.Color;

import android.os.Bundle;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.GravityCompat;

import androidx.core.widget.NestedScrollView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.chip.Chip;

import com.google.android.material.chip.ChipGroup;

import com.svd.svdagencies.R;

import com.svd.svdagencies.ui.InformationActivity;

import com.svd.svdagencies.ui.ToolsActivity;

import org.json.*;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.robolectric.Robolectric;

import org.robolectric.RobolectricTestRunner;

import org.robolectric.RuntimeEnvironment;

import org.robolectric.android.controller.ActivityController;

import org.robolectric.annotation.Config;

import org.robolectric.annotation.GraphicsMode;

import org.robolectric.annotation.LooperMode;

import org.robolectric.shadows.ShadowLooper;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.util.*;

import static org.junit.Assert.*;



/** Real Android view rendering; no live account or production API is used. */

@RunWith(RobolectricTestRunner.class)

@Config(sdk = 34, application = Application.class, manifest = Config.NONE,

        packageName = "com.svd.svdagencies", qualifiers = "w390dp-h844dp-xhdpi")

@GraphicsMode(GraphicsMode.Mode.NATIVE)

@LooperMode(LooperMode.Mode.PAUSED)

public class LayoutGalleryTest {

    public static class PreviewHost extends AppCompatActivity {}

    private final JSONArray captures = new JSONArray();

    private final JSONArray failures = new JSONArray();

    private JSONArray entries;

    private File output;

    private int sequence;

    private boolean smoke;

    private String currentRole = "admin";



    @Test public void renderAllLayouts() throws Exception {

        output = new File(System.getProperty("portal.snapshot.dir"));

        new File(output,"images").mkdirs();

        smoke = Boolean.parseBoolean(System.getProperty("portal.snapshot.smoke"));

        entries = new JSONArray(new String(Files.readAllBytes(new File(System.getProperty("portal.snapshot.manifest")).toPath()), StandardCharsets.UTF_8));

        List<String> smokeNames = Arrays.asList("admin", "user_home", "admin_bill_summary");

        for (int i=0; i<entries.length(); i++) {

            JSONObject entry=entries.getJSONObject(i);

            String name=entry.getString("name");

            // Retained subscription resources are included in the full inventory review.

            if (smoke && !smokeNames.contains(name)) continue;

            try { renderResource(entry,false); }

            catch (Throwable error) { failure(name,error); }

        }

        if (!smoke) {

            for (String role : Arrays.asList("admin","delivery","customer","user")) {

                String resource = role.equals("admin")?"admin":role.equals("delivery")?"delivery_dashboard":role.equals("user")?"user_portal":"customer";

                try { renderResource(find(resource),true); } catch(Throwable error) { failure(role+"_navigation",error); }

                if(!role.equals("admin") && !role.equals("customer"))try { renderTools(role); } catch(Throwable error) { failure(role+"_tools",error); }

            }

            for (String role : Arrays.asList("customer","user")) {

                for (String page : Arrays.asList("about","contact","privacy","terms","refund","shipment")) {

                    try { renderInformation(role,page); } catch(Throwable error) { failure(role+"_"+page,error); }

                }

                try { renderAccountPage(role,"profile",com.svd.svdagencies.ui.customer.CustomerProfileActivity.class); } catch(Throwable error) { failure(role+"_profile",error); }

                try { renderAccountPage(role,"history",com.svd.svdagencies.ui.customer.CustomerHistoryActivity.class); } catch(Throwable error) { failure(role+"_history",error); }

            }

        }

        if (!smoke) {

            try { renderAccountPage("admin","purchase_reports",com.svd.svdagencies.ui.admin.AdminPurchaseReportsActivity.class); }

            catch(Throwable error) { failure("admin_purchase_reports",error); }

        }

        if (!smoke) {

            try { renderAccountPage("admin","subscription_dashboard",com.svd.svdagencies.ui.admin.AdminSubscriptionsReportActivity.class); }

            catch(Throwable error) { failure("admin_subscription_dashboard",error); }

        }

        if (!smoke) {

            try { renderBundleProducts(); } catch(Throwable error) { failure("admin_offer_bundle_products",error); }

        }

        if (!smoke) {
            try { renderAccountPage("user","subscriptions",com.svd.svdagencies.ui.customer.CustomerSubscriptionsActivity.class); } catch(Throwable error) { failure("user_native_subscriptions",error); }
            try { renderAccountPage("admin","offer_usage",com.svd.svdagencies.ui.admin.AdminOfferUsageActivity.class); } catch(Throwable error) { failure("admin_native_offer_usage",error); }
            try { renderAccountPage("admin","gst_report",com.svd.svdagencies.ui.admin.AdminGstActivity.class); } catch(Throwable error) { failure("admin_native_gst_report",error); }
            try { renderThemePicker(); } catch(Throwable error) { failure("admin_change_theme_dialog",error); }
        }
        saveJson("captures.json",captures);

        saveJson("failures.json",failures);

        System.out.println("LAYOUT_GALLERY: "+captures.length()+" captures; "+failures.length()+" failures. "+output);

        assertEquals(failures.toString(2),0,failures.length());

    }

    private View findTextButton(View root,String text) {

        if(root instanceof android.widget.Button && text.equals(((android.widget.Button)root).getText().toString()))return root;

        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++) {

            View result=findTextButton(((ViewGroup)root).getChildAt(i),text);if(result!=null)return result;

        }

        return null;

    }

    private void renderBundleProducts() throws Exception {

        session("admin");installFixtures();

        ActivityController<com.svd.svdagencies.ui.admin.offers.AdminOffersActivity> controller=Robolectric.buildActivity(com.svd.svdagencies.ui.admin.offers.AdminOffersActivity.class).setup();

        android.app.Dialog dialog=null;

        try {

            okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit().callFactory();

            for(int pass=0;pass<4;pass++){for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(20);ShadowLooper.idleMainLooper();}

            View button=findTextButton(controller.get().findViewById(android.R.id.content),"Products");

            if(button==null)throw new IllegalStateException("Bundle Products action missing");

            button.performClick();ShadowLooper.idleMainLooper();

            dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();

            if(dialog==null)throw new IllegalStateException("Bundle product manager did not open");

            View root=dialog.getWindow().getDecorView();int width=px(root.getContext(),358);

            for(int pass=0;pass<2;pass++) {root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(px(root.getContext(),844),View.MeasureSpec.AT_MOST));root.layout(0,0,width,root.getMeasuredHeight());ShadowLooper.idleMainLooper();}

            capture(root,width,root.getMeasuredHeight(),new JSONObject().put("name","admin_offer_bundle_products").put("title","Bundle products — add, edit or remove").put("role","admin").put("group","Admin").put("kind","Dialog").put("active",true));

        } finally {if(dialog!=null)dialog.dismiss();controller.pause().stop().destroy();}

    }

    private void renderThemePicker() throws Exception {
        session("admin");
        ActivityController<ToolsActivity> controller=Robolectric.buildActivity(ToolsActivity.class).setup();
        try {
            settleSettings();
            View root=controller.get().findViewById(android.R.id.content);
            int width=px(root.getContext(),390),height=fullHeight(root,width,px(root.getContext(),844));
            capture(root,width,height,new JSONObject().put("name","admin_change_theme_dialog").put("title","Settings").put("role","admin").put("group","Admin").put("kind","Screen").put("active",true));
        } finally {controller.pause().stop().destroy();}
    }
    private void settleSettings() throws Exception {
        okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit().callFactory();
        for(int pass=0;pass<4;pass++){for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(20);ShadowLooper.idleMainLooper();}
    }
    private JSONObject find(String name) throws Exception {

        for(int i=0;i<entries.length();i++)if(entries.getJSONObject(i).getString("name").equals(name))return entries.getJSONObject(i);

        throw new IllegalArgumentException(name);

    }

    private void session(String role) {

        currentRole=role;

        RuntimeEnvironment.getApplication().getSharedPreferences("svd_session",Context.MODE_PRIVATE).edit().clear()

            .putString("role",role).putString("token","layout-preview-only")

            .putInt("user_id",90001).putBoolean("notification_permission_pending",false).commit();

        RuntimeEnvironment.getApplication().getSharedPreferences("svd_appearance",Context.MODE_PRIVATE).edit().putInt("palette",0).commit();

    }

    private int layout(String name) { return RuntimeEnvironment.getApplication().getResources().getIdentifier(name,"layout","com.svd.svdagencies"); }

    private int id(String name) { return RuntimeEnvironment.getApplication().getResources().getIdentifier(name,"id","com.svd.svdagencies"); }

    private int px(Context c,int dp) {return Math.round(dp*c.getResources().getDisplayMetrics().density);}

    private ActivityController<PreviewHost> host(String role) {

        session(role);

        ActivityController<PreviewHost> controller=Robolectric.buildActivity(PreviewHost.class);

        controller.get().setTheme(role.equals("user")?R.style.Theme_SVDAgency_User:R.style.Theme_SVDAgency);

        return controller.setup();

    }

    private void renderResource(JSONObject entry,boolean drawer) throws Exception {

        String name=entry.getString("name");String role=entry.getString("role");

        if(name.equals("admin_offers_dashboard") && !drawer) {

            renderAccountPage("admin","offer_bundles",com.svd.svdagencies.ui.admin.offers.AdminOffersActivity.class);

            JSONObject last=captures.getJSONObject(captures.length()-1);last.put("name",name);last.put("title","Offer bundles");last.put("group","Admin");saveJson("captures.json",captures);return;

        }

        if(name.equals("admin_order_timings") && !drawer) {
            renderAccountPage("admin","order_timings",com.svd.svdagencies.ui.admin.AdminOrderTimingsActivity.class);
            JSONObject last=captures.getJSONObject(captures.length()-1);last.put("name",name);last.put("title","Order timings");last.put("group","Admin");saveJson("captures.json",captures);return;
        }
        if(name.equals("admin_routes_dashboard") && !drawer) {

            renderAccountPage("admin","routes",com.svd.svdagencies.ui.admin.routes.AdminRoutesActivity.class);

            JSONObject last=captures.getJSONObject(captures.length()-1);last.put("name",name);last.put("title","Routes");last.put("group","Admin");saveJson("captures.json",captures);return;

        }

        ActivityController<PreviewHost> controller=host(role);

        PreviewHost activity=controller.get();

        try {

            View inflated=activity.getLayoutInflater().inflate(layout(name),null,false);

            boolean fragment=name.matches("(?:customer|user)_(?:home|companies|orders|bills|payment|subscriptions)");

            View root=inflated;

            if(fragment) {

                root=activity.getLayoutInflater().inflate(role.equals("user")?R.layout.user_portal:R.layout.customer,null,false);

                ((FrameLayout)root.findViewById(R.id.customerFragmentContainer)).addView(inflated);

                setText(root,"tvCustomerToolbarTitle",screenTitle(name));

                BottomNavigationView nav=root.findViewById(R.id.customerBottomNav);

                String tab=name.endsWith("companies")?"nav_companies":name.endsWith("orders")?"nav_orders":name.endsWith("bills")?"nav_bills":name.endsWith("payment")?"nav_payment":name.endsWith("subscriptions")?"nav_subscriptions":"nav_home";

                if(nav.getMenu().findItem(id(tab))!=null)nav.setSelectedItemId(id(tab));

            }

            if(name.equals("customer") || name.equals("user_portal")) {

                View home=activity.getLayoutInflater().inflate(role.equals("user")?R.layout.user_home:R.layout.customer_home,null,false);

                ((FrameLayout)root.findViewById(R.id.customerFragmentContainer)).addView(home);

                setText(root,"tvCustomerToolbarTitle",role.equals("user")?"User Portal":"Retailer Portal");

            }

            root.setVisibility(View.VISIBLE);

            activity.setContentView(root);

            if(!fragment) {setText(root,"tvToolbarTitle",screenTitle(name));setText(root,"tvCustomerToolbarTitle",screenTitle(name));}

            populate(root,name,0);

            if(name.equals("admin_customer_profile"))root.findViewById(R.id.customerCardActions).setVisibility(View.GONE);

            populateLists(root,name,entry.optJSONObject("rows"));

            populateDashboardLists(root,name);
            com.svd.svdagencies.utils.CollapsibleNavigation.applyTree(activity,root);

            boolean component=entry.getString("kind").equals("Component") || entry.getString("kind").equals("Dialog");

            int width=px(activity,390),height=component?componentHeight(root,width):px(activity,844);

            measure(root,width,height);

            if(drawer) {

                if(!(root instanceof DrawerLayout))throw new IllegalStateException("No drawer in "+name);

                ((DrawerLayout)root).openDrawer(GravityCompat.START,false);

                measure(root,width,height);

            } else if(!component) height=fullHeight(root,width,height);

            JSONObject record=new JSONObject(entry.toString());

            record.put("name",drawer?name+"_navigation":name);

            record.put("title",drawer?screenTitle(name)+" — Navigation":entry.getString("title"));

            record.put("kind",drawer?"Navigation":entry.getString("kind"));

            capture(root,width,height,record);

        } finally { controller.pause().stop().destroy(); }

    }

    private int componentHeight(View root,int width) {

        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));

        return Math.max(px(root.getContext(),64),Math.min(px(root.getContext(),2400),root.getMeasuredHeight()));

    }

    private boolean fixturesInstalled;

    private void installFixtures() throws Exception {

        if(fixturesInstalled)return;

        java.lang.reflect.Field context=com.svd.svdagencies.App.class.getDeclaredField("context");

        context.setAccessible(true);context.set(null,RuntimeEnvironment.getApplication());

        okhttp3.Interceptor fixture=chain -> {

            String path=chain.request().url().encodedPath();String json="{}";int code=200;

            if(path.contains("business-settings"))json="{\"business\":{\"pan\":\"ABCDE1234F\",\"fssai\":\"20126111000019\",\"trade_licence\":\"TL-2026-001\",\"lin\":\"AP-19-06-008-03954993\",\"details\":\"\"},\"gst\":{\"gstin\":\"\",\"effective_from\":null,\"enabled\":false}}";
            else if(path.contains("profile"))json="{\"profile\":{\"name\":\"Sample Account\",\"phone\":\"9000000001\",\"shop_name\":\"Sample Retail Store\",\"area\":\"Main Road\",\"city\":\"Gundugolanu\",\"state\":\"Andhra Pradesh\",\"pin_code\":\"534427\"}}";

            else if(path.contains("reports/monthly"))json="{\"date\":\"October 2026\",\"total_bills\":24,\"total_amount\":12480,\"average_amount\":520}";

            else if(path.contains("orders/history"))json="{\"count\":1,\"orders\":[{\"order_id\":101,\"order_number\":\"ORD-SAMPLE-101\",\"status\":\"delivered\",\"order_date\":\"24 Oct 2026\",\"delivery_date\":\"25 Oct 2026\",\"grand_total\":384,\"can_delete\":false,\"items\":[]}]}";

            else if(path.endsWith("/routes/"))json="{\"routes\":[{\"id\":1,\"name\":\"Town centre\",\"customer_count\":24},{\"id\":2,\"name\":\"North route\",\"customer_count\":18}]}";

            else if(path.endsWith("/companies/"))json="{\"count\":1,\"companies\":[{\"id\":1,\"name\":\"Dodla\",\"total_items\":12,\"total_qty\":100,\"total_value\":12500}]}";

            else if(path.contains("purchase-report/summary"))json="{\"purchases\":12500,\"paid\":10000,\"balance\":2500,\"gst\":595.24,\"records\":[{\"date\":\"2026-10-24\",\"amount\":12500,\"cgst\":297.62,\"sgst\":297.62,\"igst\":0}]}";

            else if(path.contains("admin/analytics/subscriptions"))json="{\"title\": \"Subscriptions dashboard\", \"totals\": {\"subscriptions\": 12, \"customers\": 10, \"active\": 8, \"paused\": 2, \"cancelled\": 1, \"ended\": 1, \"plan_value\": 12480, \"paid\": 11000, \"refunded\": 500, \"net_paid\": 10500, \"outstanding\": 1480, \"delivered\": 124, \"scheduled\": 56, \"skipped\": 4}, \"groups\": [{\"offer_id\": 1, \"name\": \"Monthly milk plan\", \"customer_count\": 10, \"subscriptions\": 12, \"active\": 8, \"paused\": 2, \"cancelled\": 1, \"ended\": 1, \"plan_value\": 12480, \"paid\": 11000, \"refunded\": 500, \"delivered\": 124}], \"records\": [{\"id\": 1, \"customer\": \"Sample Customer\", \"phone\": \"9000000001\", \"fields\": {\"plan\": \"Monthly milk plan\", \"status\": \"active\", \"frequency\": \"Daily\", \"start_date\": \"2026-10-01\", \"end_date\": \"2026-10-31\", \"next_delivery\": \"2026-10-26\", \"total\": 1040, \"paid\": 1040, \"refunded\": 0, \"net_paid\": 1040, \"outstanding\": 0, \"delivered\": 24, \"remaining\": 7, \"address\": \"Main Road, Gundugolanu\", \"delivery_statuses\": {\"delivered\": 24, \"scheduled\": 7}, \"items\": [\"Milk 500 ml \\u00d7 1 \\u00b7 \\u20b932 each\"], \"deliveries\": [\"2026-10-25: delivered\"]}}], \"catalog\": [{\"id\": 1, \"name\": \"Monthly milk plan\"}], \"statuses\": [{\"id\": \"active\", \"name\": \"Active\"}, {\"id\": \"paused\", \"name\": \"Paused\"}, {\"id\": \"cancelled\", \"name\": \"Cancelled\"}, {\"id\": \"ended\", \"name\": \"Ended\"}], \"error\": \"\", \"page\": 1, \"pages\": 1}";

            else if(path.contains("offers/dashboard"))json="{\"offers\": [{\"id\": 1, \"name\": \"Family dairy bundle\", \"description\": \"Included products sold together for one price.\", \"price\": 100, \"offer_for\": \"user\", \"offer_type\": \"bundle\", \"start_date\": \"2026-10-25\", \"end_date\": \"2026-11-25\", \"is_active\": false, \"items\": [{\"id\": 1, \"item_id\": 1, \"item_name\": \"Milk 500 ml\", \"buy_qty\": 2, \"offer_qty\": 0}, {\"id\": 2, \"item_id\": 2, \"item_name\": \"Curd 500 g\", \"buy_qty\": 1, \"offer_qty\": 0}]}], \"items\": [{\"id\": 1, \"name\": \"Milk 500 ml\"}, {\"id\": 2, \"name\": \"Curd 500 g\"}, {\"id\": 3, \"name\": \"Butter 100 g\"}]}";

            else if(path.equals("/api/customer/subscription-offers/"))json="{\"offers\":[{\"id\":1,\"name\":\"Monthly milk plan\",\"total_amount\":960,\"per_delivery_amount\":32,\"duration_value\":30,\"duration_unit\":\"days\",\"delivery_frequency\":\"daily\",\"items\":[]}]}";
            else if(path.equals("/api/customer/subscriptions/"))json="{\"subscriptions\":[]}";
            else code=503;

            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(code).message("Local preview fixture").body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"),json)).build();

        };

        for(retrofit2.Retrofit retrofit:Arrays.asList(com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit(),com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getAuthRetrofit())) {

            java.lang.reflect.Field interceptors=okhttp3.OkHttpClient.class.getDeclaredField("interceptors");interceptors.setAccessible(true);

            interceptors.set(retrofit.callFactory(),Collections.singletonList(fixture));

            for(retrofit2.CallAdapter.Factory factory:retrofit.callAdapterFactories()) {

                try {

                    java.lang.reflect.Field executor=factory.getClass().getDeclaredField("callbackExecutor");executor.setAccessible(true);

                    executor.set(factory,(java.util.concurrent.Executor)command -> new android.os.Handler(android.os.Looper.getMainLooper()).post(command));

                } catch(NoSuchFieldException ignored) {}

            }

        }

        fixturesInstalled=true;

    }

    private <T extends AppCompatActivity> void renderAccountPage(String role,String page,Class<T> type) throws Exception {

        session(role);installFixtures();

        ActivityController<T> controller=Robolectric.buildActivity(type).setup();

        try {

            okhttp3.OkHttpClient client=(okhttp3.OkHttpClient)com.svd.svdagencies.data.api.auth.ApiClient.INSTANCE.getRetrofit().callFactory();

            for(int pass=0;pass<4;pass++){for(int i=0;i<100 && client.dispatcher().runningCallsCount()>0;i++)Thread.sleep(20);ShadowLooper.idleMainLooper();}

            View root=controller.get().findViewById(android.R.id.content);

            int width=px(root.getContext(),390),height=fullHeight(root,width,px(root.getContext(),844));

            capture(root,width,height,new JSONObject().put("name",role+"_native_"+page).put("title",human(page)).put("role",role).put("group",role.equals("admin")?"Admin":role.equals("user")?"User":"Retailer").put("kind","Screen").put("active",true));

        } finally {controller.pause().stop().destroy();}

    }

    private void renderInformation(String role,String page) throws Exception {

        session(role);

        Intent intent=InformationActivity.Companion.intent(RuntimeEnvironment.getApplication(),page);

        ActivityController<InformationActivity> controller=Robolectric.buildActivity(InformationActivity.class,intent).setup();

        try {

            View root=controller.get().findViewById(android.R.id.content);

            int width=px(root.getContext(),390),height=px(root.getContext(),844);

            height=fullHeight(root,width,height);

            JSONObject record=new JSONObject().put("name",role+"_information_"+page).put("title",human(page)+" — Native page")

                .put("role",role).put("group",role.equals("user")?"User":"Retailer").put("kind","Screen").put("active",true);

            capture(root,width,height,record);

        } finally {controller.pause().stop().destroy();}

    }

    private void renderTools(String role) throws Exception {

        session(role);

        ActivityController<ToolsActivity> controller=Robolectric.buildActivity(ToolsActivity.class).setup();

        try {
            settleSettings();

            View root=controller.get().findViewById(android.R.id.content);

            int width=px(root.getContext(),390),height=fullHeight(root,width,px(root.getContext(),844));

            JSONObject record=new JSONObject().put("name",role+"_native_tools").put("title","Settings")

                .put("role",role).put("group",human(role.equals("customer")?"retailer":role)).put("kind","Screen").put("active",true);

            capture(root,width,height,record);

        } finally {controller.pause().stop().destroy();}

    }

    private void measure(View root,int width,int height) {
        com.svd.svdagencies.utils.CompactUi.applyTree(root);

        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));

        root.layout(0,0,width,height);

        ShadowLooper.idleMainLooper();

        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));

        root.layout(0,0,width,height);

    }

    private int scrollOverflow(View view) {

        int value=0;

        if(view.getVisibility()!=View.VISIBLE)return 0;

        if(view instanceof NestedScrollView || view instanceof ScrollView) {

            ViewGroup group=(ViewGroup)view;

            if(group.getChildCount()>0)value=Math.max(0,group.getChildAt(0).getMeasuredHeight()-view.getMeasuredHeight()+view.getPaddingTop()+view.getPaddingBottom());

        }

        if(view instanceof ViewGroup) {

            ViewGroup group=(ViewGroup)view;

            for(int i=0;i<group.getChildCount();i++)value=Math.max(value,scrollOverflow(group.getChildAt(i)));

        }

        return value;

    }

    private int fullHeight(View root,int width,int height) {

        measure(root,width,height);

        int extra=scrollOverflow(root);

        height=Math.min(px(root.getContext(),9000),height+extra);

        measure(root,width,height);

        return height;

    }

    private void capture(View root,int width,int height,JSONObject record) throws Exception {

        measure(root,width,height);

        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);

        Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.rgb(244,247,248));root.draw(canvas);

        String filename=String.format(Locale.US,"%03d_%s.png",++sequence,record.getString("name"));

        try(FileOutputStream stream=new FileOutputStream(new File(output,"images/"+filename))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}

        bitmap.recycle();

        record.put("image","images/"+filename).put("width",width).put("height",height).put("sampleData",true);

        captures.put(record);saveJson("captures.json",captures);

        System.out.println("CAPTURED "+filename+" "+width+"x"+height);

    }

    private void saveJson(String name,JSONArray data) throws Exception {

        Files.write(new File(output,name).toPath(),data.toString(2).getBytes(StandardCharsets.UTF_8));

    }

    private void failure(String name,Throwable error) throws Exception {

        Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();

        failures.put(new JSONObject().put("name",name).put("error",cause.toString()));

        System.out.println("CAPTURE_FAILED "+name+" "+cause);error.printStackTrace();

        saveJson("failures.json",failures);

    }

    private String human(String value) {

        if(value.isEmpty())return value;

        String text=value.replace('_',' ').replaceAll("([a-z])([A-Z])","$1 $2");

        return Character.toUpperCase(text.charAt(0))+text.substring(1);

    }

    private String screenTitle(String name) {

        if(name.equals("admin"))return "Milk Agency";

        if(name.equals("customer"))return "Retailer Portal";

        if(name.equals("user_portal"))return "User Portal";

        return human(name.replaceFirst("^(admin|customer|user|delivery|activity)_", "").replace("dashboard","").trim());

    }

    private void setText(View root,String name,String value) {View found=root.findViewById(id(name));if(found instanceof TextView)((TextView)found).setText(value);}

    private String resourceName(View view) {if(view.getId()<=0)return "";try{return view.getResources().getResourceEntryName(view.getId());}catch(Exception ignored){return "";}}

    private String sample(String id,String page,int row) {

        String key=id.toLowerCase(Locale.US);

        String[] products={"Milk 500 ml","Curd 500 g","Butter 100 g","Paneer 200 g"};

        if(page.equals("admin_bill_create")) {
            if(key.equals("tvtotalqty"))return "36";
            if(key.equals("tvopeningdue"))return "₹1,250.00";
            if(key.equals("tvgrandtotal"))return "₹2,402.00";
        }
        if(page.equals("delivery_bill_to_customer") && key.equals("tvgrandtotal"))return "₹0.00";
        if(key.equals("btnsubscriptiondeliverydate"))return "26 Oct 2026";
        if(key.equals("etsubscriptionoffername"))return "Monthly dairy plan";

        if(key.equals("etsubscriptionduration"))return "30";

        if(key.equals("etsubscriptionitemprice"))return "32.00";

        if(key.contains("toolbartitle"))return null;

        if(key.equals("tvbannertitle"))return "Fresh dairy offers";

        if(key.equals("tvbannersubtitle"))return "Explore this week’s selected products";

        if(key.contains("itemname"))return products[row%4];

        if(key.contains("itemcategory"))return "Dairy essentials";

        if(key.contains("stockqty"))return "48 units";

        if(key.contains("offername"))return "Weekly dairy offer";

        if(key.contains("offertype"))return "Selected products";

        if(key.equals("tvwelcome"))return "Welcome back";

        if(key.equals("usergreeting"))return "Hello, Sample User";

        if(key.equals("useraccountstatus"))return "Your personal dairy account";

        if(key.equals("tvshop"))return "Sample Retail Store";

        if(key.equals("tvphone"))return "Mobile: 90000 00001";

        if(key.equals("tvcustomername"))return "Sample Customer "+(row+1);

        if(key.equals("tvcustomerdetail"))return currentRole.equals("user")?"Orders, subscriptions and bills":"Orders, support and invoices";

        if(key.equals("tvstatlabel"))return null;

        if(key.equals("tvstatvalue"))return null;

        if(key.contains("productname") || key.equals("tvitemname") || key.equals("tvname") && page.contains("item"))return products[row%4];

        if(key.contains("customername") || key.equals("tvcustomer") || key.equals("tvdetailname"))return "Sample Customer "+(row+1);

        if(key.contains("companyname"))return "Sample Dairy";

        if(key.contains("billnumber") || key.equals("tvbillid"))return String.format(Locale.US,"INV-%04d",101+row);

        if(key.contains("orderid"))return "ORD-0101";

        if(key.contains("phone"))return "90000 00001";

        if(key.contains("selectedmonth") || key.equals("etmonth"))return "October 2026";

        if(key.contains("date") && !key.contains("update") && !key.contains("status"))return "2026-10-24";

        if(key.equals("tvprice") || key.contains("sellingprice"))return "₹32.00";

        if(key.equals("tvmrp"))return "₹34.00";

        if(key.equals("tvmargin"))return "Margin: ₹2.00";

        if(key.equals("tvqty") || key.contains("quantity"))return "12";

        if(key.equals("tvdisc"))return "0%";
        if(key.equals("tvdiscamount"))return "₹0.00";

        if(key.contains("wallet") || key.equals("tvbalance"))return "₹1,250.00";

        if(key.contains("totalamount") || key.equals("tvtotal") || key.contains("totalinvoice"))return "₹384.00";

        if(key.contains("totalbills") || key.contains("billcount"))return "24";

        if(key.contains("itemcount"))return "24";

        if(key.equals("tvbilldate"))return "24 Oct 2026";

        if(key.equals("tvstatusbadge"))return page.contains("payment")?"Verified":"Paid";

        if(key.contains("txnid") || key.contains("transactionid"))return "TXN-SAMPLE-1001";

        if(key.equals("tvdetailshop"))return "Sample Retail Store";

        if(key.equals("tvsubject"))return "Delivery enquiry";

        return null;

    }

    private void populate(View root,String page,int row) { if(resourceName(root).equals("productImage") && root instanceof android.widget.ImageView)((android.widget.ImageView)root).setImageResource(R.drawable.ic_milk_placeholder);

        if(root instanceof com.airbnb.lottie.LottieAnimationView) {

            com.airbnb.lottie.LottieAnimationView animation=(com.airbnb.lottie.LottieAnimationView)root;

            animation.setFailureListener(error -> System.out.println("Static preview: animation skipped"));animation.cancelAnimation();

        }

        if(root instanceof com.google.android.material.navigation.NavigationView) {

            com.google.android.material.navigation.NavigationView nav=(com.google.android.material.navigation.NavigationView)root;

            if(nav.getHeaderCount()>0){setText(nav.getHeaderView(0),"tvCustomerName",currentRole.equals("user")?"User Access":"Retailer Access");}

            return;

        }

        if(root instanceof ProgressBar){root.setVisibility(View.GONE);return;}

        if(root instanceof SwipeRefreshLayout)((SwipeRefreshLayout)root).setRefreshing(false);

        if(root instanceof TextView) {

            TextView text=(TextView)root;String key=resourceName(root);String value=sample(key,page,row);

            if(root instanceof EditText){((EditText)root).setCursorVisible(false);text.clearFocus();}

            if(root instanceof AutoCompleteTextView) {

                AutoCompleteTextView field=(AutoCompleteTextView)root;

                String hint=String.valueOf(field.getHint()).toLowerCase(Locale.US);

                if(key.toLowerCase(Locale.US).contains("customer") || hint.contains("customer"))value="All customers";

                else if(key.toLowerCase(Locale.US).contains("company"))value="All companies";

                else if(key.toLowerCase(Locale.US).contains("route"))value="All routes";

                if(value!=null)field.setText(value,false);

            } else if(value!=null && (!(root instanceof Button) || key.equals("btnSubscriptionDeliveryDate")))text.setText(value);

            if(text.getText().toString().toLowerCase(Locale.US).contains("loading")) {

                if(root instanceof EditText)text.setText("");else text.setText("Sample records shown");

            }

            if(root instanceof TextView && !(root instanceof EditText) && text.getText().length()==0 && page.equals("delivery_dropdown_item"))text.setText("All customers");

        }

        if(root instanceof Spinner) {

            String key=resourceName(root).toLowerCase(Locale.US);

            String value=key.contains("year")?"2026":key.contains("month")?"October":key.contains("customer")?"All customers":key.contains("company")?"All companies":key.contains("route")?"All routes":"All";

            if(key.contains("durationunit"))value="Days";

            if(key.contains("frequency"))value="Daily";

            if(key.equals("spinnersubscriptionitem") || key.equals("spinnerofferproduct"))value="Milk 500 ml";

            if(key.equals("spinnerofferfor"))value="Retailers";

            ((Spinner)root).setAdapter(new ArrayAdapter<>(root.getContext(),R.layout.delivery_dropdown_item,Collections.singletonList(value)));

        }

        if(root instanceof ChipGroup && ((ChipGroup)root).getChildCount()==0) {

            for(String label:Arrays.asList("All","Milk","Curd","Butter")) {

                Chip chip=new Chip(root.getContext());chip.setText(label);chip.setCheckable(true);chip.setChecked(label.equals("All"));((ChipGroup)root).addView(chip);

            }

        }

        if(root instanceof ViewGroup) {

            ViewGroup group=(ViewGroup)root;

            for(int i=0;i<group.getChildCount();i++)populate(group.getChildAt(i),page,row);

        }

        String key=resourceName(root);

        if(root instanceof ViewGroup && root.findViewById(id("tvStatLabel"))!=null && !key.isEmpty() && (key.startsWith("card") || key.startsWith("stat"))) {

            String label=human(key.replaceFirst("^(card|stat)",""));

            if(label.trim().isEmpty())label="Total";

            setText(root,"tvStatLabel",label);

            setText(root,"tvStatValue",label.toLowerCase(Locale.US).contains("customer")?"75":label.toLowerCase(Locale.US).contains("item")?"24":label.toLowerCase(Locale.US).contains("bill")?"24":"₹12,480.00");

        }

    }

    private String rowLayout(String page,String key,JSONObject rows) {

        if(rows!=null && rows.has(key))return rows.optString(key);

        String lower=key.toLowerCase(Locale.US);

        if(lower.contains("offer"))return page.startsWith("user")?"user_offer_card":"customer_offer_banner";

        if(lower.contains("stockproduct"))return "user_stock_product_card";

        if(lower.contains("invoice"))return "customer_statement_invoice";

        if(lower.contains("payment"))return page.startsWith("admin")?"admin_customer_payment_row":"customer_payment_row";

        if(lower.contains("quer"))return "customer_raised_query_item";

        if(lower.contains("bill"))return page.startsWith("admin")?"admin_bill_card":page.startsWith("delivery")?"delivery_today_bill":"customer_invoice_card";

        if(lower.contains("customer"))return "admin_customer_card";

        if(key.equals("welcomeProducts"))return "item_welcome_product";

        if(page.contains("orders") || lower.contains("product"))return page.startsWith("admin")?"admin_order_card":"customer_order_product";

        if(page.contains("companies") || page.contains("catalog"))return page.startsWith("admin")?"admin_company_card":page.startsWith("user")?"user_catalog_item_card":"customer_catalog_item_card";

        if(page.contains("stock"))return "admin_stock_info_item";

        if(page.contains("items"))return "admin_item_card";

        if(page.contains("enquir"))return "admin_enquiry_card";

        return "customer_catalog_item_card";

    }

    private void populateLists(View view,String page,JSONObject rows) {

        if(view instanceof com.google.android.material.navigation.NavigationView)return;

        if(view instanceof RecyclerView) {

            RecyclerView recycler=(RecyclerView)view;String key=resourceName(view);String resource=rowLayout(page,key,rows);

            if(layout(resource)==0)return;

            boolean horizontal=key.toLowerCase(Locale.US).contains("offer") || key.toLowerCase(Locale.US).contains("stockproduct") || key.equals("rvCompanyFilter") || key.equals("rvCategoryFilter") || key.equals("welcomeProducts");

            if(key.equals("rvItemCatalog")) recycler.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(view.getContext(),2));
            else recycler.setLayoutManager(new LinearLayoutManager(view.getContext(),horizontal?RecyclerView.HORIZONTAL:RecyclerView.VERTICAL,false));

            recycler.setNestedScrollingEnabled(false);recycler.setItemAnimator(null);recycler.setAdapter(new SampleAdapter(resource,horizontal));recycler.setVisibility(View.VISIBLE);

            return;

        }

        if(view instanceof ViewGroup) {

            ViewGroup group=(ViewGroup)view;

            for(int i=0;i<group.getChildCount();i++)populateLists(group.getChildAt(i),page,rows);

        }

        String key=resourceName(view).toLowerCase(Locale.US);

        if(key.contains("empty") || key.equals("layoutnoorders"))view.setVisibility(View.GONE);

        if(view instanceof SwipeRefreshLayout)view.setVisibility(View.VISIBLE);

    }

    private class SampleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final String resource;private final boolean horizontal;

        SampleAdapter(String resource,boolean horizontal){this.resource=resource;this.horizontal=horizontal;}

        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,int type) {

            View row=LayoutInflater.from(parent.getContext()).inflate(layout(resource),parent,false);

            if(horizontal && !resource.contains("filter") && !resource.equals("item_welcome_product"))row.setLayoutParams(new RecyclerView.LayoutParams(px(parent.getContext(),300),ViewGroup.LayoutParams.WRAP_CONTENT));

            return new RecyclerView.ViewHolder(row){};

        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder,int position){populate(holder.itemView,resource,position);}

        @Override public int getItemCount(){return resource.equals("delivery_bill_item_select")?4:3;}

    }

    private void populateDashboardLists(View root,String page) {

        if(!page.equals("admin"))return;

        String[][] sections={{"layoutOrdersList","admin_pending_order_preview_item"},{"layoutEnquiriesList","admin_enquiry_preview_item"},{"layoutTopSellingList","admin_top_selling_item"},{"layoutActiveOffersList","admin_offer_preview_item"},{"layoutTopDuesList","admin_top_due_customer_item"},{"layoutTopStockList","admin_top_stock_item"}};

        for(String[] section:sections){View found=root.findViewById(id(section[0]));if(found instanceof LinearLayout){LinearLayout group=(LinearLayout)found;group.removeAllViews();for(int i=0;i<2;i++){View item=LayoutInflater.from(group.getContext()).inflate(layout(section[1]),group,false);populate(item,section[1],i);group.addView(item);}}}

    }

}
