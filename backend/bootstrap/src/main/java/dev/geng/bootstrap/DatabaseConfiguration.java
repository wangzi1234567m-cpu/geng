package dev.geng.bootstrap;

import dev.geng.application.entry.EntryAccessReader;
import dev.geng.application.entry.EntryAuthorization;
import dev.geng.application.transaction.ActorTransactions;
import dev.geng.domain.entry.EntryAccessPolicy;
import dev.geng.infrastructure.entry.JdbcEntryAccessReader;
import dev.geng.infrastructure.transaction.JdbcActorTransactions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Profile("local")
public class DatabaseConfiguration {
    @Bean ActorTransactions actorTransactions(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        return new JdbcActorTransactions(jdbc, manager);
    }
    @Bean EntryAccessReader entryAccessReader(JdbcTemplate jdbc) { return new JdbcEntryAccessReader(jdbc); }
    @Bean EntryAuthorization entryAuthorization(EntryAccessReader reader) {
        return new EntryAuthorization(reader, new EntryAccessPolicy());
    }
}
