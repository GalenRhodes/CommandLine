package com.projectgalen.cmdline.tools;

public final class U {
    private static final M m = M.getInstance();

    private U()                                       { }

    public static <T> T ifNull(T obj, T defaultValue) { return ((obj == null) ? defaultValue : obj); }

    public static int[] getNonRepeating(int count, int lowerBound, int upperBound) {
        int rangeCount = (upperBound - lowerBound);

        if(count <= 0) throw new IllegalArgumentException(m.getString("msg.err.001"));
        if(rangeCount <= 0) throw new IllegalArgumentException(m.getString("msg.err.002"));
        if(rangeCount < count) {
            String msg = String.format(m.getString("msg.err.003"), lowerBound, upperBound, count);
            throw new IllegalArgumentException(msg);
        }

        int[] res = new int[count];
        res[0] = randomInRange(lowerBound, upperBound);

        for(int i = 1; i < count; i++) {
            int r = randomInRange(lowerBound, upperBound);
            while(contains(res, r)) r = randomInRange(lowerBound, upperBound);
            res[i] = r;
        }

        return res;
    }

    public static int randomInRange(int lowerBound, int upperBound) { return ((int)(Math.random() * (upperBound - lowerBound))) + lowerBound; }

    public static boolean contains(int[] ar, int value) {
        for(int i : ar) if(i == value) return true;
        return false;
    }

    @SafeVarargs
    public static <T> boolean contains(T value, T... args) {
        for(T other : args) if(value.equals(other)) return true;
        return false;
    }

    public static boolean isArrayOf(Class<?> cls, Class<?> compCls) {
        return (cls.isArray() && cls.getComponentType().isAssignableFrom(compCls));
    }
}
