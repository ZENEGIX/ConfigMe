package ch.jalu.configme.beanmapper.propertydescription;


import ch.jalu.configme.beanmapper.ConfigMeMapperException;
import ch.jalu.configme.samples.beanannotations.AnnotatedEntry;
import ch.jalu.configme.samples.beanannotations.BeanWithEmptyName;
import ch.jalu.configme.samples.beanannotations.BeanWithNameClash;
import ch.jalu.configme.samples.inheritance.Child;
import ch.jalu.configme.samples.inheritance.Middle;
import ch.jalu.configme.utils.TypeInformation;
import org.junit.Test;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static ch.jalu.configme.TestUtils.transform;
import static ch.jalu.configme.TestUtils.verifyException;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link BeanDescriptionFactoryImpl}.
 */
public class BeanDescriptionFactoryImplTest {

    @Test
    public void shouldReturnWritableProperties() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        Collection<BeanPropertyDescription> descriptions = factory.getAllProperties(SampleBean.class);

        // then
        assertThat(descriptions, hasSize(2));
        assertThat(getDescription("size", descriptions).getTypeInformation(), equalTo(new TypeInformation(int.class)));
        assertThat(getDescription("name", descriptions).getTypeInformation(), equalTo(new TypeInformation(String.class)));
    }

    @Test
    public void shouldReturnEmptyListForNonBeanClass() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when / then
        assertThat(factory.getAllProperties(List.class), empty());
    }

    @Test
    public void shouldHandleBooleanMethodsAndMatchWithFields() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        List<BeanPropertyDescription> properties = new ArrayList<>(factory.getAllProperties(BooleanTestBean.class));

        // then
        assertThat(properties, hasSize(4));
        assertThat(transform(properties, BeanPropertyDescription::getName),
            containsInAnyOrder("active", "isField", "empty", "isNotMatched"));

        // First two elements can be mapped to fields, so check their order. For the two unknown ones, we don't make any guarantees
        assertThat(properties.get(0).getName(), equalTo("active"));
        assertThat(properties.get(1).getName(), equalTo("isField"));
    }

    @Test
    public void shouldNotConsiderTransientFields() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        Collection<BeanPropertyDescription> properties = factory.getAllProperties(BeanWithTransientFields.class);

        // then
        assertThat(properties, hasSize(2));
        assertThat(transform(properties, BeanPropertyDescription::getName), contains("name", "mandatory"));
    }

    @Test
    public void shouldBeAwareOfInheritanceAndRespectOrder() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        Collection<BeanPropertyDescription> properties = factory.getAllProperties(Middle.class);

        // then
        assertThat(properties, hasSize(3));
        assertThat(transform(properties, BeanPropertyDescription::getName), contains("id", "name", "ratio"));
    }

    @Test
    public void shouldLetChildFieldsOverrideParentFields() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        Collection<BeanPropertyDescription> properties = factory.getAllProperties(Child.class);

        // then
        assertThat(properties, hasSize(5));
        assertThat(transform(properties, BeanPropertyDescription::getName),
            contains("id", "temporary", "name", "ratio", "importance"));
    }

    @Test
    public void shouldUseExportName() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when
        Collection<BeanPropertyDescription> properties = factory.getAllProperties(AnnotatedEntry.class);

        // then
        assertThat(properties, hasSize(2));
        assertThat(transform(properties, BeanPropertyDescription::getName),
            contains("id", "has-id"));
    }

    @Test
    public void shouldThrowForMultiplePropertiesWithSameName() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when / then
        verifyException(
            () -> factory.getAllProperties(BeanWithNameClash.class),
            ConfigMeMapperException.class,
            "multiple properties with name 'threshold'");
    }

    @Test
    public void shouldThrowForWhenExportNameIsNullForProperty() {
        // given
        BeanDescriptionFactory factory = new BeanDescriptionFactoryImpl();

        // when / then
        verifyException(
            () -> factory.getAllProperties(BeanWithEmptyName.class),
            ConfigMeMapperException.class,
            "may not be empty");
    }

    private static BeanPropertyDescription getDescription(String name,
                                                          Collection<BeanPropertyDescription> descriptions) {
        for (BeanPropertyDescription description : descriptions) {
            if (name.equals(description.getName())) {
                return description;
            }
        }
        throw new IllegalArgumentException(name);
    }

    private static final class SampleBean {

        private String name;
        private int size;
        private long longField; // static "getter" method
        private UUID uuid = UUID.randomUUID(); // no setter

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public UUID getUuid() {
            return uuid;
        }

        public static long getLongField() {
            // Method with normal getter name is static!
            return 0;
        }

        public void setLongField(long longField) {
            this.longField = longField;
        }
    }

    private static final class BooleanTestBean {
        private boolean isEmpty;
        private Boolean isReference;
        private boolean active;
        private String isString;
        private boolean isField;
        private boolean notMatched;

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            isEmpty = empty;
        }

        public Boolean isReference() { // "is" getter only supported for primitive boolean
            return isReference;
        }

        public void setReference(Boolean isReference) {
            this.isReference = isReference;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String isString() { // "is" only supported for boolean
            return isString;
        }

        public void setString(String isString) {
            this.isString = isString;
        }

        public boolean getIsField() {
            return isField;
        }

        public void setIsField(boolean field) {
            this.isField = field;
        }

        // -----------------
        // notMatched: creates a valid property "isNotMatched" picked up by the introspector,
        // but we should not match this to the field `notMatched`.
        // -----------------
        public boolean getIsNotMatched() {
            return notMatched;
        }

        public void setIsNotMatched(boolean notMatched) {
            this.notMatched = notMatched;
        }
    }

    private static final class BeanWithTransientFields {
        private String name;
        private transient long tempId;
        private transient boolean isSaved;
        private boolean isMandatory;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getTempId() {
            return tempId;
        }

        @Transient
        public void setTempId(long tempId) {
            this.tempId = tempId;
        }

        @Transient
        public boolean isSaved() {
            return isSaved;
        }

        public void setSaved(boolean saved) {
            isSaved = saved;
        }

        public boolean isMandatory() {
            return isMandatory;
        }

        public void setMandatory(boolean mandatory) {
            isMandatory = mandatory;
        }
    }
}
