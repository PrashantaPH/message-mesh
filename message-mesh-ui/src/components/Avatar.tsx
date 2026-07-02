import { Avatar as ChakraAvatar, Icon, type AvatarProps } from '@chakra-ui/react';
import { FiUsers } from 'react-icons/fi';

const COLORS = ['brand', 'purple', 'pink', 'teal', 'orange', 'cyan', 'green', 'red'];

function colorFor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i += 1) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return COLORS[Math.abs(hash) % COLORS.length];
}

interface Props extends AvatarProps {
  name: string;
  isGroup?: boolean;
}

export function Avatar({ name, isGroup = false, ...rest }: Props) {
  const scheme = colorFor(name);
  if (isGroup) {
    return (
      <ChakraAvatar
        bg={`${scheme}.500`}
        color="white"
        icon={<Icon as={FiUsers} />}
        {...rest}
      />
    );
  }
  return (
    <ChakraAvatar
      name={name}
      bg={`${scheme}.500`}
      color="white"
      getInitials={(n) => n.slice(0, 2).toUpperCase()}
      {...rest}
    />
  );
}
