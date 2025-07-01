package com.hit.spring.template;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MyStringTemplateResolver extends AbstractConfigurableTemplateResolver {

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public MyStringTemplateResolver() {
        super();
        setResolvablePatterns(Set.of("*"));
    }

    public void addTemplate(String name, String template) {
        templates.put(name, template);
    }

    @Override
    protected ITemplateResource computeTemplateResource(
            IEngineConfiguration configuration,
            String ownerTemplate,
            String template,
            String resourceName,
            String characterEncoding,
            Map<String, Object> templateResolutionAttributes) {

        String templateContent = templates.get(template);
        if (templateContent != null) {
            return new StringTemplateResource(templateContent);
        }

        // Nếu không tìm thấy template, coi template name chính là content
        return new StringTemplateResource(template);
    }
}
