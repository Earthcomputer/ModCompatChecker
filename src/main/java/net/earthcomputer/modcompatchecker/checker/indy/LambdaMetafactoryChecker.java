package net.earthcomputer.modcompatchecker.checker.indy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.AccessLevel;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import net.earthcomputer.modcompatchecker.util.InheritanceUtil;
import net.earthcomputer.modcompatchecker.util.OwnedClassMember;
import net.earthcomputer.modcompatchecker.util.UnimplementedMethodChecker;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum LambdaMetafactoryChecker implements IndyChecker {
    INSTANCE;

    @Override
    public void check(IndyContext context) {
        List<String> interfaces = new ArrayList<>();
        List<String> interfaceMethodDescs = new ArrayList<>();

        Type interfaceType = Type.getReturnType(context.descriptor());
        if (interfaceType.getSort() != Type.OBJECT) {
            return;
        }
        interfaces.add(interfaceType.getInternalName());

        String interfaceMethodName = context.name();

        if (!(context.args()[0] instanceof Type interfaceMethodType)) {
            return;
        }
        interfaceMethodDescs.add(interfaceMethodType.getDescriptor());

        if ("altMetafactory".equals(context.bsm().getName())) {
            if (!(context.args()[3] instanceof Integer flags)) {
                return;
            }

            int argIndex = 4;
            if ((flags & LambdaMetafactory.FLAG_MARKERS) != 0) {
                if (!(context.args()[argIndex++] instanceof Integer markerCount)) {
                    return;
                }
                for (int i = 0; i < markerCount; i++) {
                    if (!(context.args()[argIndex++] instanceof Type itfType)) {
                        return;
                    }
                    if (itfType.getSort() != Type.OBJECT) {
                        return;
                    }
                    interfaces.add(itfType.getInternalName());
                }
            }
            if ((flags & LambdaMetafactory.FLAG_BRIDGES) != 0) {
                if (!(context.args()[argIndex++] instanceof Integer bridgeCount)) {
                    return;
                }
                for (int i = 0; i < bridgeCount; i++) {
                    if (!(context.args()[argIndex++] instanceof Type bridgeType)) {
                        return;
                    }
                    if (bridgeType.getSort() != Type.METHOD) {
                        return;
                    }
                    interfaceMethodDescs.add(bridgeType.getDescriptor());
                }
            }
        }

        for (String interfaceName : interfaces) {
            IResolvedClass resolvedInterface = context.index().findClass(interfaceName);
            if (resolvedInterface == null) {
                context.addProblem(Errors.LAMBDA_INTERFACE_REMOVED, interfaceName);
                continue;
            }
            if (!AsmUtil.isClassAccessible(context.className(), interfaceName, resolvedInterface.getAccess().accessLevel())) {
                context.addProblem(Errors.LAMBDA_INTERFACE_INACCESSIBLE, interfaceName, resolvedInterface.getAccess().accessLevel().getLowerName());
            }
            if (!resolvedInterface.getAccess().isInterface()) {
                context.addProblem(Errors.LAMBDA_INTERFACE_NOT_AN_INTERFACE, interfaceName);
            }
        }

        UnimplementedMethodChecker checker = new UnimplementedMethodChecker(context.index(), AsmUtil.OBJECT, interfaces.toArray(String[]::new)) {
            @Override
            protected boolean isMethodAccessible(String containingClassName, IResolvedClass containingClass, AccessLevel accessLevel) {
                return AsmUtil.isMemberAccessible(index, context.className(), containingClassName, containingClass, accessLevel);
            }

            @Override
            protected List<OwnedClassMember> multiLookupMethod(String name, String desc) {
                if (name.equals(interfaceMethodName) && interfaceMethodDescs.contains(desc)) {
                    return List.of(new OwnedClassMember(context.className(), new ClassMember(new AccessFlags(Opcodes.ACC_PUBLIC), name, desc)));
                } else {
                    assert interfaces != null;
                    return InheritanceUtil.multiLookupMethod(index, AsmUtil.OBJECT, Arrays.asList(interfaces), name, desc);
                }
            }

            @Override
            protected void onDiamondProblem(String methodName, String methodDesc) {
                context.addProblem(Errors.LAMBDA_DIAMOND_PROBLEM, methodName, methodDesc);
            }

            @Override
            protected void onAbstractMethodUnimplemented(String methodOwner, String methodName, String methodDesc) {
                context.addProblem(Errors.LAMBDA_ABSTRACT_METHOD_UNIMPLEMENTED, methodOwner, methodName, methodDesc);
            }

            @Override
            protected void onIncorrectInterfaceMethodLookup(String interfaceName, String methodName, String methodDesc, String resolvedClassName, String problematicAccessModifier) {
                context.addProblem(Errors.LAMBDA_INCORRECT_INTERFACE_METHOD_LOOKUP, interfaceName, methodName, methodDesc, problematicAccessModifier, resolvedClassName);
            }
        };
        checker.run();
    }
}
