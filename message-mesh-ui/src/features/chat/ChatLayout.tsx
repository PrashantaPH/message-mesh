import {
  Badge,
  Box,
  Flex,
  HStack,
  IconButton,
  Menu,
  MenuButton,
  MenuDivider,
  MenuItem,
  MenuList,
  Spacer,
  Text,
  useColorMode,
  useDisclosure,
} from '@chakra-ui/react';
import {
  FiActivity,
  FiGrid,
  FiLogOut,
  FiMessageSquare,
  FiMoon,
  FiSettings,
  FiSun,
  FiUsers,
} from 'react-icons/fi';
import { Outlet, useNavigate, useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { Avatar } from '../../components/Avatar';
import { BrandLogo } from '../../components/BrandLogo';
import { ConnectionStatus } from '../../components/ConnectionStatus';
import { ConversationList } from './ConversationList';
import { ProfileModal } from './ProfileModal';
import { useAuth } from '../../hooks/useAuth';
import { useUnreadTitle } from '../../hooks/useUnreadTitle';
import { requestNotificationPermission } from '../../utils/notifications';

export function ChatLayout() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { colorMode, toggleColorMode } = useColorMode();
  const { user, isAdmin, logout } = useAuth();
  const profile = useDisclosure();
  const panelOpenOnMobile = Boolean(conversationId);

  // Reflect unread count in the tab title and ask to enable desktop alerts.
  useUnreadTitle();
  useEffect(() => {
    requestNotificationPermission();
  }, []);

  return (
    <Flex direction="column" h="100vh" overflow="hidden">
      {/* Top bar */}
      <Flex
        as="header"
        align="center"
        px={4}
        py={2.5}
        bg="panel-bg"
        borderBottom="1px solid"
        borderColor="panel-border"
        gap={2}
      >
        <BrandLogo to="/chats" size="sm" />
        <Spacer />
        <IconButton
          aria-label="Toggle color mode"
          icon={colorMode === 'light' ? <FiMoon /> : <FiSun />}
          variant="ghost"
          onClick={toggleColorMode}
        />
        <Menu>
          <MenuButton>
            <Avatar name={user?.displayName ?? 'User'} size="sm" cursor="pointer" />
          </MenuButton>
          <MenuList>
            <Box px={3} py={2}>
              <HStack spacing={2} align="center">
                <Text fontWeight={700} noOfLines={1}>
                  {user?.displayName}
                </Text>
                {isAdmin && (
                  <Badge
                    colorScheme="brand"
                    variant="subtle"
                    borderRadius="full"
                    px={2}
                    fontSize="0.65rem"
                    textTransform="uppercase"
                    letterSpacing="wide"
                  >
                    Admin
                  </Badge>
                )}
              </HStack>
              <Text fontSize="sm" color="text-muted">
                @{user?.username}
              </Text>
            </Box>
            <MenuDivider />
            <MenuItem icon={<FiSettings />} onClick={profile.onOpen}>
              Profile & settings
            </MenuItem>
            {isAdmin && (
              <>
                <MenuDivider />
                <MenuItem icon={<FiGrid />} onClick={() => navigate('/admin')}>
                  Admin dashboard
                </MenuItem>
                <MenuItem icon={<FiUsers />} onClick={() => navigate('/admin/users')}>
                  Manage users
                </MenuItem>
                <MenuItem icon={<FiMessageSquare />} onClick={() => navigate('/admin/conversations')}>
                  Manage conversations
                </MenuItem>
                <MenuItem icon={<FiActivity />} onClick={() => navigate('/admin/audit')}>
                  Audit log
                </MenuItem>
              </>
            )}
            <MenuDivider />
            <MenuItem icon={<FiLogOut />} onClick={logout}>
              Sign out
            </MenuItem>
          </MenuList>
        </Menu>
      </Flex>

      <ProfileModal isOpen={profile.isOpen} onClose={profile.onClose} />

      <ConnectionStatus />

      {/* Body */}
      <Flex flex={1} overflow="hidden">
        <Box
          as="aside"
          w={{ base: 'full', md: '300px', lg: '340px', xl: '380px' }}
          flexShrink={0}
          borderRight={{ md: '1px solid' }}
          borderColor={{ md: 'panel-border' }}
          bg="panel-bg"
          display={{ base: panelOpenOnMobile ? 'none' : 'block', md: 'block' }}
        >
          <ConversationList />
        </Box>

        <Box
          flex={1}
          minW={0}
          display={{ base: panelOpenOnMobile ? 'block' : 'none', md: 'block' }}
        >
          <Outlet />
        </Box>
      </Flex>
    </Flex>
  );
}
