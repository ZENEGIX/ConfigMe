package com.github.authme.configme.resource;

import com.github.authme.configme.TestUtils;
import com.github.authme.configme.configurationdata.ConfigurationData;
import com.github.authme.configme.properties.Property;
import com.github.authme.configme.properties.StringListProperty;
import com.github.authme.configme.samples.TestEnum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Tests that {@link YamlFileResource} exports Sets in a nice way (cf. issue #27).
 */
public class YamlSetPropertyExportTest {

    private static final String SAMPLE_FILE = "/empty_file.yml";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File configFile;

    @Before
    public void copyConfigFile() throws IOException {
        Path jarFile = TestUtils.getJarPath(SAMPLE_FILE);
        File testFile = new File(temporaryFolder.newFolder(), "config.yml");
        Files.copy(jarFile, testFile.toPath());
        configFile = testFile;
    }

    @Test
    public void shouldLoadAndExportProperly() throws IOException {
        // given
        PropertyResource resource = new YamlFileResource(configFile);
        resource.setValue("sample.ratio.fields", Arrays.asList(TestEnum.FIRST, TestEnum.SECOND, TestEnum.THIRD));
        Property<Set<TestEnum>> setProperty = new EnumSetProperty("sample.ratio.fields", Collections.emptySet());

        // when
        resource.exportProperties(new ConfigurationData(singletonList(setProperty)));
        resource.reload();

        // then
        assertThat(setProperty.getValue(resource), contains(TestEnum.FIRST, TestEnum.SECOND, TestEnum.THIRD));
        // Check that export can be read with StringListProperty too
        assertThat(new StringListProperty("sample.ratio.fields").getValue(resource),
            contains(TestEnum.FIRST.name(), TestEnum.SECOND.name(), TestEnum.THIRD.name()));

        assertThat(String.join("\n", Files.readAllLines(configFile.toPath())), containsString(
            "        fields: \n"
            + "        - 'FIRST'\n"
            + "        - 'SECOND'\n"
            + "        - 'THIRD'"));
    }

    private static final class EnumSetProperty extends Property<Set<TestEnum>> {

        EnumSetProperty(String path, Set<TestEnum> defaultValue) {
            super(path, defaultValue);
        }

        @Override
        protected Set<TestEnum> getFromResource(PropertyResource resource) {
            List<?> list = resource.getList(getPath());
            if (list == null) {
                return null;
            }
            return new LinkedHashSet<>(
                list.stream().map(v -> TestEnum.valueOf(v.toString())).collect(Collectors.toList()));
        }
    }
}