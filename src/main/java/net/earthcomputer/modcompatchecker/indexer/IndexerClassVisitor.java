package net.earthcomputer.modcompatchecker.indexer;

import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;

public final class IndexerClassVisitor extends ClassVisitor {
    private final Index index;
    private ClassIndex classIndex;

    public IndexerClassVisitor(Index index) {
        super(AsmUtil.API);
        this.index = index;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classIndex = index.addClass(name, new AccessFlags(access), superName, new ArrayList<>(Arrays.asList(interfaces)));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        classIndex.addField(new AccessFlags(access), name, descriptor);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        classIndex.addMethod(new AccessFlags(access), name, descriptor);
        return null;
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        classIndex.addPermittedSubclass(permittedSubclass);
    }

    @Override
    public void visitNestHost(String nestHost) {
        classIndex.setNestHost(nestHost);
    }

    @Override
    public void visitNestMember(String nestMember) {
        classIndex.addNestMember(nestMember);
    }
}
