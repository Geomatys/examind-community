
package com.examind.setup;

import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import com.examind.dto.fs.Collection;
import static com.examind.setup.FileSystemUtilities.parseYaml;
import static com.examind.setup.FileSystemUtilities.providerFileFilter;
import static com.examind.setup.FileSystemUtilities.serviceFileFilter;
import com.examind.setup.ProviderUtilities.ProviderSourceType;
import static com.examind.setup.ProviderUtilities.getProviderSourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConfigurationException;
import org.geotoolkit.style.MutableStyle;

/**
 * A model that contains the analysis of the configuration file system.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemAnalysis {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    public static class ProviderWithPath {
        public final Provider provider;
        public final Path ymlFile;
        public final ProviderSourceType sourceType;
        
        public ProviderWithPath(Provider provider, Path ymlFile) {
            this.provider = provider;
            this.ymlFile = ymlFile;
            this.sourceType = getProviderSourceType(provider);
        }
    }
    
    /**
     * A map of dataset identifier / service List.
     * provide the links between dataset and service for async reload.
     * Will be set to null in a synchroneous context.
     */
    public final Map<String, List<Service>> asyncInfos;
  
    /**
     * Style root directory.
     */
    public final Path styleDir;
     
    /**
     * Map of file path / unmarshalled sld style
     */
    public final Map<String, MutableStyle> styles = new HashMap<>();
    
    /**
     * Service root directory.
     */
    public final Path serviceDir;
    
    /**
     * Map of file path / unmarshalled yaml file
     */
    public final Map<String, Service> servicesWithData = new HashMap<>();
    
    /**
     * Map of file path / unmarshalled yaml file
     */
    public final Map<String, Service> services = new HashMap<>();
    
    /**
     * Provider root directory.
     */
    public final Path providerDir;
    
    /**
     * Map of file path / unmarshalled yaml file
     */
    public final Map<String, ProviderWithPath> providers = new HashMap<>();
    
    /**
     * Map of file path / unmarshalled yaml file
     */
    public final Map<String, ProviderWithPath> computedProviders = new HashMap<>();
    
    /**
     * Asynchroneous mode.
     */
    public final boolean async;
    

    /**
     * Build and compute the filesystem analysis on the supplied directories.
     * if async is set to true, async informations will be computed.
     * 
     * @param styleDir Style root directory.
     * @param servDir Service root directory.
     * @param provDir Provider root directory.
     * @param parseStyle A method to parse style.
     * @param async Asynchroneous mode.
     */
    public FileSystemAnalysis(Path styleDir, Path servDir, Path provDir, Function<Path, MutableStyle> parseStyle, boolean async) {
        asyncInfos = async ? new HashMap<>() : null;
        this.styleDir = styleDir;
        this.serviceDir = servDir;
        this.providerDir = provDir;
        this.async = async;
        analyseFileSystem(parseStyle);
    }
    
    /**
     * Analyse the filesystem.
     * 
     * @param parseStyle A method to parse style.
     */
    private void analyseFileSystem(Function<Path, MutableStyle> parseStyle) {
        // 1. styles
        try (Stream<Path> stream = Files.walk(styleDir)) {
            stream.filter(FileSystemUtilities::sldFileFilter).forEach(path -> {
                
                MutableStyle style = parseStyle.apply(path);
                if (style != null)  this.styles.put(path.toString(), style);
            });
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing the style directory", ex);
        }

        // 2. services with data
        try (Stream<Path> stream = Files.walk(serviceDir)) {
            stream.filter(p -> serviceFileFilter(p, true)).forEach(path -> {
                Service s = parseYaml(path, Service.class);
                if (s != null) {
                    try {
                        validateService(s);
                        this.servicesWithData.put(path.toString(), s);
                    } catch (ConfigurationException ex) {
                        LOGGER.log(Level.WARNING, "Error while importing service: {0}\n{1}\n", new Object[]{path, ex.getMessage()});
                    }
                }
            });
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing the service directory", ex);
        }
        
        // 3. services
        try (Stream<Path> stream = Files.walk(serviceDir)) {
            stream.filter(p -> serviceFileFilter(p, false)).forEach(path -> {
                Service s = parseYaml(path, Service.class);
                if (s != null) {
                    try {
                        validateService(s);
                        this.services.put(path.toString(), s);
                    } catch (ConfigurationException ex) {
                        LOGGER.log(Level.WARNING, "Error while importing service: {0}\n{1}\n", new Object[]{path, ex.getMessage()});
                    }
                }
            });
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing the service directory", ex);
        }

        // 4. regular providers
        try (Stream<Path> stream = Files.walk(providerDir)) {
            stream.filter(p -> providerFileFilter(p, false)).forEach(path -> {
                Provider pr = parseYaml(path, Provider.class);
                if (pr != null) {
                    try {
                        ProviderUtilities.validateProviderFile(pr);
                        this.providers.put(path.toString(), new ProviderWithPath(pr, path));
                    } catch (ConfigurationException ex) {
                        LOGGER.log(Level.WARNING, "Error while importing provider: {0}\n{1}\n", new Object[]{path, ex.getMessage()});
                    }
                }
            });
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing the provider directory", ex);
        }

        // 5. computed providers
        try (Stream<Path> stream = Files.walk(providerDir)) {
            stream.filter(p -> providerFileFilter(p, true)).forEach(path -> {
                Provider pr = parseYaml(path, Provider.class);
                if (pr != null) {
                    try {
                        ProviderUtilities.validateProviderFile(pr);
                        this.computedProviders.put(path.toString(), new ProviderWithPath(pr, path));
                    } catch (ConfigurationException ex) {
                        LOGGER.log(Level.WARNING, "Error while importing provider: {0}\n{1}\n", new Object[]{path, ex.getMessage()});
                    }
                }
            });
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing the provider directory", ex);
        }
        
        if (async) {
            this.computeRelations();
        }
    }
    
     /**
     * Look for mandatory parameters in a service yaml file.
     * 
     * @param serv A service configuration.
     * @throws ConfigurationException If a mandatory parameter is missing or invalid
     */
    private void validateService(Service serv) throws ConfigurationException {
        if (serv.getType()       == null) throw new ConfigurationException("Service type is missing.");
        if (serv.getIdentifier() == null) throw new ConfigurationException("Service identifier is missing.");

        // OPENEO is not a real ServiceDef.Specification: it is expanded into a WPS + WCS pair at creation time.
        if ("OPENEO".equalsIgnoreCase(serv.getType())) return;

        // verify service type
        try {
            ServiceDef.Specification.valueOf(serv.getType().toUpperCase());
        } catch (Exception ex) {
            throw new ConfigurationException("Service type invalid: " + serv.getType());
        }
    }
    
    /**
     * Compute relations between dataset and service for asynchroneous reload purpose.
     */
    private void computeRelations() {
        for (ProviderWithPath pwp : providers.values()) {
            String targetDataset = pwp.provider.getDataset();
            for (Service service : services.values()) {
                for (Collection coll : service.getCollections()) {
                    if (Objects.equals(coll.getDataSet(), targetDataset)) {
                        List<Service> l = asyncInfos.computeIfAbsent(targetDataset, f -> new ArrayList<Service>());
                        if (!l.contains(service)) {
                            l.add(service);
                        }
                    }
                }
            }
        }
    }
}
