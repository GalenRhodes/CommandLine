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
import com.projectgalen.cmdline.tools.M;

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

    private static final M m = M.getInstance();

    private final Class<?>[] __SUBS__;
    private       Object     __TARGET__ = this;

    public CmdLineProcess(Class<?>... subs) throws CmdLineException {
        this.__SUBS__ = subs;
        for(Class<?> cls : __SUBS__) {
            CmdSubCommand ann = cls.getAnnotation(CmdSubCommand.class);
            if(ann == null) throw new CmdLineException(m.getString("msg.err.sub_not_annotated"), cls.getName());
        }
    }

    public abstract int main() throws Exception;

    private void processArguments(String... args) throws Exception {
        List<String> options = new ArrayList<>();
        int          idx     = 0;

        if(__SUBS__.length > 0) {
            if(args.length == 0) {
                throw new CmdLineException(m.getString("msg.err.missing_command"));
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
        String  ddash    = m.getString("ddash");
        String  dash     = m.getString("dash");

        while(idx < args.length) {
            String str = args[idx++];

            if(optsOnly) {
                options.add(str);
            }
            else {
                if(str.startsWith(ddash) && str.trim().equals(ddash)) {
                    optsOnly = true;
                }
                else if(str.startsWith(ddash)) {
                    Foo x = _getField(str.substring(2), true);

                    if(x.an instanceof CmdParam) {
                        if(idx >= args.length) {
                            throw new CmdLineUsageException(m.getString("msg.err.missing_value"), str);
                        }
                    }
                    else if(x.an instanceof CmdFlag) {
                    }
                }
                else if(str.startsWith(dash) && str.trim().length() > 1) {
                    Foo x = _getField(str.substring(1, 2), false);
                }
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
        }
        catch(Exception e) {
            throw new CmdLineException(e);
        }
    }

    private Foo getField(String name) throws CmdLineException {
        return _getField(name, true);
    }

    private Foo getField(char name) throws CmdLineException {
        return _getField(String.valueOf(name), false);
    }

    private Foo _getField(String name, boolean isLong) throws CmdLineException {
        Class<?> tcls  = __TARGET__.getClass();
        String   pName = String.format("%s%s", m.getString(isLong ? "ddash" : "dash"), name);

        for(Field field : tcls.getFields()) {
            CmdFlag anFlag = field.getAnnotation(CmdFlag.class);
            if(isMatch(name, anFlag, isLong)) return new Foo(field, anFlag);
            CmdParam anParam = field.getAnnotation(CmdParam.class);
            if(isMatch(name, anParam, isLong)) return new Foo(field, anParam);
        }

        for(Method method : tcls.getMethods()) {
            CmdFlag anFlag = method.getAnnotation(CmdFlag.class);
            if(isMatch(name, anFlag, isLong)) return new Foo(validateSetter(method), anFlag);
            CmdParam anParam = method.getAnnotation(CmdParam.class);
            if(isMatch(name, anParam, isLong)) return new Foo(validateSetter(method), anParam);
        }

        throw new CmdLineUsageException(m.getString("msg.err.unknown.parameter"), pName);
    }

    private boolean isMatch(String name, CmdFlag flg, boolean isLong) {
        return flg != null && name.equals(isLong ? flg.longName() : flg.shortName());
    }

    private boolean isMatch(String name, CmdParam flg, boolean isLong) {
        return flg != null && name.equals(isLong ? flg.longName() : flg.shortName());
    }

    private AccessibleObject validateSetter(Method method) throws CmdLineException {
        if(method.getParameterCount() == 1 && method.getReturnType() == Void.class) return method;
        throw new CmdLineException(m.getString("msg.err.bad_meth_sig"));
    }

    public int handleError(Throwable e) {
        e.printStackTrace(System.err);
        return 1;
    }

    public void launch(String... args) {
        try {
            processArguments(args);
            System.exit(main());
        }
        catch(Exception e) {
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
