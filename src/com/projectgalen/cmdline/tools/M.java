package com.projectgalen.cmdline.tools;

public class M extends Resources {

    private M() { super("Messages"); }

    public static String get(String key) {
        return getInstance().getString(key);
    }

    public static M getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        private static final M INSTANCE = new M();
    }
}
