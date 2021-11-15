package com.projectgalen.cmdline.tools;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resources extends ResourceBundle {

    public static final Pattern  RX_MACRO         = Pattern.compile("(?<!\\\\)\\$\\{(\\w+(?:\\.\\w+)*)}");
    public static final String   PAT_SLASH        = "(\\\\\\\\)";
    public static final String   SUB_SLASH        = Matcher.quoteReplacement("\\");
    public static final String   MSG_ERR_NULL_KEY = "Key cannot be null.";
    public static final String[] ZOO              = { "␉", "␋", "␑", "␒", "␓", "␔", "␖", "␘", "␕", "␞", "␜", "␝", "␐" };

    private final List<ResourceBundle> bundles = new ArrayList<>();
    private final Vector<String>       allKeys = new Vector<>();
    private final ReentrantLock        lock    = new ReentrantLock();

    public Resources(String bundleName) {
        bundles.add(loadBundle(bundleName));
    }

    public void addBundle(String bundleName) {
        lock.lock();
        try {
            bundles.add(0, loadBundle(bundleName));
            allKeys.clear();
        }
        finally { lock.unlock(); }
    }

    private ResourceBundle loadBundle(String bundleName) {
        return ResourceBundle.getBundle(bundleName, Locale.getDefault(), Resources.class.getClassLoader());
    }

    @Override
    protected Object handleGetObject(String key) {
        lock.lock();
        try {
            String value = _get(key);
            return ((value == null) ? null : _replaceMacros(value));
        }
        finally { lock.unlock(); }
    }

    @Override
    public Enumeration<String> getKeys() {
        lock.lock();
        try {
            if(allKeys.size() == 0) {
                // Reload the kids...
                Set<String> s = new TreeSet<>();
                for(ResourceBundle b : bundles) s.addAll(b.keySet());
                allKeys.addAll(s);
            }
        }
        finally { lock.unlock(); }
        return allKeys.elements();
    }

    private String _get(String key) {
        if(key == null) throw new NullPointerException(MSG_ERR_NULL_KEY);
        for(ResourceBundle b : bundles) { try { return b.getString(key); } catch(Exception ignored) { } }
        return null;
    }

    private String _replaceMacros(String str) {
        String  q = _getSlashReplacement();
        String  s = str.replaceAll(PAT_SLASH, q);
        Matcher m = _getMatcher(s);
        return (m.find() ? _processMacro(m) : s).replaceAll(Pattern.quote(q), SUB_SLASH);
    }

    private Matcher _getMatcher(String s) { synchronized(RX_MACRO) { return RX_MACRO.matcher(s); } }

    private String _processMacro(Matcher m) {
        StringBuffer sb = new StringBuffer();
        do { m.appendReplacement(sb, Matcher.quoteReplacement(U.ifNull(getString(m.group(1)), m.group()))); }
        while(m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private String _getSlashReplacement() {
        int[] t1 = U.getNonRepeating(2, 0, ZOO.length);
        return (ZOO[t1[0]] + ZOO[t1[1]]);
    }
}
