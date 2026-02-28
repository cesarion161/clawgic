package com.clawgic.clawgic.controller;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClawgicControllerDtoResponseGuardrailTest {

    private static final String CLAWGIC_CONTROLLER_PACKAGE = "com.clawgic.clawgic.controller";
    private static final String CLAWGIC_MODEL_PACKAGE = "com.clawgic.clawgic.model";

    @Test
    void restControllersMustNotExposeClawgicJpaEntitiesInHandlerReturnTypes() throws Exception {
        List<Class<?>> controllers = findClawgicRestControllers();
        assertFalse(
                controllers.isEmpty(),
                "Expected at least one Clawgic @RestController for DTO guardrail coverage."
        );

        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : controllers) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                collectEntityLeakViolations(
                        ResolvableType.forMethodReturnType(method, controllerClass),
                        controllerClass.getSimpleName() + "#" + method.getName() + "() return type",
                        violations
                );
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Clawgic controllers must return DTOs, not JPA entities. Violations:\n - "
                        + String.join("\n - ", violations)
        );
    }

    private static List<Class<?>> findClawgicRestControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(CLAWGIC_CONTROLLER_PACKAGE)) {
            String className = candidate.getBeanClassName();
            if (className == null) {
                continue;
            }
            controllers.add(ClassUtils.forName(className, ClawgicControllerDtoResponseGuardrailTest.class.getClassLoader()));
        }

        controllers.sort(Comparator.comparing(Class::getName));
        return controllers;
    }

    private static void collectEntityLeakViolations(
            ResolvableType type,
            String path,
            List<String> violations
    ) {
        if (type == ResolvableType.NONE) {
            return;
        }

        Class<?> resolved = type.resolve();
        if (resolved != null && isClawgicEntity(resolved)) {
            violations.add(path + " -> " + resolved.getName());
        }

        if (resolved != null && resolved.isArray()) {
            collectEntityLeakViolations(type.getComponentType(), path + "[]", violations);
        }

        for (int i = 0; i < type.getGenerics().length; i++) {
            ResolvableType generic = type.getGeneric(i);
            if (generic != null && generic != ResolvableType.NONE) {
                collectEntityLeakViolations(generic, path + "<" + i + ">", violations);
            }
        }

        Class<?> raw = type.toClass();
        if (raw != null && raw.getTypeParameters().length == 0 && type.hasUnresolvableGenerics()) {
            for (ResolvableType nested : type.getGenerics()) {
                if (nested != null && nested != ResolvableType.NONE) {
                    collectEntityLeakViolations(nested, path + "<nested>", violations);
                }
            }
        }
    }

    private static boolean isClawgicEntity(Class<?> type) {
        return type.isAnnotationPresent(Entity.class)
                && Objects.equals(type.getPackageName(), CLAWGIC_MODEL_PACKAGE);
    }
}
