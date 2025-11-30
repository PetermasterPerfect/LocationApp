package edu.ppsm.locationtracer

// A temporary class for holding devices in memory. Will be later replaced by communication with DB

data class Device(val name: String, val location: String, val sysName: String)
object DeviceRepository {
    val devices = mutableListOf<Device>()

    fun addDevice(device: Device) {
        devices.add(device)
    }
}