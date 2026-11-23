package br.com.fincontrol;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.fincontrol")
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal = onionArchitecture()
            .domainModels("br.com.fincontrol.domain.model..")
            .domainServices("br.com.fincontrol.domain.service..")
            .applicationServices("br.com.fincontrol.application..")
            .adapter("web", "br.com.fincontrol.infrastructure.in.web..")
            .adapter("persistence", "br.com.fincontrol.infrastructure.out.persistence..")
            .adapter("security", "br.com.fincontrol.infrastructure.out.security..")
            .adapter("external", "br.com.fincontrol.infrastructure.out.external..")
            .adapter("storage", "br.com.fincontrol.infrastructure.out.storage..")
            .adapter("auditoria", "br.com.fincontrol.infrastructure.out.auditoria..");

    @ArchTest
    static final ArchRule dominioSemFramework = noClasses()
            .that().resideInAPackage("br.com.fincontrol.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "java.net.http..");

    @ArchTest
    static final ArchRule dominioSemPontoFlutuante = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("br.com.fincontrol.domain..")
            .should().notHaveRawType(double.class)
            .andShould().notHaveRawType(float.class)
            .andShould().notHaveRawType(Double.class)
            .andShould().notHaveRawType(Float.class);
}
