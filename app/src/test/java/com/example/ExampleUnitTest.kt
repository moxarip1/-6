package com.example

import com.example.data.FacebookGraphService
import com.example.model.FacebookConfig
import com.example.model.FacebookSyncMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun facebookGraphService_startsUnauthenticatedAndEmpty() = runTest {
        val service = FacebookGraphService()
        assertFalse(service.isAuthenticated)
        assertTrue(service.cachedCatalog.value.isEmpty())

        // Attempting to fetch catalog data before authenticating via Admin Dashboard must return empty
        val result = service.fetchJsonCatalogData(
            FacebookConfig(
                pageId = "104928194827162",
                accessToken = "some_unverified_token",
                syncMode = FacebookSyncMode.AUTO_VIDEO_DESCRIPTIONS
            )
        )
        assertFalse(result.isAuthenticated)
        assertFalse(result.success)
        assertTrue(result.catalog.isEmpty())
    }

    @Test
    fun validatePageAccessToken_rejectsBlankCredentialsAndKeepsCatalogEmpty() = runTest {
        val service = FacebookGraphService()
        val validation = service.validatePageAccessToken(pageId = "", accessToken = "")
        assertFalse(validation.isValid)
        assertFalse(service.isAuthenticated)
        assertEquals(0, service.cachedCatalog.value.size)
    }
}
