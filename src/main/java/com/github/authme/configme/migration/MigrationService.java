package com.github.authme.configme.migration;

import com.github.authme.configme.propertymap.PropertyMap;
import com.github.authme.configme.resource.PropertyResource;

/**
 * The migration service is called when the settings manager is instantiated. It allows to
 * validate the settings and perform migrations (e.g. delete old settings, rename settings).
 * If a migration is performed, the config file will be saved again.
 */
public interface MigrationService {

    /**
     * Checks the settings and perform any necessary migrations.
     *
     * @param resource the property resource
     * @param propertyMap the property map
     * @return {@code true} if a migration has been performed, {@code false} if the settings are up-to-date
     */
    boolean checkAndMigrate(PropertyResource resource, PropertyMap propertyMap);

}