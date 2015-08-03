/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoding.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DirectoryReader.class, IndexReader.class})
public class TestGeoNamesQueryLuceneIndex extends TestBase {
    private Directory directory;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private TopDocs topDocs;
    private GeoNamesQueryLuceneDirectoryIndex directoryIndex;

    private static final String NAME_1 = "Phoenix";
    private static final String NAME_2 = "Tempe";
    private static final String NAME_3 = "Glendale";

    private static final double LAT_1 = 1.234;
    private static final double LAT_2 = -12.34;
    private static final double LAT_3 = -1.234;

    private static final double LON_1 = 56.78;
    private static final double LON_2 = 5.678;
    private static final double LON_3 = -5.678;

    private static final String FEATURE_CODE_1 = "PPL";
    private static final String FEATURE_CODE_2 = "ADM";
    private static final String FEATURE_CODE_3 = "PCL";

    private static final long POP_1 = 1000000;
    private static final long POP_2 = 10000000;
    private static final long POP_3 = 100000000;

    private static final String ALT_NAMES_1 = "alt1,alt2";
    private static final String ALT_NAMES_2 = "alt3";
    private static final String ALT_NAMES_3 = "";

    private static final GeoEntry GEO_ENTRY_1 = new GeoEntry.Builder()
            .name(NAME_1)
            .latitude(LAT_1)
            .longitude(LON_1)
            .featureCode(FEATURE_CODE_1)
            .population(POP_1)
            .alternateNames(ALT_NAMES_1)
            .build();

    private static final GeoEntry GEO_ENTRY_2 = new GeoEntry.Builder()
            .name(NAME_2)
            .latitude(LAT_2)
            .longitude(LON_2)
            .featureCode(FEATURE_CODE_2)
            .population(POP_2)
            .alternateNames(ALT_NAMES_2)
            .build();

    private static final GeoEntry GEO_ENTRY_3 = new GeoEntry.Builder()
            .name(NAME_3)
            .latitude(LAT_3)
            .longitude(LON_3)
            .featureCode(FEATURE_CODE_3)
            .population(POP_3)
            .alternateNames(ALT_NAMES_3)
            .build();

    @Before
    public void setUp() throws IOException {
        directory = mock(Directory.class);
        // IndexReader's document() method is final, and we need to mock it.
        indexReader = PowerMockito.mock(IndexReader.class);
        indexSearcher = mock(IndexSearcher.class);
        topDocs = mock(TopDocs.class);
        directoryIndex = spy(new GeoNamesQueryLuceneDirectoryIndex());

        doReturn(directory).when(directoryIndex).createDirectory();
        doReturn(indexReader).when(directoryIndex).createIndexReader(directory);
        doReturn(indexSearcher).when(directoryIndex).createIndexSearcher(indexReader);

        mockStatic(DirectoryReader.class);
        when(DirectoryReader.indexExists(directory)).thenReturn(true);
    }

    private Document createDocumentFromGeoEntry(final GeoEntry geoEntry) {
        final Document document = new Document();

        document.add(new StringField("name", geoEntry.getName(), Field.Store.NO));
        document.add(new DoubleField("latitude", geoEntry.getLatitude(), Field.Store.NO));
        document.add(new DoubleField("longitude", geoEntry.getLongitude(), Field.Store.NO));
        document.add(new StringField("feature_code", geoEntry.getFeatureCode(), Field.Store.NO));
        document.add(new LongField("population", geoEntry.getPopulation(), Field.Store.NO));
        document.add(new StringField("alternate_names", geoEntry.getAlternateNames(),
                Field.Store.NO));

        return document;
    }

    private void setUpTopDocs(final int numResults) {
        topDocs.totalHits = numResults;
        topDocs.scoreDocs = new ScoreDoc[numResults];

        for (int i = 0; i < numResults; ++i) {
            topDocs.scoreDocs[i] = mock(ScoreDoc.class);
            topDocs.scoreDocs[i].doc = i;
        }
    }

    @Test
    public void testQueryWithExactlyMaxResults() throws IOException {
        final int requestedMaxResults = 2;

        setUpTopDocs(requestedMaxResults);

        doReturn(topDocs).when(indexSearcher).search(any(Query.class), eq(requestedMaxResults));

        doReturn(createDocumentFromGeoEntry(GEO_ENTRY_1)).when(indexReader).document(0);
        doReturn(createDocumentFromGeoEntry(GEO_ENTRY_2)).when(indexReader).document(1);

        final List<GeoEntry> results = directoryIndex.query("phoenix", requestedMaxResults);
        assertThat(results.size(), is(requestedMaxResults));

        final GeoEntry firstResult = results.get(0);
        // We don't store the alternate names, so we don't get them back with the query results.
        verifyGeoEntry(firstResult, NAME_1, LAT_1, LON_1, FEATURE_CODE_1, POP_1, null);

        final GeoEntry secondResult = results.get(1);
        // We don't store the alternate names, so we don't get them back with the query results.
        verifyGeoEntry(secondResult, NAME_2, LAT_2, LON_2, FEATURE_CODE_2, POP_2, null);
    }

    @Test
    public void testQueryWithLessThanMaxResults() throws IOException {
        final int requestedMaxResults = 2;
        final int actualResults = 1;

        setUpTopDocs(actualResults);

        doReturn(topDocs).when(indexSearcher).search(any(Query.class), eq(requestedMaxResults));

        doReturn(createDocumentFromGeoEntry(GEO_ENTRY_3)).when(indexReader).document(0);

        final List<GeoEntry> results = directoryIndex.query("phoenix", requestedMaxResults);
        assertThat(results.size(), is(actualResults));

        final GeoEntry firstResult = results.get(0);
        // We don't store the alternate names, so we don't get them back with the query results.
        verifyGeoEntry(firstResult, NAME_3, LAT_3, LON_3, FEATURE_CODE_3, POP_3, null);
    }

    @Test
    public void testQueryWithNoResults() throws IOException {
        final int requestedMaxResults = 2;
        final int actualResults = 0;

        topDocs.totalHits = actualResults;

        doReturn(topDocs).when(indexSearcher).search(any(Query.class), eq(requestedMaxResults));

        final List<GeoEntry> results = directoryIndex.query("phoenix", requestedMaxResults);
        assertThat(results.size(), is(actualResults));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankQuery() {
        directoryIndex.query("", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullQuery() {
        directoryIndex.query(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroMaxResults() {
        directoryIndex.query("phoenix", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxResults() {
        directoryIndex.query("phoenix", -1);
    }

    @Test(expected = GeoEntryQueryException.class)
    public void testNoExistingIndex() throws IOException {
        when(DirectoryReader.indexExists(directory)).thenReturn(false);
        directoryIndex.query("phoenix", 1);
    }

    @Test
    public void testExceptionInDirectoryCreation() throws IOException {
        doThrow(IOException.class).when(directoryIndex).createDirectory();

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because an IOException was thrown " +
                    " when creating the directory.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by an IOException.",
                    e.getCause(), instanceOf(IOException.class));
        }
    }

    @Test
    public void testExceptionInIndexReaderCreation() throws IOException {
        doThrow(IOException.class).when(directoryIndex).createIndexReader(any(Directory.class));

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because an IOException was thrown " +
                    "when creating the IndexReader.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by an IOException.",
                    e.getCause(), instanceOf(IOException.class));
        }
    }

    @Test
    public void testExceptionInQueryParsing() throws ParseException {
        doThrow(ParseException.class).when(directoryIndex).createQuery(anyString());

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because a ParseException was " +
                    "thrown when creating the Query.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by a ParseException.",
                    e.getCause(), instanceOf(ParseException.class));
        }
    }
}
