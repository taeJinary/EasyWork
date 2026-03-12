import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mockRegister = vi.fn();
const mockGetToken = vi.fn();
const mockGetMessaging = vi.fn();
const mockIsSupported = vi.fn();
const mockGetApps = vi.fn();
const mockInitializeApp = vi.fn();

vi.mock('firebase/app', () => ({
  getApps: () => mockGetApps(),
  initializeApp: (...args: unknown[]) => mockInitializeApp(...args),
}));

vi.mock('firebase/messaging', () => ({
  getMessaging: (...args: unknown[]) => mockGetMessaging(...args),
  getToken: (...args: unknown[]) => mockGetToken(...args),
  isSupported: () => mockIsSupported(),
}));

async function importWebPushModule() {
  vi.resetModules();
  return import('@/push/webPush');
}

describe('webPush', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.stubEnv('VITE_FIREBASE_API_KEY', 'api-key');
    vi.stubEnv('VITE_FIREBASE_AUTH_DOMAIN', 'example.firebaseapp.com');
    vi.stubEnv('VITE_FIREBASE_PROJECT_ID', 'project-id');
    vi.stubEnv('VITE_FIREBASE_MESSAGING_SENDER_ID', 'sender-id');
    vi.stubEnv('VITE_FIREBASE_APP_ID', 'app-id');
    vi.stubEnv('VITE_FIREBASE_VAPID_KEY', 'vapid-key');

    Object.defineProperty(window, 'Notification', {
      configurable: true,
      writable: true,
      value: {
        permission: 'granted',
        requestPermission: vi.fn().mockResolvedValue('granted'),
      },
    });

    Object.defineProperty(window.navigator, 'serviceWorker', {
      configurable: true,
      writable: true,
      value: {
        register: (...args: unknown[]) => mockRegister(...args),
      },
    });

    mockGetApps.mockReturnValue([]);
    mockInitializeApp.mockReturnValue({ name: 'app' });
    mockGetMessaging.mockReturnValue({ name: 'messaging' });
    mockGetToken.mockResolvedValue('issued-token');
    mockIsSupported.mockResolvedValue(true);
    mockRegister.mockResolvedValue({ scope: '/' } satisfies Partial<ServiceWorkerRegistration>);
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('registers the public firebase messaging service worker script', async () => {
    const { issueWebPushToken } = await importWebPushModule();

    await issueWebPushToken();

    expect(mockRegister).toHaveBeenCalledTimes(1);
    const [scriptUrl] = mockRegister.mock.calls[0] as [string];
    const parsed = new URL(scriptUrl, 'https://example.com');

    expect(parsed.pathname).toBe('/firebase-messaging-sw.js');
  });

  it('returns missing firebase config keys for operational diagnostics', async () => {
    vi.stubEnv('VITE_FIREBASE_API_KEY', '');
    vi.stubEnv('VITE_FIREBASE_VAPID_KEY', '');

    const { getMissingWebPushConfigKeys } = await importWebPushModule();

    expect(getMissingWebPushConfigKeys()).toEqual([
      'VITE_FIREBASE_API_KEY',
      'VITE_FIREBASE_VAPID_KEY',
    ]);
  });
});
