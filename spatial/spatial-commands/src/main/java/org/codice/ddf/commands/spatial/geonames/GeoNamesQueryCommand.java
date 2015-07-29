/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.commands.spatial.geonames;

import java.io.PrintStream;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "geonames", name = "query",
        description = "Queries the local GeoNames index.")
public final class GeoNamesQueryCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesQueryCommand.class);

    @Argument(index = 0, name = "query",
            description = "The query.",
            required = true)
    private String query = null;

    private GeoEntryQueryable geoEntryQueryable;

    public void setGeoEntryQueryable(final GeoEntryQueryable geoEntryQueryable) {
        this.geoEntryQueryable = geoEntryQueryable;
    }

    private static final PrintStream CONSOLE = System.out;

    @Override
    protected Object doExecute() {
        try {
            final List<GeoEntry> resultList = geoEntryQueryable.queryMultiple(query);
            if (resultList.size() == 0) {
                CONSOLE.println("No results.");
            }
            for (GeoEntry geoEntry : resultList) {
                CONSOLE.printf("%-30s | %-10s | %-10d | %-2.3f | %-2.3f\n", geoEntry.getName(), geoEntry.getFeatureCode(), geoEntry.getPopulation(), geoEntry.boost, geoEntry.score);
            }
        } catch (GeoEntryQueryException e) {
            LOGGER.error("Error querying GeoNames resource with query: {}", query, e);
            CONSOLE.printf("Could not query GeoNames resource with query: %s\n" + "Message: %s\n"
                    + "Check the logs for more details.\n", query, e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error("NPE", e);
        }

        return null;
    }
}
