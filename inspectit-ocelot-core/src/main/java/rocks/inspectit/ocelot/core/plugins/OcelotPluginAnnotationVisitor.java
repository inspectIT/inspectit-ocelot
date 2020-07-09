package rocks.inspectit.ocelot.core.plugins;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import rocks.inspectit.ocelot.sdk.OcelotPlugin;

class OcelotPluginAnnotationVisitor extends ClassVisitor {

    public static final String OCELOT_PLUGIN_DESCRIPTOR = "L" + Type.getInternalName(OcelotPlugin.class) + ";";

    private boolean hasOcelotPluginAnnotation;

    /**
     * Checks if a currently visited class annotated with {@link OcelotPlugin}.
     *
     * @return Returns true if the currently visited class annotated with {@link OcelotPlugin}.
     */
    public boolean hasOcelotPluginAnnotation() {
        return hasOcelotPluginAnnotation;
    }

    OcelotPluginAnnotationVisitor() {
        super(Opcodes.ASM7);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(OCELOT_PLUGIN_DESCRIPTOR)) {
            hasOcelotPluginAnnotation = true;
        }
        return null;
    }
}