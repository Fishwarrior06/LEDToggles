# LEDToggles
Quick BLE control tile for the LED Lights that came with my Desk, built by reverse engineering the original mobile application.

## 🚀 Setup & Installation

To run this project on your own device, you will need **Android Studio** installed on your computer to compile the application, and a text editor to update the hardware configuration.

### 1. Clone and Open the Project
Clone this repository and open it using **Android Studio**.

### 2. Configure Your Device MAC Address
Since Bluetooth MAC addresses are unique to each physical hardware device, you must replace the hardcoded placeholder with your own LED controller's MAC address.

1. Open `app/src/main/java/com/fishwarrior06/ledtoggle/LedTileService.kt` using Android Studio or any text editor.
2. Locate the `DEVICE_MAC` constant inside the `companion object`:
   ```kotlin
   private const val DEVICE_MAC = "XX:XX:XX:XX:XX:XX" // <-- Replace with your BLE device MAC address