package org.coshift.c_adapters.persistence.json;

import org.coshift.a_domain.person.Person;
import org.coshift.b_application.ports.PersonRepository;
import org.coshift.c_adapters.dto.PersonDetailsDto;
import org.coshift.c_adapters.mapper.PersonMapper;
import org.coshift.c_adapters.ports.PersonJsonFileAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Primärer Adapter für Personen-Persistenz.
 *
 *  – Implementiert {@link PersonRepository}.<br>
 *  – Nutzt {@link PersonJsonFileAccessor} für reines Datei-Handling.<br>
 *  – Enthält Such- sowie ID-Vergabe-Logik, bleibt aber JSON-Bibliotheksfrei.
 */
@Component
@Profile("json")
public class PersonJsonRepository implements PersonRepository {

    private final PersonJsonFileAccessor fileAccessor;
    private final AtomicLong nextId = new AtomicLong(1);

    public PersonJsonRepository(PersonJsonFileAccessor fileAccessor) {
        this.fileAccessor = fileAccessor;

        /* ----- maximale bestehende Id ermitteln -------------------- */
        fileAccessor.readAll().stream()
                .map(PersonMapper::toDomain)
                .mapToLong(Person::getId)
                .max()
                .ifPresent(maxId -> nextId.set(maxId + 1));   //  +1  !
    }

    /* ---------------- Speichern ----------------------------------- */

    @Override
    public Person save(Person person) {
        long id = person.getId() > 0
        ? person.getId()
        : nextFreeId();

        Person withId = new Person(
            id,
            person.getNickname(),
            person.getPassword(),
            person.getTimeAccountId(),
            person.getRole()
        );

        List<PersonDetailsDto> dtos = new ArrayList<>(fileAccessor.readAll());
        dtos.removeIf(dto -> Objects.equals(dto.id(), id));
        dtos.add(PersonMapper.toDetailDto(withId));

        if (!fileAccessor.writeAll(dtos)) {
            throw new IllegalStateException("Unable to persist persons JSON file.");
        }
        return withId;
    }

    /* ---------------- Lesen --------------------------------------- */

    @Override
    public Optional<Person> findById(Long id) {
        return fileAccessor.readAll().stream()
                .filter(dto -> Objects.equals(dto.id(), id))
                .findFirst()
                .map(PersonMapper::toDomain);
    }

    @Override
    public Optional<Person> findByNickname(String nickname) {
        return fileAccessor.readAll().stream()
                .filter(dto -> dto.nickname().equalsIgnoreCase(nickname))
                .findFirst()
                .map(PersonMapper::toDomain);
    }

    @Override
    public List<Person> findAll() {
        return fileAccessor.readAll().stream()
                .map(PersonMapper::toDomain)
                .collect(Collectors.toList());
    }

    /* ---------------- Löschen ------------------------------------- */

    @Override
    public void deleteById(Long id) {
        List<PersonDetailsDto> dtos = new ArrayList<>(fileAccessor.readAll());
        boolean removed = dtos.removeIf(dto -> Objects.equals(dto.id(), id));

        if (removed && !fileAccessor.writeAll(dtos)) {
            throw new IllegalStateException("Unable to persist persons JSON file.");
        }
    }

    private long nextFreeId() {
        return fileAccessor.readAll().stream()
                           .mapToLong(PersonDetailsDto::id)
                           .max()
                           .orElse(0) + 1;
    }
}