package com.hit.spring.template;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;


@Component
public class StringTemplateService {

    private final TemplateEngine templateEngine;
    private final StringTemplateResolver templateResolver;

    public StringTemplateService() {
        this.templateResolver = new StringTemplateResolver();
        this.templateResolver.setTemplateMode(TemplateMode.HTML);
        this.templateResolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    public String processFromString(String htmlTemplate, Context context) {
        return templateEngine.process(htmlTemplate, context);
    }

}