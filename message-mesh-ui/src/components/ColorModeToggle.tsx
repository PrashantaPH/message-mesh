import { IconButton, useColorMode, type IconButtonProps } from '@chakra-ui/react';
import { FiMoon, FiSun } from 'react-icons/fi';

type Props = Omit<IconButtonProps, 'aria-label' | 'icon' | 'onClick'>;

/** Light/dark mode switch — mirrors the toggle used in the chat top bar. */
export function ColorModeToggle(props: Props) {
  const { colorMode, toggleColorMode } = useColorMode();
  return (
    <IconButton
      aria-label="Toggle color mode"
      icon={colorMode === 'light' ? <FiMoon /> : <FiSun />}
      variant="ghost"
      onClick={toggleColorMode}
      {...props}
    />
  );
}
