package de.unimarburg.diz.termmapper.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@Getter
@Slf4j
public class SwisslabMap {

    private final Map<String, Set<SwisslabMapEntry>> internalMap =
        new HashMap<>();
    private final CsvPackageMetadata metadata;

    public SwisslabMap(CsvPackageMetadata metadata) {
        this.metadata = metadata;
    }

    public static SwisslabMap buildFromPackage(
        @Qualifier("mappingPackage") Resource pkg)
        throws IOException {

        var zipFile = new ZipFile(pkg.getFile());

        // metadata
        var zipEntry = zipFile.getEntry("datapackage.json");
        if (zipEntry == null) {
            log.error("Error locating metadata file in data package: {}",
                zipFile.getName());
            // TODO
            throw new IllegalArgumentException();
        }
        // parse data
        CsvPackageMetadata metadata;
        try (var inputStream = zipFile.getInputStream(zipEntry)) {
            metadata = parseMetadata(inputStream);
        }

        // mapping file
        var mappingFileName = metadata
            .getResources()
            .stream()
            .findAny()
            .orElseThrow()
            .getPath();
        var pkgResource = zipFile.getEntry(mappingFileName);
        if (pkgResource == null) {
            log.error("Error locating LOINC mapping file in data package: {}",
                zipFile.getName());
            // TODO
            throw new IllegalArgumentException();
        }
        // parse data
        try (var inputStream = zipFile.getInputStream(pkgResource)) {
            var map = new SwisslabMap(metadata);
            var entries = loadItems(inputStream);
            entries
                .forEach(i -> map.put(i.getSwl(), i));
            log.info(
                "Building Swisslab mapping, using version: {}, date: {}, git "
                    + "revision: {}. File checksum (SHA-256): {}.",
                metadata.getVersion(),
                metadata.getCreated(), metadata.getGitCommit(),
                metadata.getChecksum());
            log.info("Swisslab map initialized with {} entries.",
                map.size());
            inputStream.close();
            return map;
        }
    }

    private static List<SwisslabMapEntry> loadItems(InputStream inputStream) {
        try {
            var bootstrapSchema = CsvSchema
                .emptySchema()
                .withHeader()
                .withComments();
            var mapper = new CsvMapper().disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            MappingIterator<SwisslabMapEntry> readValues = mapper
                .readerFor(SwisslabMapEntry.class)
                .with(bootstrapSchema)
                .withFeatures(CsvParser.Feature.EMPTY_STRING_AS_NULL)
                .readValues(inputStream);
            return readValues.readAll();
        } catch (IOException e) {
            log.error(
                "Error occurred while loading object list from input stream.",
                e);
            return Collections.emptyList();
        }
    }

    public static CsvPackageMetadata parseMetadata(InputStream mappingFile)
        throws IOException {

        return new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .readerFor(CsvPackageMetadata.class)
            .readValue(mappingFile, CsvPackageMetadata.class);
    }

    public void put(String code, SwisslabMapEntry entry) {
        // source: entries are ordered with null values first
        var entries = internalMap.computeIfAbsent(code,
            s -> new TreeSet<>(
                comparing(SwisslabMapEntry::getSystem)
                    .thenComparing(
                        SwisslabMapEntry::getMeta,
                        nullsLast(naturalOrder()))
                    .thenComparing(SwisslabMapEntry::getCode)));
        entries.add(entry);
    }

    public List<SwisslabMapEntry> get(String code, String system,
                                      List<String> metaCodes) {
        if (!internalMap.containsKey(code)) {
            return List.of();
        }
        var entries = internalMap.get(code);

        return entries
            .stream()
            // filter system and potential meta code
            .filter(e -> StringUtils.equals(e.getSystem(), system))
            .filter(e -> e.getMeta() == null
                || (metaCodes != null && metaCodes.contains(e.getMeta())))
            .toList();
    }

    public int size() {
        return internalMap.size();
    }

    public Set<String> diff(SwisslabMap fromMap) {

        var keys = new HashSet<>(fromMap
            .getInternalMap()
            .keySet());

        // remove keys present in current map
        keys.removeAll(this
            .getInternalMap()
            .keySet());
        // add keys of entries that differ
        keys.addAll(this
            .getInternalMap()
            .entrySet()
            .stream()
            .filter(e ->
                // new entry or updated
                !fromMap
                    .getInternalMap()
                    .containsKey(e.getKey()) || !fromMap
                    .getInternalMap()
                    .get(e.getKey())
                    .stream()
                    .collect(Collectors.toUnmodifiableSet())
                    .equals(e.getValue()))
            .map(Entry::getKey)
            .toList());

        return keys;
    }
}
