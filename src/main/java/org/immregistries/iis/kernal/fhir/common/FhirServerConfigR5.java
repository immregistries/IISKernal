package org.immregistries.iis.kernal.fhir.common;

import ca.uhn.fhir.jpa.config.r5.JpaR5Config;
import org.immregistries.iis.kernal.fhir.ServerConfig;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Conditional(OnR5Condition.class)
@Import({
	StarterJpaConfig.class,
	JpaR5Config.class,
	ElasticsearchConfig.class,
	ServerConfig.class,
})
public class FhirServerConfigR5 {
}
