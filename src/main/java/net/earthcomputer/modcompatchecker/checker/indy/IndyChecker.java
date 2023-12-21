package net.earthcomputer.modcompatchecker.checker.indy;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface IndyChecker {
    Map<Handle, IndyChecker> CHECKERS = ((Supplier<Map<Handle, IndyChecker>>) () -> {
        Map<Handle, IndyChecker> map = new HashMap<>();

        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        ), LambdaMetafactoryChecker.INSTANCE);
        map.put(new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false
        ), LambdaMetafactoryChecker.INSTANCE);

        return map;
    }).get();

    void check(IndyContext context);
}
