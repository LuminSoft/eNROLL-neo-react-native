// ---------------------------------------------------------------------------
// Enums (string literal unions — idiomatic TypeScript, no runtime overhead)
// ---------------------------------------------------------------------------

/**
 * The environment in which the eNROLL SDK operates.
 * - `'staging'`    — test / QA environment
 * - `'production'` — live environment
 */
export type EnrollEnvironment = 'staging' | 'production';

/**
 * The mode of the enrollment flow.
 * - `'onboarding'`        — register a new user
 * - `'auth'`              — authenticate an existing user (requires `applicantId` + `levelOfTrust`)
 * - `'update'`            — re-verify / update an existing user
 * - `'signContract'`      — sign a contract template (requires `templateId`)
 * - `'forgetProfileData'` — request deletion of a user's profile data
 */
export type EnrollMode =
  | 'onboarding'
  | 'auth'
  | 'update'
  | 'signContract'
  | 'forgetProfileData';

/**
 * UI language for the enrollment flow.
 * - `'en'` — English (default)
 * - `'ar'` — Arabic (enables RTL layout)
 */
export type EnrollLocalization = 'en' | 'ar';

/**
 * Forces the document scanning step to accept only a specific document type.
 * - `'nationalIdOnly'`        — only national ID
 * - `'passportOnly'`          — only passport
 * - `'nationalIdOrPassport'`  — user chooses (default)
 */
export type EnrollForcedDocumentType =
  | 'nationalIdOnly'
  | 'passportOnly'
  | 'nationalIdOrPassport';

/**
 * Individual enrollment step identifiers.
 * Used with `enrollExitStep` to terminate the flow after a specific step.
 */
export type EnrollStepType =
  | 'phoneOtp'
  | 'personalConfirmation'
  | 'smileLiveness'
  | 'emailOtp'
  | 'saveMobileDevice'
  | 'deviceLocation'
  | 'password'
  | 'securityQuestions'
  | 'amlCheck'
  | 'termsAndConditions'
  | 'electronicSignature'
  | 'ntraCheck'
  | 'csoCheck';

// ---------------------------------------------------------------------------
// Color types
// ---------------------------------------------------------------------------

/**
 * An RGBA color value.
 * `r`, `g`, `b` are integers 0–255. `opacity` is a float 0.0–1.0 (defaults to 1.0).
 */
export interface EnrollColor {
  r: number;
  g: number;
  b: number;
  opacity?: number;
}

/**
 * Custom color overrides for the enrollment UI.
 * Every property is optional — omitted colors fall back to the SDK defaults.
 */
export interface EnrollColors {
  primary?: EnrollColor;
  secondary?: EnrollColor;
  appBackgroundColor?: EnrollColor;
  textColor?: EnrollColor;
  errorColor?: EnrollColor;
  successColor?: EnrollColor;
  warningColor?: EnrollColor;
  appWhite?: EnrollColor;
  appBlack?: EnrollColor;
}

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/**
 * Configuration object passed to `startEnroll()`.
 */
export interface StartEnrollOptions {
  // ---- Required ----
  tenantId: string;
  tenantSecret: string;
  enrollMode: EnrollMode;

  // ---- Conditionally required ----
  applicantId?: string;
  levelOfTrust?: string;
  templateId?: string;

  // ---- Optional ----
  enrollEnvironment?: EnrollEnvironment;
  localizationCode?: EnrollLocalization;
  googleApiKey?: string;
  skipTutorial?: boolean;
  correlationId?: string;
  requestId?: string;
  contractParameters?: string;
  enrollColors?: EnrollColors;
  enrollForcedDocumentType?: EnrollForcedDocumentType;
  enrollExitStep?: EnrollStepType;
}

// ---------------------------------------------------------------------------
// Result types
// ---------------------------------------------------------------------------

/**
 * Returned when the enrollment flow completes successfully.
 */
export interface EnrollSuccessResult {
  applicantId: string;
  enrollMessage?: string;
  documentId?: string;
  requestId?: string;
  exitStepCompleted: boolean;
  completedStepName?: string;
}

/**
 * Shape of the error returned when the enrollment flow fails.
 */
export interface EnrollErrorResult {
  message: string;
  code?: string;
  applicantId?: string;
}

/**
 * Payload delivered by the `'onRequestId'` event listener.
 */
export interface EnrollRequestIdResult {
  requestId: string;
}
