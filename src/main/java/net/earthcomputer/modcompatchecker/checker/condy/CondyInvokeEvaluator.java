package net.earthcomputer.modcompatchecker.checker.condy;

import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Set;

public class CondyInvokeEvaluator {
    private static final Object SENTINEL = new Object();

    private static final Set<String> CLASS_WHITELIST = Set.of(
        "java/lang/constant/ClassDesc",
        "java/lang/Enum$EnumDesc"
    );

    @Nullable
    public static Object evaluate(Handle handle, Object[] args) {
        Object result = evaluate0(handle, args);
        return result == SENTINEL ? null : result;
    }

    @Nullable
    private static Object evaluate0(Handle handle, Object[] args) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        MethodHandle mh = handleToMethodHandle(handle, lookup);
        if (mh == null) {
            return null;
        }

        Object[] argumentsToInvokeWith = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            argumentsToInvokeWith[i] = convertArgument(lookup, args[i]);
            if (argumentsToInvokeWith[i] == null) {
                return null;
            }
            if (argumentsToInvokeWith[i] == SENTINEL) {
                argumentsToInvokeWith[i] = null;
            }
        }

        try {
            Object result = mh.invokeWithArguments(argumentsToInvokeWith);
            return result == null ? SENTINEL : result;
        } catch (Throwable e) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle handleToMethodHandle(Handle handle, MethodHandles.Lookup lookup) {
        if (!CLASS_WHITELIST.contains(handle.getOwner())) {
            return null;
        }
        Type descriptor = Type.getType(handle.getDesc());
        if (descriptor.getSort() == Type.METHOD
            && Arrays.stream(descriptor.getArgumentTypes()).anyMatch(arg -> "Ljava/lang/invoke/MethodHandles$Lookup;".equals(arg.getDescriptor()))
        ) {
            return null;
        }

        Class<?> owningClass = typeToClass(lookup, Type.getObjectType(handle.getOwner()));
        if (owningClass == null) {
            return null;
        }
        try {
            return switch (handle.getTag()) {
                case Opcodes.H_GETFIELD ->
                    //noinspection DataFlowIssue
                    lookup.findGetter(owningClass, handle.getName(), typeToClass(lookup, Type.getType(handle.getDesc())));
                case Opcodes.H_GETSTATIC ->
                    //noinspection DataFlowIssue
                    lookup.findStaticGetter(owningClass, handle.getName(), typeToClass(lookup, Type.getType(handle.getDesc())));
                case Opcodes.H_PUTFIELD ->
                    //noinspection DataFlowIssue
                    lookup.findSetter(owningClass, handle.getName(), typeToClass(lookup, Type.getType(handle.getDesc())));
                case Opcodes.H_PUTSTATIC ->
                    //noinspection DataFlowIssue
                    lookup.findStaticSetter(owningClass, handle.getName(), typeToClass(lookup, Type.getType(handle.getDesc())));
                case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE ->
                    lookup.findVirtual(owningClass, handle.getName(), typeToMethodType(lookup, Type.getMethodType(handle.getDesc())));
                case Opcodes.H_INVOKESTATIC ->
                    //noinspection DataFlowIssue
                    lookup.findStatic(owningClass, handle.getName(), typeToMethodType(lookup, Type.getMethodType(handle.getDesc())));
                case Opcodes.H_NEWINVOKESPECIAL ->
                    //noinspection DataFlowIssue
                    lookup.findConstructor(owningClass, typeToMethodType(lookup, Type.getMethodType(handle.getDesc())));
                default -> null;
            };
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | NullPointerException e) {
            return null;
        }
    }

    @Nullable
    private static Class<?> typeToClass(MethodHandles.Lookup lookup, Type type) {
        return switch (type.getSort()) {
            case Type.VOID -> Void.TYPE;
            case Type.BOOLEAN -> Boolean.TYPE;
            case Type.BYTE -> Byte.TYPE;
            case Type.SHORT -> Short.TYPE;
            case Type.INT -> Integer.TYPE;
            case Type.LONG -> Long.TYPE;
            case Type.FLOAT -> Float.TYPE;
            case Type.DOUBLE -> Double.TYPE;
            case Type.OBJECT -> {
                try {
                    yield lookup.findClass(type.getClassName());
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    yield null;
                }
            }
            case Type.ARRAY -> {
                Class<?> clazz = typeToClass(lookup, type.getElementType());
                if (clazz == null) {
                    yield null;
                }
                for (int i = 0, count = type.getDimensions(); i < count; i++) {
                    clazz = clazz.arrayType();
                }
                yield clazz;
            }
            default -> throw new IllegalArgumentException("Illegal type sort " + type.getSort());
        };
    }

    private static MethodType typeToMethodType(MethodHandles.Lookup lookup, Type type) {
        Class<?> returnType = typeToClass(lookup, type.getReturnType());
        if (returnType == null) {
            return null;
        }
        Type[] paramTypes = type.getArgumentTypes();
        Class<?>[] paramClasses = new Class<?>[paramTypes.length];
        for (int i = 0; i < paramClasses.length; i++) {
            paramClasses[i] = typeToClass(lookup, paramTypes[i]);
            if (paramClasses[i] == null) {
                return null;
            }
        }
        return MethodType.methodType(returnType, paramClasses);
    }

    @Nullable
    private static Object convertArgument(MethodHandles.Lookup lookup, Object arg) {
        Type type = AsmUtil.toClassConstant(arg);
        if (type != null) {
            return typeToClass(lookup, type);
        } else if (arg instanceof Type) {
            return typeToMethodType(lookup, (Type) arg);
        } else if (arg instanceof Handle innerHandle) {
            return handleToMethodHandle(innerHandle, lookup);
        } else if (arg instanceof ConstantDynamic condy) {
            if ("java/lang/invoke/ConstantBootstraps".equals(condy.getBootstrapMethod().getOwner())) {
                return switch (condy.getBootstrapMethod().getName()) {
                    case "nullConstant" -> SENTINEL;
                    case "getStaticFinal" -> {
                        String owner;
                        if (condy.getBootstrapMethodArgumentCount() == 0) {
                            owner = condy.getDescriptor();
                        } else {
                            if (!(condy.getBootstrapMethodArgument(0) instanceof Type)) {
                                yield null;
                            }
                            owner = ((Type) condy.getBootstrapMethodArgument(0)).getDescriptor();
                        }
                        if ("Ljava/lang/Boolean;".equals(owner)) {
                            yield switch (condy.getName()) {
                                case "FALSE" -> Boolean.FALSE;
                                case "TRUE" -> Boolean.TRUE;
                                default -> null;
                            };
                        } else {
                            yield null;
                        }
                    }
                    case "invoke" -> {
                        if (condy.getBootstrapMethodArgumentCount() == 0 || !(condy.getBootstrapMethodArgument(0) instanceof Handle innerHandle)) {
                            yield null;
                        }
                        Object[] innerArgs = new Object[condy.getBootstrapMethodArgumentCount() - 1];
                        Arrays.setAll(innerArgs, j -> condy.getBootstrapMethodArgument(j + 1));
                        yield evaluate0(innerHandle, innerArgs);
                    }
                    default -> null;
                };
            } else {
                return null;
            }
        } else {
            return arg;
        }
    }
}
