package me.pompel.elauncher.unit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import me.pompel.elauncher.MainActivity;
import me.pompel.elauncher.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Runs the real layout and lifecycle against Android 4.4, without usage APIs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 19)
public class KitKatLauncherTest {
    private Context context;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear()
                .putInt("number_of_apps_preference", 2).commit();
    }

    @Test public void firstHomeStartInflatesKitKatDrawableWithoutUsageRequest() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            EditText search = activity.findViewById(R.id.search);
            assertNotNull(search.getBackground());
            assertEquals(Color.BLACK, search.getCurrentTextColor());
            LinearLayout home = activity.findViewById(R.id.HomeScreen);
            assertEquals("no last-used slot on KitKat", 2, home.getChildCount());
            assertEquals(Color.BLACK, ((TextView) home.getChildAt(0)).getCurrentTextColor());
            Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addCategory(Intent.CATEGORY_DEFAULT).setPackage(activity.getPackageName());
            assertFalse(activity.getPackageManager().queryIntentActivities(homeIntent, 0).isEmpty());
            assertFalse(PreferenceManager.getDefaultSharedPreferences(context).contains("firstLaunch"));
            assertNull(shadowOf(activity).getNextStartedActivity());
            assertEquals(false, invoke(activity, "hasUsageStatsPermission"));
            assertTrue(((Set<?>) invoke(activity, "listActiveProcessPackages")).isEmpty());
            assertEquals("", invoke(activity, "lastActiveProcessPackage"));
            controller.pause().stop().restart().start().resume();
            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test public void appDrawerSortsAndLaunchesWithoutJava8CollectionApis() throws Exception {
        install("Zulu", "fixture.zulu");
        install("Alpha", "fixture.alpha");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            RecyclerView drawer = activity.findViewById(R.id.recycler_view);
            me.pompel.elauncher.recyclerAdapter adapter = (me.pompel.elauncher.recyclerAdapter) drawer.getAdapter();
            assertNull(drawer.getItemAnimator());
            // Verify the real data order, regardless of the launcher itself also being listed.
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("appList");
            field.setAccessible(true);
            java.util.List<me.pompel.elauncher.App> apps =
                    (java.util.List<me.pompel.elauncher.App>) field.get(activity);
            int alpha = -1, zulu = -1;
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).packageId.equals("fixture.alpha")) alpha = i;
                if (apps.get(i).packageId.equals("fixture.zulu")) zulu = i;
            }
            assertTrue(alpha >= 0 && zulu > alpha);
            Method openDrawer = MainActivity.class.getDeclaredMethod("changeLayout", boolean.class, boolean.class);
            openDrawer.setAccessible(true);
            openDrawer.invoke(activity, false, false);
            drawer.measure(android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.EXACTLY));
            drawer.layout(0, 0, 600, 800);
            RecyclerView.ViewHolder holder = drawer.findViewHolderForAdapterPosition(alpha);
            assertNotNull(holder);
            holder.itemView.performClick();
            Intent launched = shadowOf(activity).getNextStartedActivity();
            assertNotNull(launched);
            assertEquals("fixture.alpha", launched.getComponent().getPackageName());
            android.widget.Filter filter = adapter.getFilter();
            Method perform = filter.getClass().getDeclaredMethod("performFiltering", CharSequence.class);
            perform.setAccessible(true);
            Object results = perform.invoke(filter, "Al");
            Method publish = filter.getClass().getDeclaredMethod("publishResults", CharSequence.class,
                    Class.forName("android.widget.Filter$FilterResults"));
            publish.setAccessible(true);
            publish.invoke(filter, "Al", results);
            assertEquals("a single filtered result launches", "fixture.alpha",
                    shadowOf(activity).getNextStartedActivity().getComponent().getPackageName());
        }
    }

    private Object invoke(MainActivity activity, String name) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(activity);
    }

    private void install(String label, String pkg) {
        PackageInfo info = new PackageInfo();
        info.packageName = pkg;
        info.applicationInfo = new ApplicationInfo();
        info.applicationInfo.packageName = pkg;
        info.applicationInfo.nonLocalizedLabel = label;
        ActivityInfo activity = new ActivityInfo();
        activity.name = pkg + ".MainActivity";
        activity.packageName = pkg;
        activity.applicationInfo = info.applicationInfo;
        activity.nonLocalizedLabel = label;
        info.activities = new ActivityInfo[]{activity};
        shadowOf(context.getPackageManager()).addPackage(info);
        ResolveInfo resolve = new ResolveInfo();
        resolve.activityInfo = activity;
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(launcher, resolve);
        Intent packaged = new Intent(launcher).setPackage(pkg);
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(packaged, resolve);
        Intent frontDoor = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_INFO).setPackage(pkg);
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(frontDoor, resolve);
    }
}
