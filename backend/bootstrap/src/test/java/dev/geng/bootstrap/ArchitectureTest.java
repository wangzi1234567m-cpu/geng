package dev.geng.bootstrap;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {
    @Test
    void coreRemainsIndependentAndAdaptersDoNotDependOnEachOther() {
        var code = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("dev.geng");
        classes().that().resideInAPackage("dev.geng.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "dev.geng.domain..")
                .check(code);
        classes().that().resideInAPackage("dev.geng.application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "dev.geng.domain..", "dev.geng.application..")
                .check(code);
        noClasses().that().resideInAPackage("dev.geng.api..")
                .should().dependOnClassesThat().resideInAnyPackage("dev.geng.infrastructure..", "dev.geng.bootstrap..")
                .check(code);
        noClasses().that().resideInAPackage("dev.geng.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage("dev.geng.api..", "dev.geng.bootstrap..")
                .allowEmptyShould(true).check(code);
        slices().matching("dev.geng.(*)..").should().beFreeOfCycles().check(code);
    }
}
