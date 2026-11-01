package me.pompel.elauncher;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.view.Window;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loaded only behind SDK >= 21 guards, keeping Dalvik away from newer types. */
@TargetApi(21)
final class Api21 {
    private Api21() { }

    static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        return ops != null && ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    static void setSystemBarColors(Window window, int color) {
        window.setStatusBarColor(color);
        window.setNavigationBarColor(color);
    }

    static Set<String> activePackages(Context context) {
        Set<String> packages = new HashSet<>();
        long end = System.currentTimeMillis();
        long begin = end - 60 * 60 * 1000L;
        List<UsageStats> stats = query(context, begin, end);
        if (stats != null) {
            for (UsageStats usage : stats) {
                if (usage.getLastTimeUsed() >= begin) packages.add(usage.getPackageName());
            }
        }
        return packages;
    }

    static String lastPackage(Context context, Set<String> excluded) {
        long end = System.currentTimeMillis();
        long begin = end - 2 * 60 * 60 * 1000L;
        long latest = begin;
        String last = "";
        List<UsageStats> stats = query(context, begin, end);
        if (stats != null) {
            for (UsageStats usage : stats) {
                String pkg = usage.getPackageName();
                if (usage.getLastTimeUsed() < latest || excluded.contains(pkg)) continue;
                if (context.getPackageManager().getLaunchIntentForPackage(pkg) == null) continue;
                latest = usage.getLastTimeUsed();
                last = pkg;
            }
        }
        return last;
    }

    // The API-22 String constant is inlined; its service is available since API 21.
    @SuppressLint("InlinedApi")
    private static List<UsageStats> query(Context context, long begin, long end) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        return manager == null ? null : manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end);
    }
}
