// Browser (desktop) notification helpers for incoming chat messages.
// All functions are no-ops when the Notification API is unavailable or the
// user has not granted permission, so callers never need to guard.

let permissionRequested = false;

/** Ask for notification permission once per session (only when still 'default'). */
export function requestNotificationPermission(): void {
  if (typeof Notification === 'undefined') return;
  if (permissionRequested) return;
  if (Notification.permission === 'default') {
    permissionRequested = true;
    void Notification.requestPermission().catch(() => undefined);
  }
}

function canNotify(): boolean {
  return typeof Notification !== 'undefined' && Notification.permission === 'granted';
}

/**
 * Show a desktop notification for a new message. Only fires while the tab is
 * hidden/unfocused so it never interrupts an active conversation. Clicking the
 * notification focuses the app window.
 */
export function showMessageNotification(options: {
  title: string;
  body: string;
  tag?: string;
}): void {
  if (!canNotify() || !document.hidden) return;
  try {
    const notification = new Notification(options.title, {
      body: options.body,
      tag: options.tag,
      icon: '/favicon.svg',
    });
    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  } catch {
    // Some environments require a ServiceWorkerRegistration to construct
    // Notification; ignore those failures rather than breaking message flow.
  }
}
