package pe.edu.ta4.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Reglas de capas por feature: si alguien las rompe, el build falla. */
@AnalyzeClasses(packages = "pe.edu.ta4", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersTalkToServicesNotToRepositories = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule controllersDoNotExposeEntities = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..model..")
        .because("la API solo expone DTOs; las entidades son detalle de persistencia");

    @ArchTest
    static final ArchRule servicesDoNotKnowTheWebLayer = noClasses()
        .that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule repositoriesAreOnlyUsedByServices = noClasses()
        .that().resideOutsideOfPackages("..service..", "..repository..")
        .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule modelIsIndependentOfTheOtherLayers = noClasses()
        .that().resideInAPackage("..model..")
        .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..");

    @ArchTest
    static final ArchRule sharedDoesNotDependOnFeatures = noClasses()
        .that().resideInAPackage("pe.edu.ta4.shared..")
        .should().dependOnClassesThat().resideInAnyPackage("pe.edu.ta4.business..", "pe.edu.ta4.faq..", "pe.edu.ta4.ruc..", "pe.edu.ta4.bot..");

    @ArchTest
    static final ArchRule businessDoesNotUseOtherFeatures = noClasses()
        .that().resideInAPackage("pe.edu.ta4.business..")
        .should().dependOnClassesThat().resideInAnyPackage("pe.edu.ta4.faq..", "pe.edu.ta4.ruc..", "pe.edu.ta4.bot..");

    @ArchTest
    static final ArchRule faqDoesNotUseOtherFeatures = noClasses()
        .that().resideInAPackage("pe.edu.ta4.faq..")
        .should().dependOnClassesThat().resideInAnyPackage("pe.edu.ta4.business..", "pe.edu.ta4.ruc..", "pe.edu.ta4.bot..");

    @ArchTest
    static final ArchRule rucDoesNotUseOtherFeatures = noClasses()
        .that().resideInAPackage("pe.edu.ta4.ruc..")
        .should().dependOnClassesThat().resideInAnyPackage("pe.edu.ta4.business..", "pe.edu.ta4.faq..", "pe.edu.ta4.bot..");

    @ArchTest
    static final ArchRule serviceImplementationsLiveInImpl = classes()
        .that().haveSimpleNameEndingWith("ServiceImpl")
        .should().resideInAPackage("..service.impl..");

    @ArchTest
    static final ArchRule controllersEndWithController = classes()
        .that().resideInAPackage("..controller..").and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .should().haveSimpleNameEndingWith("Controller");
}
