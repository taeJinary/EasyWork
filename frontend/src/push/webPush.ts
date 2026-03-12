type WebPushConfig = {
  apiKey: string;
  authDomain?: string;
  projectId: string;
  messagingSenderId: string;
  appId: string;
  vapidKey: string;
};

const REQUIRED_WEB_PUSH_CONFIG_KEYS = [
  ['VITE_FIREBASE_API_KEY', () => readConfigValue(import.meta.env.VITE_FIREBASE_API_KEY)],
  ['VITE_FIREBASE_PROJECT_ID', () => readConfigValue(import.meta.env.VITE_FIREBASE_PROJECT_ID)],
  ['VITE_FIREBASE_MESSAGING_SENDER_ID', () => readConfigValue(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID)],
  ['VITE_FIREBASE_APP_ID', () => readConfigValue(import.meta.env.VITE_FIREBASE_APP_ID)],
  ['VITE_FIREBASE_VAPID_KEY', () => readConfigValue(import.meta.env.VITE_FIREBASE_VAPID_KEY)],
] as const;

export type WebPushIssueErrorCode =
  | 'UNSUPPORTED_NOTIFICATIONS'
  | 'UNSUPPORTED_SERVICE_WORKER'
  | 'MISSING_CONFIG'
  | 'PERMISSION_NOT_GRANTED'
  | 'UNSUPPORTED_MESSAGING'
  | 'TOKEN_UNAVAILABLE';

export class WebPushIssueError extends Error {
  readonly code: WebPushIssueErrorCode;

  constructor(code: WebPushIssueErrorCode, message: string) {
    super(message);
    this.code = code;
    this.name = 'WebPushIssueError';
  }
}

function readConfigValue(value: string | boolean | undefined): string {
  return typeof value === 'string' ? value.trim() : '';
}

export function getMissingWebPushConfigKeys(): string[] {
  return REQUIRED_WEB_PUSH_CONFIG_KEYS
    .filter(([, readValue]) => !readValue())
    .map(([envKey]) => envKey);
}

function readWebPushConfig(): WebPushConfig {
  const apiKey = readConfigValue(import.meta.env.VITE_FIREBASE_API_KEY);
  const authDomain = readConfigValue(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN);
  const projectId = readConfigValue(import.meta.env.VITE_FIREBASE_PROJECT_ID);
  const messagingSenderId = readConfigValue(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID);
  const appId = readConfigValue(import.meta.env.VITE_FIREBASE_APP_ID);
  const vapidKey = readConfigValue(import.meta.env.VITE_FIREBASE_VAPID_KEY);

  if (getMissingWebPushConfigKeys().length > 0) {
    throw new WebPushIssueError('MISSING_CONFIG', 'Web push config is missing.');
  }

  return {
    apiKey,
    authDomain: authDomain || undefined,
    projectId,
    messagingSenderId,
    appId,
    vapidKey,
  };
}

export function isWebPushConfigured(): boolean {
  try {
    readWebPushConfig();
    return true;
  } catch {
    return false;
  }
}

let registrationPromise: Promise<ServiceWorkerRegistration> | null = null;

async function getFirebaseMessaging(config: WebPushConfig) {
  const [{ getApps, initializeApp }, { getMessaging, getToken, isSupported }] = await Promise.all([
    import('firebase/app'),
    import('firebase/messaging'),
  ]);

  const supported = await isSupported();
  if (!supported) {
    throw new WebPushIssueError('UNSUPPORTED_MESSAGING', 'Firebase messaging is not supported.');
  }
  const app = getApps()[0] ?? initializeApp({
    apiKey: config.apiKey,
    authDomain: config.authDomain,
    projectId: config.projectId,
    messagingSenderId: config.messagingSenderId,
    appId: config.appId,
  });

  return {
    config,
    getMessaging,
    getToken,
    app,
  };
}

function buildServiceWorkerUrl(config: WebPushConfig): string {
  if (typeof window === 'undefined') {
    return '/firebase-messaging-sw.js';
  }

  const url = new URL('/firebase-messaging-sw.js', window.location.origin);
  url.searchParams.set('apiKey', config.apiKey);
  url.searchParams.set('authDomain', config.authDomain ?? '');
  url.searchParams.set('projectId', config.projectId);
  url.searchParams.set('messagingSenderId', config.messagingSenderId);
  url.searchParams.set('appId', config.appId);
  return url.toString();
}

async function getServiceWorkerRegistration(config: WebPushConfig): Promise<ServiceWorkerRegistration> {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    throw new WebPushIssueError('UNSUPPORTED_SERVICE_WORKER', 'Service worker is not supported.');
  }

  if (!registrationPromise) {
    registrationPromise = navigator.serviceWorker.register(buildServiceWorkerUrl(config));
  }

  return registrationPromise;
}

export async function issueWebPushToken(): Promise<string> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    throw new WebPushIssueError('UNSUPPORTED_NOTIFICATIONS', 'Notifications are not supported.');
  }

  const config = readWebPushConfig();

  let permission = window.Notification.permission;
  if (permission === 'default') {
    permission = await window.Notification.requestPermission();
  }

  if (permission !== 'granted') {
    throw new WebPushIssueError('PERMISSION_NOT_GRANTED', 'Notification permission was not granted.');
  }

  const [{ getMessaging, getToken, app }, registration] = await Promise.all([
    getFirebaseMessaging(config),
    getServiceWorkerRegistration(config),
  ]);

  const token = await getToken(getMessaging(app), {
    vapidKey: config.vapidKey,
    serviceWorkerRegistration: registration,
  });

  if (!token) {
    throw new WebPushIssueError('TOKEN_UNAVAILABLE', 'Web push token was not issued.');
  }

  return token;
}
