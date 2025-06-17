package com.hit.cache.lock.processor;

import com.hit.cache.helper.ExpressionEvaluator;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;

public abstract class DistributedLockAbstractProcessor {

    private static final ExpressionEvaluator<String> EVALUATOR = new ExpressionEvaluator<>();

    protected String getValue(JoinPoint joinPoint, String condition) {
        if(StringUtils.isEmpty(condition)) return "empty";
        return getValue(joinPoint.getTarget(), joinPoint.getArgs(),
            joinPoint.getTarget().getClass(),
            ((MethodSignature) joinPoint.getSignature()).getMethod(), condition);
    }

    protected String getValue(Object object, Object[] args, Class clazz, Method method, String condition) {
        if (args == null) {
            return null;
        }
        EvaluationContext evaluationContext = EVALUATOR.createEvaluationContext(object, clazz, method, args);
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, clazz);
        return EVALUATOR.condition(condition, methodKey, evaluationContext, String.class);
    }

}
