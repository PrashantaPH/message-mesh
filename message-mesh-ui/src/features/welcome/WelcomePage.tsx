import {
  Box,
  Button,
  Divider,
  Flex,
  Heading,
  HStack,
  Icon,
  IconButton,
  Link,
  SimpleGrid,
  Stack,
  Text,
  useColorModeValue,
} from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import {
  FiArrowRight,
  FiCheck,
  FiGithub,
  FiLinkedin,
  FiLock,
  FiTwitter,
  FiZap,
} from 'react-icons/fi';
import { BrandLogo } from '../../components/BrandLogo';
import { ColorModeToggle } from '../../components/ColorModeToggle';

const NAV_LINKS = ['Product', 'Security', 'Pricing', 'Resources'];

const CARD_POINTS = [
  'Real-time delivery with read receipts',
  'Secure JWT-authenticated sessions',
  'Scales from one chat to thousands',
];

const FOOTER_COLUMNS = [
  { title: 'Product', links: ['Features', 'Security', 'Pricing', 'Roadmap'] },
  { title: 'Company', links: ['About', 'Blog', 'Careers', 'Contact'] },
  { title: 'Legal', links: ['Terms', 'Privacy', 'Cookies', 'Status'] },
];

export function WelcomePage() {
  const navigate = useNavigate();
  const goToLogin = () => navigate('/login');

  // Adaptive theme — mirrors the chat surfaces (app-bg / panel-bg) in both modes.
  const heroGradient = useColorModeValue(
    'linear(to-br, brand.50, purple.50)',
    'linear(to-br, gray.900, brand.900)',
  );
  const eyebrow = useColorModeValue('brand.600', 'brand.200');
  const checkBg = useColorModeValue('brand.50', 'whiteAlpha.200');

  return (
    <Flex direction="column" minH="100vh" bg="app-bg">
      {/* ===== Top navigation (logo top-left on every page) ===== */}
      <Flex
        as="header"
        align="center"
        justify="space-between"
        px={{ base: 5, md: 10 }}
        py={3}
        position="sticky"
        top={0}
        zIndex={10}
        bg="panel-bg"
        borderBottomWidth="1px"
        borderColor="panel-border"
      >
        <BrandLogo />

        <HStack spacing={8} display={{ base: 'none', lg: 'flex' }}>
          {NAV_LINKS.map((label) => (
            <Link
              key={label}
              fontSize="sm"
              fontWeight={600}
              color="text-muted"
              _hover={{ color: 'brand.500', textDecoration: 'none' }}
              onClick={goToLogin}
            >
              {label}
            </Link>
          ))}
        </HStack>

        <HStack spacing={2}>
          <ColorModeToggle />
          <Button
            variant="ghost"
            colorScheme="brand"
            display={{ base: 'none', sm: 'inline-flex' }}
            onClick={goToLogin}
          >
            Sign in
          </Button>
          <Button colorScheme="brand" rightIcon={<FiArrowRight />} onClick={goToLogin}>
            Get Started
          </Button>
        </HStack>
      </Flex>

      {/* ===== Hero ===== */}
      <Box flex="1" bgGradient={heroGradient} px={{ base: 5, md: 10 }} py={{ base: 12, md: 20 }}>
        <Flex
          maxW="6xl"
          mx="auto"
          direction={{ base: 'column', lg: 'row' }}
          align={{ lg: 'center' }}
          justify="space-between"
          gap={{ base: 12, lg: 16 }}
        >
          {/* Left: copy */}
          <Stack spacing={7} flex="1" maxW={{ lg: '2xl' }}>
            <Text fontSize="sm" fontWeight={800} letterSpacing="0.18em" color={eyebrow}>
              GET STARTED
            </Text>

            <Heading size="3xl" lineHeight="1.05" letterSpacing="-0.03em" fontWeight={800}>
              See how MessageMesh works.
            </Heading>

            <Text fontSize={{ base: 'md', md: 'xl' }} color="text-muted" maxW="2xl">
              Connect instantly. Chat securely. Scale effortlessly. A real-time messaging
              platform built for fast, reliable and scalable conversations.
            </Text>

            <HStack spacing={6} flexWrap="wrap" pt={1} color="text-muted">
              <HStack spacing={2}>
                <Icon as={FiZap} color="brand.500" />
                <Text fontWeight={600} fontSize="sm">
                  Instant delivery
                </Text>
              </HStack>
              <HStack spacing={2}>
                <Icon as={FiLock} color="brand.500" />
                <Text fontWeight={600} fontSize="sm">
                  Secure by design
                </Text>
              </HStack>
            </HStack>

            <Stack direction={{ base: 'column', sm: 'row' }} spacing={4} pt={2}>
              <Button
                size="lg"
                colorScheme="brand"
                rightIcon={<FiArrowRight />}
                _hover={{ transform: 'translateY(-1px)' }}
                shadow="lg"
                onClick={goToLogin}
              >
                Get Started
              </Button>
              <Button size="lg" variant="outline" colorScheme="brand" onClick={goToLogin}>
                Sign in
              </Button>
            </Stack>
          </Stack>

          {/* Right: get-started card */}
          <Box w="full" maxW={{ base: 'full', lg: 'sm' }}>
            <Box
              bg="panel-bg"
              borderWidth="1px"
              borderColor="panel-border"
              borderRadius="2xl"
              shadow="2xl"
              p={{ base: 6, md: 8 }}
            >
              <Stack spacing={5}>
                <Box>
                  <Heading size="md">Start chatting in seconds</Heading>
                  <Text fontSize="sm" mt={1} color="text-muted">
                    No setup. Create an account and your conversations are ready.
                  </Text>
                </Box>

                <Stack spacing={3}>
                  {CARD_POINTS.map((point) => (
                    <HStack key={point} align="flex-start" spacing={3}>
                      <Flex
                        boxSize={5}
                        mt={0.5}
                        align="center"
                        justify="center"
                        borderRadius="full"
                        bg={checkBg}
                        color="brand.500"
                        flexShrink={0}
                      >
                        <Icon as={FiCheck} boxSize={3} />
                      </Flex>
                      <Text fontSize="sm" color="text-muted">
                        {point}
                      </Text>
                    </HStack>
                  ))}
                </Stack>

                <Button size="lg" colorScheme="brand" rightIcon={<FiArrowRight />} onClick={goToLogin}>
                  Get Started
                </Button>

                <Text fontSize="xs" textAlign="center" color="text-muted">
                  Already have an account?{' '}
                  <Link color="brand.500" fontWeight={600} onClick={goToLogin}>
                    Sign in
                  </Link>
                </Text>
              </Stack>
            </Box>
          </Box>
        </Flex>
      </Box>

      {/* ===== Footer ===== */}
      <Box as="footer" bg="panel-bg" borderTopWidth="1px" borderColor="panel-border">
        <Box maxW="6xl" mx="auto" px={{ base: 5, md: 10 }} py={{ base: 10, md: 14 }}>
          <SimpleGrid columns={{ base: 2, md: 4 }} spacing={8}>
            <Stack spacing={4} gridColumn={{ base: 'span 2', md: 'auto' }}>
              <BrandLogo />
              <Text fontSize="sm" color="text-muted" maxW="xs">
                Fast, reliable and scalable real-time messaging for modern teams.
              </Text>
            </Stack>

            {FOOTER_COLUMNS.map((column) => (
              <Stack key={column.title} spacing={3}>
                <Text fontWeight={700} fontSize="sm">
                  {column.title}
                </Text>
                {column.links.map((link) => (
                  <Link
                    key={link}
                    fontSize="sm"
                    color="text-muted"
                    _hover={{ color: 'brand.500', textDecoration: 'none' }}
                    onClick={goToLogin}
                  >
                    {link}
                  </Link>
                ))}
              </Stack>
            ))}
          </SimpleGrid>

          <Divider my={8} borderColor="panel-border" />

          <Flex direction={{ base: 'column', sm: 'row' }} align="center" justify="space-between" gap={4}>
            <Text fontSize="sm" color="text-muted">
              © {new Date().getFullYear()} MessageMesh. All rights reserved.
            </Text>
            <HStack spacing={1}>
              {[FiTwitter, FiLinkedin, FiGithub].map((SocialIcon, i) => (
                <IconButton
                  key={i}
                  aria-label="social link"
                  icon={<SocialIcon />}
                  size="sm"
                  variant="ghost"
                  color="text-muted"
                  _hover={{ color: 'brand.500' }}
                  onClick={goToLogin}
                />
              ))}
            </HStack>
          </Flex>
        </Box>
      </Box>
    </Flex>
  );
}
