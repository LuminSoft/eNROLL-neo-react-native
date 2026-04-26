# eNROLL Neo React Native Plugin

React Native plugin for the **eNROLL Neo SDK** — lightweight eKYC identity verification for React Native apps on Android and iOS.

eNROLL is a lightweight compliance solution that prevents identity fraud and phishing. Powered by AI, it reduces errors and speeds up identification, ensuring secure verification.

> Supports both the **Old Architecture** (Bridge) and **New Architecture** (TurboModules).
>
> Full feature parity with the [Capacitor Neo plugin](https://github.com/LuminSoft/enroll-capacitor-neo) and the [Flutter Neo plugin](https://github.com/LuminSoft/eNROLL-Neo).

Current native SDK versions:
- **Android:** eNROLL-Lite-Android v1.2.6 (via JitPack)
- **iOS:** EnrollFramework xcframework + EnrollNeoCore 1.0.13 (via CocoaPods)

> This is the **Neo / Lumin Light** variant of the eNROLL SDK. For the standard eNROLL SDK with full theme and icon customization, see [enroll-react-native](https://www.npmjs.com/package/enroll-react-native).

## Requirements

| Platform | Minimum |
|----------|---------|
| React Native | >= 0.71.0 |
| Android minSdk | 24 |
| Android compileSdk | 34 |
| iOS deployment target | 15.5 |
| Kotlin | 2.0+ |
| Swift | 5.0 |
| Node.js | 18+ |

## Installation

```bash
npm install enroll-neo-react-native
# or
yarn add enroll-neo-react-native
```

### Android Setup

#### 1. Add JitPack Repository

Add the JitPack repository to your **project-level** `android/build.gradle` (or `android/settings.gradle` for newer Gradle / RN 0.76+):

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Verify minSdkVersion

Ensure `minSdkVersion` is at least **24** in your project-level `android/build.gradle`:

```gradle
ext {
    minSdkVersion = 24
    kotlinVersion = "2.0.21"
}
```

### iOS Setup

#### 1. Configure Podfile

Add the required pod sources and enable frameworks at the **top** of your `ios/Podfile`:

```ruby
source 'https://github.com/AcuantMobileSDK/AcuantPodSpecs.git'
source 'https://github.com/nicklama/enroll-podspecs.git'
source 'https://cdn.cocoapods.org/'

platform :ios, '15.5'

use_frameworks!
use_modular_headers!
```

#### 2. Add Info.plist Permissions

Add to `ios/YourApp/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture your ID and face for verification</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location for security compliance</string>
```

#### 3. Install Pods

```bash
cd ios && pod install
```

> **Note:** iOS builds require a **physical device**. The EnrollFramework does not include a simulator architecture.

### ePassport / NFC (Optional — iOS only)

If you need electronic passport NFC reading, add to `Info.plist`:

```xml
<key>com.apple.developer.nfc.readersession.felica.systemcodes</key>
<array><string>A0000002471001</string></array>
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array><string>A0000002471001</string></array>
<key>NFCReaderUsageDescription</key>
<string>We need NFC access to read your electronic passport</string>
```

Then enable **Near Field Communication Tag Reading** in Xcode → Target → Signing & Capabilities.

---

## Usage

### Basic Example

```typescript
import {
  startEnroll,
  addRequestIdListener,
  type StartEnrollOptions,
} from 'enroll-neo-react-native';

// Listen for request ID events (fires mid-flow)
const subscription = addRequestIdListener((event) => {
  console.log('Request ID:', event.requestId);
});

try {
  const result = await startEnroll({
    tenantId: 'YOUR_TENANT_ID',
    tenantSecret: 'YOUR_TENANT_SECRET',
    enrollMode: 'onboarding',
    enrollEnvironment: 'staging',
    localizationCode: 'en',
    skipTutorial: false,
  });

  console.log('Success! Applicant ID:', result.applicantId);
  console.log('Exit step completed:', result.exitStepCompleted);
} catch (error) {
  console.error('Enrollment failed:', error);
} finally {
  subscription.remove();
}
```

### Authentication Mode

```typescript
const result = await startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'auth',
  applicantId: 'APPLICANT_ID',
  levelOfTrust: 'LEVEL_OF_TRUST_TOKEN',
});
```

### Sign Contract Mode

```typescript
const result = await startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'signContract',
  templateId: '12345',
  contractParameters: '{"key": "value"}',
});
```

### Forget Profile Data Mode

```typescript
const result = await startEnroll({
  tenantId: 'YOUR_TENANT_ID',
  tenantSecret: 'YOUR_TENANT_SECRET',
  enrollMode: 'forgetProfileData',
});
```

---

## Enroll Modes

| Mode | Description | Required Params |
|------|-------------|-----------------|
| `onboarding` | Register a new user | `tenantId`, `tenantSecret` |
| `auth` | Authenticate existing user | + `applicantId`, `levelOfTrust` |
| `update` | Re-verify / update user | + `applicantId` |
| `signContract` | Sign contract templates | + `templateId` |
| `forgetProfileData` | Request profile data deletion | `tenantId`, `tenantSecret` |

## Configuration Options

| Key | Type | Required | Default | Description |
|-----|------|----------|---------|-------------|
| `tenantId` | `string` | ✅ | — | Organization tenant ID |
| `tenantSecret` | `string` | ✅ | — | Organization tenant secret |
| `enrollMode` | `EnrollMode` | ✅ | — | SDK flow mode |
| `applicantId` | `string` | mode-dep | — | Applicant ID (required for `auth`, `update`) |
| `levelOfTrust` | `string` | mode-dep | — | Level-of-trust token (required for `auth`) |
| `templateId` | `string` | mode-dep | — | Contract template ID (required for `signContract`) |
| `enrollEnvironment` | `EnrollEnvironment` | | `'staging'` | Target environment |
| `localizationCode` | `EnrollLocalization` | | `'en'` | UI language |
| `googleApiKey` | `string` | | — | Google Maps API key for location step |
| `skipTutorial` | `boolean` | | `false` | Skip the tutorial screen |
| `correlationId` | `string` | | — | Link your user ID with eNROLL request ID |
| `requestId` | `string` | | — | Resume a previous enrollment request |
| `contractParameters` | `string` | | — | JSON string of contract parameters |
| `enrollColors` | `EnrollColors` | | — | Custom UI color overrides |
| `enrollForcedDocumentType` | `EnrollForcedDocumentType` | | — | Force specific document type |
| `enrollExitStep` | `EnrollStepType` | | — | Auto-close SDK after this step |

## Success Result

| Field | Type | Description |
|-------|------|-------------|
| `applicantId` | `string` | Assigned applicant ID |
| `enrollMessage` | `string?` | Human-readable success message |
| `documentId` | `string?` | Document ID (if applicable) |
| `requestId` | `string?` | Request ID for resuming later |
| `exitStepCompleted` | `boolean` | `true` if flow ended early via `enrollExitStep` |
| `completedStepName` | `string?` | Name of the completed exit step |

## Custom Colors

```typescript
await startEnroll({
  // ...required params...
  enrollColors: {
    primary: { r: 29, g: 86, b: 184, opacity: 1.0 },
    secondary: { r: 87, g: 145, b: 219 },
    appBackgroundColor: { r: 255, g: 255, b: 255 },
    textColor: { r: 0, g: 65, b: 148 },
    errorColor: { r: 219, g: 48, b: 91 },
    successColor: { r: 97, g: 204, b: 61 },
    warningColor: { r: 249, g: 213, b: 72 },
  },
});
```

## Enrollment Step Types

Used with `enrollExitStep` to terminate the flow after a specific step:

`phoneOtp` · `personalConfirmation` · `smileLiveness` · `emailOtp` · `saveMobileDevice` · `deviceLocation` · `password` · `securityQuestions` · `amlCheck` · `termsAndConditions` · `electronicSignature` · `ntraCheck` · `csoCheck`

---

## Security Notes

- **Never hardcode** `tenantSecret`, `levelOfTrust`, or API keys in client-side code.
- Use secure storage (Keychain on iOS, Keystore on Android).
- Rooted/jailbroken devices are blocked by default.
- All SDK network calls use HTTPS.
- Regularly update the plugin to the latest stable version.

## License

MIT
