import { Box } from '@chakra-ui/react';

interface Props {
  online: boolean;
  size?: string;
}

export function PresenceBadge({ online, size = '10px' }: Props) {
  return (
    <Box
      w={size}
      h={size}
      borderRadius="full"
      bg={online ? 'green.400' : 'gray.400'}
      border="2px solid"
      borderColor="panel-bg"
      transition="background 0.2s"
    />
  );
}
