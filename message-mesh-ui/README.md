# MessageMesh — Frontend (`message-mesh-ui`)

React 18 + TypeScript + Vite + Chakra UI client for the MessageMesh backend.

## Highlights
- Polished, accessible Chakra UI with light/dark mode.
- Real-time messaging over STOMP/SockJS (`@stomp/stompjs`).
- Optimistic send with delivery ticks, typing indicators, presence.
- Rich messaging: emoji reactions, edit, delete (tombstones), threaded replies, and in-conversation search.
- Direct & group conversations: new-conversation modal, group management (rename, add/remove members, roles, mute/archive, leave), welcome screen.
- Self-service profile modal: display name, password change, account deletion.
- **Admin console** (`/admin`): dashboard metrics, user management, conversation/message moderation, and an audit-log viewer with CSV export — gated by a `RequireAdmin` route guard.
- Desktop notifications + unread-count browser-tab badge.
- Server state via TanStack Query; client/real-time state via Zustand.

## Run
```powershell
npm install
npm run dev
```
Dev server: http://localhost:5173 (proxies `/api` and `/ws` to `http://localhost:8080`).

Start the backend (`message-mesh-svc`) first, then register two users in separate browser profiles to see live messaging. Sign in as the bootstrapped admin user (default `alice`) to access the `/admin` console.

## Scripts
- `npm run dev` — start Vite dev server
- `npm run build` — type-check + production build
- `npm run preview` — serve the production build locally
- `npm run lint` — ESLint
