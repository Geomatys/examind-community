/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.database.model;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.DbSupportFactory;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.PlaceholderReplacer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import org.flywaydb.core.internal.util.Locations;
import org.flywaydb.core.internal.util.scanner.Scanner;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class FlywayUtils {

    public static Flyway createFlywayConfig(DataSource dataSource, boolean isPostgres) throws SQLException {

        final String locationString = "org/constellation/database/model/migration";

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(locationString);
        flyway.setSkipDefaultResolvers(true);

        final String encoding = "UTF-8";
        final String separator = "__";
        final String suffix = ".sql";
        final String repeatPrefix = "R";


        final HashMap<String, String> placeholders = new HashMap<>();
        final PlaceholderReplacer defaultReplacer = new ExaPlaceHolderReplacer(placeholders, "${", "}", isPostgres);

        try (final Connection connection = dataSource.getConnection()) {

            DbSupport dbSupport = DbSupportFactory.createDbSupport(connection, false);

            Flyway flywayInit = new Flyway();
            flywayInit.setDataSource(dataSource);
            flywayInit.setLocations(locationString);

            flywayInit.setEncoding(encoding);
            flywayInit.setSqlMigrationPrefix("V");
            flywayInit.setSqlMigrationSeparator(separator);
            flywayInit.setSqlMigrationSuffix(suffix);
            flywayInit.setRepeatableSqlMigrationPrefix(repeatPrefix);

            final MigrationResolver initResolvers = new SqlMigrationResolver(
                    dbSupport,
                    new Scanner(flywayInit.getClassLoader()),
                    new Locations(locationString),
                    defaultReplacer,
                    flywayInit);



            Flyway flywayPatches = new Flyway();
            flywayPatches.setDataSource(dataSource);
            flywayPatches.setLocations(locationString);

            flywayPatches.setEncoding(encoding);
            flywayPatches.setSqlMigrationPrefix("T");
            flywayPatches.setSqlMigrationSeparator(separator);
            flywayPatches.setSqlMigrationSuffix(suffix);
            flywayPatches.setRepeatableSqlMigrationPrefix(repeatPrefix);

            final MigrationResolver patchResolvers = new SqlMigrationResolver(
                    dbSupport,
                    new Scanner(flywayPatches.getClassLoader()),
                    new Locations(locationString),
                    defaultReplacer,
                    flywayPatches);
            flyway.setResolvers(initResolvers, patchResolvers);
        }

        return flyway;
    }
}
