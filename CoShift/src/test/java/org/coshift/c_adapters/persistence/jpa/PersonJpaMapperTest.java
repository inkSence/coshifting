package org.coshift.c_adapters.persistence.jpa;

import org.coshift.a_domain.person.Person;
import org.coshift.a_domain.person.PersonRole;
import org.coshift.d_frameworks.db.PersonJpaEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonJpaMapperTest {

    @Test
    void toEntityMapsAllFieldsWithId() {
        Person person = new Person(12L, "nick", "secret", 7L, PersonRole.ADMIN);

        PersonJpaEntity entity = PersonJpaMapper.toEntity(person);

        assertThat(entity.getId()).isEqualTo(12L);
        assertThat(entity.getNickname()).isEqualTo("nick");
        assertThat(entity.getPassword()).isEqualTo("secret");
        assertThat(entity.getTimeAccountId()).isEqualTo(7L);
        assertThat(entity.getRole()).isEqualTo(PersonRole.ADMIN);
    }

    @Test
    void toEntityOmitsIdWhenZero() {
        Person person = new Person(0L, "nick", "secret", 7L, PersonRole.USER);

        PersonJpaEntity entity = PersonJpaMapper.toEntity(person);

        assertThat(entity.getId()).isNull();
    }

    @Test
    void toDomainMapsAllFields() {
        PersonJpaEntity entity = new PersonJpaEntity();
        entity.setId(5L);
        entity.setNickname("lena");
        entity.setPassword("pw");
        entity.setTimeAccountId(9L);
        entity.setRole(PersonRole.USER);

        Person person = PersonJpaMapper.toDomain(entity);

        assertThat(person.getId()).isEqualTo(5L);
        assertThat(person.getNickname()).isEqualTo("lena");
        assertThat(person.getPassword()).isEqualTo("pw");
        assertThat(person.getTimeAccountId()).isEqualTo(9L);
        assertThat(person.getRole()).isEqualTo(PersonRole.USER);
    }

    @Test
    void toDomainUsesZeroWhenIdIsNull() {
        PersonJpaEntity entity = new PersonJpaEntity();
        entity.setNickname("lena");
        entity.setPassword("pw");
        entity.setTimeAccountId(9L);
        entity.setRole(PersonRole.USER);

        Person person = PersonJpaMapper.toDomain(entity);

        assertThat(person.getId()).isZero();
    }
}