package com.projectgalen.cmdline;

/* =====================================================================================================================
 *     PROJECT: CommandLine
 *    FILENAME: CmdLineProcess.java
 *     PACKAGE: com.projectgalen.cmdline
 *         IDE: IntelliJ
 *      AUTHOR: Galen Rhodes
 *        DATE: 11/13/2021
 * DESCRIPTION:
 *
 * Copyright © 2021 Project Galen. All rights reserved.
 *
 * "It can hardly be a coincidence that no language on Earth has ever produced the expression 'As pretty as an airport.'
 *  Airports are ugly. Some are very ugly. Some attain a degree of ugliness that can only be the result of special
 *  effort."
 * - Douglas Adams from "The Long Dark Tea-Time of the Soul"
 *
 * Permission to use, copy, modify, and distribute this software for any purpose with or without fee is hereby granted,
 * provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
 * AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 * ================================================================================================================== */

import com.projectgalen.cmdline.annotations.CmdFlag;
import com.projectgalen.cmdline.annotations.CmdParam;
import com.projectgalen.cmdline.annotations.CmdSubCommand;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class CmdLineProcess {

    private final Class<?>[] __SUBS__;
    private       Object     __TARGET__ = this;

    public CmdLineProcess(Class<?>... subs) throws CmdLineException {
        this.__SUBS__ = subs;
        for(Class<?> cls : __SUBS__) {
            CmdSubCommand ann = cls.getAnnotation(CmdSubCommand.class);
            if(ann == null) throw new CmdLineException("Class not annotated with CmdSubCommand: %s", cls.getName());
        }
    }

    public abstract int main() throws Exception;

    private void processArguments(String... args) throws Exception {
        List<String> options = new ArrayList<>();
        int          idx     = 0;

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
                Foo x = getField(str.substring(2));

                if(x.an instanceof CmdParam) {
                    if(idx >= args.length) throw new CmdLineUsageException("Missing value for parameter: --%s", str);
                }
                else if(x.an instanceof CmdFlag) {
                }
            }
            else if(str.startsWith("-") && str.trim().length() > 1) {
            }
        }
    }

    private void setFieldValue(Field fld, String value) throws CmdLineException {
        try {
            if(fld.getType() == String.class) {
                fld.set(__TARGET__, value);
            }
            else if(fld.getType() == Byte.class) {
                fld.set(__TARGET__, Byte.valueOf(value));
            }
            else if(fld.getType() == Short.class) {
                fld.set(__TARGET__, Short.valueOf(value));
            }
            else if(fld.getType() == Integer.class) {
                fld.set(__TARGET__, Integer.valueOf(value));
            }
            else if(fld.getType() == Long.class) {
                fld.set(__TARGET__, Long.valueOf(value));
            }
            else if(fld.getType() == Float.class) {
                fld.set(__TARGET__, Float.valueOf(value));
            }
            else if(fld.getType() == Double.class) {
                fld.set(__TARGET__, Double.valueOf(value));
            }
            else if(fld.getType() == BigDecimal.class) {
                fld.set(__TARGET__, new BigDecimal(value));
            }
            else if(fld.getType() == BigInteger.class) {
                fld.set(__TARGET__, new BigInteger(value));
            }
            else if(fld.getType().isAssignableFrom(Date.class)) {
                fld.set(__TARGET__, new SimpleDateFormat().parse(value));
            }
        } catch(Exception e) {
            throw new CmdLineException(e);
        }
    }

    private Foo getField(String name) throws CmdLineException {
        Class<?> tcls = __TARGET__.getClass();

        for(Field f : tcls.getFields()) {
            CmdFlag anFlag = f.getAnnotation(CmdFlag.class);
            if(isMatch(name, anFlag, false)) return new Foo(f, anFlag);
            CmdParam anParam = f.getAnnotation(CmdParam.class);
            if(isMatch(name, anParam, false)) return new Foo(f, anParam);
        }

        for(Method m : tcls.getMethods()) {
            CmdFlag anFlag = m.getAnnotation(CmdFlag.class);
            if(isMatch(name, anFlag, false)) return new Foo(validateSetter(m), anFlag);
            CmdParam anParam = m.getAnnotation(CmdParam.class);
            if(isMatch(name, anParam, false)) return new Foo(validateSetter(m), anParam);
        }

        throw new CmdLineUsageException("Unknown Parameter or Flag: --%s", name);
    }

    private Foo getField(char name) throws CmdLineException {
        Class<?> tcls  = __TARGET__.getClass();
        String   _name = String.valueOf(name);

        for(Field f : tcls.getFields()) {
            CmdFlag anFlag = f.getAnnotation(CmdFlag.class);
            if(isMatch(_name, anFlag, false)) return new Foo(f, anFlag);
            CmdParam anParam = f.getAnnotation(CmdParam.class);
            if(isMatch(_name, anParam, false)) return new Foo(f, anParam);
        }

        for(Method m : tcls.getMethods()) {
            CmdFlag anFlag = m.getAnnotation(CmdFlag.class);
            if(isMatch(_name, anFlag, false)) return new Foo(validateSetter(m), anFlag);
            CmdParam anParam = m.getAnnotation(CmdParam.class);
            if(isMatch(_name, anParam, false)) return new Foo(validateSetter(m), anParam);
        }

        throw new CmdLineUsageException("Unknown Parameter or Flag: --%s", _name);
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

    private static class Foo {
        AccessibleObject ao;
        Annotation       an;

        private Foo(AccessibleObject ao, Annotation an) {
            this.ao = ao;
            this.an = an;
        }
    }
}
