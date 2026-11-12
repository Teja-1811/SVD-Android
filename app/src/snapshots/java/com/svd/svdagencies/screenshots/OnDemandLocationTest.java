package com.svd.svdagencies.screenshots;

import android.Manifest;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import com.svd.svdagencies.utils.ForegroundAgentLocation;
import com.svd.svdagencies.utils.OnDemandAgentLocation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE, packageName="com.svd.svdagencies")
public class OnDemandLocationTest {
    private Application app;
    @Before public void prepare() {
        app = RuntimeEnvironment.getApplication();
        app.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit().clear()
            .putString("token", "test-token").putString("role", "delivery").putInt("user_id", 76).commit();
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        app.getSystemService(NotificationManager.class).cancelAll();
    }
    private Map<String,String> request() {
        Map<String,String> data = new HashMap<>();
        data.put("request_id", "0173ee15-e3a0-4b56-87c5-899b7d8d6000");
        data.put("agent_id", "76");
        data.put("expires_at_ms", Long.toString(System.currentTimeMillis()+120000));
        return data;
    }
    @Test public void oldContinuousOptInNeverRestartsGps() {
        app.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit().putBoolean("share_background_location", true).commit();
        assertFalse(ForegroundAgentLocation.INSTANCE.enabled(app));
        assertFalse(OnDemandAgentLocation.INSTANCE.enabled(app));
    }
    @Test public void automaticConsentBelongsToOneSignedInAgent() {
        OnDemandAgentLocation.INSTANCE.setEnabled(app, true);
        assertTrue(OnDemandAgentLocation.INSTANCE.enabled(app));
        app.getSharedPreferences("svd_session", Context.MODE_PRIVATE).edit().putInt("user_id", 2).commit();
        assertFalse(OnDemandAgentLocation.INSTANCE.enabled(app));
    }
    @Test public void automaticOffShowsNotificationWithoutStartingGps() {
        OnDemandAgentLocation.INSTANCE.receive(app, request());
        assertEquals(1, app.getSystemService(NotificationManager.class).getActiveNotifications().length);
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
    @Test public void automaticOnStartsOnlyTheShortRequestService() {
        OnDemandAgentLocation.INSTANCE.setEnabled(app, true);
        OnDemandAgentLocation.INSTANCE.receive(app, request());
        Intent started = Shadows.shadowOf(app).getNextStartedService();
        assertNotNull(started);
        assertEquals("com.svd.svdagencies.utils.AgentLocationRequestService", started.getComponent().getClassName());
        assertEquals(request().get("request_id"), started.getStringExtra("location_request_id"));
    }
    @Test public void expiredOrOtherAgentRequestsDoNotNotifyOrStartGps() {
        OnDemandAgentLocation.INSTANCE.setEnabled(app, true);
        Map<String,String> data = request(); data.put("agent_id", "2");
        OnDemandAgentLocation.INSTANCE.receive(app, data);
        data = request(); data.put("expires_at_ms", Long.toString(System.currentTimeMillis()-1));
        OnDemandAgentLocation.INSTANCE.receive(app, data);
        assertEquals(0, app.getSystemService(NotificationManager.class).getActiveNotifications().length);
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
    @Test public void backgroundPermissionMissingRequiresNotificationTap() {
        OnDemandAgentLocation.INSTANCE.setEnabled(app, true);
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        OnDemandAgentLocation.INSTANCE.receive(app, request());
        assertEquals(1, app.getSystemService(NotificationManager.class).getActiveNotifications().length);
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
}
