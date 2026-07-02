import { Flex, IconButton, Spacer, useColorMode } from '@chakra-ui/react';
import { FiArrowLeft, FiMoon, FiSun } from 'react-icons/fi';
import { useLocation, useNavigate } from 'react-router-dom';
import { BrandLogo } from '../../components/BrandLogo';

/**
 * Shared header for all admin pages: route-based Back arrow, brand logo, and
 * color-mode toggle. Back navigation is predictable and app-route based:
 *   - /admin            -> /chats
 *   - /admin/<subpage>  -> /admin
 *   - anything else     -> /chats (safe fallback)
 */
export function AdminHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const { colorMode, toggleColorMode } = useColorMode();

  const handleBack = () => {
    if (location.pathname === '/admin') {
      navigate('/chats');
    } else if (location.pathname.startsWith('/admin/')) {
      navigate('/admin');
    } else {
      navigate('/chats');
    }
  };

  return (
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
      <IconButton
        aria-label="Back"
        icon={<FiArrowLeft />}
        variant="ghost"
        onClick={handleBack}
      />
      <BrandLogo to="/chats" size="sm" />
      <Spacer />
      <IconButton
        aria-label="Toggle color mode"
        icon={colorMode === 'light' ? <FiMoon /> : <FiSun />}
        variant="ghost"
        onClick={toggleColorMode}
      />
    </Flex>
  );
}
