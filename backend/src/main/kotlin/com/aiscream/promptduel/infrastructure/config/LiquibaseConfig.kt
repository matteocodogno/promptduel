package com.aiscream.promptduel.infrastructure.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@ConfigurationProperties(prefix = "spring.liquibase")
data class LiquibaseProperties(
    @DefaultValue("true")
    val enabled: Boolean = true,
    @DefaultValue("classpath:db/changelog/db.changelog-master.yaml")
    val changeLog: String = "classpath:db/changelog/db.changelog-master.yaml",
)

@Configuration
class LiquibaseConfig {

    @Bean
    @ConditionalOnProperty(name = ["spring.liquibase.enabled"], havingValue = "true", matchIfMissing = true)
    fun liquibase(dataSource: DataSource, props: LiquibaseProperties): SpringLiquibase =
        SpringLiquibase().apply {
            this.dataSource = dataSource
            this.changeLog = props.changeLog
        }
}
