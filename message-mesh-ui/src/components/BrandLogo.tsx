import { Flex, Heading, HStack, Icon } from '@chakra-ui/react';
import { FiMessageCircle } from 'react-icons/fi';
import { Link as RouterLink } from 'react-router-dom';

interface BrandLogoProps {
  /** Where the logo links to. */
  to?: string;
  /** Wordmark text color (defaults to the current text color). */
  color?: string;
  /** Background of the rounded icon tile. */
  tileBg?: string;
  /** Icon color inside the tile. */
  iconColor?: string;
  size?: 'sm' | 'md' | 'lg';
}

const SIZES = {
  sm: { tile: 8, icon: 4, heading: 'sm' as const },
  md: { tile: 9, icon: 5, heading: 'md' as const },
  lg: { tile: 11, icon: 6, heading: 'lg' as const },
};

/**
 * MessageMesh brand mark — a rounded icon tile + wordmark. Rendered in the
 * top-left of every page so the logo always sits in the same place.
 */
export function BrandLogo({
  to = '/',
  color,
  tileBg = 'brand.500',
  iconColor = 'white',
  size = 'md',
}: BrandLogoProps) {
  const s = SIZES[size];
  return (
    <HStack
      as={RouterLink}
      to={to}
      spacing={2.5}
      _hover={{ textDecoration: 'none', opacity: 0.9 }}
      aria-label="MessageMesh home"
    >
      <Flex boxSize={s.tile} align="center" justify="center" borderRadius="xl" bg={tileBg}>
        <Icon as={FiMessageCircle} boxSize={s.icon} color={iconColor} />
      </Flex>
      <Heading size={s.heading} color={color} letterSpacing="-0.02em">
        MessageMesh
      </Heading>
    </HStack>
  );
}
