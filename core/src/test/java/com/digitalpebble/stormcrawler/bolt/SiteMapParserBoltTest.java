/**
 * Licensed to DigitalPebble Ltd under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpebble.stormcrawler.bolt;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.storm.task.OutputCollector;

import com.digitalpebble.stormcrawler.Constants;
import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.TestUtil;
import com.digitalpebble.stormcrawler.parse.ParsingTester;
import com.digitalpebble.stormcrawler.protocol.HttpHeaders;

public class SiteMapParserBoltTest extends ParsingTester {

    @Before
    public void setupParserBolt() {
        bolt = new SiteMapParserBolt();
        setupParserBolt(bolt);
    }

    // TODO add a test for a sitemap containing links
    // to other sitemap files

    @Test
    public void testSitemapParsing() throws IOException {

        prepareParserBolt("test.parsefilters.json");

        Metadata metadata = new Metadata();
        // specify that it is a sitemap file
        metadata.setValue(SiteMapParserBolt.isSitemapKey, "true");
        // and its mime-type
        metadata.setValue(HttpHeaders.CONTENT_TYPE, "application/xml");

        parse("http://www.digitalpebble.com/sitemap.xml",
                "digitalpebble.sitemap.xml", metadata);

        Assert.assertEquals(6, output.getEmitted(Constants.StatusStreamName)
                .size());
        // TODO test that the new links have the right metadata
        List<Object> fields = output.getEmitted(Constants.StatusStreamName)
                .get(0);
        Assert.assertEquals(3, fields.size());
    }

    @Test
    public void testSitemapParsingWithImageExtensions() throws IOException {
        Map parserConfig = new HashMap();
        parserConfig.put("sitemap.extensions", "IMAGE");
        prepareParserBolt("test.parsefilters.json", parserConfig);

        Metadata metadata = new Metadata();
        // specify that it is a sitemap file
        metadata.setValue(SiteMapParserBolt.isSitemapKey, "true");
        // and its mime-type
        metadata.setValue(HttpHeaders.CONTENT_TYPE, "application/xml");

        parse("http://www.digitalpebble.com/sitemap.xml",
                "digitalpebble.sitemap.extensions.image.xml", metadata);
        Values values = (Values) output.getEmitted(Constants.StatusStreamName).get(0);
        Metadata parsedMetadata = (Metadata) values.get(1);
        long numImgAttributes = parsedMetadata.keySet()
                .stream()
                .filter(key -> key.startsWith("IMAGE."))
                .count();
        Assert.assertEquals(5, numImgAttributes);
        Assert.assertEquals("This is the caption.", parsedMetadata.getFirstValue("IMAGE.caption"));
        Assert.assertEquals("http://example.com/photo.jpg", parsedMetadata.getFirstValue("IMAGE.loc"));
        Assert.assertEquals("Example photo shot in Limerick, Ireland", parsedMetadata.getFirstValue("IMAGE.title"));
        Assert.assertEquals("https://creativecommons.org/licenses/by/4.0/legalcode", parsedMetadata.getFirstValue("IMAGE.license"));
        Assert.assertEquals("Limerick, Ireland", parsedMetadata.getFirstValue("IMAGE.geo_location"));
    }

    @Test
    public void testSitemapParsingWithMobileExtensions() throws IOException {
        Map parserConfig = new HashMap();
        parserConfig.put("sitemap.extensions", "MOBILE");
        prepareParserBolt("test.parsefilters.json", parserConfig);

        Metadata metadata = new Metadata();
        // specify that it is a sitemap file
        metadata.setValue(SiteMapParserBolt.isSitemapKey, "true");
        // and its mime-type
        metadata.setValue(HttpHeaders.CONTENT_TYPE, "application/xml");

        parse("http://www.digitalpebble.com/sitemap.xml",
                "digitalpebble.sitemap.extensions.mobile.xml", metadata);
        Values values = (Values) output.getEmitted(Constants.StatusStreamName).get(0);
        Metadata parsedMetadata = (Metadata) values.get(1);
        long numImgAttributes = parsedMetadata.keySet()
                .stream()
                .filter(key -> key.startsWith("MOBILE."))
                .count();
        Assert.assertEquals(0, numImgAttributes);
    }

    @Test
    public void testSitemapParsingWithLinkExtensions() throws IOException {
        Map parserConfig = new HashMap();
        parserConfig.put("sitemap.extensions", "LINKS");
        prepareParserBolt("test.parsefilters.json", parserConfig);

        Metadata metadata = new Metadata();
        // specify that it is a sitemap file
        metadata.setValue(SiteMapParserBolt.isSitemapKey, "true");
        // and its mime-type
        metadata.setValue(HttpHeaders.CONTENT_TYPE, "application/xml");

        parse("http://www.digitalpebble.com/sitemap.xml",
                "digitalpebble.sitemap.extensions.links.xml", metadata);
        Values values = (Values) output.getEmitted(Constants.StatusStreamName).get(0);
        Metadata parsedMetadata = (Metadata) values.get(1);
        long numImgAttributes = parsedMetadata.keySet()
                .stream()
                .filter(key -> key.startsWith("LINKS."))
                .count();
        Assert.assertEquals(3, numImgAttributes);
        Assert.assertEquals("alternate", parsedMetadata.getFirstValue("LINKS.params.rel"));
        Assert.assertEquals("http://www.example.com/english/", parsedMetadata.getFirstValue("LINKS.href"));
        Assert.assertEquals("en", parsedMetadata.getFirstValue("LINKS.params.hreflang"));
    }

    @Test
    public void testSitemapParsingNoMT() throws IOException {

        Map parserConfig = new HashMap();
        parserConfig.put("sitemap.sniffContent", true);
        parserConfig.put("parsefilters.config.file", "test.parsefilters.json");
        bolt.prepare(parserConfig, TestUtil.getMockedTopologyContext(),
                new OutputCollector(output));

        Metadata metadata = new Metadata();
        // do not specify that it is a sitemap file
        // do not set the mimetype

        parse("http://www.digitalpebble.com/sitemap.xml",
                "digitalpebble.sitemap.xml", metadata);

        Assert.assertEquals(6, output.getEmitted(Constants.StatusStreamName)
                .size());
        // TODO test that the new links have the right metadata
        List<Object> fields = output.getEmitted(Constants.StatusStreamName)
                .get(0);
        Assert.assertEquals(3, fields.size());
    }

    @Test
    public void testNonSitemapParsing() throws IOException {

        prepareParserBolt("test.parsefilters.json");
        // do not specify that it is a sitemap file
        parse("http://www.digitalpebble.com", "digitalpebble.com.html",
                new Metadata());

        Assert.assertEquals(1, output.getEmitted().size());
    }

}
