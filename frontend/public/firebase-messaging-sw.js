self.addEventListener('install', function () {
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(self.clients.claim());
});

function readConfig() {
  var url = new URL(self.location.href);
  var apiKey = url.searchParams.get('apiKey') || '';
  var authDomain = url.searchParams.get('authDomain') || '';
  var projectId = url.searchParams.get('projectId') || '';
  var messagingSenderId = url.searchParams.get('messagingSenderId') || '';
  var appId = url.searchParams.get('appId') || '';

  if (!apiKey || !projectId || !messagingSenderId || !appId) {
    return null;
  }

  return {
    apiKey: apiKey,
    authDomain: authDomain || undefined,
    projectId: projectId,
    messagingSenderId: messagingSenderId,
    appId: appId,
  };
}

function initFirebaseMessaging() {
  var config = readConfig();
  if (!config) {
    return;
  }

  importScripts(
    'https://www.gstatic.com/firebasejs/12.10.0/firebase-app-compat.js',
    'https://www.gstatic.com/firebasejs/12.10.0/firebase-messaging-compat.js'
  );

  if (!self.firebase) {
    return;
  }

  if (!self.firebase.apps.length) {
    self.firebase.initializeApp(config);
  }

  var messaging = self.firebase.messaging();

  messaging.onBackgroundMessage(function (payload) {
    var title = (payload.notification && payload.notification.title) || 'TaskFlow';
    var body = payload.notification && payload.notification.body;
    var clickAction = (payload.fcmOptions && payload.fcmOptions.link) || '/notifications';

    self.registration.showNotification(title, {
      body: body,
      data: { clickAction: clickAction },
      icon: '/vite.svg',
      badge: '/vite.svg',
    });
  });
}

self.addEventListener('notificationclick', function (event) {
  event.notification.close();
  var clickAction =
    event.notification.data && typeof event.notification.data.clickAction === 'string'
      ? event.notification.data.clickAction
      : '/notifications';

  event.waitUntil(self.clients.openWindow(clickAction));
});

initFirebaseMessaging();
