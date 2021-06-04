package rocks.inspectit.ocelot.core.command.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.Command;
import rocks.inspectit.ocelot.commons.models.command.impl.ListClassesCommand;
import rocks.inspectit.ocelot.commons.models.command.response.CommandResponse;
import rocks.inspectit.ocelot.commons.models.command.response.impl.ListClassesResponse;
import rocks.inspectit.ocelot.core.command.handler.CommandExecutor;
import rocks.inspectit.ocelot.core.instrumentation.NewClassDiscoveryService;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Component
public class ListClassesCommandExecutor implements CommandExecutor {

    @Autowired
    private NewClassDiscoveryService discoveryService;

    @Override
    public boolean canExecute(Command command) {
        return command instanceof ListClassesCommand;
    }

    @Override
    public CommandResponse execute(Command command) {
        log.debug("Executing a ListClassesCommand.");

        Set<Class<?>> setCopy = new HashSet<>(discoveryService.getKnownClasses());
        ListClassesResponse.TypeElement[] result = setCopy.parallelStream().map(clazz -> {
            String[] methods = Arrays.stream(clazz.getDeclaredMethods())
                    .map(Method::toGenericString)
                    .toArray(String[]::new);

            ListClassesResponse.TypeElement element = new ListClassesResponse.TypeElement();
            element.setName(clazz.getName());
            element.setType(clazz.isInterface() ? "interface" : "class");
            element.setMethods(methods);
            return element;
        }).toArray(ListClassesResponse.TypeElement[]::new);

        System.out.println("-->> done");

        ListClassesResponse response = new ListClassesResponse();
        response.setCommandId(command.getCommandId());
        response.setResult(result);
        return response;
    }
}
