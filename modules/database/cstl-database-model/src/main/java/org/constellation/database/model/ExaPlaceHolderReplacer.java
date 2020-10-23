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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import org.flywaydb.core.internal.util.PlaceholderReplacer;

/**
 * Derivated from org.flywaydb.core.internal.util.PlaceholderReplacer for handling database migration in hsql
 * because original patches contains postgresql specific statement.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExaPlaceHolderReplacer extends PlaceholderReplacer {

    /**
     * Flag indicating if we need to remove specific postgresql specific statement
     */
    private final boolean isPostgres;

    /**
     * Creates a new PlaceholderReplacer.
     *
     * @param placeholders      A map of <placeholder, replacementValue> to apply to sql migration scripts.
     * @param placeholderPrefix The prefix of every placeholder. Usually ${
     * @param placeholderSuffix The suffix of every placeholder. Usually }
     */
    public ExaPlaceHolderReplacer(Map<String, String> placeholders, String placeholderPrefix, String placeholderSuffix, boolean isPostgres) {
        super(placeholders, placeholderPrefix, placeholderSuffix);
        this.isPostgres = isPostgres;
    }

    /**
     * Replaces the placeholders in this input string with their corresponding values.
     *
     * @param input The input to process.
     * @return The input string with all placeholders replaced.
     */
    @Override
    public String replacePlaceholders(String input) {
        String output = super.replacePlaceholders(input);

        // remove postgres specific statement
        if (!isPostgres) {

            // V1.1.0_0__initial replace all the script
            if (output.contains("Started on 2015-08-26 16:02:15 CEST")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1.1.0_0__initial.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // V1.1.1_0__cstl_user_length multi alter column not supported
            if (output.contains("-- Increase some column varchar length")) {
                output = "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"login\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"firstname\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"lastname\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"email\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"avatar\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"city\" TYPE varchar(255);\n" +
                         "ALTER TABLE \"admin\".\"cstl_user\" ALTER COLUMN \"country\" TYPE varchar(255);";
            }

            // V1.2.0_0__ChangeStyleProvider
            if (output.contains("CONSTRAINT \"style_provider_fk\"")) {
                output = output.replace("CONSTRAINT \"style_provider_fk\"", "CONSTRAINT \"admin\".\"style_provider_fk\"");
            }


            // V1.2.0_1__Attachment replace all the script
            if (output.contains("-- Add byte array field to store a attachements (used for metadata quicklook)")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1.2.0_1__Attachment.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // T1444810735__link_provider_to_sensor
            if (output.contains("sos_id integer")) {
                output = output.replace("sos_id integer", "\"sos_id\" integer");
            }
            if (output.contains("provider_id integer")) {
                output = output.replace("provider_id integer", "\"provider_id\" integer");
            }
            if (output.contains("all_sensor boolean")) {
                output = output.replace("all_sensor boolean", "\"all_sensor\" boolean");
            }

            // T1454602042__CSWProvider
            if (output.contains("csw_id integer")) {
                output = output.replace("csw_id integer", "\"csw_id\" integer");
            }
            if (output.contains("all_metadata boolean")) {
                output = output.replace("all_metadata boolean", "\"all_metadata\" boolean");
            }

            // T1456477310__desc replace all the script
            if (output.contains("ALTER TABLE \"metadata\" DROP COLUMN \"metadata_iso\"")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1456477310__desc.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // T1457694371__desc replace all the script
            if (output.contains("ALTER TABLE \"sensor\" DROP COLUMN \"metadata\"")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1457694371__desc.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // T1475157500__add_pk_csw_provider
            if (output.contains("(csw_id,provider_id)")) {
                output = output.replace("(csw_id,provider_id)", "(\"csw_id\",\"provider_id\")");
            }

            // T1479221861__thesaurus replace all the script
            if (output.contains("CREATE TABLE \"thesaurus\" (")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1479221861__thesaurus.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // T1481813341__sensor_x_sos
            if (output.contains("sensor_id integer")) {
                output = output.replace("sensor_id integer", "\"sensor_id\" integer");
            }
            if (output.contains("(sensor_id, sos_id)")) {
                output = output.replace("(sensor_id, sos_id)", "(\"sensor_id\", \"sos_id\")");
            }

            // T1491899272__attachment_fix
            if (output.contains("COLUMN content")) {
                output = output.replace("COLUMN content", "COLUMN \"content\"");
            }

            // T1508232019__hide_default_data
            if (output.contains(" id=")) {
                output = output.replace(" id=", " \"id\"=");
            }
            if (output.contains(" hidden=")) {
                output = output.replace(" hidden=", " \"hidden\"=");
            }

            //T1516693084__addDatasource replace all the script
            if (output.contains("CREATE TABLE \"admin\".\"datasource\"")) {
                try {
                    output = toString(getResourceAsStream("org/constellation/database/model/migration/HSQLDB_1516693084__addDatasource.sql"), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            //T1516805809__AddDatasourceSeq remove the script
            if (output.contains("CREATE SEQUENCE datasource_id_seq")) {
                output = "";
            }

            // T1519142389__AddDatasourceSelectPath
            if (output.contains("datasource_id integer")) {
                output = output.replace("datasource_id integer", "\"datasource_id\" integer");
            }

            //T1522417689__AddDatasourceTypes
            if (output.contains("ADD COLUMN \"type\" character varying NOT NULL")) {
                output = output.replace("ADD COLUMN \"type\" character varying NOT NULL", "ADD COLUMN \"type\" character varying(1000) NOT NULL");
            }

            //T1522757640__AddDatasourceFormat
            if (output.contains("ADD COLUMN \"format\" character varying")) {
                output = output.replace("ADD COLUMN \"format\" character varying", "ADD COLUMN \"format\" character varying(1000)");
            }

            //T1523268116__AddDatasetType
            if (output.contains("ADD COLUMN \"type\" character varying;")) {
                output = output.replace("ADD COLUMN \"type\" character varying", "ADD COLUMN \"type\" character varying(1000);");
            }

            //T1547651931__fixLayerIndex
            if (output.contains("DROP INDEX \"LAYER_NAME-SERVICE_IDX\"")) {
                output = output.replace("DROP INDEX \"LAYER_NAME-SERVICE_IDX\"", "DROP INDEX \"admin\".\"LAYER_NAME-SERVICE_IDX\"");
            }
            if (output.contains("ON layer")) {
                output = output.replace("ON layer", "ON \"admin\".\"layer\"");
            }

            // T1548687455__AddSensorProfile
            if (output.contains("ADD COLUMN profile")) {
                output = output.replace("ADD COLUMN profile", "ADD COLUMN \"profile\"");
            }

            // T1563540782__LayerAliasIndex
            if (output.contains("REATE UNIQUE INDEX \"LAYER_ALIAS-SERVICE_IDX\"")) {
                output = output.replace("(\"alias\", service)", "(\"alias\", \"service\")");
            }

            // T1569399488__AddServiceImpl
            if (output.contains("ADD COLUMN impl")) {
                output = output.replace("ADD COLUMN impl", "ADD COLUMN \"impl\"");
            }

            // various replacement
            if (output.contains("(name, namespace, service)")) {
                output = output.replace("(name, namespace, service)", "(\"name\", \"namespace\", \"service\")");
            }
            if (output.contains("(datasource_id)")) {
                output = output.replace("(datasource_id)", "(\"datasource_id\")");
            }
            if (output.contains("(sensor_id)")) {
                output = output.replace("(sensor_id)", "(\"sensor_id\")");
            }
            if (output.contains("(provider_id)")) {
                output = output.replace("(provider_id)", "(\"provider_id\")");
            }
            if (output.contains("(map_context_id)")) {
                output = output.replace("(map_context_id)", "(\"map_context_id\")");
            }
            if (output.contains("(datasource_id)")) {
                output = output.replace("(datasource_id)", "(\"datasource_id\")");
            }
            if (output.contains("(id)")) {
                output = output.replace("(id)", "(\"id\")");
            }
            if (output.contains("(sos_id)")) {
                output = output.replace("(sos_id)", "(\"sos_id\")");
            }
            if (output.contains("(csw_id)")) {
                output = output.replace("(csw_id)", "(\"csw_id\")");
            }
            if (output.contains("::character varying")) {
                output = output.replace("::character varying", "");
            }
            if (output.contains("CACHE 1")) {
                output = output.replace("CACHE 1", "");
            }

            if (output.contains("USING btree")) {
                output = output.replace("USING btree", "");
            }

            if (output.contains(" ONLY ")) {
                output = output.replace(" ONLY ", "");
            }

            if (output.contains("SET search_path = admin, pg_catalog")) {
                output = output.replace("SET search_path = admin, pg_catalog", "");
            }

            if (output.contains("TABLE \"metadata\"")) {
                output = output.replace("TABLE \"metadata\"", "TABLE \"admin\".\"metadata\"");
            }

            if (output.contains("TABLE \"internal_metadata\"")) {
                output = output.replace("TABLE \"internal_metadata\"", "TABLE \"admin\".\"internal_metadata\"");
            }

            if (output.contains("TABLE \"provider_x_csw\"")) {
                output = output.replace("TABLE \"provider_x_csw\"", "TABLE \"admin\".\"provider_x_csw\"");
            }

            if (output.contains("TABLE \"provider_x_sos\"")) {
                output = output.replace("TABLE \"provider_x_sos\"", "TABLE \"admin\".\"provider_x_sos\"");
            }

            // Bad case management for HSQL on T1580143603__SensorOmType.sql
            if (output.equals("ALTER TABLE \"admin\".\"sensor\" ADD COLUMN om_type character varying(100);")) {
                output = "ALTER TABLE \"admin\".\"sensor\" ADD COLUMN \"om_type\" character varying(100);";
            }

            /*if (output.contains("SET statement_timeout = 0")) {
                output = output.replace("SET statement_timeout = 0", "");
            }
            if (output.contains("SET client_encoding = 'UTF8'")) {
                output = output.replace("SET client_encoding = 'UTF8'", "");
            }
            if (output.contains("SET standard_conforming_strings = on")) {
                output = output.replace("SET standard_conforming_strings = on", "");
            }
            if (output.contains("SET check_function_bodies = false")) {
                output = output.replace("SET check_function_bodies = false", "");
            }
            if (output.contains("SET client_min_messages = warning")) {
                output = output.replace("SET client_min_messages = warning", "");
            }
            if (output.contains("SET default_with_oids = false")) {
                output = output.replace("SET default_with_oids = false", "");
            }
            if (output.contains(" text")) {
                output = output.replace(" text", " LONGVARCHAR");
            }
            */

        }
        return output;
    }

    public static InputStream getResourceAsStream(final String url) {
        final ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(url);
    }

    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    public static String toString(final InputStream stream, final Charset encoding) throws IOException {

        final StringBuilder sb  = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, encoding))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            stream.close();
        }
        return sb.toString();
    }
}
