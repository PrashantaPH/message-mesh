import {
  Box,
  Flex,
  Heading,
  Icon,
  SimpleGrid,
  Spacer,
  Spinner,
  Text,
} from '@chakra-ui/react';
import type { IconType } from 'react-icons';
import {
  FiActivity,
  FiChevronRight,
  FiMessageSquare,
  FiUsers,
} from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { AdminHeader } from './AdminHeader';
import { useAdminUsers } from '../../hooks/useAdminUsers';
import { useAdminConversations } from '../../hooks/useAdminConversations';
import { useAuditLog } from '../../hooks/useAuditLog';
import { useAdminStats } from '../../hooks/useAdminStats';

interface AdminCardProps {
  icon: IconType;
  title: string;
  description: string;
  count?: number;
  isLoading: boolean;
  to: string;
}

function AdminCard({ icon, title, description, count, isLoading, to }: AdminCardProps) {
  const navigate = useNavigate();
  return (
    <Box
      as="button"
      textAlign="left"
      onClick={() => navigate(to)}
      bg="panel-bg"
      border="1px solid"
      borderColor="panel-border"
      borderRadius="xl"
      p={5}
      transition="all 0.15s ease"
      _hover={{ borderColor: 'brand.400', transform: 'translateY(-2px)', shadow: 'sm' }}
    >
      <Flex align="center" mb={3} gap={3}>
        <Flex
          align="center"
          justify="center"
          boxSize={10}
          borderRadius="lg"
          bg="brand.50"
          color="brand.500"
          _dark={{ bg: 'whiteAlpha.100' }}
        >
          <Icon as={icon} boxSize={5} />
        </Flex>
        <Spacer />
        <Icon as={FiChevronRight} color="text-muted" />
      </Flex>
      <Flex align="baseline" gap={2}>
        <Heading size="md">{title}</Heading>
        {isLoading ? (
          <Spinner size="xs" color="brand.500" />
        ) : (
          count !== undefined && (
            <Text fontSize="sm" fontWeight={700} color="brand.500">
              {count}
            </Text>
          )
        )}
      </Flex>
      <Text mt={1} fontSize="sm" color="text-muted">
        {description}
      </Text>
    </Box>
  );
}

export function AdminDashboardPage() {
  // Fetch a single row from each list just to surface the total count.
  const users = useAdminUsers({ size: 1 });
  const conversations = useAdminConversations({ size: 1 });
  const audit = useAuditLog({ size: 1 });
  const stats = useAdminStats();

  return (
    <Flex direction="column" h="100vh" overflow="hidden">
      <AdminHeader />

      <Box flex={1} overflowY="auto" px={{ base: 4, md: 8 }} py={6}>
        <Box maxW="1100px" mx="auto">
          <Box mb={6}>
            <Heading size="lg">Admin dashboard</Heading>
            <Text color="text-muted" fontSize="sm">
              Manage users, conversations, and review activity.
            </Text>
          </Box>

          <SimpleGrid columns={{ base: 1, sm: 2, lg: 3 }} spacing={4}>
            <AdminCard
              icon={FiUsers}
              title="Manage users"
              description="View accounts, roles, and status."
              count={users.data?.totalElements}
              isLoading={users.isLoading}
              to="/admin/users"
            />
            <AdminCard
              icon={FiMessageSquare}
              title="Manage conversations"
              description="Oversee and moderate all chats."
              count={conversations.data?.totalElements}
              isLoading={conversations.isLoading}
              to="/admin/conversations"
            />
            <AdminCard
              icon={FiActivity}
              title="Audit log"
              description="Review recent administrative activity."
              count={audit.data?.totalElements}
              isLoading={audit.isLoading}
              to="/admin/audit"
            />
          </SimpleGrid>

          <Box mt={10} mb={4}>
            <Heading size="md">Platform overview</Heading>
            <Text color="text-muted" fontSize="sm">
              Live metrics across users, conversations and messages.
            </Text>
          </Box>

          {stats.isLoading ? (
            <Flex py={10} justify="center">
              <Spinner color="brand.500" />
            </Flex>
          ) : stats.isError || !stats.data ? (
            <Text color="text-muted" fontSize="sm">
              Couldn't load statistics.
            </Text>
          ) : (
            <SimpleGrid columns={{ base: 2, md: 3, lg: 4 }} spacing={4}>
              <StatTile label="Active users" value={stats.data.activeUsers} hint={`of ${stats.data.totalUsers} total`} />
              <StatTile label="Online now" value={stats.data.onlineUsers} accent />
              <StatTile label="Administrators" value={stats.data.adminUsers} />
              <StatTile label="Inactive users" value={stats.data.inactiveUsers} />
              <StatTile label="New users (7d)" value={stats.data.newUsersLast7Days} />
              <StatTile label="Group chats" value={stats.data.groupConversations} />
              <StatTile label="Direct chats" value={stats.data.directConversations} />
              <StatTile label="Deleted chats" value={stats.data.deletedConversations} />
              <StatTile label="Total messages" value={stats.data.totalMessages} />
              <StatTile label="Messages (7d)" value={stats.data.messagesLast7Days} accent />
              <StatTile label="Conversations" value={stats.data.totalConversations} />
              <StatTile label="Audit events" value={stats.data.auditEvents} />
            </SimpleGrid>
          )}
        </Box>
      </Box>
    </Flex>
  );
}

interface StatTileProps {
  label: string;
  value: number;
  hint?: string;
  accent?: boolean;
}

function StatTile({ label, value, hint, accent }: StatTileProps) {
  return (
    <Box
      bg="panel-bg"
      border="1px solid"
      borderColor="panel-border"
      borderRadius="xl"
      p={4}
    >
      <Text fontSize="xs" color="text-muted" textTransform="uppercase" letterSpacing="wide">
        {label}
      </Text>
      <Text fontSize="2xl" fontWeight={800} color={accent ? 'brand.500' : undefined} lineHeight="1.2">
        {value.toLocaleString()}
      </Text>
      {hint && (
        <Text fontSize="xs" color="text-muted">
          {hint}
        </Text>
      )}
    </Box>
  );
}
