package ru.fitnesscrm;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Cross-module access only via {@code *Facade}; no foreign repository/service/domain leaks.
 * <p>
 * Package prefixes use {@code ru.fitnesscrm.*} so Spring's {@code org.springframework.scheduling}
 * is not matched by a bare {@code ..scheduling..} pattern.
 */
@AnalyzeClasses(packages = "ru.fitnesscrm", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleBoundaryTest {

    // --- no foreign repositories ---

    @ArchTest
    static final ArchRule scheduling_must_not_use_foreign_repositories =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.scheduling..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.repository..",
                            "ru.fitnesscrm.finance.repository..",
                            "ru.fitnesscrm.identity.repository.."
                    );

    @ArchTest
    static final ArchRule finance_must_not_use_foreign_repositories =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.finance..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.repository..",
                            "ru.fitnesscrm.scheduling.repository..",
                            "ru.fitnesscrm.identity.repository.."
                    );

    @ArchTest
    static final ArchRule memberships_must_not_use_foreign_repositories =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.memberships..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.identity.repository..",
                            "ru.fitnesscrm.finance.repository..",
                            "ru.fitnesscrm.scheduling.repository.."
                    );

    @ArchTest
    static final ArchRule identity_must_not_use_foreign_repositories =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.identity..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.repository..",
                            "ru.fitnesscrm.finance.repository..",
                            "ru.fitnesscrm.scheduling.repository.."
                    );

    // --- no foreign application services ---

    @ArchTest
    static final ArchRule scheduling_must_not_use_foreign_services =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.scheduling..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.service..",
                            "ru.fitnesscrm.finance.service..",
                            "ru.fitnesscrm.identity.service.."
                    );

    @ArchTest
    static final ArchRule finance_must_not_use_foreign_services =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.finance..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.service..",
                            "ru.fitnesscrm.scheduling.service..",
                            "ru.fitnesscrm.identity.service.."
                    );

    @ArchTest
    static final ArchRule memberships_must_not_use_foreign_services =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.memberships..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.identity.service..",
                            "ru.fitnesscrm.finance.service..",
                            "ru.fitnesscrm.scheduling.service.."
                    );

    @ArchTest
    static final ArchRule identity_must_not_use_foreign_services =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.identity..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.service..",
                            "ru.fitnesscrm.finance.service..",
                            "ru.fitnesscrm.scheduling.service.."
                    );

    // --- no foreign JPA domain entities (facade DTOs / views only); Role enum is allowed ---

    @ArchTest
    static final ArchRule scheduling_must_not_depend_on_foreign_domain =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.scheduling..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.domain..",
                            "ru.fitnesscrm.finance.domain.."
                    );

    @ArchTest
    static final ArchRule finance_must_not_depend_on_foreign_domain =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.finance..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships.domain..",
                            "ru.fitnesscrm.scheduling.domain.."
                    );

    @ArchTest
    static final ArchRule memberships_must_not_depend_on_foreign_domain =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.memberships..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.finance.domain..",
                            "ru.fitnesscrm.scheduling.domain.."
                    );

    @ArchTest
    static final ArchRule identity_must_not_depend_on_other_business_modules =
            noClasses()
                    .that().resideInAPackage("ru.fitnesscrm.identity..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "ru.fitnesscrm.memberships..",
                            "ru.fitnesscrm.scheduling..",
                            "ru.fitnesscrm.finance.."
                    );
}
