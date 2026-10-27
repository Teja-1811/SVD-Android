package com.svd.svdagencies.screenshots;

import android.app.Application;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;
import com.svd.svdagencies.R;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertTrue;

/** Inflate every real resource using the real theme without logging in or making API requests. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=Application.class, manifest=Config.NONE,
        packageName="com.svd.svdagencies")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RedesignLayoutCoverageTest {
    public static class Host extends AppCompatActivity {}

    @Test public void everyPaletteKeepsReadableMaterialColorRoles() {
        int[] themes={R.style.Theme_SVDAgency,R.style.Theme_SVDAgency_Ocean,
                      R.style.Theme_SVDAgency_Forest,R.style.Theme_SVDAgency_User};
        int[][] pairs={
            {com.google.android.material.R.attr.colorOnSurface,com.google.android.material.R.attr.colorSurface},
            {com.google.android.material.R.attr.colorOnPrimary,com.google.android.material.R.attr.colorPrimary},
            {com.google.android.material.R.attr.colorOnSecondary,com.google.android.material.R.attr.colorSecondary},
            {com.google.android.material.R.attr.colorOnPrimaryContainer,com.google.android.material.R.attr.colorPrimaryContainer},
            {com.google.android.material.R.attr.colorOnSecondaryContainer,com.google.android.material.R.attr.colorSecondaryContainer}
        };
        for (boolean night:new boolean[]{false,true}) {
            RuntimeEnvironment.setQualifiers("w390dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
            for(int theme:themes) {
                var controller=Robolectric.buildActivity(Host.class);
                Host activity=controller.get();activity.setTheme(theme);controller.setup();
                try {
                    for(int[] pair:pairs) {
                        int foreground=MaterialColors.getColor(activity,pair[0],0);
                        int background=MaterialColors.getColor(activity,pair[1],0);
                        double contrast=ColorUtils.calculateContrast(foreground,background);
                        assertTrue("Color role contrast "+contrast+" for theme "+theme+" night="+night,contrast>=4.5);
                    }
                } finally { controller.pause().stop().destroy(); }
            }
        }
    }

    @Test public void everyLayoutInflatesAcrossSizesAndAppearances() throws Exception {
        JSONArray entries = new JSONArray(new String(Files.readAllBytes(
                new File(System.getProperty("portal.snapshot.manifest")).toPath()), StandardCharsets.UTF_8));
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (boolean night : new boolean[]{false, true}) {
            for (int width : new int[]{320, 390, 600, 840}) {
                RuntimeEnvironment.setQualifiers("sw"+width+"dp-w"+width+"dp-h844dp-"+(night?"night":"notnight")+"-mdpi");
                var controller = Robolectric.buildActivity(Host.class);
                Host activity = controller.get();
                Configuration config = new Configuration(activity.getResources().getConfiguration());
                config.fontScale = width == 320 ? 1.5f : 1f;
                activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
                activity.setTheme(R.style.Theme_SVDAgency);
                controller.setup();
                try {
                    for (int i=0; i<entries.length(); i++) {
                        String name = entries.getJSONObject(i).getString("name");
                        try {
                            int id = activity.getResources().getIdentifier(name,"layout","com.svd.svdagencies");
                            FrameLayout parent = new FrameLayout(activity);
                            activity.getLayoutInflater().inflate(id, parent, true);
                            setLargeValues(parent);
                            com.svd.svdagencies.utils.CompactUi.applyTree(parent);
                            parent.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY));
                            parent.layout(0,0,width,844);
                            assertFinancialValuesVisible(parent);
                            assertControlTouchTargets(parent);
                            checked++;
                        } catch (Throwable error) {
                            failures.add(name+" at "+width+"dp night="+night+": "+error);
                        }
                    }
                } finally { controller.pause().stop().destroy(); }
            }
        }
        System.out.println("REDESIGN_LAYOUT_COVERAGE: "+checked+" successful layout configurations");
        assertTrue(String.join("\n",failures), failures.isEmpty());
    }

    private void setLargeValues(View view) {
        if (view instanceof TextView && view.getId()==R.id.tvStatValue)
            ((TextView)view).setText("₹1,23,45,678.90");
        if (view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            setLargeValues(((ViewGroup)view).getChildAt(i));
    }

    private void assertFinancialValuesVisible(View view) {
        if (view instanceof TextView && view.getId()==R.id.tvStatValue && view.getVisibility()==View.VISIBLE) {
            TextView text=(TextView)view;
            android.text.Layout layout=text.getLayout();
            if (layout!=null && layout.getLineCount()>0 && text.getWidth()>0) {
                int last=layout.getLineCount()-1;
                assertTrue("Financial amount truncated",layout.getEllipsisCount(last)==0 &&
                        layout.getLineEnd(last)==text.getText().length());
            }
        }
        if (view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            assertFinancialValuesVisible(((ViewGroup)view).getChildAt(i));
    }

    private void assertControlTouchTargets(View view) {
        if(view.getVisibility()!=View.VISIBLE) return;
        boolean control=view instanceof android.widget.Button || view instanceof android.widget.ImageButton ||
                view instanceof android.widget.EditText || view instanceof android.widget.Spinner || view instanceof android.widget.NumberPicker;
        if(control && view.getWidth()>0 && view.getHeight()>0) {
            float minimum=48*view.getResources().getDisplayMetrics().density;
            String name=view.getId()==View.NO_ID?view.getClass().getSimpleName():view.getResources().getResourceEntryName(view.getId());
            assertTrue(name+" has a touch target "+view.getWidth()+"x"+view.getHeight(),
                       view.getWidth()+.5f>=minimum && view.getHeight()+.5f>=minimum);
        }
        // The platform picker exposes virtual increment/decrement actions and owns the wheel touch area.
        if(view instanceof android.widget.NumberPicker) return;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            assertControlTouchTargets(((ViewGroup)view).getChildAt(i));
    }
}
