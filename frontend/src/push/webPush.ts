type WebPushConfig = {
  apiKey: string;
  authDomain?: string;
  projectId: string;
  messagingSenderId: string;
  appId: string;
  vapidKey: string;
};

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

function readWebPushConfig(): WebPushConfig {
  const apiKey = readConfigValue(import.meta.env.VITE_FIREBASE_API_KEY);
  const authDomain = readConfigValue(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN);
  const projectId = readConfigValue(import.meta.env.VITE_FIREBASE_PROJECT_ID);
  const messagingSenderId = readConfigValue(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID);
  const appId = readConfigValue(import.meta.env.VITE_FIREBASE_APP_ID);
  const vapidKey = readConfigValue(import.meta.env.VITE_FIREBASE_VAPID_KEY);

  if (!apiKey || !projectId || !messagingSenderId || !appId || !vapidKey) {
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

async function getFirebaseMessaging() {
  const [{ getApps, initializeApp }, { getMessaging, getToken, isSupported }] = await Promise.all([
    import('firebase/app'),
    import('firebase/messaging'),
  ]);

  const supported = await isSupported();
  if (!supported) {
    throw new WebPushIssueError('UNSUPPORTED_MESSAGING', 'Firebase messaging is not supported.');
  }

  const config = readWebPushConfig();
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

async function getServiceWorkerRegistration(): Promise<ServiceWorkerRegistration> {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    throw new WebPushIssueError('UNSUPPORTED_SERVICE_WORKER', 'Service worker is not supported.');
  }

  if (!registrationPromise) {
    registrationPromise = navigator.serviceWorker.register(
      new URL('./firebase-messaging-sw.ts', import.meta.url),
      { type: 'module' }
    );
  }

  return registrationPromise;
}

export async function issueWebPushToken(): Promise<string> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    throw new WebPushIssueError('UNSUPPORTED_NOTIFICATIONS', 'Notifications are not supported.');
  }

  if (!isWebPushConfigured()) {
    throw new WebPushIssueError('MISSING_CONFIG', 'Web push config is missing.');
  }

  let permission = window.Notification.permission;
  if (permission === 'default') {
    permission = await window.Notification.requestPermission();
  }

  if (permission !== 'granted') {
    throw new WebPushIssueError('PERMISSION_NOT_GRANTED', 'Notification permission was not granted.');
  }

  const [{ config, getMessaging, getToken, app }, registration] = await Promise.all([
    getFirebaseMessaging(),
    getServiceWorkerRegistration(),
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
