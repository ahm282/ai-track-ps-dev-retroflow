package com.stackcraft.retroflow;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.stackcraft.retroflow", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final DescribedPredicate<JavaClass> CONTROLLERS = DescribedPredicate.describe("controllers",
        javaClass -> javaClass.isAnnotatedWith(Controller.class) || javaClass.isMetaAnnotatedWith(Controller.class));

    private static final DescribedPredicate<JavaClass> SERVICES = DescribedPredicate.describe("services",
        javaClass -> javaClass.isAnnotatedWith(Service.class) || javaClass.isMetaAnnotatedWith(Service.class));

    private static final DescribedPredicate<JavaClass> CONTROLLER_ADVICE = DescribedPredicate.describe(
        "controller advice", javaClass -> javaClass.isAnnotatedWith(ControllerAdvice.class)
            || javaClass.isMetaAnnotatedWith(ControllerAdvice.class));

    private static final DescribedPredicate<JavaMethod> REQUEST_HANDLERS = DescribedPredicate.describe(
        "request handlers", method -> method.isAnnotatedWith(RequestMapping.class)
            || method.isMetaAnnotatedWith(RequestMapping.class));

    @ArchTest
    static final ArchRule controllers_doNotDependOnRepositories = noClasses()
        .that(CONTROLLERS)
        .should().dependOnClassesThat(resideInAPackage("..repository..").or(assignableTo(Repository.class)))
        .as("Controllers must not depend on repositories")
        .because("the layering is controller -> service -> repository (AGENTS.md)");

    @ArchTest
    static final ArchRule controllers_doNotAcceptOrReturnEntities = methods()
        .that(REQUEST_HANDLERS)
        .and().areDeclaredInClassesThat(CONTROLLERS)
        .should(notAcceptOrReturnEntities())
        .as("Controller request handlers must not accept or return @Entity classes")
        .because("request and response bodies are DTOs, so entities never reach the API");

    @ArchTest
    static final ArchRule dtos_doNotDependOnEntities = noClasses()
        .that().resideInAPackage("..dto..")
        .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
        .as("Request and response classes must not depend on @Entity classes")
        .because("a DTO that wraps an entity would still serialise it into the API");

    @ArchTest
    static final ArchRule services_doNotDependOnControllers = noClasses()
        .that(SERVICES)
        .should().dependOnClassesThat(resideInAPackage("..controller..").or(CONTROLLERS))
        .as("Services must not depend on controllers")
        .because("dependencies point from the web layer to the service layer, never back");

    @ArchTest
    static final ArchRule exactlyOneControllerAdvice = classes()
        .that(CONTROLLER_ADVICE)
        .should(existExactlyOnce())
        .allowEmptyShould(true)
        .as("There must be exactly one @ControllerAdvice / @RestControllerAdvice")
        .because("all errors share one format, produced in one place");

    private static ArchCondition<JavaMethod> notAcceptOrReturnEntities() {
        return new ArchCondition<>("not accept or return @Entity classes") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Stream.concat(Stream.of(method.getReturnType()), method.getParameterTypes().stream())
                    .map(JavaType::getAllInvolvedRawTypes)
                    .flatMap(Set::stream)
                    .filter(type -> type.isAnnotatedWith(Entity.class))
                    .distinct()
                    .forEach(entity -> events.add(SimpleConditionEvent.violated(method,
                        method.getFullName() + " exposes entity " + entity.getName())));
            }
        };
    }

    private static ArchCondition<JavaClass> existExactlyOnce() {
        return new ArchCondition<>("exist exactly once") {
            private final List<JavaClass> found = new ArrayList<>();

            @Override
            public void init(Collection<JavaClass> allObjectsToTest) {
                found.clear();
            }

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                found.add(javaClass);
            }

            @Override
            public void finish(ConditionEvents events) {
                if (found.size() != 1) {
                    events.add(SimpleConditionEvent.violated(found,
                        "expected exactly 1 controller advice but found " + found.size() + ": "
                            + found.stream().map(JavaClass::getName).toList()));
                }
            }
        };
    }
}
