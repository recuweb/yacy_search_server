package net.yacy.cora.document.id;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Test;
//import junit.framework.TestCase;

public class MultiProtocolURLTest {

    @Test
    public void testSessionIdRemoval() throws MalformedURLException {
        String[][] testURIs = new String[][]{
            // meaning:  original uri, stripped version
            new String[]{"http://test.de/bla.php?phpsessionid=asdf", "http://test.de/bla.php"},
            new String[]{"http://test.de/bla?phpsessionid=asdf&fdsa=asdf", "http://test.de/bla?fdsa=asdf"},
            new String[]{"http://test.de/bla?asdf=fdsa&phpsessionid=asdf", "http://test.de/bla?asdf=fdsa"},
            new String[]{"http://test.de/bla?asdf=fdsa&phpsessionid=asdf&fdsa=asdf", "http://test.de/bla?asdf=fdsa&fdsa=asdf"},};
        TreeSet<String> idNames = new TreeSet<String>();
        idNames.add("phpsessionid");

        MultiProtocolURL.initSessionIDNames(idNames);

        for (int i = 0; i < testURIs.length; i++) {
            MultiProtocolURL uri = new MultiProtocolURL(testURIs[i][0]);

            assertEquals(uri.toNormalform(true, true), testURIs[i][1]);
        }
    }

    @Test
    public void testResolveBackpath() {
        String[][] testStrings = new String[][]{
            new String[]{"/..home", "/..home"},
            new String[]{"/test/..home/test.html", "/test/..home/test.html"},
            new String[]{"/../", "/../"},
            new String[]{"/..", "/.."},
            new String[]{"/test/..", "/"},
            new String[]{"/test/../", "/"},
            new String[]{"/test/test2/..", "/test"},
            new String[]{"/test/test2/../", "/test/"},
            new String[]{"/test/test2/../hallo", "/test/hallo"},
            new String[]{"/test/test2/../hallo/", "/test/hallo/"},
            new String[]{"/home/..test/../hallo/../", "/home/"}
        };
        String testhost = "http://localhost";
        for (int i = 0; i < testStrings.length; i++) {
            // desired conversion result
            System.out.print("testResolveBackpath: " + testStrings[i][0]);
            String shouldBe = testhost + testStrings[i][1];

            // conversion result
            String resolvedURL = "";
            try {
                resolvedURL = (new MultiProtocolURL(testhost + testStrings[i][0])).toNormalform(false);
            } catch (final MalformedURLException ex) {
                fail("malformed URL");
            }
            // test if equal
            assertEquals(shouldBe, resolvedURL);
            System.out.println(" -> " + resolvedURL);

        }
    }
        
    /**
     * Test of isLocal method, of class MultiProtocolURL. including IPv6 url
     * data
     *
     * @throws java.net.MalformedURLException
     */
    @Test
    public void testIsLocal() throws MalformedURLException {
        LinkedHashMap<String,Boolean> testurls = new LinkedHashMap<String,Boolean>(); // <url,expected_result>

        // valid IPv6 local loopback addresses
        testurls.put("http://[0:0:0:0:0:0:0:1]/index.html", Boolean.TRUE);
        testurls.put("http://[::1]/index.html", Boolean.TRUE);
        testurls.put("http://[::1]:8090/index.html", Boolean.TRUE);
        testurls.put("http://[0::1]/index.html", Boolean.TRUE);
        testurls.put("http://[::0:1]:80/index.html", Boolean.TRUE);

        testurls.put("http://[fc00:0:0:0:0:0:0:1]/index.html", Boolean.TRUE);
        testurls.put("http://[fc00::fa01:ff]/index.html", Boolean.TRUE);
        testurls.put("http://[fD00:0:0:0:0:0:0:1]/index.html", Boolean.TRUE);
        testurls.put("http://[fe80:0:0:0:0:0:0:1]/index.html", Boolean.TRUE);
        testurls.put("http://[fe80::1]/index.html", Boolean.TRUE);

        // test urls for possible issue with IPv6 misinterpretation
        testurls.put("http://fcedit.com", Boolean.FALSE);
        testurls.put("http://fdedit.com", Boolean.FALSE);
        testurls.put("http://fe8edit.com", Boolean.FALSE);

        // valid IPv6 examples taken from http://www.ietf.org/rfc/rfc2732.txt  cap 2.
        testurls.put("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html", Boolean.TRUE); // ip is link-local (fe80/10)
        testurls.put("http://[1080:0:0:0:8:800:200C:417A]/index.html", Boolean.FALSE);
        testurls.put("http://[3ffe:2a00:100:7031::1]", Boolean.FALSE);
        testurls.put("http://[1080::8:800:200C:417A]/foo", Boolean.FALSE);
        testurls.put("http://[::192.9.5.5]/ipng", Boolean.FALSE);
        testurls.put("http://[::FFFF:129.144.52.38]:80/index.html", Boolean.FALSE);
        testurls.put("http://[2010:836B:4179::836B:4179]", Boolean.FALSE);

        for (String u : testurls.keySet()) {

            MultiProtocolURL uri = new MultiProtocolURL(u);
            boolean result = uri.isLocal();

            System.out.println ("testIsLocal: " + u + " -> " + result);
            assertEquals(u, testurls.get(u), result);

        }
    }

    @Test
    public void testGetHost() throws MalformedURLException {
        String[][] testStrings = new String[][]{
            // teststring , expectedresult
            new String[]{"http://www.yacy.net", "www.yacy.net"},
            new String[]{"http://www.yacy.net:8090", "www.yacy.net"},
            new String[]{"http://www.yacy.net/test?query=test", "www.yacy.net"},
            new String[]{"http://www.yacy.net/?query=test", "www.yacy.net"},
            new String[]{"http://www.yacy.net?query=test", "www.yacy.net"},
            new String[]{"http://www.yacy.net:?query=test", "www.yacy.net"},
            new String[]{"//www.yacy.net:?query=test", "www.yacy.net"},
        };

        for (int i = 0; i < testStrings.length; i++) {
            // desired conversion result
            System.out.print("testGetHost: " + testStrings[i][0]);
            String shouldBe = testStrings[i][1];

            // conversion result
            String resolvedHost = new MultiProtocolURL(testStrings[i][0]).getHost();

            // test if equal
            assertEquals(shouldBe, resolvedHost);
            System.out.println(" -> " + resolvedHost);

        }
    }

    /**
     * Test of toNormalform method, of class MultiProtocolURL.
     */
    @Test
    public void testToNormalform() throws Exception {
        // some test url/uri with problems in the past
        String[][] testStrings = new String[][]{
            // teststring , expectedresult
            new String[]{"http://www.heise.de/newsticker/thema/%23saukontrovers", "http://www.heise.de/newsticker/thema/%23saukontrovers"}, // http://mantis.tokeek.de/view.php?id=519
            new String[]{"http://www.heise.de/newsticker/thema/#saukontrovers", "http://www.heise.de/newsticker/thema/"}, // anchor fragment
            new String[]{"http://www.liferay.com/community/wiki/-/wiki/Main/Wiki+Portlet", "http://www.liferay.com/community/wiki/-/wiki/Main/Wiki+Portlet"}, // http://mantis.tokeek.de/view.php?id=559
            new String[]{"http://de.wikipedia.org/wiki/Philippe_Ariès", "http://de.wikipedia.org/wiki/Philippe_Ari%C3%A8s"} // UTF-8 2 byte char
        };

        for (String[] testString : testStrings) {
            // desired conversion result
            System.out.print("toNormalform orig uri: " + testString[0]);
            String shouldBe = testString[1];
            // conversion result
            String resultUrl = new MultiProtocolURL(testString[0]).toNormalform(true);
            // test if equal
            assertEquals(shouldBe, resultUrl);
            System.out.println(" -> " + resultUrl);
        }
    }

    /**
     * Test of getAttribute method, of class MultiProtocolURL.
     */
    @Test
    public void testGetAttribute() throws Exception {
        // some test url/uri with problems in the past
        String[][] testStrings = new String[][]{
            // teststring , expectedresult
            new String[]{"http://yacy.net?&test", "test"}
        };

        for (String[] testString : testStrings) {
            // desired conversion result
            System.out.print("test getAttribute: " + testString[0]);
            String shouldBe = testString[1];

            MultiProtocolURL resultUrl = new MultiProtocolURL(testString[0]);
            Map<String, String> attr = resultUrl.getAttributes();

            assertEquals("", attr.get(shouldBe));
            System.out.println(" -> " + resultUrl.toNormalform(false));
        }
    }
}



