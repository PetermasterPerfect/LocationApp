package edu.ppsm.locationtracer


import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AddDeviceActivityTest {

    @Test
    fun uuidGeneration_isDeterministic() {
        val fingerprint = "test_fingerprint"

        val uuid1 = UUID.nameUUIDFromBytes(fingerprint.toByteArray()).toString()
        val uuid2 = UUID.nameUUIDFromBytes(fingerprint.toByteArray()).toString()

        assertThat(uuid1).isEqualTo(uuid2)
    }

    @Test
    fun uuidGeneration_isDifferentForDifferentFingerprints() {
        val uuid1 = UUID.nameUUIDFromBytes("fp1".toByteArray()).toString()
        val uuid2 = UUID.nameUUIDFromBytes("fp2".toByteArray()).toString()

        assertThat(uuid1).isNotEqualTo(uuid2)
    }
}
