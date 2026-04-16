package dk.ek.shift_happens.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

// With both spring-boot-starter-data-jpa and spring-boot-starter-data-neo4j on the classpath,
// both JpaBaseConfiguration and Neo4jTransactionManagerAutoConfiguration use
// @ConditionalOnMissingBean(TransactionManager.class). Whichever runs first suppresses the other,
// leaving only one TM — and everything looking for the other by name fails.
//
// Fix: define both transaction managers explicitly, bypassing both conditionals.
// JpaTransactionManager is @Primary so JPA repositories and @Transactional pick it up by default.
// Neo4jTransactionManager is wired into Neo4jTemplate explicitly via @Qualifier.
@Configuration
public class Neo4jConfig {

    @Primary
    @Bean("transactionManager")
    public JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider selectionProvider) {
        return new Neo4jTransactionManager(driver, selectionProvider);
    }

    @Bean
    public Neo4jTemplate neo4jTemplate(
            Neo4jClient neo4jClient,
            Neo4jMappingContext mappingContext,
            @Qualifier("neo4jTransactionManager") Neo4jTransactionManager neo4jTm) {
        Neo4jTemplate template = new Neo4jTemplate(neo4jClient, mappingContext);
        template.setTransactionManager(neo4jTm);
        return template;
    }
}
