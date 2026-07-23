package ru.fitnesscrm;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "ru.fitnesscrm", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleBoundaryTest {

    @ArchTest
    public static final ArchRule scheduling_should_not_depend_on_membership_repository =
            noClasses()
                    .that().resideInAPackage("..scheduling..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..memberships.repository..");

    @ArchTest
    public static final ArchRule finance_should_not_depend_on_membership_repository =
            noClasses()
                    .that().resideInAPackage("..finance..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..memberships.repository..");

    @ArchTest
    public static final ArchRule identity_should_not_depend_on_membership_repository =
            noClasses()
                    .that().resideInAPackage("..identity..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..memberships.repository..");

}
