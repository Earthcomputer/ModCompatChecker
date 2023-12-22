package net.earthcomputer.modcompatchecker.checker.condy;

import net.earthcomputer.modcompatchecker.checker.indy.IndyContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface CondyChecker {
    Map<Handle, CondyChecker> CHECKERS = ((Supplier<Map<Handle, CondyChecker>>) () -> {
        Map<Handle, CondyChecker> map = new HashMap<>();

        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "enumConstant",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;",
            false
        ), EnumConstantChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "getStaticFinal",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;",
            false
        ), FieldCondyChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "getStaticFinal",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
            false
        ), FieldCondyChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "fieldVarHandle",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
            false
        ), FieldCondyChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "staticFieldVarHandle",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;",
            false
        ), FieldCondyChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/ConstantBootstraps",
            "invoke",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;",
            false
        ), CondyInvokeChecker.INSTANCE);

        return map;
    }).get();

    void check(IndyContext context);

    default boolean overrideDefaultChecking() {
        return true;
    }
}
