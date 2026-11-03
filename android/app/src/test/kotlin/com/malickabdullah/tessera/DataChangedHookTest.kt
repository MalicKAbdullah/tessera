package com.malickabdullah.tessera

import com.malickabdullah.tessera.engine.Engine
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DataChangedHookTest {
    private val app: () -> Unit = {}
    private val configure: () -> Unit = {}

    @After
    fun clear() {
        Engine.releaseDataChanged(app)
        Engine.releaseDataChanged(configure)
    }

    @Test
    fun closingTheConfigureScreenLeavesTheAppHookAfterItResumes() {
        Engine.claimDataChanged(app)
        Engine.claimDataChanged(configure)
        Engine.releaseDataChanged(configure)
        Engine.claimDataChanged(app)
        assertSame(app, Engine.onDataChanged)
    }

    @Test
    fun aWindowReleasesOnlyItsOwnHook() {
        Engine.claimDataChanged(configure)
        Engine.releaseDataChanged(app)
        assertSame(configure, Engine.onDataChanged)
        Engine.releaseDataChanged(configure)
        assertNull(Engine.onDataChanged)
    }
}
