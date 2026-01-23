package org.coshift.d_frameworks.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.coshift.c_adapters.dto.PersonDetailsDto;
import org.coshift.c_adapters.ports.PersonJsonFileAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

/**
 * Technischer Datei-Zugriff auf den Personen-Bestand.
 * Keine Geschäftslogik – diese steckt im Adapter/Use-Case.
 */
@Repository
@Profile("json")
public class PersonGsonFileAccessor implements PersonJsonFileAccessor {


    private static Path FILE = Paths.get("data/persons", "persons.json");     
    private static final Type LIST_TYPE = new TypeToken<List<PersonDetailsDto>>() {}.getType();

    private static final Logger LOG = LoggerFactory.getLogger(PersonGsonFileAccessor.class);

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public PersonGsonFileAccessor() {} 

    public PersonGsonFileAccessor(Path file) {
        this.FILE = file;
    }

    /* -------------------- Lesen ----------------------------------- */

    @Override
    public List<PersonDetailsDto> readAll() {
        if (!Files.exists(FILE)) {
            LOG.warn("Persons file {} not found – returning empty list", FILE.toAbsolutePath());
            return Collections.emptyList();
        }
        try (var reader = Files.newBufferedReader(FILE)) {
            List<PersonDetailsDto> list = gson.fromJson(reader, LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read persons JSON file", e);
        }
    }

    /* -------------------- Schreiben ------------------------------- */

    @Override
    public boolean writeAll(List<PersonDetailsDto> persons) {
        try {
            Files.createDirectories(FILE.getParent());
            try (var writer = Files.newBufferedWriter(FILE)) {
                gson.toJson(persons, LIST_TYPE, writer);
            }
            return true;
        } catch (IOException e) {
            LOG.error("Unable to write persons JSON file: {}", e.getMessage());
            return false;
        }
    }
}