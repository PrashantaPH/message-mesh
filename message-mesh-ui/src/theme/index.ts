import { extendTheme, type ThemeConfig } from '@chakra-ui/react';
import { mode, type StyleFunctionProps } from '@chakra-ui/theme-tools';

const config: ThemeConfig = {
  initialColorMode: 'system',
  useSystemColorMode: true,
};

export const theme = extendTheme({
  config,
  fonts: {
    heading: `'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`,
    body: `'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`,
  },
  colors: {
    brand: {
      50: '#e6f2ff',
      100: '#cce4ff',
      200: '#99c9ff',
      300: '#66adff',
      400: '#3392ff',
      500: '#1a73e8',
      600: '#1666c1',
      700: '#0f4c8a',
      800: '#0a3866',
      900: '#052444',
    },
    accent: {
      500: '#7c5cff',
    },
  },
  semanticTokens: {
    colors: {
      'app-bg': { default: 'gray.50', _dark: 'gray.900' },
      'panel-bg': { default: 'white', _dark: 'gray.800' },
      'panel-border': { default: 'gray.200', _dark: 'whiteAlpha.200' },
      'bubble-own': { default: 'brand.500', _dark: 'brand.500' },
      'bubble-other': { default: 'gray.100', _dark: 'whiteAlpha.200' },
      'text-muted': { default: 'gray.500', _dark: 'gray.400' },
    },
  },
  styles: {
    global: (props: StyleFunctionProps) => ({
      'html, body, #root': {
        height: '100%',
      },
      body: {
        bg: mode('gray.50', 'gray.900')(props),
      },
      // Thin, subtle overlay-style scrollbars (Firefox)
      '*': {
        scrollbarWidth: 'thin',
        scrollbarColor: `${mode('rgba(0,0,0,0.18)', 'rgba(255,255,255,0.20)')(
          props,
        )} transparent`,
      },
      // WebKit / Chromium scrollbars
      '*::-webkit-scrollbar': { width: '8px', height: '8px' },
      '*::-webkit-scrollbar-track': { background: 'transparent' },
      '*::-webkit-scrollbar-thumb': {
        background: mode('blackAlpha.300', 'whiteAlpha.300')(props),
        borderRadius: '9999px',
        border: '2px solid transparent',
        backgroundClip: 'content-box',
        transition: 'background-color 0.2s ease',
      },
      '*::-webkit-scrollbar-thumb:hover': {
        background: mode('blackAlpha.500', 'whiteAlpha.500')(props),
        backgroundClip: 'content-box',
      },
    }),
  },
  components: {
    Button: {
      defaultProps: { colorScheme: 'brand' },
      baseStyle: { borderRadius: 'lg', fontWeight: 600 },
    },
    Input: {
      defaultProps: { focusBorderColor: 'brand.500' },
    },
    Textarea: {
      defaultProps: { focusBorderColor: 'brand.500' },
    },
  },
});
