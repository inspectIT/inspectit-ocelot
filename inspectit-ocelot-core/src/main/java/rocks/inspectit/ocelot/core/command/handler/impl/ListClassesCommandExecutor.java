package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.response.impl.ListClassesResponse;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.instrumentation.NewClassDiscoveryService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Command for listing existing classes.
 */
@Slf4j
@Component
public class ListClassesCommandExecutor implements CommandExecutor {

    private static final int ACCESS_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    @Autowired
    private NewClassDiscoveryService discoveryService;

    /**
     * Customized implementation of {@link Method#toString()}.
     */
    private static String getMethodSignature(Method method) {
        try {
            StringBuilder sb = new StringBuilder();

            printModifiersIfNonzero(sb, Modifier.methodModifiers(), method);
            specificToStringHeader(sb, method);

            sb.append('(');
            separateWithCommas(method.getParameterTypes(), sb);
            sb.append(')');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void specificToStringHeader(StringBuilder sb, Method method) {
        sb.append(method.getReturnType().getTypeName()).append(' ');
        sb.append(method.getName());
    }

    private static void separateWithCommas(Class<?>[] types, StringBuilder sb) {
        for (int j = 0; j < types.length; j++) {
            sb.append(types[j].getTypeName());
            if (j < (types.length - 1)) {
                sb.append(", ");
            }
        }
    }

    private static void printModifiersIfNonzero(StringBuilder sb, int mask, Method method) {
        boolean isDefault = method.isDefault();
        int mod = method.getModifiers() & mask;

        if (mod != 0 && !isDefault) {
            sb.append(Modifier.toString(mod)).append(' ');
        } else {
            int access_mod = mod & ACCESS_MODIFIERS;
            if (access_mod != 0) {
                sb.append(Modifier.toString(access_mod)).append(' ');
            }
            if (isDefault) {
                sb.append("default ");
            }
            mod = (mod & ~ACCESS_MODIFIERS);
            if (mod != 0) {
                sb.append(Modifier.toString(mod)).append(' ');
            }
        }
    }

    @Override
    public boolean canExecute(Command command) {
        return command instanceof ListClassesCommand;
    }

    @Override
    public CommandResponse execute(Command command) {
        ListClassesCommand lcCommand = (ListClassesCommand) command;
        String filter = lcCommand.getFilter();

        log.debug("Executing a ListClassesCommand: {}", lcCommand.getCommandId().toString());

        Set<Class<?>> setCopy = new HashSet<>(discoveryService.getKnownClasses());
        ListClassesResponse.TypeElement[] result = setCopy.parallelStream()
                .filter(clazz -> clazz.getName().contains(filter))
                .filter(this::includeClass)
                .map(clazz -> {
                    try {
                        String[] methods = Arrays.stream(clazz.getDeclaredMethods())
                                .map(ListClassesCommandExecutor::getMethodSignature)
                                .filter(Objects::nonNull)
                                .toArray(String[]::new);

                        ListClassesResponse.TypeElement element = new ListClassesResponse.TypeElement();
                        element.setName(clazz.getName());
                        element.setType(clazz.isInterface() ? "interface" : "class");
                        element.setMethods(methods);
                        return element;
                    } catch (Throwable e) {
                        log.debug("Could not add class to result list: {}", clazz);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(ListClassesResponse.TypeElement[]::new);

        log.debug("Finished executing ListClassesCommand: {}", lcCommand.getCommandId().toString());

        ListClassesResponse response = new ListClassesResponse();
        response.setCommandId(lcCommand.getCommandId());
        response.setResult(result);
        return response;
    }

    /**
     * Whether a specified class should be included or not - e.g.: for filtering lambda classes.
     *
     * @param clazz the class to check
     *
     * @return true if it should be contained in the result
     */
    private boolean includeClass(Class<?> clazz) {
        if (clazz.getName().contains("$$Lambda")) {
            return false;
        } else if (clazz.getName().startsWith("[")) {
            return false;
        } else {
            return true;
        }
    }
}
