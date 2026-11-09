package de.connect2x.trixnity.messenger.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixMediaUriTest {
    @Test
    fun acceptsMatrixServerNamesAndUrlSafeMediaIds() {
        for (server in
            listOf(
                "example.org",
                "EXAMPLE.org",
                "localhost",
                "example.org.",
                "example.org:8448",
                "127.0.0.1",
                "127.0.0.1:8448",
                "[::1]",
                "[1234:5678::abcd]:5678",
                "[2001:db8:0:0:0:0:0:1]",
                "[::ffff:192.0.2.1]",
            )) {
            assertTrue(isMatrixMediaUri("mxc://$server/Az09_-"), server)
        }
    }

    @Test
    fun rejectsNonMatrixSourcesAndInvalidUris() {
        for (uri in
            listOf(
                null,
                "https://example.org/image",
                "data:image/png;base64,abc",
                "mxc://",
                "mxc://example.org/",
                "mxc://example.org/a/b",
                "mxc://example.org/a?query",
                "mxc://example.org/a?",
                "mxc://example.org/a#",
                "mxc:example.org/image",
                "mxc:/example.org/image",
                "mxc://example.org:99999/image",
                "mxc://example.org/a#fragment",
                "mxc://example.org/a.b",
                "mxc://example.org/..",
                "mxc://example.org/%2F",
                "mxc://example.org/a+b",
                "mxc://example.org/a=b",
                "mxc://example.org/猫",
                "mxc://user@example.org/image",
                "mxc://example.org:port/image",
                "mxc://example.org:123456/image",
                "mxc:///image",
                "mxc://example.org/image\n",
            )) {
            assertFalse(isMatrixMediaUri(uri), uri)
        }
    }
}
