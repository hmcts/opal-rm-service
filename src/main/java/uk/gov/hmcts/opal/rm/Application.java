package uk.gov.hmcts.opal.rm;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.opal.rm",
        "uk.gov.hmcts.opal.common"
    }
)
@EnableFeignClients(
    basePackages = "uk.gov.hmcts.opal.common.user.authorisation.client",
    defaultConfiguration = uk.gov.hmcts.opal.rm.config.FeignConfiguration.class
)
@EnableCaching
@ConfigurationPropertiesScan
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
