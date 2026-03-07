import { vi } from 'vitest';

/**
 * Creates a mock for @/api/client (apiClient) with configurable responses.
 *
 * Usage:
 *   const mock = createApiMock();
 *   mock.get.mockResolvedValue({ data: { data: yourData } });
 */
export function createApiMock() {
  return {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    put: vi.fn(),
  };
}

/** Wraps data in the standard ApiResponse envelope */
export function apiOk<T>(data: T, message = 'success') {
  return { data: { success: true, data, message } };
}
