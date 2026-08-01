package io.github.pigerzhu.onelab.contract;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Packages whose split ratio is applied inside the target application process. */
public final class SplitViewRatioPackages {
    public static final Set<String> APP_SIDE_PACKAGES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "com.coolapk.market",
                    "com.jingdong.app.mall",
                    "com.tencent.mm",
                    "com.ss.android.lark",
                    "com.tongcheng.android"
            )));

    private SplitViewRatioPackages() {
    }
}
