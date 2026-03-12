/// <reference lib="WebWorker" />

import { initializeApp, getApps } from 'firebase/app';
import { getMessaging, onBackgroundMessage, isSupported } from 'firebase/messaging/sw';

declare const self: ServiceWorkerGlobalScope;

function readConfigValue(value: string | boolean | undefined): string {
  return typeof value === 'string' ? value.trim() : '';
}

const firebaseConfig = {
  apiKey: readConfigValue(import.meta.env.VITE_FIREBASE_API_KEY),
  authDomain: readConfigValue(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN) || undefined,
  projectId: readConfigValue(import.meta.env.VITE_FIREBASE_PROJECT_ID),
  messagingSenderId: readConfigValue(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID),
  appId: readConfigValue(import.meta.env.VITE_FIREBASE_APP_ID),
};

function hasWebPushConfig() {
  return Boolean(
    firebaseConfig.apiKey &&
      firebaseConfig.projectId &&
      firebaseConfig.messagingSenderId &&
      firebaseConfig.appId
  );
}

self.addEventListener('install', () => {
  void self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

async function initMessaging() {
  if (!hasWebPushConfig()) {
    return;
  }

  const supported = await isSupported();
  if (!supported) {
    return;
  }

  const app = getApps()[0] ?? initializeApp(firebaseConfig);
  const messaging = getMessaging(app);

  onBackgroundMessage(messaging, (payload) => {
    const title = payload.notification?.title ?? 'TaskFlow';
    const body = payload.notification?.body;
    const clickAction = payload.fcmOptions?.link ?? '/notifications';

    void self.registration.showNotification(title, {
      body,
      data: { clickAction },
      icon: '/vite.svg',
      badge: '/vite.svg',
    });
  });
}

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const clickAction = typeof event.notification.data?.clickAction === 'string'
    ? event.notification.data.clickAction
    : '/notifications';

  event.waitUntil(self.clients.openWindow(clickAction));
});

void initMessaging();
