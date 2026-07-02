import {
  Box,
  Button,
  Card,
  CardBody,
  Flex,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Heading,
  Input,
  Link,
  Stack,
  Text,
  useColorModeValue,
} from '@chakra-ui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link as RouterLink } from 'react-router-dom';
import { BrandLogo } from '../../components/BrandLogo';
import { ColorModeToggle } from '../../components/ColorModeToggle';
import { useAuth } from '../../hooks/useAuth';

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const bgGradient = useColorModeValue(
    'linear(to-br, brand.50, purple.50)',
    'linear(to-br, gray.900, brand.900)',
  );

  return (
    <Flex minH="100vh" align="center" justify="center" bgGradient={bgGradient} p={4} position="relative">
      <Box position="absolute" top={6} left={6}>
        <BrandLogo />
      </Box>
      <Box position="absolute" top={6} right={6}>
        <ColorModeToggle />
      </Box>
      <Stack spacing={6} w="full" maxW="md">
        <Card shadow="xl" borderRadius="2xl">
          <CardBody p={8}>
            <Stack spacing={5}>
              <Box textAlign="center">
                <Heading size="md">Welcome back</Heading>
                <Text color="text-muted" fontSize="sm" mt={1}>
                  Sign in to continue to your conversations
                </Text>
              </Box>

              {login.isError && (
                <Text color="red.400" fontSize="sm" textAlign="center">
                  Invalid username or password
                </Text>
              )}

              <form onSubmit={handleSubmit((values) => login.mutate(values))}>
                <Stack spacing={4}>
                  <FormControl isInvalid={Boolean(errors.username)}>
                    <FormLabel>Username</FormLabel>
                    <Input placeholder="alice" {...register('username')} autoFocus />
                    <FormErrorMessage>{errors.username?.message}</FormErrorMessage>
                  </FormControl>

                  <FormControl isInvalid={Boolean(errors.password)}>
                    <FormLabel>Password</FormLabel>
                    <Input type="password" placeholder="••••••••" {...register('password')} />
                    <FormErrorMessage>{errors.password?.message}</FormErrorMessage>
                  </FormControl>

                  <Button type="submit" size="lg" isLoading={login.isPending} mt={2}>
                    Sign in
                  </Button>
                </Stack>
              </form>

              <Text fontSize="sm" textAlign="center" color="text-muted">
                Don&apos;t have an account?{' '}
                <Link as={RouterLink} to="/register" color="brand.500" fontWeight={600}>
                  Create one
                </Link>
              </Text>
            </Stack>
          </CardBody>
        </Card>
      </Stack>
    </Flex>
  );
}
