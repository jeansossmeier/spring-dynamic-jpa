package com.joutvhu.dynamic.jpa;

import com.joutvhu.dynamic.jpa.freemarker.FreemarkerDynamicQueryTemplateHandler;
import com.joutvhu.dynamic.jpa.handlebars.HandlebarsDynamicQueryTemplateHandler;
import com.joutvhu.dynamic.jpa.support.DynamicJpaRepositoryFactoryBean;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {"com.joutvhu.dynamic.jpa.repository"},
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager",
        repositoryFactoryBeanClass = DynamicJpaRepositoryFactoryBean.class
)
public class JpaDynamicApplication {

    public static final String PROPERTY_DYNAMIC_JPA_TEMPLATE = "dynamic.jpa.template";

    public static void main(String[] args) {
        SpringApplication.run(JpaDynamicApplication.class);
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(Database.DERBY);
        jpaVendorAdapter.setGenerateDdl(true);
        return jpaVendorAdapter;
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");

        LocalContainerEntityManagerFactoryBean managerFactory = new LocalContainerEntityManagerFactoryBean();
        managerFactory.setDataSource(dataSource);
        managerFactory.setJpaVendorAdapter(jpaVendorAdapter());
        managerFactory.setPackagesToScan("com.joutvhu.dynamic.jpa.entity");

        managerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        managerFactory.setJpaProperties(properties);

        return managerFactory;
    }

    @Primary
    @Bean(name = "transactionManager")
    public JpaTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "dataSourceInitializer")
    public DataSourceInitializer emdDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("sql/table.sql"));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        initializer.setDatabasePopulator(populator);
        initializer.setEnabled(true);
        initializer.afterPropertiesSet();
        return initializer;
    }

    @Bean
    @ConditionalOnProperty(name = PROPERTY_DYNAMIC_JPA_TEMPLATE, havingValue = "freemarker")
    public DynamicQueryTemplateHandler freemarkerDynamicQueryTemplateHandler() {
        FreemarkerDynamicQueryTemplateHandler templateHandler = new FreemarkerDynamicQueryTemplateHandler();
        templateHandler.setSuffix(".dsql");
        templateHandler.setTemplateLocation("/query/freemarker");
        return templateHandler;
    }

    @Bean
    @ConditionalOnProperty(name = PROPERTY_DYNAMIC_JPA_TEMPLATE, havingValue = "handlebars")
    public DynamicQueryTemplateHandler handlebarsDynamicQueryTemplateHandler() {
        HandlebarsDynamicQueryTemplateHandler templateHandler = new HandlebarsDynamicQueryTemplateHandler();
        templateHandler.setSuffix(".dsql");
        templateHandler.setTemplateLocation("/query/handlebars");
        return templateHandler;
    }
}
