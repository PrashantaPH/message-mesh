import { Routes, Route, Navigate } from 'react-router-dom';
import { FiMessageCircle } from 'react-icons/fi';
import { WelcomePage } from '../features/welcome/WelcomePage';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { ChatLayout } from '../features/chat/ChatLayout';
import { MessagePanel } from '../features/chat/MessagePanel';
import { AdminDashboardPage } from '../features/admin/AdminDashboardPage';
import { AdminUsersPage } from '../features/admin/AdminUsersPage';
import { AdminConversationsPage } from '../features/admin/AdminConversationsPage';
import { AuditLogPage } from '../features/admin/AuditLogPage';
import { EmptyState } from '../components/EmptyState';
import { SocketProvider } from '../ws/SocketProvider';
import { PublicOnly, RequireAdmin, RequireAuth } from './guards';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<WelcomePage />} />

      <Route
        path="/login"
        element={
          <PublicOnly>
            <LoginPage />
          </PublicOnly>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnly>
            <RegisterPage />
          </PublicOnly>
        }
      />

      <Route
        path="/chats"
        element={
          <RequireAuth>
            <SocketProvider>
              <ChatLayout />
            </SocketProvider>
          </RequireAuth>
        }
      >
        <Route
          index
          element={
            <EmptyState
              icon={FiMessageCircle}
              title="Select a conversation"
              description="Choose a chat from the list or start a new one."
            />
          }
        />
        <Route path=":conversationId" element={<MessagePanel />} />
      </Route>

      <Route
        path="/admin"
        element={
          <RequireAdmin>
            <AdminDashboardPage />
          </RequireAdmin>
        }
      />

      <Route
        path="/admin/users"
        element={
          <RequireAdmin>
            <AdminUsersPage />
          </RequireAdmin>
        }
      />

      <Route
        path="/admin/conversations"
        element={
          <RequireAdmin>
            <AdminConversationsPage />
          </RequireAdmin>
        }
      />

      <Route
        path="/admin/audit"
        element={
          <RequireAdmin>
            <AuditLogPage />
          </RequireAdmin>
        }
      />

      <Route path="*" element={<Navigate to="/chats" replace />} />
    </Routes>
  );
}
