package com.examind.wps.component;

import org.opengis.parameter.ParameterValueGroup;
import com.examind.wps.api.WebProcessingProvider;
import com.examind.wps.api.WebProcessingComponent;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
public class GeotkQuartzProvider implements WebProcessingProvider {

    private static final String IDENTIFIER = "geotk-quartz";

    public static final ParameterDescriptor<String> REGISTRY;
    public static final ParameterDescriptor<String> PROCESS;

    private static final ParameterDescriptorGroup CONFIG;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        REGISTRY = builder
                .addName("registry")
                .setRequired(false)
                .setDescription("TODO")
                .create(String.class, null);

        PROCESS = builder
                .addName("process")
                .setRequired(false)
                .setDescription("TODO")
                .create(String.class, null);

        CONFIG = builder.addName(IDENTIFIER)
                .createGroup(REGISTRY, PROCESS);
    }

    final WPSScheduler scheduler;

    private GeotkQuartzProvider(@Autowired final WPSScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public WebProcessingComponent open(ParameterValueGroup config) {
        final String confId = config.getDescriptor().getName().getCode();
        if (!IDENTIFIER.equals(confId)) {
            throw new IllegalArgumentException(String.format("Given parameter does not match this provider identifier. Found: %s, Expected: %s", confId, IDENTIFIER));
        }

        final Parameters conf = Parameters.castOrWrap(config);
        final String registryName = conf.getValue(REGISTRY);
        final Stream<ProcessingRegistry> registries;
        if (registryName == null) {
            registries = StreamSupport.stream(Spliterators.spliteratorUnknownSize(ProcessFinder.getProcessFactories(), 0), true);
        } else {
            final ProcessingRegistry pr = ProcessFinder.getProcessFactory(registryName);
            if (pr == null) {
                throw new IllegalArgumentException("No processing registry found for name "+registryName);
            }
            registries = Stream.of(pr);
        }

        final Stream<ProcessDescriptor> processes;

        final Predicate<String> nameFilter = createProcessFilter(conf);
        if (nameFilter == null) {
            processes = registries.flatMap(r -> r.getDescriptors().stream());
        } else {
            processes = registries.flatMap(r -> {
                return r.getNames().stream()
                        .filter(nameFilter)
                        .map(name -> {
                            try {
                                return r.getDescriptor(name);
                            } catch (NoSuchIdentifierException e) {
                                // It should not happen, as we've got the names
                                // from the registry. Except if registry
                                // implementation is not good, or the predicate
                                // does the inverse of what it should.
                                throw new RuntimeException(e);
                            }
                        });
            });
        }

        return new GeotkQuartzComponent(processes.collect(Collectors.toList()), scheduler);
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return CONFIG;
    }

    private Predicate<String> createProcessFilter(Parameters conf) {
        String processName = conf.getValue(PROCESS);
        if (processName == null || (processName = processName.trim()).isEmpty() || "*".equals(processName)) {
            return null;
        }

        // TODO: better algorithm: check if user uses regex, wildcards etc. (it should be information in the configuration) ?
        final String finalName = processName;
        return name -> finalName.equals(name);
    }
}
