export function reportUiError(message: string, error: unknown): void {
  if (import.meta.env.MODE !== 'test') {
    console.error(message, error);
  }
}
