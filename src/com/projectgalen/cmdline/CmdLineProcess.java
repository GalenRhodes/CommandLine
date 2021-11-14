package com.projectgalen.cmdline;

/* =================================================================================================================================
 *     PROJECT: CommandLine
 *    FILENAME: CmdLiine.java
 *     PACKAGE: com.projectgalen.cmdline
 *         IDE: AppCode
 *      AUTHOR: Galen Rhodes
 *        DATE: 11/13/2021
 * DESCRIPTION:
 *
 * Copyright © 2021 Project Galen. All rights reserved.
 *
 * "It can hardly be a coincidence that no language on Earth has ever produced the expression 'As pretty as an airport.' Airports
 * are ugly. Some are very ugly. Some attain a degree of ugliness that can only be the result of special effort."
 * - Douglas Adams from "The Long Dark Tea-Time of the Soul"
 *
 * Permission to use, copy, modify, and distribute this software for any purpose with or without fee is hereby granted, provided
 * that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * ============================================================================================================================== */

import com.projectgalen.cmdline.annotations.CmdFlag;
import com.projectgalen.cmdline.annotations.CmdParam;
import com.projectgalen.cmdline.annotations.CmdSubCommand;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class CmdLineProcess {

    private final Class<?>[] __SUBS__;
    private Object __TARGET__ = this;

    public CmdLineProcess(Class<?>... subs) throws CmdLineException {
        this.__SUBS__ = subs;
        for(Class<?> cls : __SUBS__) {
            CmdSubCommand ann = cls.getAnnotation(CmdSubCommand.class);
            if(ann == null) throw new CmdLineException("Class not annotated with CmdSubCommand: %s", cls.getName());
        }
    }

    public abstract int main() throws Exception;

    protected void processArguments(String... args) throws Exception {
        List<String> options = new ArrayList<>();
        int idx = 0;

        if(__SUBS__.length > 0) {
            if(args.length == 0) {
                throw new CmdLineException("Missing command.");
            }
            else {
                for(Class<?> cls : __SUBS__) {
                    CmdSubCommand ann = cls.getAnnotation(CmdSubCommand.class);
                    if(args[0].equals(ann.name())) {
                        __TARGET__ = cls.newInstance();
                        idx++;
                        break;
                    }
                }
            }
        }

        boolean optsOnly = false;

        while(idx < args.length) {
            String str = args[idx++];

            if(optsOnly) {
                options.add(str);
            }
            else if(str.startsWith("--") && str.trim().equals("--")) {
                optsOnly = true;
            }
            else if(str.startsWith("--")) {
                AccessibleObject ao = getField(str.substring(2));
            }
            else if(str.startsWith("-") && str.trim().length() > 1) {
                AccessibleObject ao = getField(str.charAt(1));
            }
        }
    }

    protected AccessibleObject getField(String name) throws CmdLineException {
        Class<?> tcls = __TARGET__.getClass();
        for(Field f : tcls.getFields()) {
            if(isMatch(name, f.getAnnotation(CmdFlag.class), true)) return f;
            if(isMatch(name, f.getAnnotation(CmdParam.class), true)) return f;
        }
        for(Method m : tcls.getMethods()) {
            if(isMatch(name, m.getAnnotation(CmdFlag.class), true)) return validateSetter(m);
            if(isMatch(name, m.getAnnotation(CmdParam.class), true)) return validateSetter(m);
        }
        throw new CmdLineUsageException("Unknown parameter or flag: --%s", name);
    }

    protected AccessibleObject getField(char name) throws CmdLineException {
        Class<?> tcls = __TARGET__.getClass();
        String _name = String.valueOf(name);
        for(Field f : tcls.getFields()) {
            if(isMatch(_name, f.getAnnotation(CmdFlag.class), false)) return f;
            if(isMatch(_name, f.getAnnotation(CmdParam.class), false)) return f;
        }
        for(Method m : tcls.getMethods()) {
            if(isMatch(_name, m.getAnnotation(CmdFlag.class), false)) return validateSetter(m);
            if(isMatch(_name, m.getAnnotation(CmdParam.class), false)) return validateSetter(m);
        }
        throw new CmdLineUsageException("Unknown parameter or flag: -%s", _name);
    }

    private boolean isMatch(String name, CmdFlag flg, boolean isLong) {
        return flg != null && name.equals(isLong ? flg.longName() : flg.shortName());
    }

    private boolean isMatch(String name, CmdParam flg, boolean isLong) {
        return flg != null && name.equals(isLong ? flg.longName() : flg.shortName());
    }

    private AccessibleObject validateSetter(Method m) throws CmdLineException {
        if(m.getParameterCount() == 1 && m.getReturnType() == Void.class) return m;
        throw new CmdLineException("Incorrect method signature.");
    }

    public int handleError(Throwable e) {
        e.printStackTrace(System.err);
        return 1;
    }

    public void launch(String... args) {
        try {
            processArguments(args);
            System.exit(main());
        } catch(Exception e) {
            System.exit(handleError(e));
        }
    }
}
