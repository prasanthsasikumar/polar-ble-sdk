package com.polar.androidcommunications.api.ble.model.advertisement

import com.polar.androidcommunications.common.ble.BleUtils
import com.polar.androidcommunications.testrules.BleLoggerTestRule
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BleAdvertisementContentTest {
    @Rule
    @JvmField
    val bleLoggerTestRule = BleLoggerTestRule()

    private lateinit var bleAdvertisementContent: BleAdvertisementContent

    @Before
    fun setUp() {
        bleAdvertisementContent = BleAdvertisementContent()
    }

    @Test
    fun `test parse name from complete local name`() {
        // Arrange
        val testInputString = "ABC EDE aa123459"
        val map = hashMapOf<BleUtils.AD_TYPE, ByteArray>()
        map[BleUtils.AD_TYPE.GAP_ADTYPE_LOCAL_NAME_COMPLETE] = testInputString.toByteArray()

        // Act
        val name = bleAdvertisementContent.getNameFromAdvData(map)
        bleAdvertisementContent.processName(name)

        // Assert
        Assert.assertEquals(testInputString, bleAdvertisementContent.name)
        Assert.assertTrue(bleAdvertisementContent.polarDeviceType.isEmpty())
        Assert.assertTrue(bleAdvertisementContent.polarDeviceId.isEmpty())
    }

    @Test
    fun `test parse name from complete local name when Polar device`() {
        // Arrange
        val testInputString = "Polar GritX Pro aa123459"
        val map = hashMapOf<BleUtils.AD_TYPE, ByteArray>()
        map[BleUtils.AD_TYPE.GAP_ADTYPE_LOCAL_NAME_COMPLETE] = testInputString.toByteArray()

        // Act
        val name = bleAdvertisementContent.getNameFromAdvData(map)
        bleAdvertisementContent.processName(name)

        // Assert
        Assert.assertEquals(testInputString, bleAdvertisementContent.name)
        Assert.assertEquals("GritX Pro", bleAdvertisementContent.polarDeviceType)
        Assert.assertEquals("aa123459", bleAdvertisementContent.polarDeviceId)
    }

    @Test
    fun `test parse name from short local name when Polar device`() {
        // Arrange
        val testInputString = "Polar GritX Pro aa123459"
        val map = hashMapOf<BleUtils.AD_TYPE, ByteArray>()
        map[BleUtils.AD_TYPE.GAP_ADTYPE_LOCAL_NAME_SHORT] = testInputString.toByteArray()

        // Act
        val name = bleAdvertisementContent.getNameFromAdvData(map)
        bleAdvertisementContent.processName(name)

        // Assert
        Assert.assertEquals(testInputString, bleAdvertisementContent.name)
        Assert.assertEquals("GritX Pro", bleAdvertisementContent.polarDeviceType)
        Assert.assertEquals("aa123459", bleAdvertisementContent.polarDeviceId)
    }

    @Test
    fun `test parse hr from manufacturer data without hr`() {
        // Arrange
        val onlyGpbManufacturerData = byteArrayOf(
            0x6b.toByte(), 0x00.toByte(),
            0x72.toByte(), 0x08.toByte(), 0x97.toByte(), 0xc9.toByte(), 0xc3.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        val onlyBpb: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to onlyGpbManufacturerData)

        // Act
        bleAdvertisementContent.processAdvManufacturerData(onlyBpb, bleAdvertisementContent.polarHrAdvertisement)

        // Assert
        Assert.assertNotNull(bleAdvertisementContent.polarHrAdvertisement)
        Assert.assertFalse(bleAdvertisementContent.polarHrAdvertisement.isPresent)
    }

    @Test
    fun `test parse hr from manufacturer data SAGRFC23 format`() {
        // Arrange
        val gpbAndHrManufacturerData = byteArrayOf(
            0x6b.toByte(), 0x00.toByte(),
            0x72.toByte(), 0x08.toByte(), 0x97.toByte(), 0xc9.toByte(), 0xc3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7a.toByte(), 0x01.toByte(), 0x03.toByte(), 0x33.toByte(),
            0x00.toByte(), 0x00.toByte()
        )
        val gpbAndHr: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to gpbAndHrManufacturerData)

        // Act && Assert
        bleAdvertisementContent.processAdvManufacturerData(gpbAndHr, bleAdvertisementContent.polarHrAdvertisement)
        Assert.assertNotNull(bleAdvertisementContent.polarHrAdvertisement)
        Assert.assertTrue(bleAdvertisementContent.polarHrAdvertisement.isPresent)
    }

    @Test
    fun `test parse hr from manufacturer data SAGRFC31 format`() {
        // Arrange
        val onlyHrManufacturerData = byteArrayOf(0x6b.toByte(), 0x00.toByte(), 0x2b.toByte(), 0x0b.toByte(), 0xb6.toByte(), 0xac.toByte())
        val onlyHr: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to onlyHrManufacturerData)

        // Act
        bleAdvertisementContent.processAdvManufacturerData(onlyHr, bleAdvertisementContent.polarHrAdvertisement)

        // Assert
        Assert.assertNotNull(bleAdvertisementContent.polarHrAdvertisement)
        Assert.assertTrue(bleAdvertisementContent.polarHrAdvertisement.isPresent)
    }

    @Test
    fun `test parse hr from manufacturer data not Polar adv`() {
        // Arrange
        val nonPolarManufacturerData = byteArrayOf(
            0x6b.toByte(), 0x01.toByte(), // 0x006B is Polar manufacturer Id, 0x016B is not Polar
            0x72.toByte(), 0x08.toByte(), 0x97.toByte(), 0xc9.toByte(), 0xc3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7a.toByte(), 0x01.toByte(), 0x03.toByte(), 0x33.toByte(),
            0x00.toByte(), 0x00.toByte()
        )
        val nonPolar: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to nonPolarManufacturerData)

        // Act
        bleAdvertisementContent.processAdvManufacturerData(nonPolar, bleAdvertisementContent.polarHrAdvertisement)

        // Assert
        Assert.assertNotNull(bleAdvertisementContent.polarHrAdvertisement)
        Assert.assertFalse(bleAdvertisementContent.polarHrAdvertisement.isPresent)
    }

    @Test
    fun `test processing of consecutive adv packets`() {
        // Arrange
        val gpbAndHrManufacturerData = byteArrayOf(
            0x6b.toByte(), 0x00.toByte(),
            0x72.toByte(), 0x08.toByte(), 0x97.toByte(), 0xc9.toByte(), 0xc3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7a.toByte(), 0x01.toByte(), 0x03.toByte(), 0x33.toByte(),
            0x00.toByte(), 0x00.toByte()
        )
        val onlyGpbManufacturerData = byteArrayOf(
            0x6b.toByte(), 0x00.toByte(), 0x72.toByte(), 0x08.toByte(), 0x97.toByte(), 0xc9.toByte(), 0xc3.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        val emptyManufacturerData = byteArrayOf()

        val gbpAndHr: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to gpbAndHrManufacturerData)
        val onlyBpb: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to onlyGpbManufacturerData)
        val emptyManufacturer: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_MANUFACTURER_SPECIFIC to emptyManufacturerData)

        // Act & Assert
        bleAdvertisementContent.processAdvertisementData(gbpAndHr, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertEquals(true, bleAdvertisementContent.polarHrAdvertisement.isPresent)
        bleAdvertisementContent.processAdvertisementData(onlyBpb, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertEquals(false, bleAdvertisementContent.polarHrAdvertisement.isPresent)
        bleAdvertisementContent.processAdvertisementData(gbpAndHr, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertEquals(true, bleAdvertisementContent.polarHrAdvertisement.isPresent)
        bleAdvertisementContent.processAdvertisementData(emptyManufacturer, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertEquals(false, bleAdvertisementContent.polarHrAdvertisement.isPresent)
    }

    @Test
    fun `test process RSSI when less than 7 RSSI values arrived`() {
        // Arrange
        val rssiValues = listOf(0, 1, 2, 3, 4, 99)

        // Act
        for (rssi in rssiValues) {
            bleAdvertisementContent.processRssi(rssi)
        }

        // Assert
        Assert.assertEquals(rssiValues.last(), bleAdvertisementContent.rssi)
        Assert.assertEquals(rssiValues.last(), bleAdvertisementContent.medianRssi)
    }

    @Test
    fun `test process RSSI when more than 7 RSSI values arrived`() {
        // Arrange
        val rssiValues = listOf(10, 21, 2, 5, 9, 0, 8, 1, 8, 2)
        val median = 5 // 0, 1, 2, !5!, 8, 8, 9,

        // Act
        for (rssi in rssiValues) {
            bleAdvertisementContent.processRssi(rssi)
        }
        // Assert
        Assert.assertEquals(rssiValues.last(), bleAdvertisementContent.rssi)
        Assert.assertEquals(median, bleAdvertisementContent.medianRssi)
    }

    @Test
    fun `test adv contains service`() {
        // Arrange
        val services = byteArrayOf(0x0d.toByte(), 0x18.toByte(), 0xee.toByte(), 0xfe.toByte())
        val servicesAdvData: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_16BIT_MORE to services)
        val emptyServicesAdvData: HashMap<BleUtils.AD_TYPE, ByteArray> = hashMapOf(BleUtils.AD_TYPE.GAP_ADTYPE_16BIT_MORE to byteArrayOf())

        // Act & Assert
        bleAdvertisementContent.processAdvertisementData(servicesAdvData, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertTrue(bleAdvertisementContent.containsService("FEEE"))
        Assert.assertTrue(bleAdvertisementContent.containsService("180D"))
        bleAdvertisementContent.processAdvertisementData(emptyServicesAdvData, BleUtils.EVENT_TYPE.ADV_IND, 0)
        Assert.assertFalse(bleAdvertisementContent.containsService("FEEE"))
        Assert.assertFalse(bleAdvertisementContent.containsService("180D"))
    }
}