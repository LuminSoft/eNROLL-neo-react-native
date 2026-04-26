# iOS Integration Guide

## Prerequisites

- React Native >= 0.71.0
- Xcode 15+ (latest stable recommended)
- iOS deployment target 15.5+
- CocoaPods installed

## Step 1: Install the Plugin

```bash
npm install enroll-neo-react-native
```

## Step 2: Configure CocoaPods Sources

At the **top** of your `ios/Podfile` (before any other lines), add the required pod sources:

```ruby
source 'https://github.com/AcuantMobileSDK/AcuantPodSpecs.git'
source 'https://github.com/nicklama/enroll-podspecs.git'
source 'https://cdn.cocoapods.org/'
```

## Step 3: Set Deployment Target and Enable Frameworks

In your `ios/Podfile`:

```ruby
platform :ios, '15.5'

use_frameworks!
use_modular_headers!
```

## Step 4: Info.plist Permissions

Add the following keys to `ios/YourApp/Info.plist`:

```xml
<!-- Camera for document scanning and face matching -->
<key>NSCameraUsageDescription</key>
<string>Camera access is required for identity verification</string>

<!-- Location for device location step -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>Location is required for identity verification</string>

<!-- NFC for e-passport reading (optional) -->
<key>NFCReaderUsageDescription</key>
<string>NFC is used to read passport chips</string>
```

### NFC Entitlement (if using e-passport)

1. In Xcode, go to **Signing & Capabilities**
2. Click **+ Capability** and add **Near Field Communication Tag Reading**
3. In your `Info.plist`, add:

```xml
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array>
    <string>A0000002471001</string>
    <string>A0000002472001</string>
</array>
```

## Step 5: Install Pods

```bash
cd ios && pod install
```

## Step 6: Build

```bash
npx react-native run-ios
```

Or open `ios/YourApp.xcworkspace` in Xcode and build from there.

## Troubleshooting

### Pod install fails with "Unable to find a specification for EnrollNeoCore"

Ensure the pod sources are at the **top** of your Podfile:

```ruby
source 'https://github.com/AcuantMobileSDK/AcuantPodSpecs.git'
source 'https://github.com/nicklama/enroll-podspecs.git'
source 'https://cdn.cocoapods.org/'
```

### Simulator build fails (arm64 / i386)

The SDK uses an XCFramework that supports real devices. Building for simulators may require:

```ruby
# In Podfile, post_install:
installer.pods_project.targets.each do |target|
  target.build_configurations.each do |config|
    config.build_settings['EXCLUDED_ARCHS[sdk=iphonesimulator*]'] = 'i386'
  end
end
```
