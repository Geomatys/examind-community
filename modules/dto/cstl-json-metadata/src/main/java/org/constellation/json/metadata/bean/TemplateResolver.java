package org.constellation.json.metadata.bean;

import org.constellation.exception.ConfigurationException;
import org.constellation.json.metadata.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component
public class TemplateResolver {

    @Autowired
    private Map<String, Template> templates;

    public Set<String> getAvailableNames() {
        return templates.keySet();
    }

    /**
     * Return template matching given name or throw an ConfigurationException if template not found.
     *
     * @param name template bean name
     * @return Template matching given name
     * @throws ConfigurationException if template bean not found
     */
    public Template getByName(String name) throws ConfigurationException{
        Template template = templates.get(name);
        if (template == null) {
            throw new ConfigurationException("Template named " + name + " not found.");
        }
        return template;
    }

    /**
     *
     * @param metadata
     * @return
     */
    public Template resolveDefaultFromMetadata(Object metadata) {
        if (metadata != null) {
            List<Template> matching = new ArrayList<>();
            for (Template candidate : templates.values()) {
                if (candidate.matchMetadata(metadata)) {
                    matching.add(candidate);
                }
            }

            return reduceTemplateList(matching);
        }
        return null;
    }

    public Template resolveDefaultFromDataType(String dataType) {
        if (dataType != null) {
            List<Template> matching = new ArrayList<>();
            for (Template candidate : templates.values()) {
                if (candidate.matchDataType(dataType) && candidate.isDefault()) {
                    matching.add(candidate);
                }
            }
            return reduceTemplateList(matching);
        }
        return null;
    }

    /**
     * Return one template from a list.
     * If list contain only one element, return it.
     * Otherwise, return first default template.
     *
     * @param matching
     * @return a Template or null if input list is empty or null
     */
    private Template reduceTemplateList(List<Template> matching) {
        if (matching != null) {
            if (matching.size() == 1) {
                return matching.get(0);
            } else {
                for (Template template : matching) {
                    if (template.isDefault()) {
                        return template;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve default templates
     *
     * @param metadata
     * @param dataType
     * @return best matching Template or null if no match found.
     */
    public Template resolveDefault(Object metadata, String dataType) {
        Template template = resolveDefaultFromMetadata(metadata);
        if (template != null) {
            return template;
        }

        template = resolveDefaultFromDataType(dataType);
        if (template != null) {
            return template;
        }
        return null;
    }

    /**
     * Return fallback template
     * @return
     */
    public Template getFallbackTemplate() {
        Template template = templates.get("profile_import");
        if (template != null) {
            return template;
        }
        throw new IllegalStateException("Template \"profile_import\" not found in context.");
    }
}
