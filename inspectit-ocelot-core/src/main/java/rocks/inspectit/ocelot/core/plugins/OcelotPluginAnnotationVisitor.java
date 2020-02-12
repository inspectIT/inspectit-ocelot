package rocks.inspectit.ocelot.core.plugins;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Opcodes;

class OcelotPluginAnnotationVisitor extends ClassVisitor {
    public boolean hasOcelotPluginAnnotation;

    OcelotPluginAnnotationVisitor() {
        super(Opcodes.ASM7);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.endsWith("OcelotPlugin;")) {
            hasOcelotPluginAnnotation = true;
        }
        return null;
    }
}