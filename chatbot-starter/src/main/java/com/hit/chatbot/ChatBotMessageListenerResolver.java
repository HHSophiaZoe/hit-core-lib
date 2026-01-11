package com.hit.chatbot;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ChatBotMessageListenerResolver {

    private final Environment environment;

    private final ConfigurableBeanFactory beanFactory;

    /**
     * Hỗ trợ:
     * - Giá trị literal: "123456"
     * - Property placeholder: "${telegram.chat.id}"
     * - SpEL expression: "#{@beanName.getChatId()}"
     *
     * @return giá trị đã được resolve
     */
    public String resolveValue(String value) {
        if (StringUtils.isEmpty(value)) return value;

        if (this.isPropertyPlaceholder(value)) {
            return beanFactory.resolveEmbeddedValue(value);
        }

        if (this.isSpelExpression(value)) {
            return this.resolveSpelExpression(value);
        }

        return value;
    }

    public String[] resolveValues(String[] values) {
        if (values == null || values.length == 0) return new String[0];

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(this::resolveValue)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    /**
     * Kiểm tra xem value có phải là SpEL expression không
     */
    private boolean isSpelExpression(String value) {
        return value.startsWith("#{") && value.endsWith("}");
    }

    /**
     * Kiểm tra xem value có chứa property placeholder không
     */
    private boolean isPropertyPlaceholder(String value) {
        return value.contains("${") && value.endsWith("}");
    }

    /**
     * Resolve SpEL expression
     * Hỗ trợ:
     * - Bean references: #{@beanName.method()}
     * - Operators: #{1 + 2}, #{@bean.value > 10 ? 'high' : 'low'}
     *
     * @param expression SpEL expression (ví dụ: "#{@beanName.getChatId()}")
     * @return Giá trị đã evaluate
     */
    private String resolveSpelExpression(String expression) {
        // Tạo một SpEL parser
        ExpressionParser spelParser = new SpelExpressionParser();

        // Tạo evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));

        // Bỏ đi ký tự #{ và } để lấy biểu thức
        String spelExpression = expression.substring(2, expression.length() - 1);

        // Tự động thêm @ cho bean nếu chưa có
        if (!spelExpression.startsWith("@") && !spelExpression.startsWith("T(")) {
            // Tìm vị trí dấu chấm đầu tiên
            int dotIndex = spelExpression.indexOf('.');
            if (dotIndex > 0) {
                String potentialBeanName = spelExpression.substring(0, dotIndex);
                // Kiểm tra nếu đây là bean name
                if (beanFactory.containsBean(potentialBeanName)) {
                    spelExpression = "@" + spelExpression;
                }
            }
        }

        Expression exp = spelParser.parseExpression(spelExpression);

        Object result = exp.getValue(context);

        return result != null ? result.toString() : null;
    }

    /**
     * Resolve property placeholder
     * Hỗ trợ:
     * - Simple placeholder: ${property.name}
     * - Default value: ${property.name:defaultValue}
     * - Nested placeholders: ${prefix.${env}.suffix}
     * - Multiple placeholders: ${prop1}-${prop2}
     *
     * @param value Giá trị chứa placeholder (ví dụ: "${telegram.chat.id}")
     * @return Giá trị đã resolve
     */
    private String resolvePropertyPlaceholder(String value) {
        return environment.resolvePlaceholders(value);
    }

}
