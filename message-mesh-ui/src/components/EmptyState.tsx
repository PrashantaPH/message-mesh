import { Center, Icon, Text, VStack } from '@chakra-ui/react';
import type { IconType } from 'react-icons';

interface Props {
  icon: IconType;
  title: string;
  description?: string;
}

export function EmptyState({ icon, title, description }: Props) {
  return (
    <Center h="100%" w="100%" p={8}>
      <VStack spacing={3} color="text-muted" textAlign="center">
        <Icon as={icon} boxSize={12} opacity={0.5} />
        <Text fontSize="lg" fontWeight={600}>
          {title}
        </Text>
        {description && (
          <Text fontSize="sm" maxW="sm">
            {description}
          </Text>
        )}
      </VStack>
    </Center>
  );
}
