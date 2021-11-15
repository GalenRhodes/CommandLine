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
import com.projectgalen.cmdline.annotations.CmdOption;
import com.projectgalen.cmdline.annotations.CmdParam;
import com.projectgalen.cmdline.annotations.CmdSubCommand;
import com.projectgalen.cmdline.tools.M;
import com.projectgalen.cmdline.tools.U;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public abstract class CmdLineProcess {

    private static final M        m           = M.getInstance();
    private static final String[] BOOL_TRUE   = m.getString("bool.true").split("\\|");
    private static final String[] BOOL_VALUES = m.getString("bool.values").split("\\|");

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

    private void processArguments(Args args) throws Exception {
        Foo          optionsAO = getOptionsField();
        List<String> options   = new ArrayList<>();

        if(__SUBS__.length > 0) {
            if(args.args.length == 0) {
                throw new CmdLineException(m.getString("msg.err.missing_command"));
            }
            else {
                for(Class<?> cls : __SUBS__) {
                    CmdSubCommand ann = cls.getAnnotation(CmdSubCommand.class);
                    if(args.args[0].equals(ann.name())) {
                        __TARGET__ = cls.newInstance();
                        args.idx++;
                        break;
                    }
                }
            }
        }

        boolean onlyOptsRemaining = false;
        String  ddash             = m.getString("ddash");
        String  dash              = m.getString("dash");

        while(args.hasMore()) {
            String str = args.next();

            if(onlyOptsRemaining) {
                options.add(str);
            }
            else {
                if(str.equals(ddash)) {
                    onlyOptsRemaining = true;
                }
                else if(str.startsWith(ddash)) {
                    checkOptionsState(optionsAO, options);
                    handleParameterOrFlag(args, str, _getField(str.substring(ddash.length()), true), args12 -> (args12.hasMore() ? args12.next() : null));
                }
                else if(str.startsWith(dash)) {
                    checkOptionsState(optionsAO, options);
                    int l1 = dash.length();
                    int l2 = (l1 + 1);
                    int sl = str.length();
                    handleParameterOrFlag(args, str, _getField(str.substring(l1, l2), false), args1 -> ((sl > l2) ? str.substring(l2) : (args1.hasMore() ? args1.next() : null)));
                }
                else {
                    if(optionsAO == null) throw new CmdLineUsageException(m.getString("msg.err.no_opts_allowed"), str);
                    options.add(str);
                }
            }
        }

        if(optionsAO != null) {
            try {
                if(optionsAO.ao instanceof Field) ((Field)optionsAO.ao).set(__TARGET__, options.toArray(new String[0]));
                else ((Method)optionsAO.ao).invoke(__TARGET__, (Object)options.toArray(new String[0]));
            }
            catch(Exception e) {
                throw new CmdLineException(e);
            }
        }
    }

    private void checkOptionsState(Foo optionsAO, List<String> options) throws CmdLineUsageException {
        if(optionsAO != null && ((CmdOption)optionsAO.an).atEndOnly() && options.size() > 0) {
            throw new CmdLineUsageException(m.getString("msg.err.opts_at_end"));
        }
    }

    private void handleParameterOrFlag(Args args, String str, Foo x, GetParamValue z) throws CmdLineException {
        if(x.an instanceof CmdParam) {
            String v = z.get(args);
            if(v == null) {
                throw new CmdLineUsageException(m.getString("msg.err.missing_value"), str);
            }
            setParamValue(x.ao, v);
        }
        else if(x.an instanceof CmdFlag) {
            setFlag(x.ao);
        }
        else {
            throw new RuntimeException(m.getString("msg.err.004"));
        }
    }

    private void setFlag(AccessibleObject ao) throws CmdLineException {
        Class<?> type = getAOType(ao);
        if(type.isAssignableFrom(Boolean.class)) setAccessibleObject(ao, true);
        else if(type.isAssignableFrom(Character.class)) setAccessibleObject(ao, 't');
        else if(type.isAssignableFrom(String.class)) setAccessibleObject(ao, "true");
        else if(type.isAssignableFrom(Byte.class)) setAccessibleObject(ao, (byte)1);
        else if(type.isAssignableFrom(Short.class)) setAccessibleObject(ao, (short)1);
        else if(type.isAssignableFrom(Integer.class)) setAccessibleObject(ao, 1);
        else if(type.isAssignableFrom(Float.class)) setAccessibleObject(ao, (float)1);
        else if(type.isAssignableFrom(Double.class)) setAccessibleObject(ao, (double)1);
        else if(type.isAssignableFrom(BigDecimal.class)) setAccessibleObject(ao, BigDecimal.ONE);
        else if(type.isAssignableFrom(BigInteger.class)) setAccessibleObject(ao, BigInteger.ONE);
        else throw new CmdLineException("Cannot store flag value in type: %s", type.getName());
    }

    private Class<?> getAOType(AccessibleObject ao) {
        return (ao instanceof Field) ? ((Field)ao).getType() : ((Method)ao).getParameterTypes()[0];
    }

    private void setAccessibleObject(AccessibleObject ao, Object value) throws CmdLineException {
        try {
            if(ao instanceof Field) {
                ((Field)ao).set(__TARGET__, value);
            }
            else {
                ((Method)ao).invoke(__TARGET__, value);
            }
        }
        catch(Exception e) {
            throw new CmdLineException(e);
        }
    }

    private void setParamValue(AccessibleObject ao, String value) throws CmdLineException {
        try {
            Class<?> type   = getAOType(ao);
            Object   cValue = null;

            if(type.isAssignableFrom(String.class)) {
                cValue = value;
            }
            else if(type.isAssignableFrom(Boolean.class)) {
                if(U.contains(value.toLowerCase(), BOOL_VALUES)) {
                    cValue = U.contains(value.toLowerCase(), BOOL_TRUE);
                }
                else throw new CmdLineUsageException(m.getString("msg.err.param.type.bool"));
            }
            else if(type.isAssignableFrom(Byte.class)) {
                try { cValue = Byte.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.byte"));
                }
            }
            else if(type.isAssignableFrom(Short.class)) {
                try { cValue = Short.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.short"));
                }
            }
            else if(type.isAssignableFrom(Integer.class)) {
                try { cValue = Integer.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.int"));
                }
            }
            else if(type.isAssignableFrom(Long.class)) {
                try { cValue = Long.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.long"));
                }
            }
            else if(type.isAssignableFrom(Float.class)) {
                try { cValue = Float.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.float"));
                }
            }
            else if(type.isAssignableFrom(Double.class)) {
                try { cValue = Double.valueOf(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.double"));
                }
            }
            else if(type.isAssignableFrom(BigDecimal.class)) {
                try { cValue = new BigDecimal(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.bd"));
                }
            }
            else if(type.isAssignableFrom(BigInteger.class)) {
                try { cValue = new BigInteger(value); }
                catch(Exception e) {
                    throw new CmdLineUsageException(m.getString("msg.err.param.type.bi"));
                }
            }
            else throw new CmdLineException(m.getString("msg.err.bad_param_field_type"), type.getName());

            ao.setAccessible(true);
            if(ao instanceof Field) ((Field)ao).set(__TARGET__, cValue);
            else ((Method)ao).invoke(__TARGET__, cValue);
        }
        catch(Exception e) {
            throw new CmdLineException(e);
        }
    }

    private Foo getOptionsField() throws CmdLineException {
        for(Field field : __TARGET__.getClass().getFields()) {
            CmdOption an = field.getAnnotation(CmdOption.class);
            if(an != null) {
                if(U.isArrayOf(field.getType(), String.class)) return new Foo(field, an);
                throw new CmdLineException(m.getString("msg.err.opts_field"));
            }
        }
        for(Method method : __TARGET__.getClass().getMethods()) {
            CmdOption an = method.getAnnotation(CmdOption.class);
            if(an != null) {
                Class<?>[] p  = method.getParameterTypes();
                Class<?>   rt = method.getReturnType();
                if(rt == Void.class && p.length == 1 && U.isArrayOf(p[0], String.class)) return new Foo(method, an);
                throw new CmdLineException(m.getString("msg.err.opts_method"));
            }
        }
        return null;
    }

    public void launch(String... args) {
        try {
            processArguments(new Args(args));
            System.exit(main());
        }
        catch(Exception e) {
            System.exit(handleError(e));
        }
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

    private interface GetParamValue {
        String get(Args args);
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

    private static class Args {
        private final String[] args;
        private       int      idx = 0;

        private Args(String[] args) {
            this.args = args;
        }

        private boolean hasMore() { return (idx < args.length); }

        private String next()     { return args[idx++]; }
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
